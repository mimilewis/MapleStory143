package server.achievement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapleAchievements {

    private static final MapleAchievements instance = new MapleAchievements();
    private final Map<Integer, MapleAchievement> achievements = new LinkedHashMap<>();

    protected MapleAchievements() {
        achievements.put(1, new MapleAchievement("首次通过传送门", 100, 0, 0, true));
        achievements.put(2, new MapleAchievement("首次等级达到30级", 300, 0, 300000, true));
        achievements.put(3, new MapleAchievement("首次等级达到70级", 500, 0, 500000, true));
        achievements.put(4, new MapleAchievement("首次等级达到120级", 1000, 0, 1000000, true));
        achievements.put(5, new MapleAchievement("首次等级达到200级", 2000, 0, 2000000, true));
        achievements.put(7, new MapleAchievement("首次人气达到50点", 1000, 0, 0, true));
        achievements.put(9, new MapleAchievement("首次穿戴重生装备", 300, 0, 0, true));
        achievements.put(10, new MapleAchievement("首次穿戴永恒装备", 500, 0, 0, true));
        achievements.put(11, new MapleAchievement("说喜欢我们的游戏", 200, 0, 0, true));
        achievements.put(12, new MapleAchievement("首次击败BOSS女老板", 500, 0, 0, true));
        achievements.put(13, new MapleAchievement("首次击败帕普拉图斯", 500, 0, 0, true));
        achievements.put(14, new MapleAchievement("首次击败皮亚奴斯", 500, 0, 0, true));
        achievements.put(15, new MapleAchievement("首次击败扎昆", 1000, 0, 0, true));
        achievements.put(16, new MapleAchievement("首次击败暗黑龙王", 2000, 0, 0, true));
        achievements.put(17, new MapleAchievement("首次击败时间的宠儿－品克缤", 3000, 0, 0, true));
        achievements.put(18, new MapleAchievement("首次杀死1个BOSS", 200, 0, 0, true));
        achievements.put(19, new MapleAchievement("首次完成活动任务 'OX Quiz'", 600, 0, 0, true));
        achievements.put(20, new MapleAchievement("首次完成活动任务 'MapleFitness'", 600, 0, 0, true));
        achievements.put(21, new MapleAchievement("首次完成活动任务 'Ola Ola'", 600, 0, 0, true));
        achievements.put(22, new MapleAchievement("defeating BossQuest HELL mode", 600, 0, 0, true));
        achievements.put(23, new MapleAchievement("首次击败进阶扎昆", 3000, 0, 0, true));
        achievements.put(24, new MapleAchievement("首次击败进阶暗黑龙王", 3000, 0, 0, true));
        achievements.put(25, new MapleAchievement("首次完成活动任务 'Survival Challenge'", 600, 0, 0, true));
        achievements.put(26, new MapleAchievement("首次攻击超过 10000 点", 200, 0, 0, true));
        achievements.put(27, new MapleAchievement("首次攻击超过 50000 点", 300, 0, 0, true));
        achievements.put(28, new MapleAchievement("首次攻击超过 100000 点", 400, 0, 0, true));
        achievements.put(29, new MapleAchievement("首次攻击超过 500000 点", 500, 0, 0, true));
        achievements.put(30, new MapleAchievement("首次攻击达到 999999 点", 1000, 0, 0, true));
        achievements.put(31, new MapleAchievement("首次拥有 1 000 000 金币", 200, 0, 0, true));
        achievements.put(32, new MapleAchievement("首次拥有 10 000 000 金币", 400, 0, 0, true));
        achievements.put(33, new MapleAchievement("首次拥有 100 000 000 金币", 600, 0, 0, true));
        achievements.put(34, new MapleAchievement("首次拥有 1 000 000 000 金币", 800, 0, 0, true));
        achievements.put(35, new MapleAchievement("首次成功创建家族", 800, 0, 0, true));
        achievements.put(36, new MapleAchievement("首次成功创建学院", 600, 0, 0, true));
        achievements.put(37, new MapleAchievement("首次完成1个组队任务", 600, 0, 0, true));
        achievements.put(38, new MapleAchievement("首次击败班·雷昂", 2500, 0, 0, true));
        achievements.put(39, new MapleAchievement("首次击败希纳斯", 3000, 0, 0, true));
        achievements.put(40, new MapleAchievement("首次穿戴130级装备", 800, 0, 0, true));
        achievements.put(41, new MapleAchievement("首次穿戴140级装备", 1200, 0, 0, true));
        achievements.put(42, new MapleAchievement("首次砸卷成功", 300, 0, 0, true));
        achievements.put(43, new MapleAchievement("首次鉴定装备", 300, 0, 0, true));
        achievements.put(44, new MapleAchievement("首次加星成功", 300, 0, 0, true));
        achievements.put(45, new MapleAchievement("首次装备达到10星", 3000, 0, 0, true));
        achievements.put(46, new MapleAchievement("首次领袖达到60级", 800, 0, 0, true));
        achievements.put(47, new MapleAchievement("首次洞察达到60级", 800, 0, 0, true));
        achievements.put(48, new MapleAchievement("首次意志达到60级", 800, 0, 0, true));
        achievements.put(49, new MapleAchievement("首次手技达到60级", 800, 0, 0, true));
        achievements.put(50, new MapleAchievement("首次感性达到60级", 800, 0, 0, true));
        achievements.put(51, new MapleAchievement("首次魅力达到60级", 800, 0, 0, true));
        achievements.put(52, new MapleAchievement("首次鉴定出 A 级装备", 300, 0, 300000, true));
        achievements.put(53, new MapleAchievement("首次鉴定出 S 级装备", 600, 0, 600000, true));
        achievements.put(54, new MapleAchievement("首次鉴定出 SS 级装备", 1200, 0, 1200000, true));
        achievements.put(55, new MapleAchievement("首次击败希拉 - 简单模式", 1500, 0, 0, true));
        achievements.put(56, new MapleAchievement("首次击败希拉 - 困难模式", 3000, 0, 0, true));
        achievements.put(57, new MapleAchievement("首次用神奇铁砧完成了形象合成", 300, 0, 0, true));
        achievements.put(58, new MapleAchievement("首次击败阿卡伊勒", 3000, 0, 0, true));
        achievements.put(59, new MapleAchievement("首次击败混沌品克缤", 3000, 0, 0, true));
    }

    public static MapleAchievements getInstance() {
        return instance;
    }

    public MapleAchievement getById(int id) {
        return achievements.get(id);
    }

    public Integer getByMapleAchievement(MapleAchievement ma) {
        for (Entry<Integer, MapleAchievement> achievement : this.achievements.entrySet()) {
            if (achievement.getValue() == ma) {
                return achievement.getKey();
            }
        }
        return null;
    }
}
