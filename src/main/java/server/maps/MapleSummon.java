package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import client.anticheat.CheatingOffense;
import client.skills.SkillFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import constants.SkillConstants;
import constants.skills.*;
import server.MapleStatEffect;
import tools.packet.SummonPacket;

import java.awt.*;

public final class MapleSummon extends AnimatedMapleMapObject {

    private final int ownerid;
    private final int skillLevel;
    private final int ownerLevel;
    private final MapleCharacter owner;
    private final long createTime;
    private final SummonMovementType movementType; //召唤兽的移动类型
    private int skillId;
    private int fh;
    private int duration;
    private MapleMap map; //required for instanceMaps
    private int hp = 1;
    private boolean changedMap = false;
    //下面是能够攻击怪物的召唤兽处理
    private int lastSummonTickCount;
    private byte Summon_tickResetCount;
    private long Server_ClientSummonTickDiff;
    private long lastAttackTime;
    private int mobid;

    @JsonCreator
    public MapleSummon(
            @JsonProperty("owner") MapleCharacter owner,
            @JsonProperty("effect") MapleStatEffect effect,
            @JsonProperty("pos") Point pos,
            @JsonProperty("movementType") SummonMovementType movementType,
            @JsonProperty("duration") int duration,
            @JsonProperty("mobid") int mobid) {
        this(owner, effect.getSourceid(), effect.getLevel(), pos, movementType, duration, mobid);
    }

    public MapleSummon(MapleCharacter owner, int sourceid, int level, Point pos, SummonMovementType movementType, int duration, int mobid) {
        super();
        this.owner = owner;
        this.map = owner.getMap();
        this.ownerid = owner.getId();
        this.ownerLevel = owner.getLevel();
        this.skillId = sourceid;
        this.skillLevel = level;
        this.movementType = movementType;
        setPosition(pos);

        if (!is替身术()) {
            lastSummonTickCount = 0;
            Summon_tickResetCount = 0;
            Server_ClientSummonTickDiff = 0;
            lastAttackTime = 0;
        }
        this.createTime = System.currentTimeMillis();
        this.duration = duration;
        this.mobid = mobid;
    }

    /**
     * 发送召唤召唤兽的数据包
     *
     * @param client 客户端
     */
    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer() != null) {
            client.announce(SummonPacket.spawnSummon(this, true));
        }
    }

    /**
     * 发送移除召唤兽的数据包
     *
     * @param client 客户端
     */
    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(SummonPacket.removeSummon(this, false));
    }

    /**
     * 更新召唤兽对象地图数据
     *
     * @param map
     */
    public void updateMap(MapleMap map) {
        this.map = map;
    }

    /**
     * 获取召唤兽所有者的角色对象
     *
     * @return
     */
    public MapleCharacter getOwner() {
        return map.getCharacterById(ownerid);
    }

    /**
     * 获取召唤兽所有者的角色ID
     *
     * @return
     */
    public int getOwnerId() {
        return ownerid;
    }

    /**
     * 获取召唤兽所有者的角色等级
     *
     * @return
     */
    public int getOwnerLevel() {
        return ownerLevel;
    }

    /**
     * 获取召唤兽技能ID
     *
     * @return
     */
    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    /**
     * 获取召唤兽技能等级
     *
     * @return
     */
    public int getSkillLevel() {
        return skillLevel;
    }

    /**
     * 获取召唤兽支撑点
     *
     * @return
     */
    public int getFh() {
        return fh;
    }

    /**
     * 获取召唤兽Hp
     *
     * @return
     */
    public int getSummonHp() {
        return hp;
    }

    /**
     * 设置召唤的Hp
     *
     * @param hp
     */
    public void setSummonHp(int hp) {
        this.hp = hp;
    }

    /**
     * 增加召唤的Hp
     *
     * @param delta
     */
    public void addSummonHp(int delta) {
        this.hp += delta;
    }

    /**
     * 获取召唤兽对象的创建时间
     *
     * @return 召唤兽创建时间
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * 获取召唤兽持续时间
     *
     * @return
     */
    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * 写入召唤兽位置信息
     *
     * @param p 坐标
     */
    @Override
    public final void setPosition(Point p) {
        super.setPosition(p);
        MapleFoothold foothold = owner.getMap().getFootholds().findBelow(p, true);
        if (foothold == null) {
            this.fh = 0;
        } else {
            this.fh = foothold.getId();
        }
    }

    /**
     * 替身术，不会攻击的召唤兽
     *
     * @return
     */
    public boolean is替身术() {
        switch (skillId) {
            case 箭神.神箭幻影:
            case 双刀.傀儡召唤:
                return true;
        }
        return is天使召唤兽();
    }

    public boolean is天使召唤兽() {
        return SkillConstants.is天使祝福戒指(skillId);
    }

    /**
     * @return 是否会多次攻击怪物的召唤兽
     * @deprecated
     */
    public boolean isMultiAttack() {
        switch (skillId) {
            case 机械师.磁场:
            case 机械师.战争机器_泰坦:
            case 机械师.机器人发射器_RM7:
            case 豹弩游侠.召唤美洲豹_灰:
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
            case 唤灵斗师.死亡:
            case 唤灵斗师.死亡契约:
            case 唤灵斗师.死亡契约2:
            case 唤灵斗师.死亡契约3:
            case 神炮王.旋转彩虹炮:
            case 龙的传人.破城炮:
            case 林之灵.小波波:
            case 林之灵.嗨_兄弟:
            case 龙神.召唤玛瑙龙:
            case 神射手.火凤凰:
            case 箭神.冰凤凰:
            case 冰雷.冰破魔兽:
            case 火毒.火魔兽:
            case 主教.强化圣龙:
            case 双弩.精灵骑士:
            case 双弩.精灵骑士1:
            case 双弩.精灵骑士2:
            case 尖兵.全息力场_穿透:
            case 尖兵.全息力场_力场:
            case 隐士.黑暗杂耍:
            case 船长.战船轰炸机:
            case 船长.战舰炮轰_1:
            case 船长.战舰炮轰_2:
            case 船长.战舰炮轰_3:
                return true;
        }
        return false;
    }

    /**
     * 是否在召唤时显示当前角色的外观信息
     *
     * @return
     */
    public boolean showCharLook() {
        switch (skillId) {
            case 魂骑士.天人之舞:
            case 双弩.精灵元素:
            case 双弩.元素幽灵_1:
            case 双弩.元素幽灵_2:
                return true;
            default:
                return is傀儡召唤() || is影子侍从() || is粉红影子();
        }
    }

    public boolean is神箭幻影() {
        return skillId == 箭神.神箭幻影;
    }

    public boolean is灵魂助力() {
        return skillId == 黑骑士.灵魂助力;
    }

    public boolean is傀儡召唤() {
        return skillId == 双刀.傀儡召唤;
    }

    public boolean is影子侍从() {
        return skillId == 夜行者.影子侍从 || skillId == 夜行者.黑暗幻影 || skillId == 夜行者.黑暗幻影_影子40 || skillId == 夜行者.黑暗幻影_影子20;
    }

    public boolean is粉红影子() {
        return skillId == 品克缤.粉红影子_1 || skillId == 品克缤.粉红影子_2;
    }

    public boolean is机械磁场() {
        return skillId == 机械师.磁场;
    }

    public boolean is大漩涡() {
        return skillId == 炎术士.大漩涡;
    }

    public boolean is黑暗杂耍() {
        return skillId == 隐士.黑暗杂耍 || skillId == 侠盗.黑暗杂耍;
    }

    public boolean isMultiSummon() {
        return skillId == 船长.八轮重机枪;
    }

    public boolean isSummon() {
        return is天使召唤兽() || SkillFactory.getSkill(skillId).isSummonSkill();
    }

    /*
     * 召唤兽移动类型
     */
    public SummonMovementType getMovementType() {
        return movementType;
    }

    /*
     * 召唤兽攻击类型
     */
    public byte getAttackType() {
        if (is天使召唤兽()) {
            return 2;
        }
        switch (skillId) {
            case 风灵使者.绿水晶花: //新的风灵使者召唤兽技能
            case 风灵使者.钻石星尘: //绿水晶花的进阶技能
            case 机械师.磁场:
            case 炎术士.元素_火焰:
            case 炎术士.元素_火焰II:
            case 炎术士.元素_火焰III:
            case 炎术士.元素_火焰IV:
            case 炎术士.火焰化身_狮子:
            case 炎术士.火焰化身_狐狸:
            case 夜行者.影子蝙蝠_召唤兽:
            case 夜行者.影子蝙蝠:
            case 夜行者.影子侍从:
            case 夜行者.黑暗幻影_影子40:
            case 夜行者.黑暗幻影_影子20:
            case 4341006:
            case 35111005:
            case 35121010:
            case 双弩.精灵元素:
            case 双弩.元素幽灵_1:
            case 双弩.元素幽灵_2:
                return 0;
            case 箭神.神箭幻影:
            case 隐士.黑暗杂耍:
            case 侠盗.黑暗杂耍:
            case 龙神.召唤玛瑙龙:
            case 林之灵.小波波:
            case 林之灵.嗨_兄弟:
            case 炎术士.大漩涡:
//            case 神箭手.火凤凰:
            case 35101012:
            case 36121013:
            case 42111003:
                return 1;
            case 黑骑士.灵魂助力:
            case 尖兵.全息力场_支援:
                return 2;
            case 双弩.精灵骑士:
            case 双弩.精灵骑士1:
            case 双弩.精灵骑士2:
            case 尖兵.全息力场_穿透:
            case 35111001:
            case 35111009:
            case 35111010:
            case 英雄.燃灵之剑:
                return 3;
            case 机械师.机器人工厂_RM1:
                return 5;
            case 机械师.战争机器_泰坦:
            case 唤灵斗师.死亡:
            case 唤灵斗师.死亡契约:
            case 唤灵斗师.死亡契约2:
            case 唤灵斗师.死亡契约3:
                return 6;
            case 14111010:
                return 7;
            case 船长.集合船员:
            case 船长.集合船员2:
            case 船长.集合船员3:
            case 船长.集合船员4:
                return 9;
            case 豹弩游侠.召唤美洲豹_灰:
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
                return 10;
            case 船长.战船轰炸机:
            case 42100010:
                return 11;
            case 400011012:
            case 400011013:
            case 400011014:
                return 12;
            case 机械师.多重属性_M_FL:
                return 13;
        }
        return 1;
    }

    /*
     * 召唤兽移除时的提示
     */
    public byte getRemoveStatus() {
        if (is天使召唤兽()) {
            return 0x0A;
        }
        switch (skillId) {
            case 神炮王.磁性船锚:
            case 机械师.磁场:
            case 机械师.支援波动器_H_EX:
            case 机械师.机器人工厂_RM1:
                return 0x05;
            case 双弩.精灵骑士:
            case 双弩.精灵骑士1:
            case 双弩.精灵骑士2:
            case 机械师.战争机器_泰坦:
                return 0x0A;
        }
        return 0x00;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }

    public void CheckSummonAttackFrequency(MapleCharacter chr, int tickcount) {
        int tickdifference = (tickcount - lastSummonTickCount);
        if (tickdifference < SkillFactory.getSummonData(skillId).delay) {
            chr.getCheatTracker().registerOffense(CheatingOffense.FAST_SUMMON_ATTACK);
        }
        long STime_TC = System.currentTimeMillis() - tickcount;
        long S_C_Difference = Server_ClientSummonTickDiff - STime_TC;
        if (S_C_Difference > 500) {
            chr.getCheatTracker().registerOffense(CheatingOffense.FAST_SUMMON_ATTACK);
        }
        Summon_tickResetCount++;
        if (Summon_tickResetCount > 4) {
            Summon_tickResetCount = 0;
            Server_ClientSummonTickDiff = STime_TC;
        }
        lastSummonTickCount = tickcount;
    }

    public void CheckPVPSummonAttackFrequency(MapleCharacter chr) {
        long tickdifference = (System.currentTimeMillis() - lastAttackTime);
        if (tickdifference < SkillFactory.getSummonData(skillId).delay) {
            chr.getCheatTracker().registerOffense(CheatingOffense.FAST_SUMMON_ATTACK);
        }
        lastAttackTime = System.currentTimeMillis();
    }

    public boolean checkLastAttackTime() {
        if (System.currentTimeMillis() - getSkillCoolTime(skillId) * 1000 < lastAttackTime) {
            return false;
        }
        lastAttackTime = System.currentTimeMillis();
        return true;
    }

    /**
     * 召唤兽是否更换地图
     *
     * @return true = 更换 : false = 没有
     */
    public boolean isChangedMap() {
        return changedMap;
    }

    /**
     * 写入更换地图数据
     *
     * @param cm
     */
    public void setChangedMap(boolean cm) {
        this.changedMap = cm;
    }

    /**
     * 更换地图的话就取消
     *
     * @return true = 更换地图就取消 ： false = 更换地图不取消
     */
    public boolean isChangeMapCanceled() {
        return this.getMovementType() == SummonMovementType.不会移动 || this.getMovementType() == SummonMovementType.CIRCLE_STATIONARY || this.getMovementType() == SummonMovementType.自由移动;
    }

    public int getMobid() {
        return mobid;
    }

    public void setMobid(int mobid) {
        this.mobid = mobid;
    }

    public int getSkillCoolTime(int skillId) {
        switch (skillId) {
            case 唤灵斗师.死亡:
                return 9;
            case 唤灵斗师.死亡契约:
                return 8;
            case 唤灵斗师.死亡契约2:
                return 6;
            case 唤灵斗师.死亡契约3:
                return 5;
        }
        return 0;
    }

    public boolean isMutipleSummon() {
        switch (skillId) {
            case 火毒.绿水灵病毒:
            case 夜行者.影子蝙蝠_召唤兽:
            case 唤灵斗师.重生:
            case 33101008:
            case 机械师.磁场: {
                return true;
            }
        }
        return false;
    }
}
