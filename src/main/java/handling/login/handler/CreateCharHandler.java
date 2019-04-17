/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ServerConstants;
import handling.login.JobType;
import handling.login.LoginInformationProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.data.input.LittleEndianAccessor;
import tools.packet.LoginPacket;

/**
 * @author PlayDK
 */
public class CreateCharHandler {

    private static final Logger log = LogManager.getLogger(CreateCharHandler.class);

    public static void getCreatCharAuth(LittleEndianAccessor slea, MapleClient c) {
        c.announce(LoginPacket.getCreatCharAuth(c));
    }

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        /*
         * BIGBANG:
         * 0 = 反抗者
         * 1 = 冒险家
         * 2 = 骑士团
         * 3 = 战神
         * 4 = 龙神
         * 5 = 双弩
         * 6 = 恶魔
         * 7 = 幻影
         * 8 = 龙的传人
         */
        String name;
        byte gender, skin, hairColor;
        short subcategory;
        int face, hair, faceMark = 0;
        int cap = 0; // 帽子
        int top; //上衣或者套服
        int cape = 0; //披风
        int bottom = 0; //裤子
        int shoes; //鞋子
        int glove = 0; //手套
        int weapon; //武器
        int shield = 0; //盾牌

        JobType job;
        name = slea.readMapleAsciiString();
        int keymode = slea.readInt(); //V.117.1新增 键盘模式 0 = 老键盘模式  1 = 新键盘模式
        if (keymode != 0 && keymode != 1) {
            System.out.println("创建角色错误 键盘模式错误 当前模式: " + keymode);
            c.announce(LoginPacket.charNameResponse(name, (byte) 3));
            return;
        }
        slea.readInt(); // [FF FF FF FF] V.106新增 未知
        int job_type = slea.readInt();
        job = JobType.getByType(job_type);
        if (job == null) {
            System.out.println("创建角色错误 没有找到该职业类型的数据 当前职业类型: " + job_type);
            return;
        }
        // 职业开放状态
//        if (!ServerConstants.JOB_OPENLIST[job.type]) {
//            c.announce(LoginPacket.charNameResponse(name, (byte) 3));
//            c.announce(MaplePacketCreator.serverNotice(1, "该职业已关闭创建。"));
//            return;
//        }
        subcategory = slea.readShort(); //暗影双刀 = 1  龙的传人 = 10
        JobType jobType = JobType.getByType(job_type);
        gender = slea.readByte();
        skin = slea.readByte();
        hairColor = slea.readByte();
        face = slea.readInt();
        hair = slea.readInt();
        if (job.faceMark) {
            faceMark = slea.readInt();
        }
        if (job == JobType.林之灵) {
            slea.readInt(); //帽子
            slea.readInt(); //披风
        } else if (job == JobType.剑豪 || job == JobType.阴阳师) {
            cap = slea.readInt();
        }
        top = slea.readInt(); //上衣或者套服
        if (job.cape) {
            cape = slea.readInt(); //披风
        }
        // 104 = 上衣 105 = 套服 如果是套服就没有裤子
        if (top / 10000 == 104 || job.bottom) {
            bottom = slea.readInt(); //裤子
        }
        shoes = slea.readInt(); //鞋子
        if (job == JobType.剑豪 || job == JobType.阴阳师) {
            glove = slea.readInt();
        }
        weapon = slea.readInt(); //武器
        if (slea.available() >= 4) {
            shield = slea.readInt();
        }
        if (ServerConstants.isShowPacket()) {
            log.info("\r\n名字: " + name
                    + "\r\n职业: " + job_type
                    + "\r\n性别: " + gender
                    + "\r\n皮肤: " + skin
                    + "\r\n头发: " + hairColor
                    + "\r\n脸型: " + face
                    + "\r\n发型: " + hair
                    + "\r\n脸饰: " + faceMark
                    + "\r\n帽子: " + cap
                    + "\r\n上衣: " + top
                    + "\r\n裤子: " + bottom
                    + "\r\n鞋子: " + shoes
                    + "\r\n手套: " + glove
                    + "\r\n武器: " + weapon
                    + "\r\n盾牌: " + shield + "\r\n");
        }
        //检测角色的装备是否合法
        MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        int[] items = new int[]{top, bottom, cape, shoes, weapon, shield};
        for (int i : items) {
            if (!LoginInformationProvider.getInstance().isEligibleItem(i)) {
                log.info("[作弊] 新建角色装备检测失败 名字: " + name + " 职业: " + job_type + " 道具ID: " + i + " - " + li.getName(i));
                c.announce(LoginPacket.charNameResponse(name, (byte) 3));
                return;
            }
        }
        //生成1个新角色的模版
        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setWorld((byte) c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair); // + 头发颜色
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skin);
        newchar.setDecorate(faceMark);
        newchar.setGmLevel(c.getGmLevel());
        if (job == JobType.神之子) {
            newchar.setLevel((short) 100);
            newchar.getStat().str = 518;
            newchar.getStat().dex = 4;
            newchar.getStat().luk = 4;
            newchar.getStat().int_ = 4;
            newchar.getStat().maxhp = 6485;
            newchar.getStat().hp = 6485;
            newchar.getStat().maxmp = 100;
            newchar.getStat().mp = 100;
            newchar.setRemainingSp(3, 0);
            newchar.setRemainingSp(3, 1);
        } else if (job == JobType.林之灵) {
            newchar.getStat().maxhp = 570;
            newchar.getStat().hp = 570;
            newchar.getStat().maxmp = 270;
            newchar.getStat().mp = 270;
            newchar.setRemainingAp((short) 45);
            newchar.setRemainingSp(3, 0);
            newchar.updateInfoQuest(59300, "bTail=1;bEar=1;TailID=5010119;EarID=5010116", false);
        } else if (job == JobType.超能力者) {
            newchar.getStat().maxhp = 570;
            newchar.getStat().hp = 570;
            newchar.getStat().maxmp = 0;
            newchar.getStat().mp = 0;
            newchar.setRemainingAp((short) 45);
        } else if (job == JobType.恶魔猎手) {
            newchar.getStat().mp = 10;
            newchar.getStat().maxmp = 10;
        }

        // 部分特殊职业出生要设置为10级
        switch (job) {
            case 林之灵:
            case 超能力者:
            case 夜光:
            case 狂龙:
            case 萝莉:
            case 尖兵:
            case 隐月:
            case 剑豪:
            case 阴阳师:
            case 米哈尔:
            case 幻影:
            case 恶魔猎手:
                newchar.setLevel((short) 10);
                newchar.setRemainingAp((short) 50);
        }

        //给新角色装备
        MapleInventory equipedIv = newchar.getInventory(MapleInventoryType.EQUIPPED);
        Item item;
        int[][] equips = new int[][]{
                {cap, -1},
                {top, -5}, //上衣或者套服
                {bottom, -6}, //裤子
                {shoes, -7}, //鞋子
                {glove, -8}, //手套
                {cape, -9}, //披风
                {weapon, -11}, //武器
                {shield, -10}}; //盾牌
        for (int[] i : equips) {
            if (i[0] > 0) {
                item = li.getEquipById(i[0]);
                item.setPosition((byte) i[1]);
                item.setGMLog("角色创建");
                equipedIv.addFromDB(item);
            }
        }
        //给新角色药水
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000013, (byte) 0, (short) 100, (byte) 0));
        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000014, (byte) 0, (short) 100, (byte) 0));
        //给新角色指南书
        int[][] guidebooks = new int[][]{{4161001, 0}, {4161047, 1}, {4161048, 2000}, {4161052, 2001}, {4161054, 3}, {4161079, 2002}};
        int guidebook = 0;
        for (int[] i : guidebooks) {
            if (newchar.getJob() == i[1]) {
                guidebook = i[0];
            } else if (newchar.getJob() / 1000 == i[1]) {
                guidebook = i[0];
            }
        }
        if (guidebook > 0) {
            newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(guidebook, (byte) 0, (short) 1, (byte) 0));
        }
        //骑士团职业给角色任务
        if (job.equals(JobType.骑士团)) {
            newchar.setQuestAdd(MapleQuest.getInstance(20022), (byte) 1, "1");
            newchar.setQuestAdd(MapleQuest.getInstance(20010), (byte) 1, null);
        }
        //开始检测发送创建新的角色
        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()) && (c.isGm() || c.canMakeCharacter(c.getWorld()))) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, subcategory, keymode == 0);
            c.announce(LoginPacket.addNewCharEntry(newchar, true));
            c.createdChar(newchar.getId());
        } else {
            c.announce(LoginPacket.addNewCharEntry(newchar, false));
        }
    }
}
