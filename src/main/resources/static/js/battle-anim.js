(function (window) {
  var TICK_MS = 42;

  function sleep(ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  }

  function cloneUnits(units) {
    return (units || []).map(function (u) {
      return Object.assign({}, u);
    });
  }

  function findAttacker(oldMap, newUnits) {
    var candidate = null;
    newUnits.forEach(function (u) {
      var old = oldMap[u.unitId];
      if (!old || !u.alive || !old.alive) return;
      if (u.actionBar === 0 && old.actionBar > 0 && u.hp === old.hp) {
        candidate = u;
      }
    });
    return candidate;
  }

  function findHpDrops(oldMap, newUnits) {
    return newUnits.filter(function (u) {
      var old = oldMap[u.unitId];
      return old && u.hp < old.hp;
    });
  }

  window.BattleAnim = {
    TICK_MS: TICK_MS,

    playStep: function (vm, oldBattle, newBattle) {
      var tickGain = (newBattle && newBattle.actionTickGain) || 1;
      var oldMap = {};
      cloneUnits(oldBattle.units).forEach(function (u) {
        oldMap[u.unitId] = u;
      });

      var attacker = findAttacker(oldMap, newBattle.units);
      var hpDrops = findHpDrops(oldMap, newBattle.units);

      var display = newBattle.units.map(function (u) {
        var old = oldMap[u.unitId];
        return Object.assign({}, u, {
          actionBar: old && old.alive ? (old.actionBar || 0) : (u.actionBar || 0),
          hp: old ? old.hp : u.hp,
          alive: old ? old.alive : u.alive
        });
      });

      var growTargets = {};
      newBattle.units.forEach(function (u) {
        if (attacker && u.unitId === attacker.unitId) {
          growTargets[u.unitId] = u.actionValue || 0;
        } else {
          growTargets[u.unitId] = u.actionBar || 0;
        }
      });

      vm.animating = true;
      vm.actingUnitId = '';
      vm.hitUnitId = '';
      vm.growingUnitIds = display.filter(function (u) { return u.alive; }).map(function (u) { return u.unitId; });
      vm.renderUnits = display;

      function syncRender() {
        vm.renderUnits = display.map(function (u) { return Object.assign({}, u); });
      }

      function growPhase() {
        var stillGrowing = false;
        var attackerReady = false;

        display.forEach(function (u) {
          if (!u.alive) return;
          var target = growTargets[u.unitId];
          if (u.actionBar < target) {
            u.actionBar = Math.min(target, u.actionBar + tickGain);
            stillGrowing = true;
          }
          if (attacker && u.unitId === attacker.unitId && u.actionBar >= (u.actionValue || 0)) {
            attackerReady = true;
          }
        });

        syncRender();

        if (stillGrowing && !(attacker && attackerReady)) {
          setTimeout(growPhase, TICK_MS);
          return;
        }

        if (attacker && attackerReady) {
          attackPhase();
          return;
        }

        finish();
      }

      function attackPhase() {
        vm.actingUnitId = attacker.unitId;
        vm.growingUnitIds = [];

        setTimeout(function () {
          var hitPromises = hpDrops.map(function (target) {
            var old = oldMap[target.unitId];
            var displayUnit = display.find(function (u) { return u.unitId === target.unitId; });
            if (!displayUnit || !old) return Promise.resolve();

            vm.hitUnitId = target.unitId;
            return animateHp(displayUnit, old.hp, target.hp).then(function () {
              displayUnit.hp = target.hp;
              displayUnit.alive = target.alive;
              syncRender();
            });
          });

          Promise.all(hitPromises).then(function () {
            var actorDisplay = display.find(function (u) { return u.unitId === attacker.unitId; });
            if (actorDisplay) {
              actorDisplay.actionBar = 0;
            }

            display.forEach(function (u) {
              var finalUnit = newBattle.units.find(function (x) { return x.unitId === u.unitId; });
              if (!finalUnit) return;
              if (attacker && u.unitId === attacker.unitId) return;
              u.actionBar = finalUnit.actionBar;
              u.hp = finalUnit.hp;
              u.alive = finalUnit.alive;
            });

            vm.actingUnitId = '';
            vm.hitUnitId = '';
            syncRender();

            setTimeout(finish, 120);
          });
        }, 260);
      }

      function animateHp(unit, fromHp, toHp) {
        var steps = 8;
        var step = 0;
        return new Promise(function (resolve) {
          function tick() {
            step++;
            var t = step / steps;
            unit.hp = Math.round(fromHp + (toHp - fromHp) * t);
            syncRender();
            if (step < steps) {
              setTimeout(tick, 35);
            } else {
              unit.hp = toHp;
              syncRender();
              resolve();
            }
          }
          tick();
        });
      }

      function finish() {
        vm.battle = newBattle;
        vm.renderUnits = cloneUnits(newBattle.units);
        vm.animating = false;
        vm.actingUnitId = '';
        vm.hitUnitId = '';
        vm.growingUnitIds = [];
      }

      return new Promise(function (resolve) {
        var origFinish = finish;
        finish = function () {
          origFinish();
          resolve();
        };
        growPhase();
      });
    }
  };
})(window);
