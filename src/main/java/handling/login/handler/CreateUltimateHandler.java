/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.skills.SkillEntry;
import constants.GameConstants;
import constants.JobConstants;
import handling.login.JobType;
import handling.login.LoginInformationProvider;
import handling.world.WorldBroadcastService;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianAccessor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author PlayDK
 */
public class CreateUltimateHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        if (!c.isLoggedIn() || c.getPlayer() == null || c.getPlayer().getLevel() < 120 || c.getPlayer().getQuestStatus(20734) != 0 || c.getPlayer().getQuestStatus(20616) != 2 || !JobConstants.is骑士团(c.getPlayer().getJob())) {
            c.getPlayer().dropMessage(1, "建立终极冒险家失败：\r\n1.等级不够120级\r\n2.任务未完成\r\n3.职业不是骑士团职业\r\n4.已创建过终极冒险家的角色");
            c.announce(MaplePacketCreator.createUltimate(1));
            return;
        }
        if (!c.canMakeCharacter(c.getPlayer().getWorld())) {
            c.announce(MaplePacketCreator.createUltimate(2));
            return;
        }
        String name = slea.readMapleAsciiString();
        int job = slea.readInt(); //job ID

        int face = slea.readInt();
        int hair = slea.readInt();

//        int hat = slea.readInt();
//        int top = slea.readInt();
//        int glove = slea.readInt();
//        int shoes = slea.readInt();
        slea.skip(16);
        int weapon = slea.readInt();
        if (job == 110 || job == 120) {
            weapon = 1402092;
        } else if (job == 130) {
            weapon = 1432082;
        } else if (job == 210 || job == 220 || job == 230) {
            weapon = 1372081;
        } else if (job == 310) {
            weapon = 1452108;
        } else if (job == 320) {
            weapon = 1462095;
        } else if (job == 410) {
            weapon = 1472119;
        } else if (job == 420) {
            weapon = 1332127;
        } else if (job == 510) {
            weapon = 1482081;
        } else if (job == 520) {
            weapon = 1492082;
        } else {
            c.getPlayer().dropMessage(1, "建立终极冒险家失败，职业ID: " + job + " 不正确。");
            c.announce(MaplePacketCreator.createUltimate(1));
            return;
        }

        byte gender = c.getPlayer().getGender();

        JobType jobType = JobType.终极冒险家;
        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setJob(job);
        newchar.setWorld(c.getPlayer().getWorld());
        newchar.setFace(face);
        newchar.setHair(hair);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor((byte) 3); //troll
        newchar.setLevel((short) 50);
        newchar.getStat().str = (short) 4;
        newchar.getStat().dex = (short) 4;
        newchar.getStat().int_ = (short) 4;
        newchar.getStat().luk = (short) 4;
        newchar.setRemainingAp((short) 254); //49*5 + 25 - 16
        newchar.setRemainingSp(job / 100 == 2 ? 128 : 122); //2 from job advancements. 120 from leveling. (mages get +6)
        newchar.getStat().maxhp += 150; //Beginner 10 levels
        newchar.getStat().maxmp += 125;
        switch (job) {
            case 110:
            case 120:
            case 130:
                newchar.getStat().maxhp += 600; //Job Advancement
                newchar.getStat().maxhp += 2000; //Levelup 40 times
                newchar.getStat().maxmp += 200;
                break;
            case 210:
            case 220:
            case 230:
                newchar.getStat().maxmp += 600;
                newchar.getStat().maxhp += 500; //Levelup 40 times
                newchar.getStat().maxmp += 2000;
                break;
            case 310:
            case 320:
            case 410:
            case 420:
            case 520:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 900; //Levelup 40 times
                newchar.getStat().maxmp += 600;
                break;
            case 510:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 450; //Levelup 20 times
                newchar.getStat().maxmp += 300;
                newchar.getStat().maxhp += 800; //Levelup 20 times
                newchar.getStat().maxmp += 400;
                break;
            default:
                return;
        }
        for (int i = 2490; i < 2507; i++) {
            newchar.setQuestAdd(MapleQuest.getInstance(i), (byte) 2, null);
        }
        newchar.setQuestAdd(MapleQuest.getInstance(29947), (byte) 2, null);
        newchar.setQuestAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER), (byte) 0, c.getPlayer().getName());

        Map<Integer, SkillEntry> sDate = new HashMap<>();
        sDate.put(1074 + (job / 100), new SkillEntry((byte) 5, (byte) 5, -1));
        sDate.put(80, new SkillEntry((byte) 1, (byte) 1, -1));
        newchar.changeSkillLevel_Skip(sDate, false);

        MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        int[] items = new int[]{1142257, 1003159, 1052304, 1082290, 1072476, weapon};
        for (byte i = 0; i < items.length; i++) {
            Item item = li.getEquipById(items[i]);
            item.setPosition((byte) (i + 1));
            newchar.getInventory(MapleInventoryType.EQUIP).addFromDB(item);
        }
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 100, (byte) 0));
        c.getPlayer().fakeRelog();
        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm())) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, (short) 0, true);
            MapleQuest.getInstance(20734).forceComplete(c.getPlayer(), 1101000);
            c.announce(MaplePacketCreator.createUltimate(0));
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, "[系统公告] 恭喜玩家 " + c.getPlayer().getName() + " 创建了终极冒险家。"));
        } else {
            c.announce(MaplePacketCreator.createUltimate(3));
        }
    }
}
