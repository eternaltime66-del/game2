(function (window) {
  function escapeHtml(text) {
    return String(text || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function role(name) {
    return '<span class="log-role">[' + escapeHtml(name) + ']</span>';
  }

  function skillName(name) {
    return '<span class="log-skill-name">「' + escapeHtml(name) + '」</span>';
  }

  function formatActionLog(log) {
    if (log.actorName && log.targetName && log.damage != null) {
      var html = role(log.actorName) + ' 攻击 ' + role(log.targetName)
        + ' 造成 <span class="log-damage">' + escapeHtml(log.damage) + '</span> 伤害';
      if (log.killed) {
        html += '，' + role(log.targetName) + '<span class="log-kill"> 死亡</span>';
      }
      return html;
    }
    var m = String(log.text || '').match(/^(.+?) 攻击 (.+?) 造成 ([\d.]+) 伤害(?:，(.+?) 死亡)?$/);
    if (m) {
      var html2 = role(m[1]) + ' 攻击 ' + role(m[2])
        + ' 造成 <span class="log-damage">' + escapeHtml(m[3]) + '</span> 伤害';
      if (m[4]) {
        html2 += '，' + role(m[4]) + '<span class="log-kill"> 死亡</span>';
      }
      return html2;
    }
    return escapeHtml(log.text);
  }

  function formatSkillLog(log) {
    if (log.actorName && log.targetName && log.damage != null) {
      var skillFromText = String(log.text || '').match(/「(.+?)」/);
      var skill = skillFromText ? skillFromText[1] : '技能';
      var html = role(log.actorName) + skillName(skill) + ' 对 ' + role(log.targetName)
        + ' 造成 <span class="log-damage">' + escapeHtml(log.damage) + '</span> 伤害';
      if (log.killed) {
        html += '，' + role(log.targetName) + '<span class="log-kill"> 死亡</span>';
      }
      return html;
    }

    var healMatch = String(log.text || '').match(/^(.+?)「(.+?)」为 (.+?) 恢复 ([\d.]+) 生命$/);
    if (healMatch) {
      return role(healMatch[1]) + skillName(healMatch[2]) + ' 为 ' + role(healMatch[3])
        + ' 恢复 <span class="log-heal">' + escapeHtml(healMatch[4]) + '</span> 生命';
    }

    var castMatch = String(log.text || '').match(/^(.+?) 释放「(.+?)」$/);
    if (castMatch) {
      return role(castMatch[1]) + ' 释放' + skillName(castMatch[2]);
    }

    var dmgMatch = String(log.text || '').match(/^(.+?)「(.+?)」对 (.+?) 造成 ([\d.]+) 伤害(?:，(.+?) 死亡)?$/);
    if (dmgMatch) {
      var html2 = role(dmgMatch[1]) + skillName(dmgMatch[2]) + ' 对 ' + role(dmgMatch[3])
        + ' 造成 <span class="log-damage">' + escapeHtml(dmgMatch[4]) + '</span> 伤害';
      if (dmgMatch[5]) {
        html2 += '，' + role(dmgMatch[5]) + '<span class="log-kill"> 死亡</span>';
      }
      return html2;
    }

    return escapeHtml(log.text);
  }

  window.BattleLogFmt = {
    html: function (log) {
      if (!log) return '';
      if (log.type === 'ACTION') {
        return formatActionLog(log);
      }
      if (log.type === 'SKILL') {
        return formatSkillLog(log);
      }
      if (log.type === 'WAVE') {
        return '<span class="log-wave-text">' + escapeHtml(log.text) + '</span>';
      }
      if (log.type === 'RESULT') {
        return '<span class="log-result-text">' + escapeHtml(log.text) + '</span>';
      }
      if (log.type === 'LOOT') {
        return '<span class="log-loot-text">' + escapeHtml(log.text) + '</span>';
      }
      return escapeHtml(log.text);
    }
  };
})(window);
