package client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import handling.world.PlayerBuffValueHolder;
import handling.world.WorldFindService;
import server.MapleStatEffect;
import tools.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MaplePlayerBuffManager {

    private final String owner;
    private final java.util.concurrent.locks.ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final java.util.concurrent.ConcurrentHashMap<MapleBuffStat, MapleBuffEffect> effects = new ConcurrentHashMap<>();
    private final java.util.concurrent.CopyOnWriteArrayList<MapleBuff> buffs = new CopyOnWriteArrayList<>();

    @JsonCreator
    public MaplePlayerBuffManager(@JsonProperty("owner") String owner) {
        this.owner = owner;
    }

    private MapleCharacter getPlayer() {
        return WorldFindService.getInstance().findCharacterByName(owner);
    }

    /**
     * 注册增益效果
     *
     * @param effect
     * @param starttime
     * @param schedule
     * @param stat
     */
    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, List<Pair<MapleBuffStat, Integer>> stat) {
        try {
            lock.writeLock().lock();
            ArrayList<MapleBuffEffect> buffEffects = new ArrayList<>();
            MapleBuff buff = new MapleBuff(effect, starttime, schedule);
            for (Pair<MapleBuffStat, Integer> pair : stat) {
                MapleBuffEffect buffEffect = new MapleBuffEffect(buff, pair.getLeft(), pair.getRight());
                effects.put(buffEffect.stat, buffEffect);
                buffEffects.add(buffEffect);
            }
            buffEffects.trimToSize();
            buff.buffEffects = buffEffects;
            buffs.add(buff);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isActiveBuffedValue(int skillid) {
        try {
            lock.readLock().lock();
            for (MapleBuffEffect be : effects.values()) {
                if (be.getBuff().getEffect().isSkill() && be.getBuff().getEffect().getSourceid() == skillid) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        if (effect == null) {
            return null;
        }
        try {
            lock.readLock().lock();
            return effects.containsKey(effect) ? effects.get(effect).value
                    : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isBuffFrom(MapleBuffStat stat, int skill) {
        if (stat == null) {
            return false;
        }
        try {
            lock.readLock().lock();
            return effects.containsKey(stat) && (effects.get(stat).getBuff().getEffect().isSkill() && effects.get(stat).getBuff().getEffect().getSourceid() == skill);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getBuffSource(MapleBuffStat stat) {
        if (stat == null) {
            return -1;
        }
        try {
            lock.readLock().lock();
            return effects.containsKey(stat) ? effects.get(stat).getBuff()
                    .getEffect().getSourceid() : -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<MapleStatEffect> getBuffEffects() {
        try {
            lock.readLock().lock();
            ArrayList<MapleStatEffect> almseret = new ArrayList<>();
            HashSet<Integer> hs = new HashSet<>();
            for (MapleBuffEffect buffEffect : effects.values()) {
                if (buffEffect != null && buffEffect.getEffect() != null) {
                    Integer nid = buffEffect.getEffect().isSkill() ? buffEffect.getEffect().getSourceid() : -buffEffect.getEffect().getSourceid();
                    if (!hs.contains(nid)) {
                        almseret.add(buffEffect.getEffect());
                        hs.add(nid);
                    }
                }
            }
            return almseret;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        try {
            lock.readLock().lock();
            MapleBuffEffect effect1 = effects.get(effect);
            if (effect1 != null) {
                effect1.value = value;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        return effects.containsKey(effect) ? effects.get(effect).getBuff().startTime : null;
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        return effects.containsKey(effect) ? effects.get(effect).getEffect() : null;
    }

    public List<MapleBuffStat> getBuffStats(MapleStatEffect effect, long startTime) {
        try {
            lock.readLock().lock();
            List<MapleBuffStat> stats = new ArrayList<>();
            for (MapleBuff buff : buffs) {
                if (buff.getEffect().sameSource(effect) && (startTime == -1 || buff.startTime == startTime)) {
                    for (MapleBuffEffect buffEffect : buff.buffEffects) {
                        stats.add(buffEffect.stat);
                    }
                    break;
                }
            }
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }

    public MapleStatEffect deregisterBuffStats(MapleBuff buff) {
        return deregisterBuffStats(buff, null);
    }

    /**
     * 注销BUFF对象
     *
     * @param buff
     * @param stats
     * @return
     */
    public MapleStatEffect deregisterBuffStats(MapleBuff buff, List<MapleBuffStat> stats) {
        if (buff == null) {
            return null;
        }
        try {
            lock.writeLock().lock();
            for (MapleBuffEffect buffEffect : buff.buffEffects) {
                if (stats != null) {
                    stats.add(buffEffect.stat);
                }
                effects.remove(buffEffect.stat);
            }
            if (buff.scheduledFuture != null) {
                buff.scheduledFuture.cancel(false);
                buff.scheduledFuture = null;
            }
            buffs.remove(buff);
            return buff.effect;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public MapleBuff getBuff(MapleBuffStat stat) {
        try {
            lock.readLock().lock();
            MapleBuff buff = null;
            MapleBuffEffect buffEffect = effects.get(stat);
            if (buffEffect != null) {
                buff = buffEffect.getBuff();
            }
            return buff;
        } finally {
            lock.readLock().unlock();
        }
    }

    public MapleBuff getBuff(int skillId) {
        try {
            lock.readLock().lock();
            for (MapleBuff buff : buffs) {
                if (buff.getId() == skillId) {
                    return buff;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public MapleBuff getBuff(MapleStatEffect effect) {
        try {
            lock.readLock().lock();
            MapleBuff buff = null;
            for (MapleBuff mb : buffs) {
                if (mb.effect.sameSource(effect)) {
                    buff = mb;
                    break;
                }
            }
            return buff;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 兼容老代码函数
     *
     * @return
     */
    public List<PlayerBuffValueHolder> getAllBuffs() {
        try {
            lock.readLock().lock();
            List<PlayerBuffValueHolder> ret = new ArrayList<>();
            for (MapleBuff buff : buffs) {
                Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                for (MapleBuffEffect buffEffect : buff.buffEffects) {
                    stat.put(buffEffect.getStat(), buffEffect.getValue());
                }
                ret.add(new PlayerBuffValueHolder(buff.getStartTime(), buff.getEffect(), stat));
            }
            return ret;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 兼容老代码函数
     */
    public void cancelMagicDoor() {
        for (MapleBuff buff : buffs) {
            if (buff.getEffect().is时空门()) {
                cancelEffect(buff);
            }
        }
    }

    /**
     * 兼容老代码函数
     */
    public void dispel() {
        for (MapleBuff buff : buffs) {
            if (buff.getEffect().isSkill()) {
                cancelEffect(buff);
            }
        }
    }

    /**
     * 兼容老代码函数
     */
    public void cancelAllBuffs() {
        for (MapleBuff buff : buffs) {
            cancelEffect(buff);
        }
    }

    /**
     * 兼容老代码函数
     */
    public void cancelMorphs() {
        for (MapleBuff buff : buffs) {
            if (buff.getEffect().isMorph()
                    && buff.getEffect().getSourceid() != 5111005
                    && buff.getEffect().getSourceid() != 5121003) {
                cancelEffect(buff);
            }
        }
    }

    /**
     * 检查是否有指定ID的增益效果
     *
     * @param skillId
     * @return
     */
    public boolean hasBuff(int skillId) {
        for (MapleBuff buff : buffs) {
            if (buff.getEffect().getSourceid() == skillId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 兼容老代码函数
     *
     * @param skillid
     */
    public void dispelSkill(int skillid) {
        for (MapleBuff buff : buffs) {
            if (skillid == 0) { // 人物死亡时
                if (buff.effect.isSkill() && buff.effect.getSourceid() % 20000000 == 1004) {
                    cancelEffect(buff);
                }
            } else if (buff.effect.isSkill()
                    && buff.effect.getSourceid() == skillid) {
                cancelEffect(buff);
            }
        }
    }

    public void cancelEffect(MapleBuff buff) {
        cancelEffect(buff, false);
    }

    public void cancelEffect(MapleBuff buff, boolean overwrite) {
        if (getPlayer() != null) {
            getPlayer().cancelEffect(buff.getEffect(), overwrite, buff.getStartTime());
        }
    }

    public List<MapleBuffStat> getSpawnList(Map<MapleBuffStat, Integer> list) {
        ArrayList<MapleBuffStat> _buffs = new ArrayList<>();
        for (MapleBuffStat buffStat : list.keySet()) {
            if (effects.containsKey(buffStat) && effects.get(buffStat).getEffect().isRefreshstyle()) {
                _buffs.add(buffStat);
            }
        }
        return _buffs;
    }

    public boolean hasBufferStat(MapleBuffStat stat) {
        return effects.containsKey(stat);
    }

    public boolean buffIsEmpty() {
        return buffs.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String playerName = "";
        if (getPlayer() != null) {
            playerName = getPlayer().getName();
        }
        sb.append("技能储存对象：").append(this.getClass().getName()).append("\r\n");
        sb.append("玩家名字：");
        sb.append(playerName);
        sb.append("\r\n");
        sb.append("应用技能：\r\n");
        sb.append("BuffCount:").append(buffs.size()).append("\r\n");
        try {
            lock.readLock().lock();
            for (MapleBuff buff : buffs) {
                sb.append("Buff Id:").append(buff.getId()).append("\r\n");
                sb.append("StartTime:").append(buff.getStartTime()).append("\r\n");
                sb.append("ScheduledFuture:").append(buff.getScheduledFuture()).append("\r\n");
                for (MapleBuffEffect buffEffect : buff.buffEffects) {
                    sb.append("Stat :").append(buffEffect.stat.toString()).append(" - Value:").append(buffEffect.value).append("\r\n");
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        sb.append("Effects :").append(effects.size()).append("\r\n");
        try {
            lock.readLock().lock();
            for (MapleBuffEffect buffStat : effects.values()) {
                sb.append("From Buff:").append(buffStat.getBuff()).append("\r\n");
                sb.append("Stat:").append(buffStat.getStat()).append("-");
                sb.append("Value:").append(buffStat.getValue()).append("\r\n");
            }
        } finally {
            lock.readLock().unlock();
        }
        return sb.toString();
    }

    public void cancelSkill(int skill) {
        for (MapleBuff buff : buffs) {
            if (buff.getId() == skill) {
                if (getPlayer() != null) {
                    getPlayer().cancelEffect(buff.getEffect(), false, buff.getStartTime());
                }
                break;
            }
        }
    }

    public void silentRemoveSkill(int skill) {
        for (MapleBuff buff : buffs) {
            if (buff.getId() == skill) {
                if (getPlayer() != null) {
                    deregisterBuffStats(buff);
                }
                break;
            }
        }
    }

    public static class MapleBuff {

        private final MapleStatEffect effect;
        private final long startTime;
        private ScheduledFuture scheduledFuture;
        private List<MapleBuffEffect> buffEffects;

        public MapleBuff(MapleStatEffect effect, long startTime,
                         ScheduledFuture scheduledFuture) {
            this.effect = effect;
            this.startTime = startTime;
            this.scheduledFuture = scheduledFuture;
        }

        public int getId() {
            return getEffect().getSourceid();
        }

        public MapleStatEffect getEffect() {
            return effect;
        }

        public long getStartTime() {
            return startTime;
        }

        public ScheduledFuture getScheduledFuture() {
            return scheduledFuture;
        }

        @Override
        public String toString() {
            return "ID :" + getId() + ";StartTime:" + getStartTime() + ";Schedule:" + getScheduledFuture();
        }
    }

    public static class MapleBuffEffect {

        private final MapleBuff buff;
        private MapleBuffStat stat;
        private Integer value;

        public MapleBuffEffect(MapleBuff buff, Integer value) {
            this.buff = buff;
            this.value = value;
        }

        public MapleBuffEffect(MapleBuff buff, MapleBuffStat stat, Integer value) {
            this.buff = buff;
            this.stat = stat;
            this.value = value;
        }

        public MapleBuff getBuff() {
            return buff;
        }

        public MapleStatEffect getEffect() {
            return getBuff().getEffect();
        }

        public MapleBuffStat getStat() {
            return stat;
        }

        public Integer getValue() {
            return value;
        }
    }
}
