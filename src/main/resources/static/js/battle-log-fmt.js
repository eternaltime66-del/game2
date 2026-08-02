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

  window.BattleLogFmt = {
    html: function (log) {
      if (!log) return '';
      if (log.type === 'ACTION') {
        return formatActionLog(log);
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
