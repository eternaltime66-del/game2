/**
 * 后台自定义下拉框 AdminSelect（Vue 3 组件）
 * 用法:
 *   <admin-select v-model="form.enabled" :options="yesNoOptions" />
 *   <admin-select v-model="form.type" :options="meta.list" value-key="code" label-key="label" searchable />
 */
(function (window) {
  var AdminSelect = {
    name: 'AdminSelect',
    props: {
      modelValue: { default: null },
      options: { type: Array, default: function () { return []; } },
      valueKey: { type: String, default: 'value' },
      labelKey: { type: String, default: 'label' },
      placeholder: { type: String, default: '请选择' },
      searchable: { type: Boolean, default: false },
      disabled: { type: Boolean, default: false },
      clearable: { type: Boolean, default: false }
    },
    emits: ['update:modelValue', 'change'],
    data: function () {
      return {
        open: false,
        query: '',
        popover: { top: 0, left: 0, width: 240, maxHeight: 280 }
      };
    },
    computed: {
      normalized: function () {
        var vk = this.valueKey;
        var lk = this.labelKey;
        return (this.options || []).map(function (raw, idx) {
          if (raw == null) return null;
          if (typeof raw === 'string' || typeof raw === 'number') {
            return { value: raw, label: String(raw), group: '', hint: '', raw: raw };
          }
          var val = raw[vk] != null ? raw[vk] : (raw.value != null ? raw.value : raw.code != null ? raw.code : raw.id);
          var lab = raw[lk] != null ? raw[lk] : (raw.label != null ? raw.label : raw.name != null ? raw.name : String(val));
          var grp = raw.group != null ? raw.group : (raw.groupLabel != null ? raw.groupLabel : '');
          var hint = raw.hint != null ? raw.hint : (raw.title != null ? raw.title : (raw.tooltip != null ? raw.tooltip : ''));
          return { value: val, label: lab, group: grp || '', hint: hint || '', raw: raw };
        }).filter(Boolean);
      },
      filtered: function () {
        var q = (this.query || '').trim().toLowerCase();
        if (!q) return this.normalized;
        return this.normalized.filter(function (o) {
          return String(o.label).toLowerCase().indexOf(q) >= 0
            || String(o.value).toLowerCase().indexOf(q) >= 0
            || String(o.group || '').toLowerCase().indexOf(q) >= 0;
        });
      },
      filteredGroups: function () {
        var list = this.filtered;
        var order = [];
        var map = {};
        list.forEach(function (o) {
          var g = o.group || '';
          if (!map[g]) {
            map[g] = { label: g, options: [] };
            order.push(map[g]);
          }
          map[g].options.push(o);
        });
        return order;
      },
      selected: function () {
        var mv = this.modelValue;
        if (mv === null || mv === undefined || mv === '') return null;
        return this.normalized.find(function (o) { return AdminSelect.sameValue(o.value, mv); }) || null;
      },
      displayLabel: function () {
        if (!this.selected) return '';
        var g = this.selected.group;
        return g ? (g + ' · ' + this.selected.label) : this.selected.label;
      },
      autoSearch: function () {
        return this.searchable || this.normalized.length > 8;
      }
    },
    watch: {
      open: function (val) {
        if (val) {
          this.query = '';
          this.$nextTick(this.updatePopover);
        }
      }
    },
    mounted: function () {
      this._onDoc = this.onDocClick.bind(this);
      this._onWin = this.updatePopover.bind(this);
      document.addEventListener('click', this._onDoc, true);
      window.addEventListener('resize', this._onWin);
      window.addEventListener('scroll', this._onWin, true);
    },
    unmounted: function () {
      document.removeEventListener('click', this._onDoc, true);
      window.removeEventListener('resize', this._onWin);
      window.removeEventListener('scroll', this._onWin, true);
    },
    methods: {
      sameValue: function (a, b) {
        return AdminSelect.sameValue(a, b);
      },
      toggle: function () {
        if (this.disabled) return;
        this.open = !this.open;
      },
      close: function () {
        this.open = false;
      },
      onDocClick: function (e) {
        if (!this.open) return;
        var el = this.$refs.root;
        var pop = this.$refs.popover;
        if (el && el.contains(e.target)) return;
        if (pop && pop.contains(e.target)) return;
        this.close();
      },
      updatePopover: function () {
        var el = this.$refs.root;
        if (!el) return;
        var rect = el.getBoundingClientRect();
        var gap = 6;
        var maxH = 280;
        var spaceBelow = window.innerHeight - rect.bottom - gap;
        var spaceAbove = rect.top - gap;
        var openUp = spaceBelow < 180 && spaceAbove > spaceBelow;
        var height = Math.min(maxH, openUp ? spaceAbove : spaceBelow);
        if (height < 120) height = Math.min(maxH, Math.max(spaceBelow, spaceAbove, 120));
        this.popover = {
          top: openUp ? rect.top - gap - height : rect.bottom + gap,
          left: rect.left,
          width: rect.width,
          maxHeight: height,
          openUp: openUp
        };
      },
      pick: function (opt) {
        this.$emit('update:modelValue', opt.value);
        this.$emit('change', opt.value);
        this.close();
      },
      clear: function (e) {
        e.stopPropagation();
        this.$emit('update:modelValue', '');
        this.$emit('change', '');
      }
    },
    template: ''
      + '<div class="admin-select" :class="{ open: open, disabled: disabled, \'has-value\': !!selected }" ref="root">'
      + '  <div class="admin-select-trigger" :class="{ disabled: disabled }" tabindex="0" role="combobox" :aria-expanded="open" :title="selected && selected.hint ? selected.hint : undefined" @click="toggle" @keydown.enter.prevent="toggle" @keydown.space.prevent="toggle">'
      + '    <span class="admin-select-value" :class="{ placeholder: !selected }">{{ selected ? displayLabel : placeholder }}</span>'
      + '    <span class="admin-select-icons">'
      + '      <span v-if="clearable && selected && !disabled" class="admin-select-clear" role="button" tabindex="-1" @click.stop="clear" title="清除"><i class="admin-select-clear-x" aria-hidden="true"></i></span>'
      + '      <span class="admin-select-chevron" aria-hidden="true"></span>'
      + '    </span>'
      + '  </div>'
      + '  <teleport to="body">'
      + '    <div v-if="open" ref="popover" class="admin-select-popover" :style="{ top: popover.top + \'px\', left: popover.left + \'px\', width: popover.width + \'px\', maxHeight: popover.maxHeight + \'px\' }">'
      + '      <div v-if="autoSearch" class="admin-select-search-wrap">'
      + '        <input type="text" class="admin-select-search" v-model="query" placeholder="搜索…" @click.stop />'
      + '      </div>'
      + '      <div class="admin-select-list" role="listbox">'
      + '        <template v-for="(g, gi) in filteredGroups" :key="\'g-\' + gi + \'-\' + (g.label || \'\')">'
      + '          <div v-if="g.label" class="admin-select-group">{{ g.label }}</div>'
      + '          <button v-for="(opt, idx) in g.options" :key="opt.value + \'-\' + idx" type="button" class="admin-select-option" :class="{ active: sameValue(opt.value, modelValue) }" :title="opt.hint || undefined" @click="pick(opt)">'
      + '            <span class="admin-select-option-label">{{ opt.label }}</span>'
      + '            <span v-if="sameValue(opt.value, modelValue)" class="admin-select-check">✓</span>'
      + '          </button>'
      + '        </template>'
      + '        <div v-if="!filtered.length" class="admin-select-empty">无匹配项</div>'
      + '      </div>'
      + '    </div>'
      + '  </teleport>'
      + '</div>'
  };

  AdminSelect.sameValue = function (a, b) {
    if (a === b) return true;
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    return String(a) === String(b);
  };

  window.AdminSelectComponent = AdminSelect;
})(window);
