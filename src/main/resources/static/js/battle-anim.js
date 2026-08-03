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



    var resetAlive = newUnits.filter(function (u) {

      var old = oldMap[u.unitId];

      return old && u.alive && old.alive && u.actionBar === 0 && u.hp === old.hp

        && (old.actionBar || 0) > 0;

    });

    if (resetAlive.length === 1) {

      return resetAlive[0];

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

    var i;

    for (i = logs.length - 1; i >= 0; i--) {

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

      function scaled(ms) {
        var speed = Math.max(1, vm.battleSpeed || 1);
        return Math.max(8, Math.round(ms / speed));
      }

      var oldMap = {};

      cloneUnits(oldBattle.units).forEach(function (u) {

        oldMap[u.unitId] = u;

      });



      var attacker = findAttacker(oldMap, newBattle.units, newBattle);

      var hpDrops = findHpDrops(oldMap, newBattle.units);

      var victims = victimIdSet(hpDrops);

      var isAoe = hpDrops.length > 1;



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



      vm.animating = true;

      vm.actingUnitId = '';

      vm.hitUnitId = '';

      vm.hitUnitIds = [];

      vm.dyingUnitIds = [];

      vm.growingUnitIds = [];

      vm.renderUnits = display;



      function syncRender() {

        vm.renderUnits = display.map(function (u) { return Object.assign({}, u); });

      }



      function aborted() {

        return vm.skipRequested;

      }



      function lockAttackerBarReady() {

        if (!attacker) return;

        var actorDisplay = display.find(function (u) { return u.unitId === attacker.unitId; });

        if (!actorDisplay) return;

        actorDisplay.actionBar = actorDisplay.actionValue || actorDisplay.actionBar || 0;

        syncRender();

      }



      function resetAttackerBarAfterAttack() {

        if (!attacker) return;

        var actorDisplay = display.find(function (u) { return u.unitId === attacker.unitId; });

        if (actorDisplay) {

          actorDisplay.actionBar = 0;

        }

      }



      function animateHp(unit, fromHp, toHp, steps) {

        steps = steps || 8;

        var step = 0;

        return new Promise(function (resolve) {

          function tick() {

            step++;

            var t = step / steps;

            unit.hp = Math.round(fromHp + (toHp - fromHp) * t);

            syncRender();

            if (step < steps) {

              setTimeout(tick, scaled(isAoe ? 24 : 35));

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

        vm.hitUnitIds = [];

        vm.growingUnitIds = [];

        vm.dyingUnitIds = [];

      }



      function growOthersPhase() {

        if (aborted()) {

          finish();

          return;

        }



        var stillGrowing = false;

        display.forEach(function (u) {

          var finalUnit = newBattle.units.find(function (x) { return x.unitId === u.unitId; });

          if (!finalUnit) return;



          if (attacker && u.unitId === attacker.unitId) {

            u.actionBar = finalUnit.actionBar || 0;

            u.hp = finalUnit.hp;

            u.alive = finalUnit.alive;

            return;

          }



          if (!u.alive && !victims[u.unitId]) {

            u.hp = finalUnit.hp;

            u.alive = finalUnit.alive;

            u.actionBar = finalUnit.actionBar || 0;

            return;

          }



          var targetBar = finalUnit.actionBar || 0;

          if ((u.actionBar || 0) < targetBar) {

            u.actionBar = Math.min(targetBar, (u.actionBar || 0) + tickGain);

            stillGrowing = true;

          } else {

            u.actionBar = targetBar;

          }

          u.hp = finalUnit.hp;

          u.alive = finalUnit.alive;

        });



        vm.growingUnitIds = display.filter(function (u) {

          if (!u.alive) return false;

          if (attacker && u.unitId === attacker.unitId) return false;

          var finalUnit = newBattle.units.find(function (x) { return x.unitId === u.unitId; });

          if (!finalUnit) return false;

          return (u.actionBar || 0) < (finalUnit.actionBar || 0);

        }).map(function (u) { return u.unitId; });



        syncRender();



        if (stillGrowing) {

          setTimeout(growOthersPhase, scaled(TICK_MS));

          return;

        }

        finish();

      }



      function finishAttackPhase() {

        if (aborted()) {

          finish();

          return;

        }

        resetAttackerBarAfterAttack();

        vm.actingUnitId = '';

        vm.hitUnitId = '';

        vm.hitUnitIds = [];

        syncRender();

        setTimeout(function () {

          vm.dyingUnitIds = [];

          growOthersPhase();

        }, isAoe ? scaled(220) : scaled(160));

      }



      function attackPhase() {

        if (aborted()) {

          finish();

          return;

        }

        if (attacker) {

          vm.actingUnitId = attacker.unitId;

          vm.growingUnitIds = [];

          lockAttackerBarReady();

        }



        var windupMs = scaled(isAoe ? 200 : 260);

        setTimeout(function () {

          if (aborted()) {

            finish();

            return;

          }



          if (isAoe) {

            vm.hitUnitIds = hpDrops.map(function (t) { return t.unitId; });

            vm.hitUnitId = '';

            hpDrops.forEach(function (target) {

              var old = oldMap[target.unitId];

              if (vm.spawnDamageFloat && old) {

                vm.spawnDamageFloat(target.unitId, findActionDamage(newBattle, target, old));

              }

            });

            syncRender();



            var hpPromises = hpDrops.map(function (target) {

              var displayUnit = display.find(function (u) { return u.unitId === target.unitId; });

              var old = oldMap[target.unitId];

              if (!displayUnit || !old) return Promise.resolve();

              return animateHp(displayUnit, old.hp, target.hp, 4);

            });



            Promise.all(hpPromises).then(function () {

              hpDrops.forEach(function (target) {

                var displayUnit = display.find(function (u) { return u.unitId === target.unitId; });

                if (!displayUnit) return;

                displayUnit.hp = target.hp;

                if (target.hp <= 0 || target.alive === false) {

                  displayUnit.alive = false;

                  if (vm.dyingUnitIds.indexOf(target.unitId) < 0) {

                    vm.dyingUnitIds.push(target.unitId);

                  }

                } else {

                  displayUnit.alive = target.alive;

                }

              });

              syncRender();

              return sleep(scaled(280));

            }).then(finishAttackPhase);

            return;

          }



          var hitPromises = hpDrops.map(function (target) {

            var old = oldMap[target.unitId];

            var displayUnit = display.find(function (u) { return u.unitId === target.unitId; });

            if (!displayUnit || !old) return Promise.resolve();



            vm.hitUnitId = target.unitId;

            vm.hitUnitIds = [];

            if (vm.spawnDamageFloat) {

              vm.spawnDamageFloat(target.unitId, findActionDamage(newBattle, target, old));

            }

            syncRender();

            return animateHp(displayUnit, old.hp, target.hp, 8).then(function () {

              displayUnit.hp = target.hp;

              if (target.hp <= 0 || target.alive === false) {

                displayUnit.alive = false;

                if (vm.dyingUnitIds.indexOf(target.unitId) < 0) {

                  vm.dyingUnitIds.push(target.unitId);

                }

                syncRender();

                return sleep(scaled(380));

              }

              displayUnit.alive = target.alive;

              syncRender();

            });

          });



          Promise.all(hitPromises).then(finishAttackPhase);

        }, windupMs);

      }



      function runPreActionTickPhase(callback) {

        if (aborted()) {

          finish();

          return;

        }



        if (!attacker) {

          callback();

          return;

        }



        var actorDisplay = display.find(function (u) { return u.unitId === attacker.unitId; });

        if (!actorDisplay) {

          callback();

          return;

        }



        var readyBar = actorDisplay.actionValue || 0;

        if (readyBar <= 0 || (actorDisplay.actionBar || 0) >= readyBar) {

          callback();

          return;

        }



        function tickStep() {

          if (aborted()) {

            finish();

            return;

          }



          var actor = display.find(function (u) { return u.unitId === attacker.unitId; });

          if (!actor || (actor.actionBar || 0) >= readyBar) {

            vm.growingUnitIds = [];

            syncRender();

            callback();

            return;

          }



          display.forEach(function (u) {

            if (!u.alive) return;

            var cap = u.actionValue || 0;

            if (cap <= 0) return;

            u.actionBar = Math.min(cap, (u.actionBar || 0) + tickGain);

          });



          vm.growingUnitIds = display.filter(function (u) {

            if (!u.alive) return false;

            var cap = u.actionValue || 0;

            return cap > 0 && (u.actionBar || 0) < cap;

          }).map(function (u) { return u.unitId; });



          syncRender();

          setTimeout(tickStep, scaled(TICK_MS));

        }



        tickStep();

      }



      function beginStepAnimation() {

        runPreActionTickPhase(function () {

          if (hpDrops.length) {

            attackPhase();

          } else if (attacker) {

            resetAttackerBarAfterAttack();

            growOthersPhase();

          } else {

            growOthersPhase();

          }

        });

      }



      return new Promise(function (resolve) {

        var origFinish = finish;

        finish = function () {

          origFinish();

          resolve();

        };

        beginStepAnimation();

      });

    }

  };

})(window);

