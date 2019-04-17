/* 
 * ���յ�
 */

function init() {
    em.setProperty("state", "0");
    em.setProperty("leader", "true");
}

function setup(eim, leaderid) {
    em.setProperty("state", "1");
    em.setProperty("leader", "true");
    var eim = em.newInstance("ZChaosPQ");
    var map = eim.setInstanceMap(863010600);
    map.resetFully();
    eim.getMapFactory().getMap(863010600).killAllMonsters(false);
    var mob1 = em.getMonster(9390601);
    var mob2 = em.getMonster(9390602);
    var mob3 = em.getMonster(9390601);
    var mob4 = em.getMonster(9390602);
    var mob = em.getMonster(9390600);
    var modified = em.newMonsterStats();
    modified.setOHp(mob.getMobMaxHp() * 2000);
    modified.setOMp(mob.getMobMaxMp() * 9000);
    mob.setOverrideStats(modified);
    eim.registerMonster(mob);
    map.spawnMonsterOnGroundBelow(mob1, new java.awt.Point(7, 61));
    map.spawnMonsterOnGroundBelow(mob2, new java.awt.Point(7, 61));
    map.spawnMonsterOnGroundBelow(mob3, new java.awt.Point(7, 61));
    map.spawnMonsterOnGroundBelow(mob4, new java.awt.Point(7, 61));
    map.spawnMonsterOnGroundBelow(mob, new java.awt.Point(7, 61));
    eim.startEventTimer(1000 * 60 * 60); // 30 min
    return eim;
}

function playerEntry(eim, player) {
    var map = eim.getMapInstance(0);
    player.changeMap(map, map.getPortal(0));
}

function playerRevive(eim, player) {
    return false;
}

function changedMap(eim, player, mapid) {
    if (mapid != 863010600) {
        eim.unregisterPlayer(player);
        if (eim.disposeIfPlayerBelow(0, 0)) {
            em.setProperty("state", "0");
            em.setProperty("leader", "true");
        }
    }
}

function playerDisconnected(eim, player) {
    eim.disposeIfPlayerBelow(100, 910000000);
    em.setProperty("state", "0");
    em.setProperty("leader", "true");
    return 0;
}

function monsterValue(eim, mobId) {
    return 1;
}

function playerExit(eim, player) {
    eim.unregisterPlayer(player);
    if (eim.disposeIfPlayerBelow(0, 0)) {
        em.setProperty("state", "0");
        em.setProperty("leader", "true");
    }
}
function scheduledTimeout(eim) {
    eim.disposeIfPlayerBelow(100, 910000000);
    em.setProperty("state", "0");
    em.setProperty("leader", "true");
}

function clearPQ(eim) {
    scheduledTimeout(eim);
}

function allMonstersDead(eim) {
    eim.getMapFactory().getMap(863010600).killAllMonsters(false);
    /* var state = em.getProperty("state");
     if (state.equals("1")) {
     em.setProperty("state", "2");
     } else if (state.equals("2")) {
     em.setProperty("state", "3");
     }*/
}

function leftParty(eim, player) {
}
function disbandParty(eim) {
}
function playerDead(eim, player) {
}
function cancelSchedule() {
}