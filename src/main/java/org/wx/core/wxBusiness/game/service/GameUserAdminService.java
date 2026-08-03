package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.account.entity.Member;
import org.wx.core.wxBusiness.account.entity.MoneyRecord;
import org.wx.core.wxBusiness.account.entity.enums.MemberRole;
import org.wx.core.wxBusiness.account.service.MemberService;
import org.wx.core.wxBusiness.account.service.MoneyRecordService;
import org.wx.core.wxBusiness.game.entity.*;

@Service
public class GameUserAdminService {

    @Resource
    private MemberService memberService;
    @Resource
    private GameHeroService gameHeroService;
    @Resource
    private GameHeroEquipService gameHeroEquipService;
    @Resource
    private GameWarehouseService gameWarehouseService;
    @Resource
    private GameInventoryService gameInventoryService;
    @Resource
    private GameBattleBagService gameBattleBagService;
    @Resource
    private GameItemLogService gameItemLogService;
    @Resource
    private MoneyRecordService moneyRecordService;
    @Resource
    private PveBattleService pveBattleService;

    @Transactional(rollbackFor = Exception.class)
    public void resetGameData(String uid) {
        ErrorFactory.notNull(uid, "用户ID不能为空");
        Member member = memberService.getById(uid);
        ErrorFactory.notNull(member, "用户不存在");
        ErrorFactory.throwError(MemberRole.ADMIN.equals(member.getMemberRole()), "不能清空管理员账号");

        gameInventoryService.remove(new LambdaQueryWrapper<GameInventory>().eq(GameInventory::getUid, uid));
        gameBattleBagService.remove(new LambdaQueryWrapper<GameBattleBag>().eq(GameBattleBag::getUid, uid));
        gameItemLogService.remove(new LambdaQueryWrapper<GameItemLog>().eq(GameItemLog::getUid, uid));
        moneyRecordService.remove(new LambdaQueryWrapper<MoneyRecord>().eq(MoneyRecord::getUid, uid));

        clearHeroEquip(uid);
        resetHero(uid);
        resetWarehouse(uid);
        pveBattleService.clearUserBattleCache(uid);
    }

    @Transactional(rollbackFor = Exception.class)
    public void grantItem(String uid, String itemId, int quantity) {
        ErrorFactory.notNull(uid, "用户ID不能为空");
        ErrorFactory.notNull(itemId, "请选择物品");
        ErrorFactory.throwError(quantity <= 0, "数量必须大于 0");

        Member member = memberService.getById(uid);
        ErrorFactory.notNull(member, "用户不存在");
        ErrorFactory.throwError(MemberRole.ADMIN.equals(member.getMemberRole()), "不能向管理员账号赠送物品");

        gameInventoryService.addWarehouseItem(
                uid,
                itemId,
                quantity,
                GameItemLog.REASON_ADMIN_GRANT,
                "admin",
                "后台赠送"
        );
    }

    private void clearHeroEquip(String uid) {
        gameHeroEquipService.remove(new LambdaQueryWrapper<GameHeroEquip>().eq(GameHeroEquip::getUid, uid));
        gameHeroEquipService.getOrInit(uid);
    }

    private void resetHero(String uid) {
        GameHero hero = gameHeroService.findByUid(uid);
        if (hero == null) {
            gameHeroService.initHero(uid);
            return;
        }
        GameHero defaults = GameHero.defaultHero(uid);
        hero.setName(defaults.getName());
        hero.setMaxHp(defaults.getMaxHp());
        hero.setHp(defaults.getHp());
        hero.setAttack(defaults.getAttack());
        hero.setDefense(defaults.getDefense());
        hero.setActionValue(defaults.getActionValue());
        hero.setOptimalCarryWeight(defaults.getOptimalCarryWeight());
        gameHeroService.updateById(hero);
    }

    private void resetWarehouse(String uid) {
        GameWarehouse warehouse = gameWarehouseService.find().eq(GameWarehouse::getUid, uid).one();
        if (warehouse == null) {
            gameWarehouseService.getOrInit(uid);
            return;
        }
        gameWarehouseService.update(new LambdaUpdateWrapper<GameWarehouse>()
                .eq(GameWarehouse::getId, warehouse.getId())
                .set(GameWarehouse::getMaxSlots, GameWarehouse.DEFAULT_MAX_SLOTS));
    }
}
