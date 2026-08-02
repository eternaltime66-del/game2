(function () {
  var host = location.hostname;
  if (host !== 'localhost' && host !== '127.0.0.1') {
    return;
  }

  var script = document.createElement('script');
  script.src = 'http://' + host + ':35729/livereload.js?snipver=1';
  script.async = true;
  script.onload = function () {
    console.info('[dev-live] LiveReload 已连接 (静态资源保存后自动刷新)');
  };
  script.onerror = function () {
    console.warn('[dev-live] LiveReload 未连接，请用 dev 配置启动应用 (run-dev.cmd 或 Spring Boot dev 启动项)');
  };
  document.head.appendChild(script);
})();
