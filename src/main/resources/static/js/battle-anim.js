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

  function findAttacker(oldMap, newUnits, newBattle) {
    var logs = (newBattle && newBattle.logs) || [];
    var i;
    for (i = logs.length - 1; i >= 0; i--) {
      var log = logs[i];
      if (log.type !== 'ACTION' && log.type !== 'SKILL') continue;
      var matched = matchLogActor(log, newUnits);
      if (matched) return matched;
    }

    var candidate = null;
    newUnits.forEach(function (u) {
      var old = oldMap[u.unitId];
      if (!old || !u.alive || !old.alive) return;
      if (u.actionBar === 0 && old.actionBar > 0 && u.hp === old.hp) {
        candidate = u;
      }
    });
    if (candidate) return candidate;

    // 首动：旧状态行动条为 0，出手后仍为 0，其它单位行动条有增长
    var resetAlive = newUnits.filter(function (u) {
      var old = oldMap[u.unitId];
      return old && u.alive && old.alive && u.actionBar === 0 && u.hp === old.hp
        && (old.actionBar || 0) === 0;
    });
    if (resetAlive.length === 1) {
      var acted = resetAlive[0];
      var othersGrew = newUnits.some(function (u) {
        if (u.unitId === acted.unitId || !u.alive) return false;
        var old = oldMap[u.unitId];
        return old && (u.actionBar || 0) > (old.actionBar || 0);
      });
      if (othersGrew) return acted;
    }

    return null;
  }

  function matchLogActor(log, units) {
    if (!log) return null;
    if (log.actorName) {
      var named = null;
      units.forEach(function (u) {
        if (u.name === log.actorName) named = u;
      });
      if (named) return named;
    }
    if (!log.text) return null;
    var matched = null;
    units.forEach(function (u) {
      if (u.name && log.text.indexOf(u.name) === 0) {
        matched = u;
      }
    });
    return matched;
  }

  function findHpDrops(oldMap, newUnits) {
    var drops = [];
    var newIds = {};

    newUnits.forEach(function (u) {
      newIds[u.unitId] = true;
      var old = oldMap[u.unitId];
      if (old && old.alive && u.hp < old.hp) {
        drops.push(u);
      }
    });

    Object.keys(oldMap).forEach(function (id) {
      if (newIds[id]) return;
      var old = oldMap[id];
      if (!old || !old.alive || old.side !== 'MONSTER') return;
      drops.push({
        unitId: old.unitId,
        side: old.side,
        name: old.name,
        hp: 0,
        maxHp: old.maxHp,
        actionValue: old.actionValue,
        actionBar: old.actionBar,
        alive: false
      });
    });

    return drops;
  }

  function victimIdSet(hpDrops) {
    var ids = {};
    hpDrops.forEach(function (t) {
      ids[t.unitId] = true;
    });
    return ids;
  }

  function findActionDamage(newBattle, target, oldUnit) {
    var logs = newBattle.logs || [];
    for (var i = logs.length - 1; i >= 0; i--) {
      var log = logs[i];
      if (log.type !== 'ACTION' && log.type !== 'SKILL') continue;
      if (log.targetName === target.name && log.damage != null) {
        return String(log.damage);
      }
    }
    if (oldUnit && target.hp != null) {
      return String(Math.max(0, oldUnit.hp - target.hp));
    }
    return '0';
  }

  window.BattleAnim = {
    TICK_MS: TICK_MS,

    playStep: function (vm, oldBattle, newBattle) {
      var tickGain = (newBattle && newBattle.actionTickGain) || 1;
      var oldMap = {};
      cloneUnits(oldBattle.units).forEach(function (u) {
        oldMap[u.unitId] = u;
      });

      var attacker = findAttacker(oldMap, newBattle.units, newBattle);
      var hpDrops = findHpDrops(oldMap, newBattle.units);
      var victims = victimIdSet(hpDrops);

      var display = newBattle.units.map(function (u) {
        var old = oldMap[u.unitId];
        var tookDamage = old && old.alive && u.hp < old.hp;
        return Object.assign({}, u, {
          actionBar: old && old.alive ? (old.actionBar || 0) : (u.actionBar || 0),
          hp: old ? old.hp : u.hp,
          alive: tookDamage ? true : (old ? old.alive : u.alive)
        });
      });

      hpDrops.forEach(function (target) {
        if (display.some(function (u) { return u.unitId === target.unitId; })) return;
        var old = oldMap[target.unitId];
        if (!old) return;
        display.push(Object.assign({}, old, {
          hp: old.hp,
          alive: true,
          actionBar: old.actionBar || 0
        }));
      });

      var growTargets = {};
      display.forEach(function (u) {
        var finalUnit = newBattle.units.find(function (x) { return x.unitId === u.unitId; });
        if (!finalUnit) {
          growTargets[u.unitId] = u.actionBar || 0;
          return;
        }
        if (attacker && u.unitId === attacker.unitId) {
          growTargets[u.unitId] = finalUnit.actionValue || 0;
        } else {
          growTargets[u.unitId] = finalUnit.actionBar || 0;
        }
      });

      vm.animating = true;
      vm.actingUnitId = '';
      vm.hitUnitId = '';
      vm.dyingUnitIds = [];
      vm.growingUnitIds = display.filter(function (u) {
        return u.alive && !victims[u.unitId];
      }).map(function (u) { return u.unitId; });
      vm.renderUnits = display;

      function syncRender() {
        vm.renderUnits = display.map(function (u) { return Object.assign({}, u); });
      }

      function aborted() {
        return vm.skipRequested;
      }

      function growPhase() {
        if (aborted()) {
          finish();
          return;
        }
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
        if (aborted()) {
          finish();
          return;
        }
        vm.actingUnitId = attacker.unitId;
        vm.growingUnitIds = [];

        setTimeout(function () {
          if (aborted()) {
            finish();
            return;
          }
          var hitPromises = hpDrops.map(function (target) {
            var old = oldMap[target.unitId];
            var displayUnit = display.find(function (u) { return u.unitId === target.unitId; });
            if (!displayUnit || !old) return Promise.resolve();

            vm.hitUnitId = target.unitId;
            if (vm.spawnDamageFloat) {
              vm.spawnDamageFloat(target.unitId, findActionDamage(newBattle, target, old));
            }
            return animateHp(displayUnit, old.hp, target.hp).then(function () {
              displayUnit.hp = target.hp;
              if (target.hp <= 0 || target.alive === false) {
                displayUnit.alive = false;
                if (vm.dyingUnitIds.indexOf(target.unitId) < 0) {
                  vm.dyingUnitIds.push(target.unitId);
                }
                syncRender();
                return sleep(380);
              }
              displayUnit.alive = target.alive;
              syncRender();
            });
          });

          Promise.all(hitPromises).then(function () {
            if (aborted()) {
              finish();
              return;
            }
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

            setTimeout(function () {
              vm.dyingUnitIds = [];
              finish();
            }, 120);
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
        if (!aborted()) {
          vm.battle = newBattle;
          vm.renderUnits = cloneUnits(newBattle.units);
        }
        vm.animating = false;
        vm.actingUnitId = '';
        vm.hitUnitId = '';
        vm.growingUnitIds = [];
        vm.dyingUnitIds = [];
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
