package org.wx.core.wxBusiness.game.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BattleBagVo {

    private List<BattleBagItemVo> items = new ArrayList<>();

    /** 背包内物品重量（不含已穿戴） */
    private BigDecimal bagWeight;

    /** 已穿戴装备重量 */
    private BigDecimal equipWeight;

    /** 战斗负重 = 装备重量 + 背包重量 */
    private BigDecimal totalWeight;

    private BigDecimal optimalCarryWeight;

    /** 超重百分比（相对最佳负重） */
    private BigDecimal excessPercent;

    /** 空手行动值（角色基础） */
    private Integer unarmedActionValue;

    private Integer baseActionValue;

    private Integer effectiveActionValue;

    /** 基础攻击 */
    private Integer heroAttack;

    /** 装备攻击加成（武器等） */
    private Integer equipAttack;

    /** 总攻击 = 基础攻击 + 装备攻击 */
    private Integer totalAttack;

    /** 普攻伤害值 */
    private Integer normalAttackDamage;

    /** 基础防御 */
    private Integer heroDefense;

    /** 装备防御加成 */
    private Integer equipDefense;

    /** 总防御 */
    private Integer totalDefense;

    /** 装备生命加成 */
    private Integer equipBonusHp;

    /** 总生命上限（含装备） */
    private Integer totalMaxHp;

    private BigDecimal damageRatio;

    private EquippedWeaponVo equippedWeapon;

    /** 全部装备槽位概览 */
    private List<HeroEquipSlotVo> equipSlots = new ArrayList<>();

    /** 当前生效/已持有的被动技能 */
    private List<HeroPassiveDetailVo> activePassives = new ArrayList<>();

    /** 属性构成明细（含被动百分比） */
    private List<HeroStatBreakdownRowVo> statBreakdown = new ArrayList<>();

    /** 当前生效普攻技能详情 */
    private ItemFinishedSkillDetailVo basicAttackSkill;

    /** 普攻来源说明 */
    private String basicAttackSourceLabel;

    /** 武器技能槽（大招扳机） */
    private ItemTriggerSlotDetailVo weaponSkill;

    /** 武器技能来源 */
    private String weaponSkillSourceLabel;
}
