(function () {
  var host = location.hostname;
  if (host !== 'localhost' && host !== '127.0.0.1') {
    return;
  }

  var script = document.createElement('script');
  script.src = 'http://' + host + ':35729/livereload.js?snipver=1';
  script.async = true;
  document.head.appendChild(script);
})();
