package org.wx.core.wxBusiness.game.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wx.core.wxBusiness.game.entity.enums.SkillCompareOp;
import org.wx.core.wxBusiness.game.entity.enums.SkillOperandKind;
import org.wx.core.wxBusiness.game.entity.enums.SkillReadType;
import org.wx.core.wxBusiness.game.entity.enums.TriggerMode;
import org.wx.core.wxBusiness.game.entity.enums.TriggerQuickPreset;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillConditionItemVo;
import org.wx.core.wxBusiness.game.entity.skill.SkillFormulaGroupVo;
import org.wx.core.wxBusiness.game.entity.skill.PassiveConditionVo;
import org.wx.core.wxBusiness.game.entity.skill.PassiveEffectVo;

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

    public String writeConditionGroups(List<SkillConditionGroupVo> groups) {
        return write(groups == null ? List.of() : groups);
    }

    public List<SkillConditionGroupVo> readConditionGroups(String json) {
        return readList(json, new TypeReference<>() {});
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

    /** 快捷预设展开为精准条件组 */
    public List<SkillConditionGroupVo> expandQuickPreset(TriggerQuickPreset preset) {
        if (preset == null) {
            return defaultConditionGroups();
        }
        SkillConditionItemVo item = new SkillConditionItemVo();
        item.setLeftKind(SkillOperandKind.READ.name());
        item.setLeftRead(preset.getReadType().name());
        if (preset.getScopeFilter() != null) {
            item.setLeftFilter(preset.getScopeFilter().name());
        }
        if (preset == TriggerQuickPreset.ACTION_VALUE_FULL) {
            item.setOp(SkillCompareOp.GTE.name());
            item.setRightKind(SkillOperandKind.READ.name());
            item.setRightRead(SkillReadType.CHAR_MAX_ACTION.name());
        } else {
            item.setOp(SkillCompareOp.MOD.name());
            item.setRightKind(SkillOperandKind.CONST.name());
            item.setRightConst(BigDecimal.ONE);
        }
        SkillConditionGroupVo group = new SkillConditionGroupVo();
        group.setItems(List.of(item));
        return List.of(group);
    }

    public List<SkillConditionGroupVo> resolveSlotConditions(String triggerMode, String quickPreset, String conditionsJson) {
        TriggerMode mode = TriggerMode.parse(triggerMode);
        if (mode == TriggerMode.QUICK) {
            return expandQuickPreset(TriggerQuickPreset.parse(quickPreset));
        }
        List<SkillConditionGroupVo> groups = readConditionGroups(conditionsJson);
        return groups.isEmpty() ? defaultConditionGroups() : groups;
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
