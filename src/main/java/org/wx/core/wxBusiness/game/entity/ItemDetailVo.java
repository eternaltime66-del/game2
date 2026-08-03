package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ItemDetailVo implements ItemTagHolder {

    private String id;

    private String code;

    private String name;

    private String icon;

    private Integer maxStack;

    private BigDecimal weight;

    /** @deprecated 兼容旧前端，取首个标签编码 */
    private String itemTag;

    /** 标签编码列表，如 MATERIAL, WEAPON */
    private List<String> itemTagCodes = new ArrayList<>();

    private List<String> tags = new ArrayList<>();

    /** 武器攻击力 */
    private Integer weaponAttack;

    /** 武器基础行动值 */
    private Integer weaponBaseActionValue;

    /** 武器伤害比例 */
    private BigDecimal weaponDamageRatio;

    /** 护甲生命加成 */
    private Integer armorBonusHp;

    /** 护甲防御 */
    private Integer armorDefense;

    /** 护甲攻击加成（饰品等） */
    private Integer armorBonusAttack;

    private String remark;

    /** 合成公式（预留，后续扩展） */
    private List<ItemCraftPreviewVo> crafts = new ArrayList<>();

    /** 装备扳机槽 → 特性主动（与后台「特性主动」Tab 一致） */
    private List<ItemTriggerSlotDetailVo> triggerSlots = new ArrayList<>();

    /** 武器固定普攻槽 */
    private ItemTriggerSlotDetailVo basicAttackSlot;

    /** 武器固定大招槽 */
    private ItemTriggerSlotDetailVo ultimateSlot;

    /** 装备/徽章授予的被动技能 */
    private List<ItemPassiveDetailVo> passiveSkills = new ArrayList<>();
}
