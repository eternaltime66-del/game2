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
    }
  };
})(window);
