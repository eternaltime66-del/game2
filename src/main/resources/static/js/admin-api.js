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
    levelTree: function () {
      return request('/back/game/level/tree', {}).then(ensureSuccess);
    },
    saveMode: function (body) {
      return request('/back/game/level/mode/save', { json: true, body: body }).then(ensureSuccess);
    },
    saveChapter: function (body) {
      return request('/back/game/level/chapter/save', { json: true, body: body }).then(ensureSuccess);
    },
    saveStageGroup: function (body) {
      return request('/back/game/level/stage-group/save', { json: true, body: body }).then(ensureSuccess);
    },
    saveStage: function (body) {
      return request('/back/game/level/stage/save', { json: true, body: body }).then(ensureSuccess);
    }
  };
})(window);
