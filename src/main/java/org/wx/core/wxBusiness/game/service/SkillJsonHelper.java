package org.wx.core.wxBusiness.game.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wx.core.wxBusiness.game.entity.enums.ConditionZoneMode;
import org.wx.core.wxBusiness.game.entity.enums.SkillCompareOp;
import org.wx.core.wxBusiness.game.entity.enums.SkillOperandKind;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadType;
import org.wx.core.wxBusiness.game.entity.enums.TriggerMode;
import org.wx.core.wxBusiness.game.entity.enums.TriggerQuickPreset;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionItemVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaTokenVo;
import org.wx.core.wxBusiness.game.entity.skill.PassiveConditionVo;
import org.wx.core.wxBusiness.game.entity.skill.PassiveEffectVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillPrerequisiteVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillSlotConditionsVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SkillJsonHelper {

    private final ObjectMapper mapper = new ObjectMapper();

    public String writeFormulas(List<SkillFormulaGroupVo> formulas) {
        return write(formulas == null ? List.of() : formulas);
    }

    public List<SkillFormulaGroupVo> readFormulas(String json) {
        return readList(json, new TypeReference<>() {});
    }

    public String writeSlotConditions(SkillSlotConditionsVo slot) {
        return write(slot == null ? defaultSlotConditions() : normalizeSlot(slot));
    }

    /** @deprecated 请用 {@link #writeSlotConditions(SkillSlotConditionsVo)} */
    public String writeConditionGroups(List<SkillConditionGroupVo> groups) {
        return writeSlotConditions(fromLegacyGroups(groups));
    }

    public SkillSlotConditionsVo readSlotConditions(String json) {
        if (json == null || json.isBlank()) {
            return defaultSlotConditions();
        }
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || root.isNull()) {
                return defaultSlotConditions();
            }
            if (root.isArray()) {
                List<SkillConditionGroupVo> legacy = mapper.convertValue(root, new TypeReference<>() {});
                return fromLegacyGroups(legacy);
            }
            SkillSlotConditionsVo slot = mapper.treeToValue(root, SkillSlotConditionsVo.class);
            return normalizeSlot(slot != null ? slot : defaultSlotConditions());
        } catch (Exception ex) {
            return defaultSlotConditions();
        }
    }

    /** @deprecated 请用 {@link #readSlotConditions(String)} */
    public List<SkillConditionGroupVo> readConditionGroups(String json) {
        return readSlotConditions(json).getConditionGroups();
    }

    public String writePassiveConditions(List<PassiveConditionVo> list) {
        return write(list == null ? List.of() : list);
    }

    public List<PassiveConditionVo> readPassiveConditions(String json) {
        return readList(json, new TypeReference<>() {});
    }

    public String writePassiveEffects(List<PassiveEffectVo> list) {
        return write(list == null ? List.of() : list);
    }

    public List<PassiveEffectVo> readPassiveEffects(String json) {
        return readList(json, new TypeReference<>() {});
    }

    public SkillSlotConditionsVo expandQuickPresetAsSlot(TriggerQuickPreset preset) {
        SkillSlotConditionsVo slot = new SkillSlotConditionsVo();
        slot.setPrerequisiteMode(ConditionZoneMode.NONE.name());
        slot.setPrerequisites(new ArrayList<>());
        slot.setNumericMode(ConditionZoneMode.CONFIG.name());
        slot.setConditionGroups(expandQuickPreset(preset));
        return slot;
    }

    /** 快捷预设展开为数值条件组 */
    public List<SkillConditionGroupVo> expandQuickPreset(TriggerQuickPreset preset) {
        if (preset == null) {
            return defaultConditionGroups();
        }
        SkillConditionItemVo item = new SkillConditionItemVo();
        item.setOp(preset == TriggerQuickPreset.ACTION_VALUE_FULL
                ? SkillCompareOp.GTE.name()
                : SkillCompareOp.MOD.name());

        SkillFormulaTokenVo left = new SkillFormulaTokenVo();
        left.setKind("READ");
        left.setRead(preset.getReadType().name());
        if (preset.getScopeFilter() != null) {
            left.setFilter(preset.getScopeFilter().name());
        }
        item.setLeftTokens(List.of(left));
        item.setLeftKind(SkillOperandKind.READ.name());
        item.setLeftRead(preset.getReadType().name());
        if (preset.getScopeFilter() != null) {
            item.setLeftFilter(preset.getScopeFilter().name());
        }

        if (preset == TriggerQuickPreset.ACTION_VALUE_FULL) {
            SkillFormulaTokenVo right = new SkillFormulaTokenVo();
            right.setKind("READ");
            right.setRead(SkillReadType.CHAR_MAX_ACTION.name());
            item.setRightTokens(List.of(right));
            item.setRightKind(SkillOperandKind.READ.name());
            item.setRightRead(SkillReadType.CHAR_MAX_ACTION.name());
        } else {
            SkillFormulaTokenVo right = new SkillFormulaTokenVo();
            right.setKind("CONST");
            right.setValue(BigDecimal.ONE);
            item.setRightTokens(List.of(right));
            item.setRightKind(SkillOperandKind.CONST.name());
            item.setRightConst(BigDecimal.ONE);
        }
        SkillConditionGroupVo group = new SkillConditionGroupVo();
        group.setItems(List.of(item));
        return List.of(group);
    }

    public SkillSlotConditionsVo resolveSlotConditions(String triggerMode, String quickPreset, String conditionsJson) {
        TriggerMode mode = TriggerMode.parse(triggerMode);
        if (mode == TriggerMode.QUICK) {
            return expandQuickPresetAsSlot(TriggerQuickPreset.parse(quickPreset));
        }
        return readSlotConditions(conditionsJson);
    }

    public SkillSlotConditionsVo defaultSlotConditions() {
        SkillSlotConditionsVo slot = new SkillSlotConditionsVo();
        slot.setPrerequisiteMode(ConditionZoneMode.NONE.name());
        slot.setPrerequisites(new ArrayList<>());
        slot.setNumericMode(ConditionZoneMode.NONE.name());
        slot.setConditionGroups(new ArrayList<>());
        return slot;
    }

    public List<SkillConditionGroupVo> defaultConditionGroups() {
        SkillConditionGroupVo group = new SkillConditionGroupVo();
        group.setItems(new ArrayList<>());
        return List.of(group);
    }

    public List<PassiveConditionVo> defaultPassiveConditions() {
        PassiveConditionVo none = new PassiveConditionVo();
        none.setType("NONE");
        return List.of(none);
    }

    /** 旧版：每组内嵌前置 → 提升到槽位级 */
    public SkillSlotConditionsVo fromLegacyGroups(List<SkillConditionGroupVo> groups) {
        SkillSlotConditionsVo slot = defaultSlotConditions();
        if (groups == null || groups.isEmpty()) {
            return slot;
        }
        List<SkillPrerequisiteVo> prereqs = new ArrayList<>();
        List<SkillConditionGroupVo> numeric = new ArrayList<>();
        for (SkillConditionGroupVo g : groups) {
            if (g == null) {
                continue;
            }
            if (g.getPrerequisites() != null) {
                for (SkillPrerequisiteVo p : g.getPrerequisites()) {
                    if (p != null) {
                        prereqs.add(p);
                    }
                }
            }
            if (g.getItems() != null && !g.getItems().isEmpty()) {
                SkillConditionGroupVo ng = new SkillConditionGroupVo();
                ng.setItems(new ArrayList<>(g.getItems()));
                numeric.add(ng);
            }
        }
        if (!prereqs.isEmpty()) {
            slot.setPrerequisiteMode(ConditionZoneMode.CONFIG.name());
            slot.setPrerequisites(prereqs);
        }
        if (!numeric.isEmpty()) {
            slot.setNumericMode(ConditionZoneMode.CONFIG.name());
            slot.setConditionGroups(numeric);
        }
        return slot;
    }

    public SkillSlotConditionsVo normalizeSlot(SkillSlotConditionsVo raw) {
        SkillSlotConditionsVo slot = raw != null ? raw : new SkillSlotConditionsVo();
        if (slot.getPrerequisites() == null) {
            slot.setPrerequisites(new ArrayList<>());
        }
        if (slot.getConditionGroups() == null) {
            slot.setConditionGroups(new ArrayList<>());
        }
        // 清理组内遗留前置字段
        for (SkillConditionGroupVo g : slot.getConditionGroups()) {
            if (g == null) {
                continue;
            }
            g.setPrerequisiteMode(null);
            g.setPrerequisites(null);
            g.setNumericMode(null);
        }
        ConditionZoneMode prereqMode = ConditionZoneMode.parse(slot.getPrerequisiteMode());
        if (slot.getPrerequisiteMode() == null || slot.getPrerequisiteMode().isBlank()) {
            prereqMode = slot.getPrerequisites().isEmpty() ? ConditionZoneMode.NONE : ConditionZoneMode.CONFIG;
        }
        slot.setPrerequisiteMode(prereqMode.name());
        if (prereqMode == ConditionZoneMode.NONE) {
            slot.setPrerequisites(new ArrayList<>());
        }

        ConditionZoneMode numericMode = ConditionZoneMode.parse(slot.getNumericMode());
        if (slot.getNumericMode() == null || slot.getNumericMode().isBlank()) {
            boolean hasItems = slot.getConditionGroups().stream()
                    .anyMatch(g -> g != null && g.getItems() != null && !g.getItems().isEmpty());
            numericMode = hasItems ? ConditionZoneMode.CONFIG : ConditionZoneMode.NONE;
        }
        slot.setNumericMode(numericMode.name());
        if (numericMode == ConditionZoneMode.NONE) {
            slot.setConditionGroups(new ArrayList<>());
        }
        return slot;
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("JSON序列化失败", ex);
        }
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<T> list = mapper.readValue(json, type);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
}
