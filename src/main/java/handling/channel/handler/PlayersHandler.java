package handling.channel.handler;

import client.*;
import client.anticheat.CheatingOffense;
import client.anticheat.ReportType;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import handling.opcode.EffectOpcode;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.reactor.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.*;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianAccessor;
import tools.packet.EffectPacket;
import tools.packet.UIPacket;

import java.awt.*;

public class PlayersHandler {

    private static final Logger log = LogManager.getLogger(PlayersHandler.class.getName());

    public static void Note(LittleEndianAccessor slea, MapleCharacter chr) {
        byte type = slea.readByte();
        switch (type) {
            case 0:
                String name = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                boolean fame = slea.readByte() > 0;
                slea.readInt(); //0?
                Item itemz = chr.getCashInventory().findByCashId((int) slea.readLong());
                if (itemz == null || !itemz.getGiftFrom().equalsIgnoreCase(name) || !chr.getCashInventory().canSendNote(itemz.getUniqueId())) {
                    return;
                }
                try {
                    chr.sendNote(name, msg, fame ? 1 : 0);
                    chr.getCashInventory().sendedNote(itemz.getUniqueId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 1: //删除
                /*
                 * Send NOTE_ACTION [00DB] (14)
                 * DB 00
                 * 01 - 删除
                 * 02 00 - 2条信息
                 * 01 - 人气
                 * 01 00 00 00 - 消息的SQLid
                 * 02 00 00 00 - 消息的SQLid
                 */
                int num = slea.readShort();
                slea.readByte(); //未知
                for (int i = 0; i < num; i++) {
                    int id = slea.readInt();
                    int giveFame = slea.readByte();
                    chr.deleteNote(id, giveFame);
                }
                break;
            default:
                System.out.println("Unhandled note action, " + type + "");
        }
    }

    public static void GiveFame(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int who = slea.readInt();
        int mode = slea.readByte();
        int famechange = mode == 0 ? -1 : 1;
        MapleCharacter target = chr.getMap().getCharacterById(who);
        if (target == null || target == chr) { // faming self
            chr.getCheatTracker().registerOffense(CheatingOffense.FAMING_SELF, "不能对自身操作.");
            return;
        } else if (chr.getLevel() < 15) {
            chr.getCheatTracker().registerOffense(CheatingOffense.FAMING_UNDER_15, "等级小于15级.");
            return;
        }
        switch (chr.canGiveFame(target)) {
            case OK:
                if (Math.abs(target.getFame() + famechange) <= 99999) {
                    target.addFame(famechange);
                    target.updateSingleStat(MapleStat.人气, target.getFame());
                }
                if (!chr.isGM()) {
                    chr.hasGivenFame(target);
                }
                c.announce(MaplePacketCreator.giveFameResponse(mode, target.getName(), target.getFame()));
                target.getClient().announce(MaplePacketCreator.receiveFame(mode, chr.getName()));
                break;
            case NOT_TODAY:
                c.announce(MaplePacketCreator.giveFameErrorResponse(3));
                break;
            case NOT_THIS_MONTH:
                c.announce(MaplePacketCreator.giveFameErrorResponse(4));
                break;
        }
    }

    public static void UseDoor(LittleEndianAccessor slea, MapleCharacter chr) {
        int oid = slea.readInt();
        boolean mode = slea.readByte() == 0; // specifies if backwarp or not, 1 town to target, 0 target to town
        for (MapleMapObject obj : chr.getMap().getAllDoorsThreadsafe()) {
            MapleDoor door = (MapleDoor) obj;
            if (door.getOwnerId() == oid) {
                door.warp(chr, mode);
                break;
            }
        }
    }

    public static void UseMechDoor(LittleEndianAccessor slea, MapleCharacter chr) {
        int oid = slea.readInt();
        Point pos = slea.readPos();
        int mode = slea.readByte(); // specifies if backwarp or not, 1 town to target, 0 target to town
        if (chr != null) {
            chr.send(MaplePacketCreator.enableActions());
            for (MapleMapObject obj : chr.getMap().getAllMechDoorsThreadsafe()) {
                MechDoor door = (MechDoor) obj;
                if (door == null) {
                    continue;
                }
                if (door.getOwnerId() == oid && door.getId() == mode) {
                    chr.checkFollow();
                    chr.getMap().movePlayer(chr, pos);
                    break;
                }
            }
        }
    }

    /*
     * 使用神圣源泉
     */
    public static void UseHolyFountain(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        int mode = slea.readByte(); //0x02 为恢复Hp
        int oid = slea.readInt();
        int skillId = slea.readInt();
        Point position = slea.readPos();
        MapleMist mist = c.getPlayer().getMap().getMistByOid(oid);
        if (mist == null || !mist.isHolyFountain()) {
            return;
        }
        if (mist.getHealCount() > 0 && mist.getBox().contains(position)) {
            MapleCharacter owner = chr.getMap().getCharacterById(mist.getOwnerId());
            if (mist.getOwnerId() == chr.getId() || (owner != null && owner.getParty() != null && chr.getParty() != null && owner.getParty().getPartyId() == chr.getParty().getPartyId())) {
                int healHp = (int) (chr.getStat().getCurrentMaxHp() * (mist.getSource().getX() / 100.0));
                chr.addHP(healHp);
                mist.setHealCount(mist.getHealCount() - 1);
                if (chr.isAdmin()) {
                    chr.dropMessage(5, "使用神圣源泉 - 恢复血量: " + healHp + " 百分比: " + (mist.getSource().getX() / 100.0) + " 剩余次数: " + mist.getHealCount());
                }
                c.announce(EffectPacket.showOwnBuffEffect(skillId, EffectOpcode.UserEffect_SkillAffected.getValue(), chr.getLevel(), mist.getSkillLevel()));
                chr.getMap().broadcastMessage(chr, EffectPacket.showBuffeffect(chr, skillId, EffectOpcode.UserEffect_SkillAffected.getValue(), chr.getLevel(), mist.getSkillLevel()), false);
            }
        } else if (chr.isAdmin()) {
            chr.dropMessage(5, "使用神圣源泉出现错误 - 源泉恢复的剩余次数: " + mist.getHealCount() + " 模式: " + mode + " 是否在范围内: " + mist.getBox().contains(position));
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    public static void TransformPlayer(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        chr.updateTick(slea.readInt());
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        String target = slea.readMapleAsciiString();
        Item toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        switch (itemId) {
            case 2212000: //圣诞节组队药水
                MapleCharacter search_chr = chr.getMap().getCharacterByName(target);
                if (search_chr != null) {
                    MapleItemInformationProvider.getInstance().getItemEffect(2210023).applyTo(search_chr);
                    search_chr.dropMessage(6, chr.getName() + " has played a prank on you!");
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                } else {
                    chr.dropMessage(1, "在当前地图中未找到 '" + target + "' 的玩家.");
                }
                break;
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    public static void HitReactor(LittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        int charPos = slea.readInt();
        short stance = slea.readShort();
        MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (reactor == null || !reactor.isAlive()) {
            return;
        }
        reactor.hitReactor(charPos, stance, c);
    }

    public static void TouchReactor(LittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        boolean touched = slea.available() == 0 || slea.readByte() > 0; //the byte is probably the state to set it to
        MapleReactor reactor = c.getPlayer().getMap().getReactorByOid(oid);
        if (!touched || reactor == null || !reactor.isAlive() || reactor.getTouch() == 0) {
            //System.out.println("点击反应堆出现错误 - !touched: " + !touched + " !isAlive: " + !reactor.isAlive() + " Touch: " + reactor.getTouch());
            return;
        }
        if (c.getPlayer().isAdmin()) {
            c.getPlayer().dropMessage(5, "反应堆信息 - oid: " + oid + " Touch: " + reactor.getTouch() + " isTimerActive: " + reactor.isTimerActive() + " ReactorType: " + reactor.getReactorType());
        }
        if (reactor.getTouch() == 2) {
            ReactorScriptManager.getInstance().act(c, reactor); //not sure how touched boolean comes into play
        } else if (reactor.getTouch() == 1 && !reactor.isTimerActive()) {
            if (reactor.getReactorType() == 100) {
                int itemid = GameConstants.getCustomReactItem(reactor.getReactorId(), reactor.getReactItem().getLeft());
                if (c.getPlayer().haveItem(itemid, reactor.getReactItem().getRight())) {
                    if (reactor.getArea().contains(c.getPlayer().getTruePosition())) {
                        MapleInventoryManipulator.removeById(c, ItemConstants.getInventoryType(itemid), itemid, reactor.getReactItem().getRight(), true, false);
                        reactor.hitReactor(c);
                    } else {
                        c.getPlayer().dropMessage(5, "距离太远。请靠近后重新尝试。");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "You don't have the item required.");
                }
            } else {
                reactor.hitReactor(c); //just hit it
            }
        }
    }

    public static void UseRune(LittleEndianAccessor slea, MapleCharacter chr) {
        slea.readLong();
        int n3 = (int) (chr.getRuneNextActionTime() - System.currentTimeMillis());
        if (n3 >= 0 && chr.getLevel() >= 30) {
            if (chr.isAdmin()) {
                chr.send(MaplePacketCreator.RuneAction(7, 0));
            } else {
                long l2 = 900000;
                if ((long) n3 > l2) {
                    chr.setRuneNextActionTime(l2);
                }
                chr.send(MaplePacketCreator.RuneAction(2, n3));
            }
        } else if (chr.getLevel() < 30) {
            chr.send(UIPacket.getSpecialTopMsg("使用符文必须等级达到30级以上！", 3, 20, 0));
            chr.sendEnableActions();
        } else {
            chr.send(MaplePacketCreator.RuneAction(7, 0));
        }
    }

    public static void UseRuneSkillReq(LittleEndianAccessor slea, MapleCharacter player) {
        boolean bl2 = slea.readByte() == 1;
        if (bl2) {
            if (player.getMap().getAllRuneThreadsafe() != null) {
                player.getMap().getAllRuneThreadsafe().get(0).applyToPlayer(player);
            }
            long l2 = 900000;
            player.setRuneNextActionTime(l2);
        } else {
            int n2 = 60000;
            player.setRuneNextActionTime(n2);
            player.send(MaplePacketCreator.RuneAction(2, n2));
        }
    }

    public static void FollowRequest(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter tt = c.getPlayer().getMap().getCharacterById(slea.readInt());
        if (slea.readByte() > 0) {
            //1 when changing map
            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getFollowId() == c.getPlayer().getId()) {
                tt.setFollowOn(true);
                c.getPlayer().setFollowOn(true);
            } else {
                c.getPlayer().checkFollow();
            }
            return;
        }
        if (slea.readByte() > 0) { //cancelling follow
            tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getFollowId() == c.getPlayer().getId() && c.getPlayer().isFollowOn()) {
                c.getPlayer().checkFollow();
            }
            return;
        }
        if (tt != null && tt.getPosition().distanceSq(c.getPlayer().getPosition()) < 10000 && tt.getFollowId() == 0 && c.getPlayer().getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
            tt.setFollowId(c.getPlayer().getId());
            tt.setFollowOn(false);
            tt.setFollowInitiator(false);
            c.getPlayer().setFollowOn(false);
            c.getPlayer().setFollowInitiator(false);
            tt.getClient().announce(MaplePacketCreator.followRequest(c.getPlayer().getId()));
        } else {
            c.announce(MaplePacketCreator.serverNotice(1, "距离太远。"));
        }
    }

    public static void FollowReply(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getFollowId() > 0 && c.getPlayer().getFollowId() == slea.readInt()) {
            MapleCharacter tt = c.getPlayer().getMap().getCharacterById(c.getPlayer().getFollowId());
            if (tt != null && tt.getPosition().distanceSq(c.getPlayer().getPosition()) < 10000 && tt.getFollowId() == 0 && tt.getId() != c.getPlayer().getId()) { //estimate, should less
                boolean accepted = slea.readByte() > 0;
                if (accepted) {
                    tt.setFollowId(c.getPlayer().getId());
                    tt.setFollowOn(true);
                    tt.setFollowInitiator(false);
                    c.getPlayer().setFollowOn(true);
                    c.getPlayer().setFollowInitiator(true);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.followEffect(tt.getId(), c.getPlayer().getId(), null));
                } else {
                    c.getPlayer().setFollowId(0);
                    tt.setFollowId(0);
                    tt.getClient().announce(MaplePacketCreator.getFollowMsg(5));
                }
            } else {
                if (tt != null) {
                    tt.setFollowId(0);
                    c.getPlayer().setFollowId(0);
                }
                c.announce(MaplePacketCreator.serverNotice(1, "距离太远."));
            }
        } else {
            c.getPlayer().setFollowId(0);
        }
    }

    /*
     * 1112300 - 月长石戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112301 - 月长石戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112302 - 月长石戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112303 - 闪耀新星戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112304 - 闪耀新星戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112305 - 闪耀新星戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112306 - 金心戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112307 - 金心戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112308 - 金心戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112309 - 银翼戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112310 - 银翼戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112311 - 银翼戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
     * 1112315 - 恩爱夫妻结婚戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指可能会#c消失#。
     * 1112316 - 恩爱夫妻结婚戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指可能会#c消失#。
     * 1112317 - 恩爱夫妻结婚戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指可能会#c消失#。
     * 1112318 - 鸳鸯夫妻结婚戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指可能会#c消失#。
     * 1112319 - 鸳鸯夫妻结婚戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指可能会#c消失#。
     * 1112320 - 鸳鸯夫妻结婚戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指可能会#c消失#。
     *
     * 2240004 - 月长石戒指 - 用月亮的石头和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240005 - 月长石戒指2克拉 - 用月亮的石头和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240006 - 月长石戒指3克拉 - 用月亮的石头和23克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240007 - 闪耀新星戒指 - 用星星的石头和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240008 - 闪耀新星戒指2克拉 - 用星星的石头和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240009 - 闪耀新星戒指3克拉 - 用星星的石头和3克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240010 - 金心戒指 - 用黄金和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240011 - 金心戒指2克拉 - 用黄金和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240012 - 金心戒指3克拉 - 用黄金和3克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240013 - 银翼戒指 - 用银和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240014 - 银翼戒指2克拉 - 用银和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     * 2240015 - 银翼戒指3克拉 - 用银和3克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
     */
    public static void DoRing(MapleClient c, String name, int itemid) {
        int newItemId = getMarriageNewItemId(itemid);
        MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        /*
         * MarriageId 判断是否结婚
         * MarriageItemId 判断求婚状态
         */
        int errcode = 0x00;
        if (JobConstants.is神之子(c.getPlayer().getJob())) { //该道具不能用于神之子
            errcode = 0x15;
        } else if (c.getPlayer().getMarriageId() > 0) { //您已经是结婚的状态.
            errcode = 0x1D;
        } else if (c.getPlayer().getMarriageItemId() > 0) { //您已经是订婚的状态.
            errcode = 0x1B;
        } else if (!c.getPlayer().haveItem(itemid, 1) || itemid < 2240004 || itemid > 2240015) { //订婚失败
            errcode = 0x0F;
        } else if (chr == null) { //当前频道、地图找不到该角色或角色名错误.
            errcode = 0x16;
        } else if (JobConstants.is神之子(chr.getJob())) { //对方不在同一地图.
            errcode = 0x15;
        } else if (chr.getMapId() != c.getPlayer().getMapId()) { //对方不在同一地图.
            errcode = 0x17;
        } else if (chr.getGender() == c.getPlayer().getGender()) { //同性不能结婚.
            errcode = 0x1A;
        } else if (chr.getMarriageId() > 0) { //对方已经是结婚的状态.
            errcode = 0x1E;
        } else if (chr.getMarriageItemId() > 0) { //对方已经是订婚的状态.
            errcode = 0x1C;
        } else if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "")) { //道具栏已满.请整理其他窗口.
            errcode = 0x18;
            //System.err.println("自己是否有位置: " + !MapleInventoryManipulator.checkSpace(c, newItemId, 1, ""));
        } else if (!MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) { //对方的道具栏已满.
            errcode = 0x19;
            //System.err.println("对方是否有位置: " + !MapleInventoryManipulator.checkSpace(c, newItemId, 1, ""));
        }
        if (errcode > 0) {
            c.announce(MaplePacketCreator.sendEngagement((byte) errcode, 0, null, null));
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().setMarriageItemId(itemid);
        chr.send(MaplePacketCreator.sendEngagementRequest(c.getPlayer().getName(), c.getPlayer().getId()));
    }

    public static void RingAction(LittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        if (mode == 0) {
            DoRing(c, slea.readMapleAsciiString(), slea.readInt());
        } else if (mode == 1) {
            c.getPlayer().setMarriageItemId(0);
        } else if (mode == 2) { //accept/deny proposal
            boolean accepted = slea.readByte() > 0;
            String name = slea.readMapleAsciiString();
            int id = slea.readInt();
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (c.getPlayer().getMarriageId() > 0 || chr == null || chr.getId() != id || chr.getMarriageItemId() <= 0 || !chr.haveItem(chr.getMarriageItemId(), 1) || chr.getMarriageId() > 0 || !chr.isAlive() || chr.getEventInstance() != null || !c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null) {
                c.announce(MaplePacketCreator.sendEngagement((byte) 0x1F, 0, null, null)); //对方处于无法接受求婚的状态.
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
            if (accepted) {
                int itemid = chr.getMarriageItemId();
                int newItemId = getMarriageNewItemId(itemid);
                if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "") || !MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
                    c.announce(MaplePacketCreator.sendEngagement((byte) 0x15, 0, null, null));
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                try {
                    int[] ringID = MapleRing.makeRing(newItemId, c.getPlayer(), chr);
                    Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[1]);
                    MapleRing ring = MapleRing.loadFromDb(ringID[1]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(c, eq);
                    eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[0]);
                    ring = MapleRing.loadFromDb(ringID[0]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(chr.getClient(), eq);
                    MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, chr.getMarriageItemId(), 1, false, false);
                    chr.send(MaplePacketCreator.sendEngagement((byte) 0x0D, newItemId, chr, c.getPlayer())); //恭喜你订婚成功.
                    chr.setMarriageId(c.getPlayer().getId());
                    c.getPlayer().setMarriageId(chr.getId());
                    chr.fakeRelog();
                    c.getPlayer().fakeRelog();
                    WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.yellowChat("[系统公告] 恭喜：" + c.getPlayer().getName() + " 和 " + chr.getName() + "结为夫妻。 希望你们在 " + chr.getClient().getChannelServer().getServerName() + " 游戏中玩的愉快!"));
                } catch (Exception e) {
                    log.error("戒指操作错误", e);
                }
            } else {
                chr.send(MaplePacketCreator.sendEngagement((byte) 0x20, 0, null, null));
            }
            c.announce(MaplePacketCreator.enableActions());
            chr.setMarriageItemId(0);
        } else if (mode == 3) { //drop, only works for ETC
            int itemId = slea.readInt();
            MapleInventoryType type = ItemConstants.getInventoryType(itemId);
            Item item = c.getPlayer().getInventory(type).findById(itemId);
            if (item != null && type == MapleInventoryType.ETC && itemId / 10000 == 421) {
                MapleInventoryManipulator.drop(c, type, item.getPosition(), item.getQuantity());
            }
        }
    }

    private static int getMarriageNewItemId(int itemId) {
        int newItemId;
        if (itemId == 2240004) { //月长石戒指 - 用月亮的石头和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112300; //月长石戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240005) { //月长石戒指2克拉 - 用月亮的石头和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112301; //月长石戒指2克拉 -  爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240006) { //月长石戒指3克拉 - 用月亮的石头和23克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚
            newItemId = 1112302; //月长石戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240007) { //闪耀新星戒指 - 用星星的石头和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112303; //闪耀新星戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240008) { //闪耀新星戒指2克拉 - 用星星的石头和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112304; //闪耀新星戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240009) { //闪耀新星戒指3克拉 - 用星星的石头和3克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112305; //闪耀新星戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240010) { //金心戒指 - 用黄金和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112306; //金心戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240011) { //金心戒指2克拉 - 用黄金和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112307; //金心戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240012) { //金心戒指3克拉 - 用黄金和3克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112308; //金心戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240013) { //银翼戒指 - 用银和钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112309; //银翼戒指1克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240014) { //银翼戒指2克拉 - 用银和2克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112310; //银翼戒指2克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else if (itemId == 2240015) { //银翼戒指3克拉 - 用银和3克拉的钻石加工而成的求婚戒指。可以用来向心爱的异性求婚。
            newItemId = 1112311; //银翼戒指3克拉 - 爱情与婚姻的象征。\n注：结婚人士如果#c离婚#，该戒指将会#c消失#。
        } else {
            throw new RuntimeException("Invalid Item Maker id");
        }
        return newItemId;
    }

    public static void Solomon(LittleEndianAccessor slea, MapleClient c) {
        c.announce(MaplePacketCreator.enableActions());
        c.getPlayer().updateTick(slea.readInt());
        Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slea.readShort());
        if (item == null || item.getItemId() != slea.readInt() || item.getQuantity() <= 0 || c.getPlayer().getGachExp() > 0 || c.getPlayer().getLevel() > 50 || MapleItemInformationProvider.getInstance().getItemEffect(item.getItemId()).getEXP() <= 0) {
            return;
        }
        c.getPlayer().setGachExp(c.getPlayer().getGachExp() + MapleItemInformationProvider.getInstance().getItemEffect(item.getItemId()).getEXP());
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, item.getPosition(), (short) 1, false);
        c.getPlayer().updateSingleStat(MapleStat.GACHAPONEXP, c.getPlayer().getGachExp());
    }

    public static void GachExp(LittleEndianAccessor slea, MapleClient c) {
        c.announce(MaplePacketCreator.enableActions());
        c.getPlayer().updateTick(slea.readInt());
        if (c.getPlayer().getGachExp() <= 0) {
            return;
        }
        c.getPlayer().gainExp(c.getPlayer().getGachExp() * GameConstants.getExpRate_Quest(c.getPlayer().getLevel()), true, true, false);
        c.getPlayer().setGachExp(0);
        c.getPlayer().updateSingleStat(MapleStat.GACHAPONEXP, 0);
    }

    public static void Report(LittleEndianAccessor slea, MapleClient c) {
        //0 = success 1 = unable to locate 2 = once a day 3 = you've been reported 4+ = unknown reason
        MapleCharacter other;
        ReportType type;
        type = ReportType.getById(slea.readByte());
        other = c.getPlayer().getMap().getCharacterByName(slea.readMapleAsciiString());
        //then,byte(?) and string(reason)
        if (other == null || type == null || other.isIntern()) {
            //c.announce(MaplePacketCreator.report(4));
            c.getPlayer().dropMessage(1, "举报错误.");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.REPORT_QUEST));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        long currentTime = System.currentTimeMillis();
        long theTime = Long.parseLong(stat.getCustomData());
        if (theTime + 7200000 > currentTime && !c.getPlayer().isIntern()) {
            c.getPlayer().dropMessage(5, "每2小时才能举报1次.");
        } else {
            stat.setCustomData(String.valueOf(currentTime));
            other.addReport(type);
            //c.announce(MaplePacketCreator.report(GameConstants.GMS ? 2 : 0));
            c.getPlayer().dropMessage(1, "举报完成.");
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    public static boolean inArea(MapleCharacter chr) {
        for (Rectangle rect : chr.getMap().getAreas()) {
            if (rect.contains(chr.getTruePosition())) {
                return true;
            }
        }
        for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
            if (mist.getOwnerId() == chr.getId() && mist.getMistType() == 2 && mist.getBox().contains(chr.getTruePosition())) {
                return true;
            }
        }
        return false;
    }

    /*
     * 测谎仪系统
     */
    public static void LieDetector(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr, boolean isItem) { // Person who used 
        if (chr == null || chr.getMap() == null) {
            return;
        }
        String target = slea.readMapleAsciiString();
        byte slot = 0;
        if (isItem) {
            if (!chr.getCheatTracker().canLieDetector()) {
                chr.dropMessage(1, "您已经使用过一次，暂时还无法使用测谎仪道具.");
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
            slot = (byte) slea.readShort(); // 01 00 (first pos in use) 
            int itemId = slea.readInt(); // B0 6A 21 00 
            Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
            if (toUse == null || toUse.getQuantity() <= 0 || toUse.getItemId() != itemId || itemId != 2190000) {
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        } else if (!chr.isIntern()) { // Manager using skill. Lie Detector Skill 
            chr.getClient().disconnect(true, false);
            c.getSession().close();
            return;
        }
        if ((FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit()) && isItem) || chr.getMap().getReturnMapId() == chr.getMapId()) {
            chr.dropMessage(5, "当前地图无法使用测谎仪.");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MapleCharacter search_chr = chr.getMap().getCharacterByName(target);
        if (search_chr == null || search_chr.getId() == chr.getId() || search_chr.isIntern() && !chr.isIntern()) {
            chr.dropMessage(1, "未找到角色.");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (search_chr.getEventInstance() != null || search_chr.getMapId() == GameConstants.JAIL) {
            chr.dropMessage(5, "当前地图无法使用测谎仪.");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (search_chr.getAntiMacro().inProgress()) {
            c.announce(MaplePacketCreator.LieDetectorResponse((byte) 0x03));
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (search_chr.getAntiMacro().isPassed() && isItem || search_chr.getAntiMacro().getAttempt() == 2) {
            c.announce(MaplePacketCreator.LieDetectorResponse((byte) 0x02));
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (!search_chr.getAntiMacro().startLieDetector(chr.getName(), isItem, false)) {
            chr.dropMessage(5, "使用测谎仪失败."); //error occured, usually cannot access to captcha server 
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (isItem) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
        }
        search_chr.dropMessage(5, chr.getName() + " 对你使用了测谎仪.");
    }

    public static void LieDetectorResponse(LittleEndianAccessor slea, MapleClient c) { // Person who typed 
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        String answer = slea.readMapleAsciiString();
        MapleLieDetector ld = c.getPlayer().getAntiMacro();
        if (!ld.inProgress() || (ld.isPassed() && ld.getLastType() == 0) || ld.getAnswer() == null || answer.length() <= 0) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (answer.equalsIgnoreCase(ld.getAnswer())) {
            MapleCharacter search_chr = c.getPlayer().getMap().getCharacterByName(ld.getTester());
            if (search_chr != null && search_chr.getId() != c.getPlayer().getId()) {
                search_chr.dropMessage(5, c.getPlayer().getName() + " 通过了测谎仪的检测.");
            }
            c.announce(MaplePacketCreator.LieDetectorResponse((byte) 0x0C, (byte) 1));
            c.getPlayer().gainMeso(5000, true);
            ld.end();
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] 玩家: " + c.getPlayer().getName() + " (等级 " + c.getPlayer().getLevel() + ") 通过了测谎仪检测。"));
        } else if (ld.getAttempt() < 3) { // 图片重置
            ld.startLieDetector(ld.getTester(), ld.getLastType() == 0, true); // new attempt 
        } else {
            MapleCharacter search_chr = c.getPlayer().getMap().getCharacterByName(ld.getTester());
            if (search_chr != null && search_chr.getId() != c.getPlayer().getId()) {
                search_chr.dropMessage(5, c.getPlayer().getName() + " 没用通过测谎仪的检测，恭喜你获得7000的金币.");
                search_chr.gainMeso(7000, true);
            }
            ld.end();
            c.getPlayer().getClient().announce(MaplePacketCreator.LieDetectorResponse((byte) 0x0A, (byte) 4));
            MapleMap map = c.getChannelServer().getMapFactory().getMap(GameConstants.JAIL);
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData(String.valueOf(30 * 60));
            c.getPlayer().changeMap(map, map.getPortal(0));
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] 玩家: " + c.getPlayer().getName() + " (等级 " + c.getPlayer().getLevel() + ") 未通过测谎仪检测，系统将其监禁30分钟！"));
        }
    }

    public static void LieDetectorRefresh(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        MapleLieDetector ld = c.getPlayer().getAntiMacro();
        if (ld.getAttempt() < 3) { // 图片重置
            ld.startLieDetector(ld.getTester(), ld.getLastType() == 0, true);
        } else {
            ld.end();
            c.getPlayer().getClient().announce(MaplePacketCreator.LieDetectorResponse((byte) 0x0A, (byte) 4));
            MapleMap map = c.getChannelServer().getMapFactory().getMap(GameConstants.JAIL);
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData(String.valueOf(30 * 60));
            c.getPlayer().changeMap(map, map.getPortal(0));
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] 玩家: " + c.getPlayer().getName() + " (等级 " + c.getPlayer().getLevel() + ") 未通过测谎仪检测，系统将其监禁30分钟！"));
        }
    }

    public static void updateRedLeafHigh(LittleEndianAccessor slea, MapleClient c) {
        slea.readInt();
        slea.readInt();
        int joejoe = slea.readInt();
        slea.readInt();
        int hermoninny = slea.readInt();
        slea.readInt();
        int littledragon = slea.readInt();
        slea.readInt();
        int ika = slea.readInt();
        slea.readInt();
        int wooden = slea.readInt();
        if (joejoe + hermoninny + littledragon + ika != c.getPlayer().getFriendShipToAdd()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        c.getPlayer().setFriendShipPoints(joejoe, hermoninny, littledragon, ika, wooden);
    }
}
