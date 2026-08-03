package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBase.factory.PageFactory;
import org.wx.core.wxBase.unit.WordUnit;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameItemTag;
import org.wx.core.wxBusiness.game.entity.enums.PassiveConditionType;
import org.wx.core.wxBusiness.game.entity.enums.PassiveEffectType;
import org.wx.core.wxBusiness.game.entity.enums.PassiveSkillKind;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GamePassiveSkillAdminService {

    @Resource
    private GamePassiveSkillService passiveSkillService;
    @Resource
    private GameSkillBadgeService skillBadgeService;
    @Resource
    private GameItemPassiveService itemPassiveService;
    @Resource
    private GameItemService itemService;
    @Resource
    private GameMonsterService monsterService;
    @Resource
    private GameMonsterPassiveService monsterPassiveService;
    @Resource
    private GameReferenceCleanupService referenceCleanupService;

    public IPage<AdminPassiveSkillVo> listPassiveSkills(GamePassiveSkill query) {
        return passiveSkillService.pageQuery(query).convert(this::toPassiveVo);
    }

    public AdminPassiveSkillVo getPassiveSkillDetail(String id) {
        GamePassiveSkill skill = passiveSkillService.getById(id);
        ErrorFactory.notNull(skill, "被动技能不存在");
        return toPassiveVo(skill);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminPassiveSkillVo savePassiveSkill(AdminPassiveSkillVo vo) {
        ErrorFactory.notNull(vo.getCode(), "编码不能为空");
        ErrorFactory.notNull(vo.getName(), "名称不能为空");
        ErrorFactory.notNull(vo.getEffectType(), "效果类型不能为空");
        PassiveEffectType effectType = PassiveEffectType.parse(vo.getEffectType());
        ErrorFactory.notNull(effectType, "效果类型无效");
        ErrorFactory.notNull(vo.getEffectValue(), "效果数值不能为空");

        PassiveConditionType conditionType = PassiveConditionType.parse(
                vo.getConditionType() != null ? vo.getConditionType() : PassiveConditionType.NONE.name());
        ErrorFactory.notNull(conditionType, "生效条件无效");
        if (conditionType == PassiveConditionType.REQUIRE_EQUIP) {
            ErrorFactory.notNull(vo.getConditionEquipItemId(), "请选择条件装备");
            GameItem item = itemService.getById(vo.getConditionEquipItemId());
            ErrorFactory.notNull(item, "条件装备不存在");
        }

        GamePassiveSkill entity = new GamePassiveSkill();
        entity.setId(vo.getId());
        entity.setCode(vo.getCode().trim().toUpperCase());
        entity.setName(vo.getName().trim());
        PassiveSkillKind kind = PassiveSkillKind.parse(vo.getPassiveKind());
        entity.setPassiveKind(kind.name());
        entity.setConditionType(conditionType.name());
        entity.setConditionEquipItemId(conditionType == PassiveConditionType.REQUIRE_EQUIP
                ? vo.getConditionEquipItemId() : null);
        entity.setEffectType(effectType.name());
        entity.setEffectValue(vo.getEffectValue());
        entity.setSort(vo.getSort() != null ? vo.getSort() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        entity.setRemark(vo.getRemark());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(generateUniquePassiveId(vo.getCode()));
            passiveSkillService.save(entity);
        } else {
            passiveSkillService.updateById(entity);
        }
        return getPassiveSkillDetail(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removePassiveSkill(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        referenceCleanupService.removePassiveSkillBindings(id);
        passiveSkillService.removeById(id);
    }

    public IPage<AdminSkillBadgeVo> listSkillBadges(GameItem query) {
        List<GameItem> badges = itemService.find().list().stream()
                .filter(item -> ItemTagHelper.hasTag(item, GameItemTag.SKILL_BADGE))
                .collect(Collectors.toList());
        List<AdminSkillBadgeVo> vos = badges.stream().map(this::toSkillBadgeVo).collect(Collectors.toList());
        Page<AdminSkillBadgeVo> page = PageFactory.defaultPage();
        int current = (int) page.getCurrent();
        int size = (int) page.getSize();
        int from = Math.max(0, (current - 1) * size);
        int to = Math.min(vos.size(), from + size);
        page.setRecords(from >= vos.size() ? List.of() : vos.subList(from, to));
        page.setTotal(vos.size());
        return page;
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminSkillBadgeVo saveSkillBadge(AdminSkillBadgeVo vo) {
        ErrorFactory.notNull(vo.getCode(), "编码不能为空");
        ErrorFactory.notNull(vo.getName(), "名称不能为空");
        ErrorFactory.notNull(vo.getPassiveSkillId(), "被动技能不能为空");
        GamePassiveSkill passive = passiveSkillService.getById(vo.getPassiveSkillId());
        ErrorFactory.notNull(passive, "被动技能不存在");

        String itemId = vo.getItemId();
        if (itemId == null || itemId.isBlank()) {
            itemId = "item_" + vo.getCode().trim().toLowerCase();
        }

        GameItem item = itemService.getById(itemId);
        boolean isNew = item == null;
        if (isNew) {
            item = new GameItem();
            item.setId(itemId);
        }
        item.setCode(vo.getCode().trim().toUpperCase());
        item.setName(vo.getName().trim());
        item.setIcon(vo.getIcon() != null ? vo.getIcon() : "🎖");
        item.setItemTags(GameItemTag.SKILL_BADGE.name());
        item.setMaxStack(1);
        item.setSort(vo.getSort() != null ? vo.getSort() : 0);
        item.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        item.setRemark(vo.getRemark());
        if (isNew) {
            itemService.save(item);
        } else {
            itemService.updateById(item);
        }

        GameSkillBadge badge = skillBadgeService.getByItemId(itemId);
        if (badge == null) {
            badge = new GameSkillBadge();
            badge.setItemId(itemId);
        }
        badge.setPassiveSkillId(vo.getPassiveSkillId());
        if (skillBadgeService.getByItemId(itemId) == null) {
            skillBadgeService.save(badge);
        } else {
            skillBadgeService.updateById(badge);
        }

        vo.setItemId(itemId);
        vo.setId(itemId);
        return toSkillBadgeVo(itemService.getById(itemId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeSkillBadge(String itemId) {
        ErrorFactory.notNull(itemId, "物品ID不能为空");
        referenceCleanupService.removeItemBindings(itemId);
        skillBadgeService.removeById(itemId);
        itemService.removeById(itemId);
    }

    public List<TriggerOptionVo> listPassiveConditionTypes() {
        return Arrays.stream(PassiveConditionType.values()).map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<TriggerOptionVo> listPassiveEffectTypes() {
        return Arrays.stream(PassiveEffectType.values()).map(type -> {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    public List<GamePassiveSkill> listPassiveSkillOptions() {
        return passiveSkillService.find()
                .eq(GamePassiveSkill::getEnabled, 1)
                .orderByAsc(GamePassiveSkill::getSort)
                .orderByAsc(GamePassiveSkill::getName)
                .list();
    }

    public PassiveSkillMetaVo meta() {
        PassiveSkillMetaVo vo = new PassiveSkillMetaVo();
        vo.setConditionTypes(listPassiveConditionTypes());
        vo.setEffectTypes(listPassiveEffectTypes());
        vo.setPassiveSkillOptions(listPassiveSkillOptions().stream().map(ps -> {
            TriggerOptionVo opt = new TriggerOptionVo();
            opt.setCode(ps.getId());
            opt.setLabel(ps.getName() + " (" + ps.getId() + ")");
            return opt;
        }).collect(Collectors.toList()));
        return vo;
    }

    public List<AdminItemPassiveVo> listItemPassivesByItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        return itemPassiveService.listByItemId(itemId).stream()
                .map(this::toItemPassiveVo)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminItemPassiveVo saveItemPassive(AdminItemPassiveVo vo) {
        ErrorFactory.notNull(vo.getItemId(), "物品ID不能为空");
        ErrorFactory.notNull(vo.getPassiveSkillId(), "被动技能不能为空");
        GameItem item = itemService.getById(vo.getItemId());
        ErrorFactory.notNull(item, "物品不存在");
        GamePassiveSkill passive = passiveSkillService.getById(vo.getPassiveSkillId());
        ErrorFactory.notNull(passive, "被动技能不存在");

        GameItemPassive entity = new GameItemPassive();
        entity.setId(vo.getId());
        entity.setItemId(vo.getItemId());
        entity.setPassiveSkillId(vo.getPassiveSkillId());
        entity.setSort(vo.getSort() != null ? vo.getSort() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        entity.setRemark(vo.getRemark());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(generateUniqueItemPassiveId());
            itemPassiveService.save(entity);
        } else {
            itemPassiveService.updateById(entity);
        }
        return toItemPassiveVo(itemPassiveService.getById(entity.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeItemPassive(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        itemPassiveService.removeById(id);
    }

    private AdminPassiveSkillVo toPassiveVo(GamePassiveSkill skill) {
        AdminPassiveSkillVo vo = new AdminPassiveSkillVo();
        vo.setId(skill.getId());
        vo.setCode(skill.getCode());
        vo.setName(skill.getName());
        PassiveSkillKind kind = PassiveSkillKind.parse(skill.getPassiveKind());
        vo.setPassiveKind(kind.name());
        vo.setPassiveKindLabel(kind.getLabel());
        vo.setConditionType(skill.getConditionType());
        PassiveConditionType ct = PassiveConditionType.parse(skill.getConditionType());
        if (ct != null) {
            vo.setConditionTypeLabel(ct.getLabel());
        }
        vo.setConditionEquipItemId(skill.getConditionEquipItemId());
        if (skill.getConditionEquipItemId() != null) {
            GameItem item = itemService.getById(skill.getConditionEquipItemId());
            if (item != null) {
                vo.setConditionEquipItemName(item.getName());
            }
        }
        vo.setEffectType(skill.getEffectType());
        PassiveEffectType et = PassiveEffectType.parse(skill.getEffectType());
        if (et != null) {
            vo.setEffectTypeLabel(et.getLabel());
        }
        vo.setEffectValue(skill.getEffectValue());
        vo.setSort(skill.getSort());
        vo.setEnabled(skill.getEnabled());
        vo.setRemark(skill.getRemark());
        return vo;
    }

    private AdminSkillBadgeVo toSkillBadgeVo(GameItem item) {
        AdminSkillBadgeVo vo = new AdminSkillBadgeVo();
        vo.setId(item.getId());
        vo.setItemId(item.getId());
        vo.setCode(item.getCode());
        vo.setName(item.getName());
        vo.setIcon(item.getIcon());
        vo.setSort(item.getSort());
        vo.setEnabled(item.getEnabled());
        vo.setRemark(item.getRemark());
        GameSkillBadge badge = skillBadgeService.getByItemId(item.getId());
        if (badge != null) {
            vo.setPassiveSkillId(badge.getPassiveSkillId());
            GamePassiveSkill passive = passiveSkillService.getById(badge.getPassiveSkillId());
            if (passive != null) {
                vo.setPassiveSkillName(passive.getName());
            }
        }
        return vo;
    }

    private String generateUniquePassiveId(String code) {
        if (code != null && !code.isBlank()) {
            String base = "psv_" + code.trim().toLowerCase();
            if (passiveSkillService.getById(base) == null) {
                return base;
            }
        }
        String id;
        do {
            id = "psv_" + WordUnit.randomKey(8, 3);
        } while (passiveSkillService.getById(id) != null);
        return id;
    }

    public List<AdminMonsterPassiveVo> listMonsterPassivesByMonster(String monsterId) {
        if (monsterId == null || monsterId.isBlank()) {
            return List.of();
        }
        return monsterPassiveService.listByMonsterId(monsterId).stream()
                .map(this::toMonsterPassiveVo)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminMonsterPassiveVo saveMonsterPassive(AdminMonsterPassiveVo vo) {
        ErrorFactory.notNull(vo.getMonsterId(), "怪物ID不能为空");
        ErrorFactory.notNull(vo.getPassiveSkillId(), "被动技能不能为空");
        GameMonster monster = monsterService.getById(vo.getMonsterId());
        ErrorFactory.notNull(monster, "怪物不存在");
        GamePassiveSkill passive = passiveSkillService.getById(vo.getPassiveSkillId());
        ErrorFactory.notNull(passive, "被动技能不存在");

        GameMonsterPassive entity = new GameMonsterPassive();
        entity.setId(vo.getId());
        entity.setMonsterId(vo.getMonsterId());
        entity.setPassiveSkillId(vo.getPassiveSkillId());
        entity.setSort(vo.getSort() != null ? vo.getSort() : 0);
        entity.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        entity.setRemark(vo.getRemark());

        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(generateUniqueMonsterPassiveId());
            monsterPassiveService.save(entity);
        } else {
            monsterPassiveService.updateById(entity);
        }
        return toMonsterPassiveVo(monsterPassiveService.getById(entity.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMonsterPassive(String id) {
        ErrorFactory.notNull(id, "ID不能为空");
        monsterPassiveService.removeById(id);
    }

    private AdminMonsterPassiveVo toMonsterPassiveVo(GameMonsterPassive binding) {
        AdminMonsterPassiveVo vo = new AdminMonsterPassiveVo();
        vo.setId(binding.getId());
        vo.setMonsterId(binding.getMonsterId());
        vo.setPassiveSkillId(binding.getPassiveSkillId());
        vo.setSort(binding.getSort());
        vo.setEnabled(binding.getEnabled());
        vo.setRemark(binding.getRemark());
        GameMonster monster = monsterService.getById(binding.getMonsterId());
        if (monster != null) {
            vo.setMonsterName(monster.getName());
        }
        GamePassiveSkill passive = passiveSkillService.getById(binding.getPassiveSkillId());
        if (passive != null) {
            vo.setPassiveSkillName(passive.getName());
            PassiveConditionType ct = PassiveConditionType.parse(passive.getConditionType());
            if (ct != null) {
                vo.setConditionTypeLabel(ct.getLabel());
            }
            PassiveEffectType et = PassiveEffectType.parse(passive.getEffectType());
            if (et != null) {
                vo.setEffectTypeLabel(et.getLabel());
            }
            vo.setEffectValue(passive.getEffectValue());
        }
        return vo;
    }

    private String generateUniqueMonsterPassiveId() {
        String id;
        do {
            id = "mpsv_" + WordUnit.randomKey(8, 3);
        } while (monsterPassiveService.getById(id) != null);
        return id;
    }

    private AdminItemPassiveVo toItemPassiveVo(GameItemPassive binding) {
        AdminItemPassiveVo vo = new AdminItemPassiveVo();
        vo.setId(binding.getId());
        vo.setItemId(binding.getItemId());
        vo.setPassiveSkillId(binding.getPassiveSkillId());
        vo.setSort(binding.getSort());
        vo.setEnabled(binding.getEnabled());
        vo.setRemark(binding.getRemark());
        GameItem item = itemService.getById(binding.getItemId());
        if (item != null) {
            vo.setItemName(item.getName());
        }
        GamePassiveSkill passive = passiveSkillService.getById(binding.getPassiveSkillId());
        if (passive != null) {
            vo.setPassiveSkillName(passive.getName());
            PassiveConditionType ct = PassiveConditionType.parse(passive.getConditionType());
            if (ct != null) {
                vo.setConditionTypeLabel(ct.getLabel());
            }
            PassiveEffectType et = PassiveEffectType.parse(passive.getEffectType());
            if (et != null) {
                vo.setEffectTypeLabel(et.getLabel());
            }
            vo.setEffectValue(passive.getEffectValue());
        }
        return vo;
    }

    private String generateUniqueItemPassiveId() {
        String id;
        do {
            id = "itp_" + WordUnit.randomKey(8, 3);
        } while (itemPassiveService.getById(id) != null);
        return id;
    }
}
