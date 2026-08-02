package org.wx.core.wxBusiness.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wx.core.wxBase.factory.ErrorFactory;
import org.wx.core.wxBusiness.game.entity.*;
import org.wx.core.wxBusiness.game.entity.enums.GameSkillTargetType;
import org.wx.core.wxBusiness.game.entity.enums.GameTriggerEffectType;
import org.wx.core.wxBusiness.game.mapper.GameSkillEffectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GameSkillAdminService {

    @Resource
    private GameSkillService skillService;
    @Resource
    private GameSkillEffectMapper skillEffectMapper;

    public IPage<GameSkill> list(GameSkill query) {
        return skillService.pageQuery(query);
    }

    public AdminGameSkillVo getDetail(String skillId) {
        GameSkill skill = skillService.getById(skillId);
        ErrorFactory.notNull(skill, "技能不存在");
        return buildVo(skill);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminGameSkillVo save(AdminGameSkillVo vo) {
        ErrorFactory.notNull(vo, "技能不能为空");
        ErrorFactory.notNull(vo.getCode(), "技能编码不能为空");
        ErrorFactory.notNull(vo.getName(), "技能名称不能为空");

        GameSkill skill = new GameSkill();
        skill.setId(vo.getId());
        skill.setCode(vo.getCode().trim().toUpperCase());
        skill.setName(vo.getName().trim());
        skill.setSort(vo.getSort() != null ? vo.getSort() : 0);
        skill.setEnabled(vo.getEnabled() != null ? vo.getEnabled() : 1);
        skill.setRemark(vo.getRemark());

        if (skill.getId() == null || skill.getId().isBlank()) {
            skill.setId("sk_" + skill.getCode().toLowerCase());
            skillService.save(skill);
        } else {
            skillService.updateById(skill);
        }

        skillEffectMapper.delete(new LambdaQueryWrapper<GameSkillEffect>()
                .eq(GameSkillEffect::getSkillId, skill.getId()));

        List<AdminGameSkillEffectVo> effects = vo.getEffects() != null ? vo.getEffects() : List.of();
        int sort = 1;
        for (AdminGameSkillEffectVo effectVo : effects) {
            if (effectVo == null || effectVo.getEffectType() == null || effectVo.getEffectType().isBlank()) {
                continue;
            }
            GameTriggerEffectType effectType = GameTriggerEffectType.parse(effectVo.getEffectType());
            ErrorFactory.notNull(effectType, "技能效果类型无效: " + effectVo.getEffectType());

            GameSkillTargetType targetType = GameSkillTargetType.parse(effectVo.getTargetType());
            if (targetType == null) {
                targetType = GameSkillTargetType.SELF;
            }

            BigDecimal value = effectVo.getEffectValue() != null ? effectVo.getEffectValue() : BigDecimal.ZERO;
            ErrorFactory.throwError(value.compareTo(BigDecimal.ZERO) <= 0, "技能效果数值必须大于0");

            GameSkillEffect effect = new GameSkillEffect();
            String effectId = effectVo.getId();
            if (effectId == null || effectId.isBlank()) {
                effectId = skill.getId() + "_eff_" + sort;
            }
            effect.setId(effectId);
            effect.setSkillId(skill.getId());
            effect.setEffectType(effectType.name());
            effect.setEffectValue(value);
            effect.setTargetType(targetType.name());
            effect.setSort(effectVo.getSort() != null ? effectVo.getSort() : sort);
            effect.setRemark(effectVo.getRemark());
            skillEffectMapper.insert(effect);
            sort++;
        }

        return getDetail(skill.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(String skillId) {
        ErrorFactory.notNull(skillId, "技能ID不能为空");
        skillEffectMapper.delete(new LambdaQueryWrapper<GameSkillEffect>()
                .eq(GameSkillEffect::getSkillId, skillId));
        skillService.removeById(skillId);
    }

    public List<TriggerOptionVo> listEffectOptions() {
        List<TriggerOptionVo> options = new ArrayList<>();
        for (GameTriggerEffectType type : GameTriggerEffectType.allSorted()) {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setSort(type.getSort());
            options.add(vo);
        }
        return options;
    }

    public List<TriggerOptionVo> listTargetOptions() {
        List<TriggerOptionVo> options = new ArrayList<>();
        for (GameSkillTargetType type : GameSkillTargetType.values()) {
            TriggerOptionVo vo = new TriggerOptionVo();
            vo.setCode(type.name());
            vo.setLabel(type.getLabel());
            vo.setSort(type.getSort());
            options.add(vo);
        }
        return options;
    }

    private AdminGameSkillVo buildVo(GameSkill skill) {
        AdminGameSkillVo vo = new AdminGameSkillVo();
        vo.setId(skill.getId());
        vo.setCode(skill.getCode());
        vo.setName(skill.getName());
        vo.setSort(skill.getSort());
        vo.setEnabled(skill.getEnabled());
        vo.setRemark(skill.getRemark());

        List<GameSkillEffect> effects = skillEffectMapper.selectList(
                new LambdaQueryWrapper<GameSkillEffect>()
                        .eq(GameSkillEffect::getSkillId, skill.getId())
                        .orderByAsc(GameSkillEffect::getSort));
        List<AdminGameSkillEffectVo> effectVos = new ArrayList<>();
        for (GameSkillEffect effect : effects) {
            AdminGameSkillEffectVo ev = new AdminGameSkillEffectVo();
            ev.setId(effect.getId());
            ev.setSkillId(effect.getSkillId());
            ev.setEffectType(effect.getEffectType());
            GameTriggerEffectType et = GameTriggerEffectType.parse(effect.getEffectType());
            if (et != null) {
                ev.setEffectTypeLabel(et.getLabel());
            }
            ev.setEffectValue(effect.getEffectValue());
            ev.setTargetType(effect.getTargetType());
            GameSkillTargetType tt = GameSkillTargetType.parse(effect.getTargetType());
            if (tt != null) {
                ev.setTargetTypeLabel(tt.getLabel());
            }
            ev.setSort(effect.getSort());
            ev.setRemark(effect.getRemark());
            effectVos.add(ev);
        }
        vo.setEffects(effectVos);
        return vo;
    }
}
