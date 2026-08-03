(function (window) {
  function isImageIcon(icon) {
    if (!icon) return false;
    var s = String(icon);
    return s.indexOf('/') === 0 || s.indexOf('http://') === 0 || s.indexOf('https://') === 0 || /\.(png|jpg|jpeg|webp|gif|svg)$/i.test(s);
  }

  window.ItemIcon = {
    isImage: isImageIcon,
    src: function (icon) {
      return isImageIcon(icon) ? icon : '';
    },
    text: function (icon) {
      return isImageIcon(icon) ? '' : (icon || '📦');
    }
  };
})(window);
