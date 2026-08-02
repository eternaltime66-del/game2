(function (window) {
  var TICK_MS = 42;
  var ATTACK_WINDUP_MS = 260;
  var ATTACK_TAIL_MS = 280;
  var HP_STEP_MS = 35;

  function animDelay(vm, ms) {
    if (vm && vm.skipAnim) return 0;
    var speed = (vm && vm.battleSpeed) || 1;
    return Math.max(1, Math.round(ms / speed));
  }

  function shouldFastFinish(vm) {
    return !!(vm && vm.skipAnim);
  }

  function cloneUnits(units) {
    return (units || []).map(function (u) {
      return Object.assign({}, u);
    });
  }

  function indexUnits(units) {
    var map = {};
    (units || []).forEach(function (u) {
      map[u.unitId] = u;
    });
    return map;
  }

  function buildDisplay(oldBattle, newBattle, oldMap) {
    var newMap = indexUnits(newBattle.units);
    var seen = {};
    var display = [];

    (oldBattle.units || []).forEach(function (oldU) {
      seen[oldU.unitId] = true;
      var newU = newMap[oldU.unitId];
      display.push({
        unitId: oldU.unitId,
        side: oldU.side,
        name: oldU.name,
        maxHp: oldU.maxHp,
        attack: oldU.attack != null ? oldU.attack : (newU && newU.attack),
        actionValue: oldU.actionValue,
        monsterId: oldU.monsterId,
        actionBar: oldU.alive ? (oldU.actionBar || 0) : ((newU && newU.actionBar) || 0),
        hp: oldU.hp,
        alive: oldU.alive
      });
    });

    (newBattle.units || []).forEach(function (u) {
      if (seen[u.unitId]) return;
      var old = oldMap[u.unitId];
      display.push({
        unitId: u.unitId,
        side: u.side,
        name: u.name,
        maxHp: u.maxHp,
        attack: u.attack,
        actionValue: u.actionValue,
        monsterId: u.monsterId,
        actionBar: old && old.alive ? (old.actionBar || 0) : (u.actionBar || 0),
        hp: old ? old.hp : u.hp,
        alive: old ? old.alive : u.alive
      });
    });

    return display;
  }

  function findAttacker(oldMap, newUnits, newBattle) {
    var logs = (newBattle && newBattle.logs) || [];
    var i;
    for (i = logs.length - 1; i >= 0; i--) {
      var log = logs[i];
      if (log.type !== 'ACTION' || !log.actorName) continue;
      var matched = null;
      newUnits.forEach(function (u) {
        if (u.name === log.actorName) matched = u;
      });
      if (matched) return matched;
      Object.keys(oldMap).forEach(function (id) {
        if (oldMap[id].name === log.actorName) matched = oldMap[id];
      });
      if (matched) return matched;
    }

    var candidate = null;
    (newBattle.units || []).forEach(function (u) {
      var old = oldMap[u.unitId];
      if (!old || !u.alive || !old.alive) return;
      if (u.actionBar === 0 && old.actionBar > 0 && u.hp === old.hp) {
        candidate = u;
      }
    });
    return candidate;
  }

  function findHpDrops(oldMap, newBattle) {
    var newMap = indexUnits(newBattle.units);
    var drops = [];

    Object.keys(oldMap).forEach(function (unitId) {
      var old = oldMap[unitId];
      if (!old || !old.alive) return;
      var neu = newMap[unitId];
      if (neu && neu.hp < old.hp) {
        drops.push(neu);
        return;
      }
      if (!neu && old.side === 'MONSTER' && old.hp > 0) {
        drops.push(Object.assign({}, old, { hp: 0, alive: false }));
      }
    });

    return drops;
  }

  function findActionDamage(newBattle, target, oldUnit) {
    var logs = newBattle.logs || [];
    for (var i = logs.length - 1; i >= 0; i--) {
      var log = logs[i];
      if (log.type === 'ACTION' && log.targetName === target.name && log.damage != null) {
        return String(log.damage);
      }
    }
    if (oldUnit && target.hp != null) {
      return String(Math.max(0, oldUnit.hp - target.hp));
    }
    return '0';
  }

  function findAttackerUnit(display, attacker) {
    if (!attacker) return null;
    return display.find(function (u) { return u.unitId === attacker.unitId; }) || null;
  }

  window.BattleAnim = {
    TICK_MS: TICK_MS,

    playStep: function (vm, oldBattle, newBattle) {
      if (shouldFastFinish(vm)) {
        vm.battle = newBattle;
        vm.renderUnits = cloneUnits(newBattle.units);
        vm.animating = false;
        vm.actingUnitId = '';
        vm.hitUnitId = '';
        vm.growingUnitIds = [];
        return Promise.resolve();
      }

      var tickGain = (newBattle && newBattle.actionTickGain) || 1;
      var oldMap = indexUnits(oldBattle.units);

      var attacker = findAttacker(oldMap, newBattle.units, newBattle);
      var hpDrops = findHpDrops(oldMap, newBattle);
      var display = buildDisplay(oldBattle, newBattle, oldMap);

      var finalBars = {};
      (newBattle.units || []).forEach(function (u) {
        finalBars[u.unitId] = u.actionBar || 0;
      });

      vm.animating = true;
      vm.actingUnitId = '';
      vm.hitUnitId = '';
      vm.growingUnitIds = display.filter(function (u) { return u.alive; }).map(function (u) { return u.unitId; });
      vm.renderUnits = display;

      function syncRender() {
        vm.renderUnits = display.map(function (u) { return Object.assign({}, u); });
      }

      function syncFinalBars() {
        display.forEach(function (u) {
          var finalUnit = (newBattle.units || []).find(function (x) { return x.unitId === u.unitId; });
          if (finalUnit) {
            u.actionBar = finalUnit.actionBar || 0;
            u.hp = finalUnit.hp;
            u.alive = finalUnit.alive;
            return;
          }
          if (u.side === 'MONSTER' && u.hp > 0) {
            u.hp = 0;
          }
        });
      }

      function growPhase() {
        if (shouldFastFinish(vm)) {
          finish();
          return;
        }
        if (attacker) {
          var atkDisplay = findAttackerUnit(display, attacker);
          var atkMax = attacker.actionValue || 0;
          var anyGrew = false;

          display.forEach(function (u) {
            if (!u.alive) return;
            var cap = u.actionValue || 9999;
            if (u.actionBar < cap) {
              u.actionBar = Math.min(cap, u.actionBar + tickGain);
              anyGrew = true;
            }
          });

          syncRender();

          var attackerReady = atkDisplay && atkDisplay.actionBar >= atkMax;
          if (attackerReady) {
            attackPhase();
            return;
          }
          if (anyGrew) {
            setTimeout(growPhase, animDelay(vm, TICK_MS));
            return;
          }
          attackPhase();
          return;
        }

        var stillGrowing = false;
        display.forEach(function (u) {
          if (!u.alive) return;
          var target = finalBars[u.unitId] || 0;
          if (u.actionBar < target) {
            u.actionBar = Math.min(target, u.actionBar + tickGain);
            stillGrowing = true;
          }
        });

        syncRender();
        if (stillGrowing) {
          setTimeout(growPhase, animDelay(vm, TICK_MS));
          return;
        }
        finish();
      }

      function attackPhase() {
        if (shouldFastFinish(vm)) {
          finish();
          return;
        }
        vm.actingUnitId = attacker.unitId;
        vm.growingUnitIds = [];

        setTimeout(function () {
          if (shouldFastFinish(vm)) {
            finish();
            return;
          }
          var hitPromises = hpDrops.map(function (target) {
            var old = oldMap[target.unitId];
            var displayUnit = display.find(function (u) { return u.unitId === target.unitId; });
            if (!displayUnit || !old) return Promise.resolve();

            var toHp = target.hp != null ? target.hp : 0;
            vm.hitUnitId = target.unitId;
            if (vm.spawnDamageFloat) {
              vm.spawnDamageFloat(target.unitId, findActionDamage(newBattle, target, old));
            }
            return animateHp(displayUnit, old.hp, toHp).then(function () {
              displayUnit.hp = toHp;
              syncRender();
            });
          });

          Promise.all(hitPromises).then(function () {
            var actorDisplay = findAttackerUnit(display, attacker);
            if (actorDisplay) {
              actorDisplay.actionBar = 0;
            }
            syncFinalBars();
            vm.actingUnitId = '';
            vm.hitUnitId = '';
            syncRender();
            setTimeout(finish, animDelay(vm, ATTACK_TAIL_MS));
          });
        }, animDelay(vm, ATTACK_WINDUP_MS));
      }

      function animateHp(unit, fromHp, toHp) {
        if (shouldFastFinish(vm)) {
          unit.hp = toHp;
          syncRender();
          return Promise.resolve();
        }
        var steps = 8;
        var step = 0;
        return new Promise(function (resolve) {
          function tick() {
            if (shouldFastFinish(vm)) {
              unit.hp = toHp;
              syncRender();
              resolve();
              return;
            }
            step++;
            var t = step / steps;
            unit.hp = Math.round(fromHp + (toHp - fromHp) * t);
            syncRender();
            if (step < steps) {
              setTimeout(tick, animDelay(vm, HP_STEP_MS));
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
