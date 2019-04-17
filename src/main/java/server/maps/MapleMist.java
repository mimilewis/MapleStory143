package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.skills.*;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;

/**
 * 表示冒险岛地图中所有具体烟幕效果的对象.例如侠盗职业的四转技能烟幕弹
 *
 * @author dongjak
 */
public class MapleMist extends MapleMapObject {

    private final Rectangle mistPosition;
    private final boolean isMobMist;
    private final int skilllevel;
    private final int ownerId;
    private MapleStatEffect source;
    private MobSkill skill;
    private boolean isPoisonMist;
    private boolean isRecoverMist;
    private boolean isFacingLeft;
    private int skillDelay;
    private int mistType;
    private ScheduledFuture<?> schedule = null, poisonSchedule = null;
    private boolean isHolyFountain; //是否为神圣源泉
    private int healCount; //神圣源泉恢复的总次数
    private boolean isBurnAreas;
    private Point ownerPosition;
    private int subtype;
    private int force, forcex;

    public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill, Point position) {
        this.mistPosition = mistPosition;
        this.ownerId = mob.getObjectId();
        this.skill = skill;
        this.skilllevel = skill.getSkillLevel();
        this.isMobMist = true;
        this.isPoisonMist = true;
        this.isRecoverMist = false;
        this.mistType = 0;
        this.skillDelay = 0;
        this.force = skill.getForce();
        this.forcex = skill.getForcex();
        this.setPosition(position);
    }

    /*
     * 角色技能召唤的烟雾
     */
    public MapleMist(Rectangle mistPosition, MapleCharacter owner, MapleStatEffect source, Point point) {
        this.mistPosition = mistPosition;
        this.ownerPosition = owner.getPosition();
        this.ownerId = owner.getId();
        this.source = source;
        this.skillDelay = 10;
        this.isMobMist = false;
        this.isPoisonMist = false;
        this.isRecoverMist = false;
        this.healCount = 0;
        this.isHolyFountain = false;
        this.skilllevel = owner.getTotalSkillLevel(SkillFactory.getSkill(source.getSourceid()));
        this.isFacingLeft = owner.isFacingLeft();
        this.setPosition(point);
        switch (source.getSourceid()) {
            case 主教.神圣源泉:
                this.healCount = source.getY();
                this.isHolyFountain = true;
                break;
            case 唤灵斗师.避难所:
                this.mistType = 3;
                break;
            case 1076:  //奥兹的火牢术屏障
            case 火毒.致命毒雾:
                this.isPoisonMist = true;
                break;
            case 炎术士.燃烧领域:
                this.isBurnAreas = true;
                this.skillDelay = 2;
                break;
            case 林之灵.火焰屁:
                this.isPoisonMist = true;
                this.mistPosition.y += 50;
                break;
            case 林之灵.喵喵空间:
                this.skillDelay = 2;
                this.isRecoverMist = true;
                break;
            case 战神.摩诃领域_MIST:
                this.skillDelay = 2;
                this.isRecoverMist = true;
                break;
            default:
                mistType = 0;
        }
    }

    public MapleMist(Rectangle mistPosition, MapleCharacter owner) {
        this.mistPosition = mistPosition;
        this.ownerId = owner.getId();
        this.source = new MapleStatEffect();
        this.source.setSourceid(火毒.致命毒雾);
        this.skilllevel = 30;
        this.mistType = 0;
        this.isMobMist = false;
        this.isPoisonMist = false;
        this.skillDelay = 10;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MIST;
    }

    @Override
    public Point getPosition() {
        return mistPosition.getLocation();
    }

    @Override
    public void setPosition(Point position) {
    }

    public Skill getSourceSkill() {
        return SkillFactory.getSkill(source.getSourceid());
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setSchedule(ScheduledFuture<?> s) {
        this.schedule = s;
    }

    public ScheduledFuture<?> getPoisonSchedule() {
        return poisonSchedule;
    }

    public void setPoisonSchedule(ScheduledFuture<?> s) {
        this.poisonSchedule = s;
    }

    /*
     * 是否为怪物召唤的烟雾
     */
    public boolean isMobMist() {
        return isMobMist;
    }

    /*
     * 是否为中毒效果的烟雾
     */
    public boolean isPoisonMist() {
        return isPoisonMist;
    }

    /*
     * 是否为恢复效果的烟雾
     */
    public boolean isRecoverMist() {
        return isRecoverMist;
    }

    /*
     * 是否为牧师的神圣源泉
     */
    public boolean isHolyFountain() {
        return isHolyFountain;
    }

    /*
     * 是否为炎术士燃烧领域
    */
    public boolean isBurnAreas() {
        return isBurnAreas;
    }

    public int getHealCount() {
        return isHolyFountain() ? healCount : 0;
    }

    public void setHealCount(int count) {
        healCount = count;
    }

    public int getMistType() {
        return mistType;
    }

    public void setMistType(int mistType) {
        this.mistType = mistType;
    }

    public int getSkillDelay() {
        return skillDelay;
    }

    public void setSkillDelay(int skillDelay) {
        this.skillDelay = skillDelay;
    }

    public int getSkillLevel() {
        return skilllevel;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public MobSkill getMobSkill() {
        return this.skill;
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    public MapleStatEffect getSource() {
        return source;
    }

    public byte[] fakeSpawnData(int level) {
        return MaplePacketCreator.spawnMist(this);
    }

    public Point getOwnerPosition() {
        return ownerPosition;
    }

    public boolean isFacingLeft() {
        return isFacingLeft;
    }

    public int getSubtype() {
        return subtype;
    }

    public void setSubtype(int subtype) {
        this.subtype = subtype;
    }

    public int getForce() {
        return force;
    }

    public int getForcex() {
        return forcex;
    }

    @Override
    public void sendSpawnData(MapleClient c) {
        c.announce(MaplePacketCreator.spawnMist(this));
    }

    @Override
    public void sendDestroyData(MapleClient c) {
        c.announce(MaplePacketCreator.removeMist(getObjectId(), false));
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }
}
