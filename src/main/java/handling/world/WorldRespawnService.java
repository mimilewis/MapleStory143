/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleCoolDownValueHolder;
import client.MapleDiseaseValueHolder;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.PetDataFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import client.status.NewMonsterStatusEffect;
import handling.channel.ChannelServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer.WorldTimer;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import tools.MaplePacketCreator;
import tools.Pair;

import java.util.List;
import java.util.Map;

/**
 * @author admin
 */
public class WorldRespawnService {

    private static final Logger log = LogManager.getLogger(WorldRespawnService.class.getName());

    private WorldRespawnService() {
        log.info("正在启动[WorldRespawnService]");
        Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);
        for (int i = 1; i <= chs.length; i++) {
            WorldTimer.getInstance().register(new Respawn(i), 1000);
        }
    }

    public static WorldRespawnService getInstance() {
        return SingletonHolder.instance;
    }

    public static void handleMap(MapleMap map, int numTimes, int size, long now) {
        /*
         * 检测地图上面的道具消失时间
         */
        if (map.getItemsSize() > 0) {
            for (MapleMapItem item : map.getAllItemsThreadsafe()) {
                if (item.shouldExpire(now)) {
                    item.expire(map);
                } else if (item.shouldFFA(now)) {
                    item.setDropType((byte) 2);
                }
            }
        }
        /*
         * 如果地图上面有玩家或者地图为: 931000500 秘密地图 - 美洲豹栖息地
         */
        boolean canSpawnRune = false;
        if (map.getCharactersSize() > 0 || map.getId() == 931000500) {
            /*
             * 该地图是否能刷新怪物
             */
            if (map.canSpawn(now)) {
                map.respawn(false, now);
            }
            /*
             * 对该地图里面所有玩家进行技能冷却检测
             */
            boolean hurt = map.canHurt(now);
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                handleCooldowns(chr, numTimes, hurt, now);
            }
            /*
             * 对地图里面的怪物进行检测
             */
            if (map.getMobsSize() > 0) {
                for (MapleMonster mons : map.getAllMonstersThreadsafe()) {
                    if (mons.getStats().getLevel() >= 30) {
                        canSpawnRune = true;
                    }
                    if (mons.isAlive()) {

                        if (mons.shouldKill(now)) {
                            map.killMonster(mons);
                        }

                        if (mons.shouldDrop(now)) {
                            mons.doDropItem(now);
                        }

                        if (mons.getStatiSize() > 0) {
                            for (MonsterStatusEffect mse : mons.getAllBuffs()) {
                                if (mse.shouldCancel(now)) {
                                    mons.cancelSingleStatus(mse);
                                }
                            }
                        }

                        if (mons.getNewMobEffects().size() > 0) {
                            for (Map.Entry<Integer, List<Pair<MonsterStatus, NewMonsterStatusEffect>>> entry : mons.getNewMobEffects().entrySet()) {
                                for (Pair<MonsterStatus, NewMonsterStatusEffect> pair : entry.getValue()) {
                                    if (pair.getRight().shouldCancel(now)) {
                                        mons.cancelNewStatus(entry.getKey());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (canSpawnRune && map.getRunesSize() == 0 && !map.isBossMap() && map.getFirstUserEnter().equals("") && map.getUserEnter().equals("")) {
            map.respawnRune();
        }
    }

    public static void handleCooldowns(MapleCharacter chr, int numTimes, boolean hurt, long now) {
        /*
         * 角色为空返回
         */
        if (chr == null) {
            return;
        }
        /*
         * 角色有冷却中的技能
         */
        if (chr.getCooldownSize() > 0) {
            for (MapleCoolDownValueHolder m : chr.getCooldowns()) {
                if (m.startTime + m.length < now) {
                    int skillId = m.skillId;
                    chr.removeCooldown(skillId);
                    chr.send(MaplePacketCreator.skillCooldown(skillId, 0));
                }
            }
        }
        /*
         * 如果角色没有死亡
         */
        if (chr.isAlive()) {
            chr.doAnimabuff();
            if (chr.getJob() == 131 || chr.getJob() == 132) {
                if (chr.canBlood(now)) {
                    chr.doDragonBlood();
                }
            }
            if (chr.canRecover(now)) {
                chr.doRecovery();
            }
            //必须是恶魔复仇者才可以
            if (chr.getJob() == 3121 || chr.getJob() == 3122) {
                if (chr.canRecoverEM(now)) {
                    chr.doRecoveryEM();
                }
            }
            if (chr.canHPRecover(now)) {
                chr.addHP((int) chr.getStat().getHealHP());
            }
            if (chr.canMPRecover(now)) {
                chr.addMP((int) chr.getStat().getHealMP());
                if (chr.getJob() == 3111 || chr.getJob() == 3112) {
                    chr.addDemonMp((int) chr.getStat().getHealMP());
                }
            }
            //尖兵能量恢复
            if (chr.canRecoverPower(now)) {
                chr.doRecoveryPower();
            }
            if (chr.getLevel() >= 200) {
                chr.check5thJobQuest();
            }
            //处理暴击蓄能效果
            if (chr.canRecoverCrit(now)) {
                chr.handleCritStorage();
            }
            //处理林之灵组队被动BUFF
            if (chr.canPartyPassiveBuff(now)) {
                chr.doPartyPassiveBuff();
            }
            if (chr.canFairy(now)) {
                chr.doFairy();
            }
            if (chr.canFish(now)) {
                chr.doFish(now);
            }
            if (chr.canExpiration(now)) {
                chr.expirationTask();
            }
            if (chr.canMorphLost(now)) {
                chr.morphLostTask();
            }
            if (chr.canRepeatEffect(now)) {
                chr.doRepeatEffect();
            }
        }
        /*
         * 检测角色负面状态BUFF的取消时间
         */
        if (chr.getDiseaseSize() > 0) {
            for (MapleDiseaseValueHolder m : chr.getAllDiseases()) {
                if (m != null && m.startTime + m.length < now) {
                    chr.dispelDebuff(m.disease);
                }
            }
        }
        /*
         * 对角色坐骑进行检测
         */
        if (numTimes % 7 == 0 && chr.getMount() != null && chr.getMount().canTire(now)) {
            chr.getMount().increaseFatigue();
        }
        if (numTimes % 30 == 0) { //we're parsing through the characters anyway (:
            for (MaplePet pet : chr.getSummonedPets()) {
                if (pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0) {
                    pet.setSecondsLeft(pet.getSecondsLeft() - 1);
                    if (pet.getSecondsLeft() <= 0) {
                        chr.unequipSpawnPet(pet, true, true);
                        return;
                    }
                }
                int newFullness = pet.getFullness() - PetDataFactory.getHunger(pet.getPetItemId());
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    chr.unequipSpawnPet(pet, true, true);
                } else {
                    pet.setFullness(newFullness);
                    chr.petUpdateStats(pet, true);
                }
            }
        }
        if (hurt && chr.isAlive()) {
            /*
             * 749040100 - 隐藏地图 - 纯净雪人栖息地
             */
            if (chr.getInventory(MapleInventoryType.EQUIPPED).findById(chr.getMap().getHPDecProtect()) == null) {
                if (chr.getMapId() == 749040100 && chr.getInventory(MapleInventoryType.CASH).findById(5451000) == null) { //minidungeon
                    chr.addHP(-chr.getMap().getHPDec());
                } else if (chr.getMapId() != 749040100) {
                    chr.addHP(-(chr.getMap().getHPDec() - (chr.getBuffedValue(MapleBuffStat.HP_LOSS_GUARD) == null ? 0 : chr.getBuffedValue(MapleBuffStat.HP_LOSS_GUARD))));
                }
            }
        }
    }

    private static class Respawn implements Runnable {

        private final ChannelServer cserv;
        private int numTimes = 0;

        public Respawn(int ch) {
            String sb = "[Respawn Worker] Registered for channel " + ch;
            cserv = ChannelServer.getInstance(ch);
            log.info(sb);
        }

        @Override
        public void run() {
            numTimes++;
            long now = System.currentTimeMillis();
            if (!cserv.hasFinishedShutdown()) {
                for (MapleMap map : cserv.getMapFactory().getAllLoadedMaps()) { //iterating through each map o_x
                    handleMap(map, numTimes, map.getCharactersSize(), now);
                }
            }
        }
    }

    private static class SingletonHolder {

        protected static final WorldRespawnService instance = new WorldRespawnService();
    }
}
