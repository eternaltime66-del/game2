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
        ErrorFactory.notNull(vo.getName(), "名称不能为空");
        ErrorFactory.notNull(vo.getTargetType(), "目标槽不能为空");
        SkillTargetType targetType = SkillTargetType.parse(vo.getTargetType());
        ErrorFactory.notNull(targetType, "目标槽类型无效");

        String code = vo.getCode();
        if (code == null || code.isBlank()) {
            code = generateSkillCodeFromName(vo.getName());
        }

        GameFinishedSkill skill = new GameFinishedSkill();
        skill.setId(vo.getId());
        skill.setCode(code.trim().toUpperCase());
        skill.setName(vo.getName().trim());
        skill.setTargetType(targetType.name());
        skill.setTargetParam(vo.getTargetParam());
        skill.setCatL1(normalizeCatL1(vo.getCatL1()));
        skill.setCatL2(normalizeCatL2(vo.getCatL2()));
        skill.setCatL3(normalizeCatL3(vo.getCatL3()));
        skill.setCatL4(normalizeCatL4(vo.getCatL4()));
        skill.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        skill.setRemark(vo.getRemark());

        if (skill.getId() == null || skill.getId().isBlank()) {
            skill.setId(generateUniqueFinishedSkillId(code));
            finishedSkillService.save(skill);
        } else {
            finishedSkillService.updateById(skill);
        }

        finishedSkillEffectMapper.delete(new LambdaQueryWrapper<GameFinishedSkillEffect>()
                .eq(GameFinishedSkillEffect::getFinishedSkillId, skill.getId()));

        List<AdminFinishedSkillEffectVo> effects = vo.getEffects() != null ? vo.getEffects() : List.of();
        int sort = 1;
        for (AdminFinishedSkillEffectVo effectVo : effects) {
            if (effectVo == null || effectVo.getEffectKind() == null || effectVo.getEffectKind().isBlank()) {
                continue;
            }
            AdvancedEffectKind kind = AdvancedEffectKind.parse(effectVo.getEffectKind());
            ErrorFactory.notNull(kind, "效果种类无效");

            GameFinishedSkillEffect effect = new GameFinishedSkillEffect();
            effect.setId(effectVo.getId() != null && !effectVo.getId().isBlank()
                    ? effectVo.getId() : skill.getId() + "_eff_" + sort);
            effect.setFinishedSkillId(skill.getId());
            effect.setEffectKind(kind.name());
            effect.setSort(effectVo.getSort() != null ? effectVo.getSort() : sort);

            if (kind == AdvancedEffectKind.ACTION_VALUE) {
                ErrorFactory.notNull(effectVo.getActionDelta(), "行动值增减不能为空");
                effect.setOutcomeType(EffectOutcomeType.HEAL.name());
                effect.setActionDelta(effectVo.getActionDelta());
            } else {
                EffectOutcomeType outcome = EffectOutcomeType.parse(effectVo.getOutcomeType());
                ErrorFactory.notNull(outcome, "伤害/治疗类型不能为空");
                effect.setOutcomeType(outcome.name());
                if (kind == AdvancedEffectKind.STAT_FORMULA) {
                    StatRefType statRef = StatRefType.parse(effectVo.getStatRef());
                    ErrorFactory.notNull(statRef, "属性引用不能为空");
                    effect.setStatRef(statRef.name());
                    effect.setRatioY(effectVo.getRatioY() != null ? effectVo.getRatioY() : BigDecimal.ONE);
                    int useWeapon = effectVo.getUseWeaponRatio() != null ? effectVo.getUseWeaponRatio() : 0;
                    effect.setUseWeaponRatio(useWeapon);
                    effect.setRatioZ(null);
                } else {
                    ErrorFactory.notNull(effectVo.getFixedValue(), "固定数值不能为空");
                    effect.setFixedValue(effectVo.getFixedValue());
                }
            }
            effect.setRemark(effectVo.getRemark());
            finishedSkillEffectMapper.insert(effect);
            sort++;
        }
        return getFinishedSkillDetail(skill.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFinishedSkill(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
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
        ErrorFactory.notNull(vo.getTriggerSlotType(), "扳机槽不能为空");
        ErrorFactory.notNull(vo.getFinishedSkillId(), "成品技能不能为空");

        if (bindItem) {
            GameItem item = gameItemService.getById(vo.getItemId());
            ErrorFactory.notNull(item, "物品不存在");
        } else {
            GameMonster monster = monsterService.getById(vo.getMonsterId());
            ErrorFactory.notNull(monster, "怪物不存在");
        }

        TriggerSlotType slotType = TriggerSlotType.parse(vo.getTriggerSlotType());
        ErrorFactory.notNull(slotType, "扳机槽类型无效");
        validateTriggerParam(slotType, vo.getTriggerParam(), vo.getTriggerRefId());

        TriggerSlotKind slotKind = TriggerSlotKind.parse(vo.getSlotKind());
        if (slotType == TriggerSlotType.ACTION_VALUE_FULL
                && (vo.getSlotKind() == null || vo.getSlotKind().isBlank())) {
            slotKind = TriggerSlotKind.BASIC_ATTACK;
        }
        if (slotKind == TriggerSlotKind.BASIC_ATTACK) {
            slotType = TriggerSlotType.ACTION_VALUE_FULL;
            if (bindItem) {
                ensureSingleBasicAttackSlotForItem(vo.getItemId(), vo.getId());
            } else {
                ensureSingleBasicAttackSlotForMonster(vo.getMonsterId(), vo.getId());
            }
        } else if (slotKind == TriggerSlotKind.ULTIMATE) {
            ErrorFactory.throwError(slotType == TriggerSlotType.ACTION_VALUE_FULL,
                    "大招槽不可使用「行动值满」扳机");
            if (bindItem) {
                ensureSingleUltimateSlotForItem(vo.getItemId(), vo.getId());
            } else {
                ensureSingleUltimateSlotForMonster(vo.getMonsterId(), vo.getId());
            }
        }

        GameFinishedSkill finished = finishedSkillService.getById(vo.getFinishedSkillId());
        ErrorFactory.notNull(finished, "成品技能不存在");

        GameTriggerSlot entity = new GameTriggerSlot();
        entity.setId(vo.getId());
        entity.setItemId(bindItem ? vo.getItemId() : null);
        entity.setMonsterId(bindMonster ? vo.getMonsterId() : null);
        entity.setSlotKind(slotKind.name());
        entity.setTriggerSlotType(slotType.name());
        entity.setTriggerParam(vo.getTriggerParam());
        entity.setTriggerRefId(vo.getTriggerRefId());
        entity.setFinishedSkillId(vo.getFinishedSkillId());
        if (vo.getMaxCastCount() != null && vo.getMaxCastCount() <= 0) {
            entity.setMaxCastCount(null);
        } else {
            entity.setMaxCastCount(vo.getMaxCastCount());
        }
        entity.setSort(vo.getSort() != null ? vo.getSort() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        entity.setRemark(vo.getRemark());

        normalizeBlankTriggerRef(entity);

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(generateUniqueTriggerSlotId());
            triggerSlotService.save(entity);
        } else {
            LambdaUpdateWrapper<GameTriggerSlot> wrapper = triggerSlotService.updateWrapper()
                    .eq(GameTriggerSlot::getId, entity.getId())
                    .set(GameTriggerSlot::getItemId, entity.getItemId())
                    .set(GameTriggerSlot::getMonsterId, entity.getMonsterId())
                    .set(GameTriggerSlot::getSlotKind, entity.getSlotKind())
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
        return Arrays.stream(SkillTargetType.values()).map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setSort(0);
            return vo;
        }).collect(Collectors.toList());
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
        String base = "SKILL";
        if (name != null && !name.isBlank()) {
            base = name.trim().replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5]", "");
            if (base.length() > 12) {
                base = base.substring(0, 12);
            }
            if (base.isBlank()) {
                base = "SKILL";
            }
        }
        return base.toUpperCase() + "_" + WordUnit.randomKey(4, 2);
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
        ErrorFactory.notNull(param, slotType.getLabel() + " 必须设置阈值");
        ErrorFactory.throwError(param.compareTo(BigDecimal.ZERO) <= 0, "阈值必须大于0");
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

        List<GameFinishedSkillEffect> effects = finishedSkillEffectService.listByFinishedSkillId(skill.getId());
        List<AdminFinishedSkillEffectVo> effectVos = new ArrayList<>();
        for (GameFinishedSkillEffect effect : effects) {
            AdminFinishedSkillEffectVo ev = new AdminFinishedSkillEffectVo();
            ev.setId(effect.getId());
            ev.setFinishedSkillId(effect.getFinishedSkillId());
            ev.setEffectKind(effect.getEffectKind());
            AdvancedEffectKind ek = AdvancedEffectKind.parse(effect.getEffectKind());
            if (ek != null) {
                ev.setEffectKindLabel(ek.getLabel());
            }
            ev.setOutcomeType(effect.getOutcomeType());
            EffectOutcomeType ot = EffectOutcomeType.parse(effect.getOutcomeType());
            if (ot != null) {
                ev.setOutcomeTypeLabel(ot.getLabel());
            }
            ev.setStatRef(effect.getStatRef());
            StatRefType sr = StatRefType.parse(effect.getStatRef());
            if (sr != null) {
                ev.setStatRefLabel(sr.getLabel());
            }
            ev.setRatioY(effect.getRatioY());
            ev.setUseWeaponRatio(effect.getUseWeaponRatio());
            ev.setRatioZ(effect.getRatioZ());
            ev.setFixedValue(effect.getFixedValue());
            ev.setActionDelta(effect.getActionDelta());
            ev.setSort(effect.getSort());
            ev.setRemark(effect.getRemark());
            effectVos.add(ev);
        }
        vo.setEffects(effectVos);
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

    private String generateUniqueFinishedSkillId(String code) {
        if (code != null && !code.isBlank()) {
            String base = "fin_" + code.trim().toLowerCase();
            if (finishedSkillService.getById(base) == null) {
                return base;
            }
        }
        String id;
        do {
            id = "fin_" + WordUnit.randomKey(8, 3);
        } while (finishedSkillService.getById(id) != null);
        return id;
    }

    private String generateUniqueTriggerSlotId() {
        String id;
        do {
            id = "ts_" + WordUnit.randomKey(8, 3);
        } while (triggerSlotService.getById(id) != null);
        return id;
    }
}
