(function (window) {
  var cfg = window.APP_CONFIG || {};
  var ADMIN_TOKEN_KEY = 'adminToken';

  function getAdminToken() {
    return localStorage.getItem(ADMIN_TOKEN_KEY) || '';
  }

  function saveAdminToken(token) {
    if (token) localStorage.setItem(ADMIN_TOKEN_KEY, token);
  }

  function clearAdminToken() {
    localStorage.removeItem(ADMIN_TOKEN_KEY);
  }

  function request(path, options) {
    options = options || {};
    var method = options.method || 'POST';
    var headers = Object.assign({
      token: getAdminToken()
    }, options.headers || {});

    var url = (cfg.apiBase || '') + path;
    if (options.query) {
      var qs = new URLSearchParams(options.query).toString();
      if (qs) url += (url.indexOf('?') >= 0 ? '&' : '?') + qs;
    }

    var init = { method: method, headers: headers };
    if (options.body != null) {
      if (options.json) {
        headers['Content-Type'] = 'application/json';
        init.body = JSON.stringify(options.body);
      } else {
        headers['Content-Type'] = 'application/x-www-form-urlencoded';
        init.body = new URLSearchParams(options.body).toString();
      }
    }

    return fetch(url, init).then(function (res) { return res.json(); });
  }

  function ensureSuccess(result) {
    if (!result || result.success === false) {
      var err = new Error((result && result.msg) || '请求失败');
      err.result = result;
      throw err;
    }
    return result;
  }

  function pageList(path, body, query) {
    return request(path, {
      json: true,
      body: body || {},
      query: query || { current: 1, size: 100 }
    }).then(ensureSuccess);
  }

  window.AdminApi = {
    getAdminToken: getAdminToken,
    saveAdminToken: saveAdminToken,
    clearAdminToken: clearAdminToken,
    login: function (account, password) {
      return request('/back/login', {
        body: { account: account, password: password }
      }).then(ensureSuccess);
    },
    memberList: function (query) {
      return pageList('/back/member/list', { memberRole: 'USER' }, query);
    },
    loginAsUser: function (uid) {
      return request('/back/member/login/as', {
        json: true,
        body: { id: uid }
      }).then(ensureSuccess);
    },
    levelTree: function () {
      return request('/back/game/level/tree', {}).then(ensureSuccess);
    },
    stageList: function (query) {
      return pageList('/back/game/level/stage/list', {}, query);
    },
    saveStage: function (body) {
      return request('/back/game/level/stage/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeStage: function (id) {
      return request('/back/game/level/stage/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    monsterList: function (query) {
      return pageList('/back/game/battle/monster/list', {}, query);
    },
    saveMonster: function (body) {
      return request('/back/game/battle/monster/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeMonster: function (id) {
      return request('/back/game/battle/monster/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    waveList: function (query) {
      return pageList('/back/game/battle/wave/list', {}, query);
    },
    saveWave: function (body) {
      return request('/back/game/battle/wave/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeWave: function (id) {
      return request('/back/game/battle/wave/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    waveMonsterList: function (waveId) {
      return pageList('/back/game/battle/wave-monster/list', { waveId: waveId }, { current: 1, size: 100 });
    },
    saveWaveMonster: function (body) {
      return request('/back/game/battle/wave-monster/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeWaveMonster: function (id) {
      return request('/back/game/battle/wave-monster/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    stageBattleDetail: function (stageId) {
      return request('/back/game/battle/stage/detail', { body: { stageId: stageId } }).then(ensureSuccess);
    },
    itemList: function (query) {
      return pageList('/back/game/item/list', {}, query);
    },
    saveItem: function (body) {
      return request('/back/game/item/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeItem: function (id) {
      return request('/back/game/item/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    itemTagOptions: function () {
      return request('/back/game/item/tag/options', {}).then(ensureSuccess);
    },
    dropList: function (query, filterBody) {
      return pageList('/back/game/item/drop/list', filterBody || {}, query);
    },
    saveDrop: function (body) {
      return request('/back/game/item/drop/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeDrop: function (id) {
      return request('/back/game/item/drop/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    craftRecipeList: function (query) {
      return pageList('/back/game/craft/recipe/list', {}, query);
    },
    craftRecipeDetail: function (recipeId) {
      return request('/back/game/craft/recipe/detail', { body: { recipeId: recipeId } }).then(ensureSuccess);
    },
    saveCraftRecipe: function (body) {
      return request('/back/game/craft/recipe/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeCraftRecipe: function (id) {
      return request('/back/game/craft/recipe/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    triggerList: function (query, filterBody) {
      return pageList('/back/game/trigger/list', filterBody || {}, query);
    },
    triggerListByItem: function (itemId) {
      return request('/back/game/trigger/by-item', { body: { itemId: itemId } }).then(ensureSuccess);
    },
    saveTrigger: function (body) {
      return request('/back/game/trigger/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeTrigger: function (id) {
      return request('/back/game/trigger/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    triggerPhaseOptions: function () {
      return request('/back/game/trigger/phase/options', {}).then(ensureSuccess);
    },
    skillList: function (query) {
      return pageList('/back/game/skill/list', {}, query);
    },
    skillDetail: function (skillId) {
      return request('/back/game/skill/detail', { body: { skillId: skillId } }).then(ensureSuccess);
    },
    saveSkill: function (body) {
      return request('/back/game/skill/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeSkill: function (id) {
      return request('/back/game/skill/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    skillEffectOptions: function () {
      return request('/back/game/skill/effect/options', {}).then(ensureSuccess);
    },
    skillTargetOptions: function () {
      return request('/back/game/skill/target/options', {}).then(ensureSuccess);
    }
  };
})(window);
