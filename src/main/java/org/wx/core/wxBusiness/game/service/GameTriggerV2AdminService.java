package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.*;
import org.wx.core.wxBusiness.game.mapper.GameFinishedSkillEffectMapper;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameTriggerV2AdminService {

    @Resource
    private GameFinishedSkillService finishedSkillService;
    @Resource
    private GameFinishedSkillEffectMapper finishedSkillEffectMapper;
    @Resource
    private GameCompleteSkillService completeSkillService;
    @Resource
    private GameFinishedSkillEffectService finishedSkillEffectService;
    @Resource
    private GameTriggerSlotService triggerSlotService;
    @Resource
    private GameItemService gameItemService;
    @Resource
    private GameMonsterService monsterService;
    @Resource
    private GameReferenceCleanupService referenceCleanupService;
    @Resource
    private SkillJsonHelper skillJsonHelper;

    public static final String UNIVERSAL_BASIC_ATTACK_ID = "fin_normal_attack";

    public List<AdminTriggerSlotVo> listTriggerSlotsByMonster(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return List.of();
        }
        return triggerSlotService.listByMonsterId(monsterId).stream()
                .map(this::buildTriggerSlotVo)
                .collect(Collectors.toList());
    }

    public IPage<GameFinishedSkill> listFinishedSkills(GameFinishedSkill query) {
        return finishedSkillService.pageQuery(query);
    }

    public AdminFinishedSkillVo getFinishedSkillDetail(String id) {
        GameFinishedSkill skill = finishedSkillService.getById(id);
        ErrorFactory.notNull(skill, "成品技能不存在");
        return buildFinishedSkillVo(skill);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminFinishedSkillVo saveFinishedSkill(AdminFinishedSkillVo vo) {
        ErrorFactory.throwError(UNIVERSAL_BASIC_ATTACK_ID.equals(vo.getId()), "通用普攻为只读，不可修改");
        ErrorFactory.notNull(vo.getName(), "名称不能为空");
        ErrorFactory.notNull(vo.getTargetType(), "目标槽不能为空");
        SkillTargetType targetType = SkillTargetType.parse(vo.getTargetType());
        ErrorFactory.notNull(targetType, "目标槽类型无效");
        ErrorFactory.throwError(targetType.isLegacy(), "请使用新目标槽类型");

        String code = vo.getCode();
        if (code == null || code.isBlank()) {
            code = generateSkillCodeFromName(vo.getName());
        }

        int hitFrequency = vo.getHitFrequency() != null ? vo.getHitFrequency() : 1;
        ErrorFactory.throwError(hitFrequency < 1, "频率槽最小为1");

        Integer maxCast = Boolean.TRUE.equals(vo.getMaxCastUnlimited()) ? null : vo.getMaxCastCount();
        if (maxCast != null && maxCast <= 0) {
            maxCast = null;
        }

        GameFinishedSkill skill = new GameFinishedSkill();
        skill.setId(vo.getId());
        skill.setCode(code.trim().toUpperCase());
        skill.setName(vo.getName().trim());
        skill.setTargetType(targetType.name());
        skill.setTargetParam(vo.getTargetParam());
        skill.setHitFrequency(hitFrequency);
        skill.setMaxCastCount(maxCast);
        skill.setFormulasJson(skillJsonHelper.writeFormulas(vo.getFormulas()));
        skill.setCatL1(normalizeCatL1(vo.getCatL1()));
        skill.setCatL2(normalizeCatL2(vo.getCatL2()));
        skill.setCatL3(normalizeCatL3(vo.getCatL3()));
        skill.setCatL4(normalizeCatL4(vo.getCatL4()));
        skill.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        skill.setRemark(vo.getRemark());

        boolean createNew = skill.getId() == null || skill.getId().isBlank() || !isAsciiSkillId(skill.getId())
                || finishedSkillService.getById(skill.getId()) == null;
        if (createNew) {
            if (skill.getId() == null || skill.getId().isBlank() || !isAsciiSkillId(skill.getId())) {
                skill.setId(generateUniqueFinishedSkillId());
            }
            finishedSkillService.save(skill);
        } else {
            finishedSkillService.updateById(skill);
        }

        // 旧效果表清空，统一走公式组
        finishedSkillEffectMapper.delete(new LambdaQueryWrapper<GameFinishedSkillEffect>()
                .eq(GameFinishedSkillEffect::getFinishedSkillId, skill.getId()));
        return getFinishedSkillDetail(skill.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFinishedSkill(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        ErrorFactory.throwError(UNIVERSAL_BASIC_ATTACK_ID.equals(id), "通用普攻不可删除");
        referenceCleanupService.removeFinishedSkillBindings(id);
        finishedSkillEffectMapper.delete(new LambdaQueryWrapper<GameFinishedSkillEffect>()
                .eq(GameFinishedSkillEffect::getFinishedSkillId, id));
        finishedSkillService.removeById(id);
    }

    public IPage<AdminCompleteSkillVo> listCompleteSkills(GameCompleteSkill query) {
        IPage<GameCompleteSkill> page = completeSkillService.pageQuery(query);
        return page.convert(this::buildCompleteSkillVo);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminCompleteSkillVo saveCompleteSkill(AdminCompleteSkillVo vo) {
        ErrorFactory.notNull(vo.getCode(), "编码不能为空");
        ErrorFactory.notNull(vo.getName(), "名称不能为空");
        ErrorFactory.notNull(vo.getTriggerSlotType(), "扳机槽不能为空");
        ErrorFactory.notNull(vo.getFinishedSkillId(), "成品技能不能为空");

        TriggerSlotType slotType = TriggerSlotType.parse(vo.getTriggerSlotType());
        ErrorFactory.notNull(slotType, "扳机槽类型无效");
        validateTriggerParam(slotType, vo.getTriggerParam(), vo.getTriggerRefId());

        GameFinishedSkill finished = finishedSkillService.getById(vo.getFinishedSkillId());
        ErrorFactory.notNull(finished, "成品技能不存在");

        GameCompleteSkill entity = new GameCompleteSkill();
        entity.setId(vo.getId());
        entity.setCode(vo.getCode().trim().toUpperCase());
        entity.setName(vo.getName().trim());
        entity.setTriggerSlotType(slotType.name());
        entity.setTriggerParam(vo.getTriggerParam());
        entity.setTriggerRefId(vo.getTriggerRefId());
        entity.setFinishedSkillId(vo.getFinishedSkillId());
        CompleteSkillBindType bindType = CompleteSkillBindType.parse(
                vo.getBindType() != null ? vo.getBindType() : CompleteSkillBindType.DEFAULT.name());
        entity.setBindType(bindType.name());
        entity.setBindRefId(vo.getBindRefId());
        entity.setSort(vo.getSort() != null ? vo.getSort() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        entity.setRemark(vo.getRemark());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId("cmp_" + entity.getCode().toLowerCase());
            completeSkillService.save(entity);
        } else {
            completeSkillService.updateById(entity);
        }
        return buildCompleteSkillVo(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeCompleteSkill(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        completeSkillService.removeById(id);
    }

    public IPage<AdminTriggerSlotVo> listTriggerSlots(GameTriggerSlot query) {
        IPage<GameTriggerSlot> page = triggerSlotService.pageQuery(query);
        return page.convert(this::buildTriggerSlotVo);
    }

    public List<AdminTriggerSlotVo> listTriggerSlotsByItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        return triggerSlotService.listByItemId(itemId).stream()
                .map(this::buildTriggerSlotVo)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminTriggerSlotVo saveTriggerSlot(AdminTriggerSlotVo vo) {
        boolean bindItem = vo.getItemId() != null && !vo.getItemId().isBlank();
        boolean bindMonster = vo.getMonsterId() != null && !vo.getMonsterId().isBlank();
        ErrorFactory.throwError(!bindItem && !bindMonster, "物品ID或怪物ID不能为空");
        ErrorFactory.throwError(bindItem && bindMonster, "不能同时绑定物品和怪物");
        ErrorFactory.notNull(vo.getFinishedSkillId(), "扳机技能不能为空");

        if (bindItem) {
            GameItem item = gameItemService.getById(vo.getItemId());
            ErrorFactory.notNull(item, "物品不存在");
        } else {
            GameMonster monster = monsterService.getById(vo.getMonsterId());
            ErrorFactory.notNull(monster, "怪物不存在");
        }

        TriggerMode mode = TriggerMode.parse(vo.getTriggerMode());
        TriggerQuickPreset quickPreset = TriggerQuickPreset.parse(vo.getQuickPreset());
        if (mode == TriggerMode.QUICK) {
            ErrorFactory.notNull(quickPreset, "请选择快捷扳机预设");
        }

        TriggerSlotKind slotKind = TriggerSlotKind.parse(vo.getSlotKind());
        if (slotKind == null) {
            slotKind = TriggerSlotKind.TRAIT_ACTIVE;
        }
        if (slotKind == TriggerSlotKind.BASIC_ATTACK) {
            if (bindItem) {
                ensureSingleBasicAttackSlotForItem(vo.getItemId(), vo.getId());
            } else {
                ensureSingleBasicAttackSlotForMonster(vo.getMonsterId(), vo.getId());
            }
        } else if (slotKind == TriggerSlotKind.ULTIMATE) {
            if (bindItem) {
                ensureSingleUltimateSlotForItem(vo.getItemId(), vo.getId());
            } else {
                ensureSingleUltimateSlotForMonster(vo.getMonsterId(), vo.getId());
            }
        }

        GameFinishedSkill finished = finishedSkillService.getById(vo.getFinishedSkillId());
        ErrorFactory.notNull(finished, "扳机技能不存在");

        List<org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo> groups =
                mode == TriggerMode.QUICK
                        ? skillJsonHelper.expandQuickPreset(quickPreset)
                        : (vo.getConditionGroups() != null ? vo.getConditionGroups() : skillJsonHelper.defaultConditionGroups());

        GameTriggerSlot entity = new GameTriggerSlot();
        entity.setId(vo.getId());
        entity.setItemId(bindItem ? vo.getItemId() : null);
        entity.setMonsterId(bindMonster ? vo.getMonsterId() : null);
        entity.setSlotKind(slotKind.name());
        entity.setTriggerMode(mode.name());
        entity.setQuickPreset(mode == TriggerMode.QUICK && quickPreset != null ? quickPreset.name() : null);
        entity.setConditionsJson(skillJsonHelper.writeConditionGroups(groups));
        entity.setTriggerSlotType(null);
        entity.setTriggerParam(null);
        entity.setTriggerRefId(null);
        entity.setFinishedSkillId(vo.getFinishedSkillId());
        entity.setMaxCastCount(null);
        entity.setSort(vo.getSort() != null ? vo.getSort() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        entity.setRemark(vo.getRemark());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(generateUniqueTriggerSlotId());
            triggerSlotService.save(entity);
        } else {
            LambdaUpdateWrapper<GameTriggerSlot> wrapper = triggerSlotService.updateWrapper()
                    .eq(GameTriggerSlot::getId, entity.getId())
                    .set(GameTriggerSlot::getItemId, entity.getItemId())
                    .set(GameTriggerSlot::getMonsterId, entity.getMonsterId())
                    .set(GameTriggerSlot::getSlotKind, entity.getSlotKind())
                    .set(GameTriggerSlot::getTriggerMode, entity.getTriggerMode())
                    .set(GameTriggerSlot::getQuickPreset, entity.getQuickPreset())
                    .set(GameTriggerSlot::getConditionsJson, entity.getConditionsJson())
                    .set(GameTriggerSlot::getTriggerSlotType, entity.getTriggerSlotType())
                    .set(GameTriggerSlot::getTriggerParam, entity.getTriggerParam())
                    .set(GameTriggerSlot::getTriggerRefId, entity.getTriggerRefId())
                    .set(GameTriggerSlot::getFinishedSkillId, entity.getFinishedSkillId())
                    .set(GameTriggerSlot::getMaxCastCount, entity.getMaxCastCount())
                    .set(GameTriggerSlot::getSort, entity.getSort())
                    .set(GameTriggerSlot::getEnabled, entity.getEnabled())
                    .set(GameTriggerSlot::getRemark, entity.getRemark());
            triggerSlotService.update(wrapper);
        }
        GameTriggerSlot saved = triggerSlotService.getById(entity.getId());
        return buildTriggerSlotVo(saved != null ? saved : entity);
    }

    private void normalizeBlankTriggerRef(GameTriggerSlot entity) {
        if (entity.getTriggerRefId() != null && entity.getTriggerRefId().isBlank()) {
            entity.setTriggerRefId(null);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeTriggerSlot(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        triggerSlotService.removeById(id);
    }

    public List<TriggerOptionVo> listTriggerSlotTypeOptions() {
        return TriggerSlotType.allSorted().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setSort(type.getSort());
            vo.setNeedParam(type.isNeedParam());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listTargetTypeOptions() {
        return Arrays.stream(SkillTargetType.values())
                .filter(type -> !type.isLegacy())
                .map(type -> {
                    TriggerOptionVo vo = new TriggerOptionVo();
                    vo.setCode(type.name());
                    vo.setLabel(type.getLabel());
                    vo.setSort(0);
                    return vo;
                }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listReadTypeOptions() {
        return SkillReadType.allSorted().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setSort(type.getSort());
            vo.setNeedParam(type.isNeedSkillFilter());
            vo.setFlag(type.isNeedSkillFilter() ? "FILTER" : (type.isEventScoped() ? "EVENT" : null));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listCompareOpOptions() {
        return SkillCompareOp.all().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel() + " (" + type.getSymbol() + ")");
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listFormulaOutcomeOptions() {
        return SkillFormulaOutcome.all().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listTriggerModeOptions() {
        return TriggerMode.all().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listQuickPresetOptions() {
        return TriggerQuickPreset.all().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listSkillScopeFilterCastOptions() {
        return SkillScopeFilter.forCast().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listSkillScopeFilterHitOptions() {
        return SkillScopeFilter.forHit().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public AdminFinishedSkillVo getUniversalBasicAttack() {
        return getFinishedSkillDetail(UNIVERSAL_BASIC_ATTACK_ID);
    }

    public List<TriggerOptionVo> listEffectKindOptions() {
        return Arrays.stream(AdvancedEffectKind.values()).map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listStatRefOptions() {
        return Arrays.stream(StatRefType.values()).map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listOutcomeTypeOptions() {
        return Arrays.stream(EffectOutcomeType.values()).map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listBindTypeOptions() {
        return Arrays.stream(CompleteSkillBindType.values()).map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<GameFinishedSkill> listFinishedSkillOptions() {
        return finishedSkillService.find()
                .eq(GameFinishedSkill::getEnabled, 1)
                .orderByAsc(GameFinishedSkill::getName)
                .list();
    }

    public FinishedSkillCategoryMetaVo finishedSkillCategoryMeta() {
        FinishedSkillCategoryMetaVo meta = new FinishedSkillCategoryMetaVo();
        meta.setCatL1(Arrays.stream(FinishedSkillCatL1.values()).map(this::toCatOption).collect(Collectors.toList()));
        meta.setCatL2(Arrays.stream(FinishedSkillCatL2.values()).map(this::toCatOption).collect(Collectors.toList()));
        meta.setCatL4(Arrays.stream(FinishedSkillCatL4.values()).map(this::toCatOption).collect(Collectors.toList()));
        return meta;
    }

    private TriggerOptionVo toCatOption(FinishedSkillCatL1 cat) {
        TriggerOptionVo vo = new TriggerOptionVo();
        vo.setCode(cat.name());
        vo.setLabel(cat.getLabel());
        return vo;
    }

    private TriggerOptionVo toCatOption(FinishedSkillCatL2 cat) {
        TriggerOptionVo vo = new TriggerOptionVo();
        vo.setCode(cat.name());
        vo.setLabel(cat.getLabel());
        return vo;
    }

    private TriggerOptionVo toCatOption(FinishedSkillCatL4 cat) {
        TriggerOptionVo vo = new TriggerOptionVo();
        vo.setCode(cat.name());
        vo.setLabel(cat.getLabel());
        return vo;
    }

    private String normalizeCatL1(String code) {
        return FinishedSkillCatL1.parse(code).name();
    }

    private String normalizeCatL2(String code) {
        return FinishedSkillCatL2.parse(code).name();
    }

    private String normalizeCatL3(String value) {
        if (value == null || value.isBlank()) {
            return "通用";
        }
        return value.trim();
    }

    private String normalizeCatL4(String code) {
        return FinishedSkillCatL4.parse(code).name();
    }

    private String generateSkillCodeFromName(String name) {
        return "SKILL_" + WordUnit.randomLowerAlpha(6).toUpperCase();
    }

    private boolean isAsciiSkillId(String id) {
        return id != null && id.matches("^[a-z]+_[a-z0-9_]+$");
    }

    private void ensureSingleBasicAttackSlotForItem(String itemId, String keepId) {
        for (GameTriggerSlot existing : triggerSlotService.listByItemId(itemId)) {
            if (!TriggerSlotKind.isBasicAttack(existing)) {
                continue;
            }
            if (keepId != null && keepId.equals(existing.getId())) {
                continue;
            }
            triggerSlotService.removeById(existing.getId());
        }
    }

    private void ensureSingleBasicAttackSlotForMonster(String monsterId, String keepId) {
        for (GameTriggerSlot existing : triggerSlotService.listByMonsterId(monsterId)) {
            if (!TriggerSlotKind.isBasicAttack(existing)) {
                continue;
            }
            if (keepId != null && keepId.equals(existing.getId())) {
                continue;
            }
            triggerSlotService.removeById(existing.getId());
        }
    }

    private void ensureSingleUltimateSlotForItem(String itemId, String keepId) {
        for (GameTriggerSlot existing : triggerSlotService.listByItemId(itemId)) {
            if (!TriggerSlotKind.isUltimate(existing)) {
                continue;
            }
            if (keepId != null && keepId.equals(existing.getId())) {
                continue;
            }
            triggerSlotService.removeById(existing.getId());
        }
    }

    private void ensureSingleUltimateSlotForMonster(String monsterId, String keepId) {
        for (GameTriggerSlot existing : triggerSlotService.listByMonsterId(monsterId)) {
            if (!TriggerSlotKind.isUltimate(existing)) {
                continue;
            }
            if (keepId != null && keepId.equals(existing.getId())) {
                continue;
            }
            triggerSlotService.removeById(existing.getId());
        }
    }

    private void validateTriggerParam(TriggerSlotType slotType, BigDecimal param, String refId) {
        if (!slotType.isNeedParam()) {
            return;
        }
        if (slotType == TriggerSlotType.FINISHED_SKILL_CAST_COUNT) {
            ErrorFactory.notNull(refId, "释放次数类扳机必须指定关联成品技能");
            return;
        }
        ErrorFactory.notNull(param, slotType.getLabel() + " 必须设置频率");
        ErrorFactory.throwError(param.compareTo(BigDecimal.ZERO) <= 0, "频率必须大于0");
    }

    private AdminFinishedSkillVo buildFinishedSkillVo(GameFinishedSkill skill) {
        AdminFinishedSkillVo vo = new AdminFinishedSkillVo();
        vo.setId(skill.getId());
        vo.setCode(skill.getCode());
        vo.setName(skill.getName());
        vo.setTargetType(skill.getTargetType());
        SkillTargetType tt = SkillTargetType.parse(skill.getTargetType());
        if (tt != null) {
            vo.setTargetTypeLabel(tt.getLabel());
        }
        vo.setTargetParam(skill.getTargetParam());
        vo.setHitFrequency(skill.getHitFrequency() != null ? skill.getHitFrequency() : 1);
        vo.setMaxCastCount(skill.getMaxCastCount());
        vo.setMaxCastUnlimited(skill.getMaxCastCount() == null);
        vo.setFormulas(skillJsonHelper.readFormulas(skill.getFormulasJson()));
        vo.setReadonly(UNIVERSAL_BASIC_ATTACK_ID.equals(skill.getId()));
        vo.setCatL1(skill.getCatL1());
        vo.setCatL2(skill.getCatL2());
        vo.setCatL3(skill.getCatL3());
        vo.setCatL3Label(skill.getCatL3());
        vo.setCatL4(skill.getCatL4());
        FinishedSkillCatL1 c1 = FinishedSkillCatL1.parse(skill.getCatL1());
        vo.setCatL1Label(c1.getLabel());
        FinishedSkillCatL2 c2 = FinishedSkillCatL2.parse(skill.getCatL2());
        vo.setCatL2Label(c2.getLabel());
        FinishedSkillCatL4 c4 = FinishedSkillCatL4.parse(skill.getCatL4());
        vo.setCatL4Label(c4.getLabel());
        vo.setEnabled(skill.getEnabled());
        vo.setRemark(skill.getRemark());
        vo.setEffects(new ArrayList<>());
        return vo;
    }

    private AdminCompleteSkillVo buildCompleteSkillVo(GameCompleteSkill entity) {
        AdminCompleteSkillVo vo = new AdminCompleteSkillVo();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setTriggerSlotType(entity.getTriggerSlotType());
        TriggerSlotType st = TriggerSlotType.parse(entity.getTriggerSlotType());
        if (st != null) {
            vo.setTriggerSlotTypeLabel(st.getLabel());
        }
        vo.setTriggerParam(entity.getTriggerParam());
        vo.setTriggerRefId(entity.getTriggerRefId());
        vo.setFinishedSkillId(entity.getFinishedSkillId());
        GameFinishedSkill fs = finishedSkillService.getById(entity.getFinishedSkillId());
        if (fs != null) {
            vo.setFinishedSkillName(fs.getName());
        }
        if (entity.getTriggerRefId() != null) {
            GameFinishedSkill ref = finishedSkillService.getById(entity.getTriggerRefId());
            if (ref != null) {
                vo.setTriggerRefName(ref.getName());
            }
        }
        vo.setBindType(entity.getBindType());
        CompleteSkillBindType bt = CompleteSkillBindType.parse(entity.getBindType());
        if (bt != null) {
            vo.setBindTypeLabel(bt.getLabel());
        }
        vo.setBindRefId(entity.getBindRefId());
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private AdminTriggerSlotVo buildTriggerSlotVo(GameTriggerSlot entity) {
        AdminTriggerSlotVo vo = new AdminTriggerSlotVo();
        vo.setId(entity.getId());
        vo.setItemId(entity.getItemId());
        GameItem item = gameItemService.getById(entity.getItemId());
        if (item != null) {
            vo.setItemName(item.getName());
        }
        if (entity.getMonsterId() != null) {
            GameMonster monster = monsterService.getById(entity.getMonsterId());
            if (monster != null) {
                vo.setMonsterName(monster.getName());
            }
        }
        vo.setMonsterId(entity.getMonsterId());
        TriggerSlotKind kind = TriggerSlotKind.isBasicAttack(entity)
                ? TriggerSlotKind.BASIC_ATTACK
                : TriggerSlotKind.isUltimate(entity)
                ? TriggerSlotKind.ULTIMATE
                : TriggerSlotKind.TRAIT_ACTIVE;
        vo.setSlotKind(kind.name());
        vo.setSlotKindLabel(kind.getLabel());
        TriggerMode mode = TriggerMode.parse(entity.getTriggerMode());
        vo.setTriggerMode(mode.name());
        vo.setTriggerModeLabel(mode.getLabel());
        vo.setQuickPreset(entity.getQuickPreset());
        TriggerQuickPreset qp = TriggerQuickPreset.parse(entity.getQuickPreset());
        if (qp != null) {
            vo.setQuickPresetLabel(qp.getLabel());
        }
        vo.setConditionGroups(skillJsonHelper.resolveSlotConditions(
                entity.getTriggerMode(), entity.getQuickPreset(), entity.getConditionsJson()));
        vo.setTriggerSlotType(entity.getTriggerSlotType());
        TriggerSlotType st = TriggerSlotType.parse(entity.getTriggerSlotType());
        if (st != null) {
            vo.setTriggerSlotTypeLabel(st.getLabel());
        }
        vo.setTriggerParam(entity.getTriggerParam());
        vo.setTriggerRefId(entity.getTriggerRefId());
        vo.setFinishedSkillId(entity.getFinishedSkillId());
        vo.setMaxCastCount(entity.getMaxCastCount());
        GameFinishedSkill fs = finishedSkillService.getById(entity.getFinishedSkillId());
        if (fs != null) {
            vo.setFinishedSkillName(fs.getName());
        }
        if (entity.getTriggerRefId() != null) {
            GameFinishedSkill ref = finishedSkillService.getById(entity.getTriggerRefId());
            if (ref != null) {
                vo.setTriggerRefName(ref.getName());
            }
        }
        vo.setSort(entity.getSort());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    private String generateUniqueFinishedSkillId() {
        String id;
        do {
            id = "fin_" + WordUnit.randomLowerAlpha(8);
        } while (finishedSkillService.getById(id) != null);
        return id;
    }

    private String generateUniqueTriggerSlotId() {
        String id;
        do {
            id = "ts_" + WordUnit.randomLowerAlpha(8);
        } while (triggerSlotService.getById(id) != null);
        return id;
    }
}