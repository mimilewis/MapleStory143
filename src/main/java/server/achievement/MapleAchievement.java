package server.achievement;

import client.MapleCharacter;
import handling.world.WorldBroadcastService;
import tools.MaplePacketCreator;

/**
 * @author KyleShum
 */
public class MapleAchievement {

    private final boolean notice; //是否公告
    private String name; //成就提示
    private int cashReward = 0; //抵用卷奖励
    private int expReward = 0; //经验奖励
    private int mesoReward = 0; //金币奖励
    private int itemReward = 0; //道具奖励

    public MapleAchievement(String name, int cash, int exp, int meso) {
        this.name = name;
        this.cashReward = cash;
        this.expReward = exp;
        this.mesoReward = meso;
        this.notice = true;
    }

    public MapleAchievement(String name, int cash, int exp, int meso, boolean notice) {
        this.name = name;
        this.cashReward = cash;
        this.expReward = exp;
        this.mesoReward = meso;
        this.notice = notice;
    }

    /**
     * 成就提示内容
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 成就点卷奖励
     */
    public int getCashReward() {
        return cashReward;
    }

    public void setCashReward(int cash) {
        this.cashReward = cash;
    }

    /**
     * 成就经验奖励
     */
    public int getExpReward() {
        return expReward;
    }

    public void setExpReward(int exp) {
        this.expReward = exp;
    }

    /**
     * 成就金币奖励
     */
    public int getMesoReward() {
        return mesoReward;
    }

    public void setMesoReward(int meso) {
        this.mesoReward = meso;
    }

    /**
     * 成就道具奖励
     */
    public int getItemReward() {
        return itemReward;
    }

    public void setItemReward(int itemId) {
        this.itemReward = itemId;
    }

    /**
     * 是否全服公告
     */
    public boolean getNotice() {
        return notice;
    }

    /**
     * 获得成就奖励
     */
    public void finishAchievement(MapleCharacter chr) {
        String message = " 获得 ";
        if (this.getCashReward() > 0) {
            message += cashReward + " 点抵用卷 ";
            chr.modifyCSPoints(2, cashReward, true);
        }
        if (this.getExpReward() > 0) {
            message += expReward + " 点经验 ";
            chr.gainExp(expReward, true, true, true);
        }
        if (this.getMesoReward() > 0) {
            message += mesoReward + " 金币.";
            chr.gainMeso(mesoReward, true, true);
        }
        chr.setAchievementFinished(MapleAchievements.getInstance().getByMapleAchievement(this));
        if (notice && !chr.isIntern()) {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, "[成就系统] 祝贺 " + chr.getLevel() + "级 玩家: " + chr.getName() + " " + name + message));
        } else {
            chr.send(MaplePacketCreator.serverNotice(5, "[成就系统] 您因为 " + name + message));
        }
    }
}
