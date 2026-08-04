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
        ErrorFactory.notNull(vo.getName(), "名称不能为空");
        ErrorFactory.throwError(vo.getFormulas() == null || vo.getFormulas().isEmpty(), "请至少添加一条公式");

        String code = vo.getCode();
        if (code == null || code.isBlank()) {
            code = generateSkillCodeFromName(vo.getName());
        }

        List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo> formulas = normalizeFormulaGroups(vo.getFormulas());
        org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo first = formulas.get(0);
        SkillTargetType targetType = SkillTargetType.parse(first.getTargetType());
        ErrorFactory.notNull(targetType, "公式目标槽不能为空");
        ErrorFactory.throwError(targetType.isLegacy(), "请使用新目标槽类型");

        boolean isUniversalBasic = UNIVERSAL_BASIC_ATTACK_ID.equals(vo.getId());
        GameFinishedSkill skill = new GameFinishedSkill();
        skill.setId(isUniversalBasic ? UNIVERSAL_BASIC_ATTACK_ID : vo.getId());
        skill.setCode(isUniversalBasic ? "NORMAL_ATTACK" : code.trim().toUpperCase());
        skill.setName(vo.getName().trim());
        // 技能级保留首条公式快照（兼容旧展示 / DB 非空）
        skill.setTargetType(targetType.name());
        skill.setTargetParam(first.getTargetParam());
        skill.setHitFrequency(first.getHitFrequency() != null ? first.getHitFrequency() : 1);
        skill.setMaxCastCount(first.getMaxCastCount());
        skill.setFormulasJson(skillJsonHelper.writeFormulas(formulas));
        if (isUniversalBasic) {
            skill.setCatL1(FinishedSkillCatL1.PERSON.name());
            skill.setCatL2(FinishedSkillCatL2.GENERAL.name());
            skill.setCatL3(vo.getCatL3() != null && !vo.getCatL3().isBlank() ? vo.getCatL3().trim() : "普攻");
            skill.setCatL4(FinishedSkillCatL4.BASIC_ATTACK.name());
        } else {
            skill.setCatL1(normalizeCatL1(vo.getCatL1()));
            skill.setCatL2(normalizeCatL2(vo.getCatL2()));
            skill.setCatL3(normalizeCatL3(vo.getCatL3()));
            skill.setCatL4(normalizeCatL4(vo.getCatL4()));
        }
        skill.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        skill.setRemark(vo.getRemark());

        boolean createNew = !isUniversalBasic
                && (skill.getId() == null || skill.getId().isBlank() || !isAsciiSkillId(skill.getId())
                || finishedSkillService.getById(skill.getId()) == null);
        if (isUniversalBasic) {
            ErrorFactory.notNull(finishedSkillService.getById(UNIVERSAL_BASIC_ATTACK_ID), "通用普攻不存在，请先初始化");
            skill.setId(UNIVERSAL_BASIC_ATTACK_ID);
            finishedSkillService.updateById(skill);
        } else if (createNew) {
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

        if (isPersonActive(skill)) {
            syncPersonActivePackage(skill, vo);
        }
        return getFinishedSkillDetail(skill.getId());
    }

    private List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo> normalizeFormulaGroups(
            List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo> raw) {
        List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo> list = new ArrayList<>();
        for (org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo fg : raw) {
            ErrorFactory.notNull(fg, "公式不能为空");
            SkillTargetType targetType = SkillTargetType.parse(fg.getTargetType());
            ErrorFactory.notNull(targetType, "每条公式必须配置目标槽");
            ErrorFactory.throwError(targetType.isLegacy(), "请使用新目标槽类型");
            int hitFrequency = fg.getHitFrequency() != null ? fg.getHitFrequency() : 1;
            ErrorFactory.throwError(hitFrequency < 1, "频率槽最小为1");
            Integer maxCast = fg.getMaxCastCount();
            if (maxCast != null && maxCast <= 0) {
                maxCast = null;
            }
            org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo copy =
                    new org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo();
            copy.setTargetType(targetType.name());
            copy.setTargetParam(fg.getTargetParam());
            copy.setHitFrequency(hitFrequency);
            copy.setMaxCastCount(maxCast);
            copy.setOutcome(fg.getOutcome());
            copy.setTokens(fg.getTokens() != null ? fg.getTokens() : new ArrayList<>());
            list.add(copy);
        }
        return list;
    }

    private boolean isPersonActive(GameFinishedSkill skill) {
        return skill != null
                && FinishedSkillCatL1.PERSON.name().equals(skill.getCatL1())
                && FinishedSkillCatL4.ACTIVE.name().equals(skill.getCatL4());
    }

    /** 人物主动 = 技能物品(SKILL) + 扳机槽 + 实体技能 */
    private void syncPersonActivePackage(GameFinishedSkill skill, AdminFinishedSkillVo vo) {
        // 快捷扳机已下线：统一按精准条件保存
        AdminTriggerSlotVo slotVo = new AdminTriggerSlotVo();
        slotVo.setId(vo.getTriggerSlotId() != null && !vo.getTriggerSlotId().isBlank()
                ? vo.getTriggerSlotId()
                : null);
        GameTriggerSlot existingSlot = findPersonActiveSlot(skill.getId());
        if (slotVo.getId() == null && existingSlot != null) {
            slotVo.setId(existingSlot.getId());
        }
        String itemId = vo.getSkillItemId();
        if ((itemId == null || itemId.isBlank()) && existingSlot != null) {
            itemId = existingSlot.getItemId();
        }
        GameItem item = (itemId != null && !itemId.isBlank()) ? gameItemService.getById(itemId) : null;
        if (item == null) {
            item = new GameItem();
            item.setId("item_sk_" + WordUnit.randomLowerAlpha(8));
            item.setCode("SK_" + skill.getCode());
            item.setName(skill.getName());
            item.setIcon(null);
            item.setWeight(BigDecimal.ZERO);
            item.setItemTags(GameItemTag.SKILL.name());
            item.setMaxStack(1);
            item.setSort(0);
            item.setEnabled(skill.getEnabled() != null ? skill.getEnabled() : 1);
            item.setRemark("人物主动技能");
            gameItemService.save(item);
        } else {
            item.setName(skill.getName());
            item.setEnabled(skill.getEnabled() != null ? skill.getEnabled() : 1);
            if (!GameItemTag.contains(item.getItemTags(), GameItemTag.SKILL)) {
                item.setItemTags(GameItemTag.SKILL.name());
            }
            if (item.getCode() == null || item.getCode().isBlank()) {
                item.setCode("SK_" + skill.getCode());
            }
            gameItemService.updateById(item);
        }

        slotVo.setItemId(item.getId());
        slotVo.setSlotKind(TriggerSlotKind.TRAIT_ACTIVE.name());
        slotVo.setTriggerMode(TriggerMode.PRECISE.name());
        slotVo.setQuickPreset(null);
        slotVo.setPrerequisiteMode(vo.getPrerequisiteMode());
        slotVo.setPrerequisites(vo.getPrerequisites());
        slotVo.setNumericMode(vo.getNumericMode());
        slotVo.setConditionGroups(vo.getConditionGroups());
        slotVo.setFinishedSkillId(skill.getId());
        slotVo.setSort(existingSlot != null && existingSlot.getSort() != null ? existingSlot.getSort() : 0);
        slotVo.setEnabled(skill.getEnabled() != null ? skill.getEnabled() : 1);
        slotVo.setRemark(vo.getRemark());
        saveTriggerSlot(slotVo);
    }

    private GameTriggerSlot findPersonActiveSlot(String finishedSkillId) {
        if (finishedSkillId == null || finishedSkillId.isBlank()) {
            return null;
        }
        List<GameTriggerSlot> slots = triggerSlotService.find()
                .eq(GameTriggerSlot::getFinishedSkillId, finishedSkillId)
                .list();
        for (GameTriggerSlot slot : slots) {
            if (slot.getItemId() == null || slot.getItemId().isBlank()) {
                continue;
            }
            GameItem item = gameItemService.getById(slot.getItemId());
            if (item != null && GameItemTag.contains(item.getItemTags(), GameItemTag.SKILL)) {
                return slot;
            }
        }
        return null;
    }

    /** 后台赠送：可选人物主动技能（已生成技能物品） */
    public List<org.wx.core.wxBusiness.api.vo.MemberPersonSkillOptionVo> listPersonActiveGrantOptions() {
        List<GameFinishedSkill> skills = finishedSkillService.find()
                .eq(GameFinishedSkill::getCatL1, FinishedSkillCatL1.PERSON.name())
                .eq(GameFinishedSkill::getCatL4, FinishedSkillCatL4.ACTIVE.name())
                .eq(GameFinishedSkill::getEnabled, 1)
                .orderByAsc(GameFinishedSkill::getCode)
                .list();
        List<org.wx.core.wxBusiness.api.vo.MemberPersonSkillOptionVo> out = new ArrayList<>();
        for (GameFinishedSkill skill : skills) {
            GameTriggerSlot slot = findPersonActiveSlot(skill.getId());
            if (slot == null || slot.getItemId() == null || slot.getItemId().isBlank()) {
                continue;
            }
            org.wx.core.wxBusiness.api.vo.MemberPersonSkillOptionVo opt =
                    new org.wx.core.wxBusiness.api.vo.MemberPersonSkillOptionVo();
            opt.setId(skill.getId());
            opt.setCode(skill.getCode());
            opt.setName(skill.getName());
            opt.setSkillItemId(slot.getItemId());
            opt.setCatL2(skill.getCatL2());
            FinishedSkillCatL2 c2 = FinishedSkillCatL2.parse(skill.getCatL2());
            opt.setCatL2Label(c2.getLabel());
            out.add(opt);
        }
        return out;
    }

    /** 解析人物主动技能对应的技能物品 ID */
    public String requirePersonActiveSkillItemId(String finishedSkillId) {
        ErrorFactory.notNull(finishedSkillId, "请选择人物技能");
        GameFinishedSkill skill = finishedSkillService.getById(finishedSkillId);
        ErrorFactory.notNull(skill, "人物技能不存在");
        ErrorFactory.throwError(!isPersonActive(skill), "仅可赠送人物主动技能");
        GameTriggerSlot slot = findPersonActiveSlot(finishedSkillId);
        ErrorFactory.notNull(slot, "该人物技能尚未生成技能物品，请先在后台保存一次");
        ErrorFactory.notNull(slot.getItemId(), "该人物技能尚未生成技能物品，请先在后台保存一次");
        return slot.getItemId();
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

        TriggerMode incomingMode = TriggerMode.parse(vo.getTriggerMode());
        TriggerQuickPreset quickPreset = TriggerQuickPreset.parse(vo.getQuickPreset());
        // 快捷扳机已下线：统一落库为精准；若仍收到旧 QUICK 载荷则展开为条件
        org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo slotConds;
        if (incomingMode == TriggerMode.QUICK) {
            slotConds = skillJsonHelper.expandQuickPresetAsSlot(quickPreset);
        } else {
            slotConds = new org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo();
            slotConds.setPrerequisiteMode(vo.getPrerequisiteMode());
            slotConds.setPrerequisites(vo.getPrerequisites());
            slotConds.setNumericMode(vo.getNumericMode());
            slotConds.setConditionGroups(vo.getConditionGroups());
            slotConds = skillJsonHelper.normalizeSlot(slotConds);
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
            if (incomingMode == TriggerMode.QUICK
                    || slotConds.getConditionGroups() == null
                    || slotConds.getConditionGroups().isEmpty()) {
                slotConds = skillJsonHelper.expandQuickPresetAsSlot(TriggerQuickPreset.ACTION_VALUE_FULL);
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

        GameTriggerSlot entity = new GameTriggerSlot();
        entity.setId(vo.getId());
        entity.setItemId(bindItem ? vo.getItemId() : null);
        entity.setMonsterId(bindMonster ? vo.getMonsterId() : null);
        entity.setSlotKind(slotKind.name());
        entity.setTriggerMode(TriggerMode.PRECISE.name());
        entity.setQuickPreset(null);
        entity.setConditionsJson(skillJsonHelper.writeSlotConditions(slotConds));
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
            vo.setGroup(type.getGroup());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listCompareOpOptions() {
        return SkillCompareOp.all().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setHint(type.getSymbol());
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
        return List.of();
    }

    public List<TriggerOptionVo> listSkillScopeFilterCastOptions() {
        return SkillScopeFilter.forCast().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setNeedParam(type.needsSkillRef());
            vo.setFlag(type.needsSkillRef() ? "SKILL_REF" : null);
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listSkillScopeFilterHitOptions() {
        return SkillScopeFilter.forHit().stream().map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setNeedParam(type.needsSkillRef());
            vo.setFlag(type.needsSkillRef() ? "SKILL_REF" : null);
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
        fillLegacyFormulaMeta(vo.getFormulas(), skill);
        vo.setReadonly(false);
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
        if (isPersonActive(skill)) {
            GameTriggerSlot slot = findPersonActiveSlot(skill.getId());
            if (slot != null) {
                vo.setSkillItemId(slot.getItemId());
                vo.setTriggerSlotId(slot.getId());
                vo.setTriggerMode(TriggerMode.PRECISE.name());
                vo.setQuickPreset(null);
                org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo slotConds =
                        skillJsonHelper.resolveSlotConditions(
                                slot.getTriggerMode(), slot.getQuickPreset(), slot.getConditionsJson());
                vo.setPrerequisiteMode(slotConds.getPrerequisiteMode());
                vo.setPrerequisites(slotConds.getPrerequisites());
                vo.setNumericMode(slotConds.getNumericMode());
                vo.setConditionGroups(slotConds.getConditionGroups());
            } else {
                vo.setTriggerMode(TriggerMode.PRECISE.name());
                vo.setQuickPreset(null);
                org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo empty =
                        skillJsonHelper.defaultSlotConditions();
                empty.setNumericMode(ConditionZoneMode.CONFIG.name());
                empty.setConditionGroups(skillJsonHelper.defaultConditionGroups());
                vo.setPrerequisiteMode(empty.getPrerequisiteMode());
                vo.setPrerequisites(empty.getPrerequisites());
                vo.setNumericMode(empty.getNumericMode());
                vo.setConditionGroups(empty.getConditionGroups());
            }
        }
        return vo;
    }

    private void fillLegacyFormulaMeta(List<org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo> formulas,
                                       GameFinishedSkill skill) {
        if (formulas == null || formulas.isEmpty() || skill == null) {
            return;
        }
        boolean legacy = formulas.stream()
                .allMatch(f -> f.getTargetType() == null || f.getTargetType().isBlank());
        if (!legacy) {
            return;
        }
        for (org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo fg : formulas) {
            fg.setTargetType(skill.getTargetType());
            fg.setTargetParam(skill.getTargetParam());
            fg.setHitFrequency(skill.getHitFrequency() != null ? skill.getHitFrequency() : 1);
            fg.setMaxCastCount(skill.getMaxCastCount());
        }
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
        // 后台编辑统一按精准展示；旧数据经 resolveSlotConditions 迁移为槽位级双区
        vo.setTriggerMode(TriggerMode.PRECISE.name());
        vo.setTriggerModeLabel(TriggerMode.PRECISE.getLabel());
        vo.setQuickPreset(null);
        vo.setQuickPresetLabel(null);
        org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo slotConds =
                skillJsonHelper.resolveSlotConditions(
                        entity.getTriggerMode(), entity.getQuickPreset(), entity.getConditionsJson());
        vo.setPrerequisiteMode(slotConds.getPrerequisiteMode());
        vo.setPrerequisites(slotConds.getPrerequisites());
        vo.setNumericMode(slotConds.getNumericMode());
        vo.setConditionGroups(slotConds.getConditionGroups());
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