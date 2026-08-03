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
      query: query || { current: 1, size: 50 }
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
      return request('/back/member/list', {
        json: true,
        body: { memberRole: 'USER' },
        query: query || { current: 1, size: 20 }
      }).then(ensureSuccess);
    },
    loginAsUser: function (uid) {
      return request('/back/member/login/as', {
        json: true,
        body: { id: uid }
      }).then(ensureSuccess);
    },
    resetMemberGame: function (uid) {
      return request('/back/member/reset/game', {
        json: true,
        body: { id: uid }
      }).then(ensureSuccess);
    },
    grantMemberItem: function (body) {
      return request('/back/member/grant/item', {
        json: true,
        body: body
      }).then(ensureSuccess);
    },
    levelTree: function () {
      return request('/back/game/level/tree', {}).then(ensureSuccess);
    },
    saveMode: function (body) {
      return request('/back/game/level/mode/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeMode: function (id) {
      return request('/back/game/level/mode/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    saveChapter: function (body) {
      return request('/back/game/level/chapter/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeChapter: function (id) {
      return request('/back/game/level/chapter/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    saveStageGroup: function (body) {
      return request('/back/game/level/stage-group/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeStageGroup: function (id) {
      return request('/back/game/level/stage-group/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    saveStage: function (body) {
      return request('/back/game/level/stage/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeStage: function (id) {
      return request('/back/game/level/stage/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    monsterList: function (query) {
      return request('/back/game/battle/monster/list', {
        json: true,
        body: {},
        query: query || { current: 1, size: 50 }
      }).then(ensureSuccess);
    },
    saveMonster: function (body) {
      return request('/back/game/battle/monster/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeMonster: function (id) {
      return request('/back/game/battle/monster/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    saveWave: function (body) {
      return request('/back/game/battle/wave/save', { json: true, body: body }).then(ensureSuccess);
    },
    saveWaveMonster: function (body) {
      return request('/back/game/battle/wave-monster/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeWaveMonster: function (id) {
      return request('/back/game/battle/wave-monster/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    autoPlaceWaveMonsters: function (waveId) {
      return request('/back/game/battle/wave-monster/auto-place', { body: { waveId: waveId } }).then(ensureSuccess);
    },
    stageBattleDetail: function (stageId) {
      return request('/back/game/battle/stage/detail', { body: { stageId: stageId } }).then(ensureSuccess);
    },
    itemList: function (query) {
      return pageList('/back/game/item/all/list', {}, query);
    },
    allItemList: function () {
      return request('/back/game/item/all/list', {}).then(ensureSuccess);
    },
    materialList: function (query) {
      return pageList('/back/game/item/material/list', {}, query);
    },
    saveMaterial: function (body) {
      return request('/back/game/item/material/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeMaterial: function (id) {
      return request('/back/game/item/material/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    weaponList: function (query) {
      return pageList('/back/game/item/weapon/list', {}, query);
    },
    saveWeapon: function (body) {
      return request('/back/game/item/weapon/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeWeapon: function (id) {
      return request('/back/game/item/weapon/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    armorList: function (query) {
      return pageList('/back/game/item/armor/list', {}, query);
    },
    saveArmor: function (body) {
      return request('/back/game/item/armor/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeArmor: function (id) {
      return request('/back/game/item/armor/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    glovesList: function (query) {
      return pageList('/back/game/item/gloves/list', {}, query);
    },
    saveGloves: function (body) {
      return request('/back/game/item/gloves/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeGloves: function (id) {
      return request('/back/game/item/gloves/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    helmetList: function (query) {
      return pageList('/back/game/item/helmet/list', {}, query);
    },
    saveHelmet: function (body) {
      return request('/back/game/item/helmet/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeHelmet: function (id) {
      return request('/back/game/item/helmet/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    legsList: function (query) {
      return pageList('/back/game/item/legs/list', {}, query);
    },
    saveLegs: function (body) {
      return request('/back/game/item/legs/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeLegs: function (id) {
      return request('/back/game/item/legs/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    accessoryList: function (query) {
      return pageList('/back/game/item/accessory/list', {}, query);
    },
    saveAccessory: function (body) {
      return request('/back/game/item/accessory/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeAccessory: function (id) {
      return request('/back/game/item/accessory/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    saveItem: function (body) {
      return request('/back/game/item/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeItem: function (id) {
      return request('/back/game/item/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    recipeList: function (query) {
      return pageList('/back/game/recipe/list', {}, query);
    },
    recipeDetail: function (id) {
      return request('/back/game/recipe/detail', { body: { id: id } }).then(ensureSuccess);
    },
    saveRecipe: function (body) {
      return request('/back/game/recipe/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeRecipe: function (id) {
      return request('/back/game/recipe/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    itemTagOptions: function () {
      return request('/back/game/item/tag/options', {}).then(ensureSuccess);
    },
    finishedSkillList: function (query, filter) {
      return pageList('/back/game/trigger-v2/finished-skill/list', filter || {}, query);
    },
    finishedSkillDetail: function (id) {
      return request('/back/game/trigger-v2/finished-skill/detail', { body: { id: id } }).then(ensureSuccess);
    },
    saveFinishedSkill: function (body) {
      return request('/back/game/trigger-v2/finished-skill/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeFinishedSkill: function (id) {
      return request('/back/game/trigger-v2/finished-skill/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    passiveSkillMeta: function () {
      return request('/back/game/passive-skill/meta', {}).then(ensureSuccess);
    },
    passiveSkillList: function (query, filter) {
      return pageList('/back/game/passive-skill/list', filter || {}, query);
    },
    savePassiveSkill: function (body) {
      return request('/back/game/passive-skill/save', { json: true, body: body }).then(ensureSuccess);
    },
    removePassiveSkill: function (id) {
      return request('/back/game/passive-skill/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    skillBadgeList: function (query) {
      return pageList('/back/game/passive-skill/badge/list', {}, query);
    },
    saveSkillBadge: function (body) {
      return request('/back/game/passive-skill/badge/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeSkillBadge: function (id) {
      return request('/back/game/passive-skill/badge/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    monsterPassiveByMonster: function (monsterId) {
      return request('/back/game/passive-skill/monster-passive/by-monster', { body: { monsterId: monsterId } }).then(ensureSuccess);
    },
    saveMonsterPassive: function (body) {
      return request('/back/game/passive-skill/monster-passive/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeMonsterPassive: function (id) {
      return request('/back/game/passive-skill/monster-passive/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    itemPassiveByItem: function (itemId) {
      return request('/back/game/passive-skill/item-passive/by-item', { body: { itemId: itemId } }).then(ensureSuccess);
    },
    saveItemPassive: function (body) {
      return request('/back/game/passive-skill/item-passive/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeItemPassive: function (id) {
      return request('/back/game/passive-skill/item-passive/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    triggerSlotList: function (query, filter) {
      return pageList('/back/game/trigger-v2/trigger-slot/list', filter || {}, query);
    },
    triggerSlotByMonster: function (monsterId) {
      return request('/back/game/trigger-v2/trigger-slot/by-monster', { body: { monsterId: monsterId } }).then(ensureSuccess);
    },
    triggerSlotByItem: function (itemId) {
      return request('/back/game/trigger-v2/trigger-slot/by-item', { body: { itemId: itemId } }).then(ensureSuccess);
    },
    saveTriggerSlot: function (body) {
      return request('/back/game/trigger-v2/trigger-slot/save', { json: true, body: body }).then(ensureSuccess);
    },
    removeTriggerSlot: function (id) {
      return request('/back/game/trigger-v2/trigger-slot/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    v2TriggerTypes: function () {
      return request('/back/game/trigger-v2/meta/trigger-types', {}).then(ensureSuccess);
    },
    v2TargetTypes: function () {
      return request('/back/game/trigger-v2/meta/target-types', {}).then(ensureSuccess);
    },
    v2EffectKinds: function () {
      return request('/back/game/trigger-v2/meta/effect-kinds', {}).then(ensureSuccess);
    },
    v2StatRefs: function () {
      return request('/back/game/trigger-v2/meta/stat-refs', {}).then(ensureSuccess);
    },
    v2OutcomeTypes: function () {
      return request('/back/game/trigger-v2/meta/outcome-types', {}).then(ensureSuccess);
    },
    v2BindTypes: function () {
      return request('/back/game/trigger-v2/meta/bind-types', {}).then(ensureSuccess);
    },
    v2FinishedSkillOptions: function () {
      return request('/back/game/trigger-v2/meta/finished-skill-options', {}).then(ensureSuccess);
    },
    v2FinishedSkillCategories: function () {
      return request('/back/game/trigger-v2/meta/finished-skill-categories', {}).then(ensureSuccess);
    },
    characterTemplateList: function () {
      return request('/back/game/character/template/list', {}).then(ensureSuccess);
    },
    characterTemplateSave: function (body) {
      return request('/back/game/character/template/save', { json: true, body: body }).then(ensureSuccess);
    },
    professionList: function () {
      return request('/back/game/character/profession/list', {}).then(ensureSuccess);
    },
    professionSave: function (body) {
      return request('/back/game/character/profession/save', { json: true, body: body }).then(ensureSuccess);
    },
    professionRemove: function (id) {
      return request('/back/game/character/profession/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    },
    professionSkillList: function (professionId) {
      return request('/back/game/character/profession/skill/list', {
        body: { professionId: professionId }
      }).then(ensureSuccess);
    },
    professionSkillSave: function (body) {
      return request('/back/game/character/profession/skill/save', { json: true, body: body }).then(ensureSuccess);
    },
    professionSkillRemove: function (id) {
      return request('/back/game/character/profession/skill/remove', { json: true, body: { id: id } }).then(ensureSuccess);
    }
  };
})(window);
