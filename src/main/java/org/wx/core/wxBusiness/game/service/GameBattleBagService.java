package org.wx.core.wxBusiness.game.service;

import org.springframework.stereotype.Service;
import org.wx.core.wxBase.base.WxServiceImpl;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.GameBattleBag;
import org.wx.core.wxBusiness.game.mapper.GameBattleBagMapper;

import java.util.List;
import java.util.Objects;

@Service
public class GameBattleBagService extends WxServiceImpl<GameBattleBagMapper, GameBattleBag> {

    public List<GameBattleBag> listByUid(String uid) {
        return this.find()
                .eq(GameBattleBag::getUid, uid)
                .orderByAsc(GameBattleBag::getSort)
                .orderByAsc(GameBattleBag::getId)
                .list();
    }

    public GameBattleBag findByUidAndItemId(String uid, String itemId) {
        return this.find()
                .eq(GameBattleBag::getUid, uid)
                .eq(GameBattleBag::getItemId, itemId)
                .one();
    }

    /** 从背包扣除指定数量，数量不足时抛错 */
    public int consumeQuantity(String uid, String itemId, int quantity) {
        ErrorFactory.throwError(quantity <= 0, "扣除数量无效");
        GameBattleBag row = findByUidAndItemId(uid, itemId);
        ErrorFactory.notNull(row, "背包中没有该物品");
        int before = row.getQuantity() != null ? row.getQuantity() : 0;
        int after = before - quantity;
        ErrorFactory.throwError(after < 0, "背包数量不足");
        if (after == 0) {
            this.removeById(row.getId());
        } else {
            row.setQuantity(after);
            this.updateById(row);
        }
        return before;
    }

    /** 向背包增加指定数量，无记录时新建 */
    public int grantQuantity(String uid, String itemId, int quantity) {
        ErrorFactory.throwError(quantity <= 0, "增加数量无效");
        GameBattleBag row = findByUidAndItemId(uid, itemId);
        int before = 0;
        if (row == null) {
            row = new GameBattleBag();
            row.setUid(uid);
            row.setItemId(itemId);
            row.setQuantity(quantity);
            row.setSort(nextSort(uid));
            this.save(row);
        } else {
            before = row.getQuantity() != null ? row.getQuantity() : 0;
            row.setQuantity(before + quantity);
            this.updateById(row);
        }
        return before;
    }

    private int nextSort(String uid) {
        return listByUid(uid).stream()
                .map(GameBattleBag::getSort)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
