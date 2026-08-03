package org.wx.core.wxBusiness.game.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.wx.core.wxBusiness.game.entity.BattleLootEntry;
import org.wx.core.wxBusiness.game.entity.GameItem;
import org.wx.core.wxBusiness.game.entity.GameMonsterDrop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MonsterDropService {

    @Resource
    private GameMonsterDropService gameMonsterDropService;
    @Resource
    private GameItemService gameItemService;

    public List<BattleLootEntry> rollDrops(String monsterId) {
        List<GameMonsterDrop> configs = gameMonsterDropService.listEnabledByMonsterId(monsterId);
        List<BattleLootEntry> results = new ArrayList<>();
        for (GameMonsterDrop config : configs) {
            if (!rollRate(config.getDropRate())) {
                continue;
            }
            int qty = rollQuantity(config.getMinQty(), config.getMaxQty());
            if (qty <= 0) {
                continue;
            }
            GameItem item = gameItemService.getById(config.getItemId());
            if (item == null || !Integer.valueOf(1).equals(item.getEnabled())) {
                continue;
            }
            BattleLootEntry entry = new BattleLootEntry();
            entry.setItemId(item.getId());
            entry.setItemCode(item.getCode());
            entry.setItemName(item.getName());
            entry.setIcon(item.getIcon());
            entry.setQuantity(qty);
            results.add(entry);
        }
        return results;
    }

    private boolean rollRate(Integer dropRate) {
        int rate = dropRate != null ? dropRate : 0;
        if (rate <= 0) {
            return false;
        }
        if (rate >= 100) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(100) < rate;
    }

    /** 数量越大概率越低；min=0 时均匀随机 [0,max] */
    private int rollQuantity(Integer minQty, Integer maxQty) {
        int min = minQty != null ? minQty : 0;
        int max = maxQty != null ? maxQty : min;
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        if (min == max) {
            return min;
        }
        if (min <= 0) {
            return ThreadLocalRandom.current().nextInt(max + 1);
        }
        int totalWeight = 0;
        for (int q = min; q <= max; q++) {
            int w = max - q + 1;
            totalWeight += w * w;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (int q = min; q <= max; q++) {
            int w = max - q + 1;
            roll -= w * w;
            if (roll < 0) {
                return q;
            }
        }
        return min;
    }
}
