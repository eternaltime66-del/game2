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

    return '<span class="log-skill-name">[' + escapeHtml(name) + ']</span>';

  }



  function axisPrefix(log) {

    if (log.axis == null) {

      return '';

    }

    var n = String(log.axis).padStart(5, '0');

    return '<span class="log-axis">[' + n + ']</span> <span class="log-arrow">→</span> ';

  }



  function appendFormula(html, log) {

    if (log.damageFormula) {

      html += ' <span class="log-formula">' + escapeHtml(log.damageFormula) + '</span>';

    }

    return html;

  }



  function shortenSkillName(name) {

    var text = String(name || '').trim();

    if (!text) {

      return '技能';

    }

    if (text.indexOf(' - ') === -1 && text.indexOf('·') === -1) {

      return text;

    }

    var parts = text.split(/\s*[-·]\s*/);

    if (parts.length >= 2) {

      return parts[parts.length - 2] + ' - ' + parts[parts.length - 1];

    }

    return text;

  }



  function resolveSkillName(log) {

    if (log.skillName) {

      return shortenSkillName(log.skillName);

    }

    var m = String(log.text || '').match(/发动[「\[](.+?)[」\]]|「(.+?)」|\[(.+?)\]/);

    if (m) {

      return shortenSkillName(m[1] || m[2] || m[3]);

    }

    return '技能';

  }



  function formatActionLog(log) {

    if (log.actorName && log.targetName && log.damage != null) {

      var html = role(log.actorName) + ' 攻击 ' + role(log.targetName)

        + ' 造成 <span class="log-damage">' + escapeHtml(log.damage) + '</span> 伤害';

      html = appendFormula(html, log);

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

      var skill = resolveSkillName(log);

      var html = role(log.actorName) + ' 发动 ' + skillName(skill) + ' 对 ' + role(log.targetName)

        + ' 造成 <span class="log-damage">' + escapeHtml(log.damage) + '</span> 伤害';

      html = appendFormula(html, log);

      if (log.killed) {

        html += '，' + role(log.targetName) + '<span class="log-kill"> 死亡</span>';

      }

      return html;

    }



    var healMatch = String(log.text || '').match(/^(.+?) 发动[「\[](.+?)[」\]] 为 (.+?) 恢复 ([\d.]+) 生命$/);

    if (!healMatch) {

      healMatch = String(log.text || '').match(/^(.+?)「(.+?)」为 (.+?) 恢复 ([\d.]+) 生命$/);

    }

    if (healMatch) {

      return role(healMatch[1]) + ' 发动 ' + skillName(shortenSkillName(healMatch[2])) + ' 为 ' + role(healMatch[3])

        + ' 恢复 <span class="log-heal">' + escapeHtml(healMatch[4]) + '</span> 生命';

    }



    var castMatch = String(log.text || '').match(/^(.+?) 释放[「\[](.+?)[」\]]$/);

    if (castMatch) {

      return role(castMatch[1]) + ' 发动 ' + skillName(shortenSkillName(castMatch[2]));

    }



    var dmgMatch = String(log.text || '').match(/^(.+?) 发动[「\[](.+?)[」\]] 对 (.+?) 造成 ([\d.]+) 伤害(?:，(.+?) 死亡)?$/);

    if (!dmgMatch) {

      dmgMatch = String(log.text || '').match(/^(.+?)「(.+?)」对 (.+?) 造成 ([\d.]+) 伤害(?:，(.+?) 死亡)?$/);

    }

    if (dmgMatch) {

      var html2 = role(dmgMatch[1]) + ' 发动 ' + skillName(shortenSkillName(dmgMatch[2])) + ' 对 ' + role(dmgMatch[3])

        + ' 造成 <span class="log-damage">' + escapeHtml(dmgMatch[4]) + '</span> 伤害';

      html2 = appendFormula(html2, log);

      if (dmgMatch[5]) {

        html2 += '，' + role(dmgMatch[5]) + '<span class="log-kill"> 死亡</span>';

      }

      return html2;

    }



    return escapeHtml(log.text);

  }



  function formatBody(log) {

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



  window.BattleLogFmt = {

    html: function (log) {

      return axisPrefix(log) + formatBody(log);

    }

  };

})(window);

