(function (window) {
  var cfg = window.APP_CONFIG || {};

  function getToken() {
    return localStorage.getItem(cfg.tokenKey || 'token') || '';
  }

  function saveToken(token) {
    if (token) {
      localStorage.setItem(cfg.tokenKey || 'token', token);
    }
  }

  function clearToken() {
    localStorage.removeItem(cfg.tokenKey || 'token');
  }

  function request(path, params) {
    var body = new URLSearchParams();
    Object.keys(params || {}).forEach(function (key) {
      if (params[key] !== undefined && params[key] !== null) {
        body.append(key, params[key]);
      }
    });

    return fetch((cfg.apiBase || '') + path, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        token: getToken()
      },
      body: body.toString()
    }).then(function (res) {
      return res.json();
    });
  }

  function ensureSuccess(result) {
    if (!result || result.success === false) {
      var err = new Error((result && result.msg) || '请求失败');
      err.result = result;
      throw err;
    }
    return result;
  }

  window.GameApi = {
    getToken: getToken,
    saveToken: saveToken,
    clearToken: clearToken,
    sendEmailCode: function (email) {
      return request('/api/unit/send/code', {
        account: email,
        type: cfg.codeType
      }).then(ensureSuccess);
    },
    emailRegister: function (email, emsCode, psd, psdAgain) {
      return request('/api/account/email/register', {
        email: email,
        emsCode: emsCode,
        psd: psd,
        psdAgain: psdAgain
      }).then(ensureSuccess);
    },
    emailLogin: function (email, password) {
      return request('/api/account/email/login', {
        email: email,
        password: password
      }).then(ensureSuccess);
    },
    emailLoginByCode: function (email, emsCode) {
      return request('/api/account/email/login/code', {
        email: email,
        emsCode: emsCode
      }).then(ensureSuccess);
    },
    emailForget: function (email, emsCode, psd, psdAgain) {
      return request('/api/account/email/forget', {
        email: email,
        emsCode: emsCode,
        psd: psd,
        psdAgain: psdAgain
      }).then(ensureSuccess);
    },
    getHeroInfo: function () {
      return request('/api/game/hero/info', {}).then(ensureSuccess);
    },
    syncCharacters: function () {
      return request('/api/game/character/sync', {}).then(ensureSuccess);
    },
    getStageSelectList: function (chapterId) {
      return request('/api/game/level/stage/select/list', {
        chapterId: chapterId || 'chapter_main'
      }).then(ensureSuccess);
    },
    battleStart: function (stageId) {
      return request('/api/game/battle/start', {
        stageId: stageId
      }).then(ensureSuccess);
    },
    battleNext: function (battleId) {
      return request('/api/game/battle/next', {
        battleId: battleId
      }).then(ensureSuccess);
    },
    battleSkip: function (battleId) {
      return request('/api/game/battle/skip', {
        battleId: battleId
      }).then(ensureSuccess);
    },
    battleMonsterDetail: function (monsterId, stageId) {
      return request('/api/game/battle/monster/detail', {
        monsterId: monsterId,
        stageId: stageId || ''
      }).then(ensureSuccess);
    },
    battleState: function (battleId) {
      return request('/api/game/battle/state', {
        battleId: battleId
      }).then(ensureSuccess);
    },
    getMaterialSource: function (itemId) {
      return request('/api/game/item/material/source', {
        itemId: itemId
      }).then(ensureSuccess);
    },
    getWarehouseInfo: function () {
      return request('/api/game/warehouse/info', {}).then(ensureSuccess);
    },
    discardWarehouseSlots: function (slotNos) {
      return request('/api/game/warehouse/discard', {
        slotNos: slotNos
      }).then(ensureSuccess);
    },
    getItemLogList: function () {
      return request('/api/game/warehouse/log/list', {}).then(ensureSuccess);
    },
    getItemDetail: function (itemId) {
      return request('/api/game/item/detail', {
        itemId: itemId
      }).then(ensureSuccess);
    },
    getPrepSummary: function () {
      return request('/api/game/prep/summary', {}).then(ensureSuccess);
    },
    getBattleBagInfo: function () {
      return request('/api/game/prep/bag/info', {}).then(ensureSuccess);
    },
    batchBagToWarehouse: function (bagIds) {
      return request('/api/game/prep/bag/to-warehouse', {
        bagIds: bagIds
      }).then(ensureSuccess);
    },
    batchWarehouseToBag: function (slotNos) {
      return request('/api/game/prep/warehouse/to-bag', {
        slotNos: slotNos
      }).then(ensureSuccess);
    },
    prepTransfer: function (fromType, fromKey, toType, toKey, quantity) {
      return request('/api/game/prep/transfer', {
        fromType: fromType,
        fromKey: fromKey,
        toType: toType,
        toKey: toKey,
        quantity: quantity
      }).then(ensureSuccess);
    },
    equipWeapon: function (itemId) {
      return request('/api/game/prep/equip/weapon', {
        itemId: itemId
      }).then(ensureSuccess);
    },
    unequipWeapon: function () {
      return request('/api/game/prep/unequip/weapon', {}).then(ensureSuccess);
    },
    equipSlot: function (slot, itemId) {
      return request('/api/game/prep/equip', {
        slot: slot,
        itemId: itemId
      }).then(ensureSuccess);
    },
    unequipSlot: function (slot) {
      return request('/api/game/prep/unequip', {
        slot: slot
      }).then(ensureSuccess);
    },
    getHeroFormation: function () {
      return request('/api/game/prep/formation/get', {}).then(ensureSuccess);
    },
    saveHeroFormation: function (slotCol, slotRow) {
      return request('/api/game/prep/formation/save', {
        slotCol: slotCol,
        slotRow: slotRow
      }).then(ensureSuccess);
    },
    getCraftList: function () {
      return request('/api/game/craft/list', {}).then(ensureSuccess);
    },
    executeCraft: function (recipeId) {
      return request('/api/game/craft/execute', {
        recipeId: recipeId
      }).then(ensureSuccess);
    },
    getItemDropSources: function (itemId) {
      return request('/api/game/item/drop-sources', {
        itemId: itemId
      }).then(ensureSuccess);
    },
    getLevelTree: function () {
      return request('/api/game/level/tree', {}).then(ensureSuccess);
    }
  };
})(window);
