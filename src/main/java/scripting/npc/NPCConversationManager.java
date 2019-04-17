package scripting.npc;

import client.*;
import client.inventory.*;
import client.skills.Skill;
import client.skills.SkillEntry;
import client.skills.SkillFactory;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.*;
import constants.BattleConstants.MobExp;
import constants.BattleConstants.PokedexEntry;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.channel.handler.PlayersHandler;
import handling.login.LoginInformationProvider;
import handling.opcode.NPCTalkOP;
import handling.world.*;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.party.ExpeditionType;
import handling.world.party.MapleParty;
import handling.world.party.MaplePartyCharacter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventInstanceManager;
import scripting.item.ItemScriptManager;
import scripting.quest.QuestScriptManager;
import server.*;
import server.RankingWorker.PokebattleInformation;
import server.RankingWorker.PokedexInformation;
import server.RankingWorker.PokemonInformation;
import server.RankingWorker.RankingInformation;
import server.Timer.CloneTimer;
import server.life.*;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleSlideMenu;
import server.maps.events.Event_PyramidSubway;
import server.market.MarketEngine;
import server.market.MarketEngine.ItemEntry;
import server.quest.MapleQuest;
import server.shop.MapleShopFactory;
import server.squad.MapleSquad;
import tools.*;
import tools.packet.*;

import javax.script.Invocable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class NPCConversationManager extends AbstractPlayerInteraction {

    private static final Logger log = LogManager.getLogger(NPCConversationManager.class.getName());
    private final int npcId;
    private final String npcMode;
    private final Invocable iv;
    private final ScriptType type;
    public boolean pendingDisposal = false;
    private String getText;

    public NPCConversationManager(MapleClient c, int npc, String npcMode, ScriptType type, Invocable iv) {
        super(c, npc, npcMode);
        this.npcId = npc;
        this.npcMode = npcMode;
        this.iv = iv;
        this.type = type;
    }

    public static String showMobImg(int mob) {
        MapleMonster monster = MapleLifeFactory.getMonster(mob);
        if (monster.getStats().getLink() != 0) {
            mob = monster.getStats().getLink();
        }
        String mobStr = String.valueOf(mob);
        while (mobStr.length() < 7) {
            mobStr = "0" + mobStr;
        }
        return "#fMob/" + mobStr + ".img/stand/0#";
    }

    public ScriptType getType() {
        return type;
    }

    public Invocable getIv() {
        return iv;
    }

    public int getNpc() {
        return npcId;
    }

    public String getScript() {
        return npcMode;
    }

    public String getNpcMode() {
        return npcMode;
    }

    public void safeDispose() {
        pendingDisposal = true;
    }

    public void dispose() {
        switch (type) {
            case NPC:
            case ON_USER_ENTER:
            case ON_FIRST_USER_ENTER:
                NPCScriptManager.getInstance().dispose(getClient());
                break;
            case ITEM:
                ItemScriptManager.getInstance().dispose(getClient());
                break;
            default:
                QuestScriptManager.getInstance().dispose(getClient());
        }
    }

    public void sendNext(String text) {
        sendNext(text, id);
    }

    public void sendNext(String text, int id) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x00, text, "00 01", (byte) 0x00));
    }

    public void sendPlayerToNpc(String text) {
        sendNextS(text, (byte) 3, id);
    }

    public void sendNextNoESC(String text) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextNoESC(String text, int id) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextS(String text, byte type) {
        sendNextS(text, type, id);
    }

    public void sendNextS(String text, byte type, int idd) {
        if (text.contains("#L")) {
            sendSimpleS(text, type);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x00, text, "00 01", type, idd));
    }

    public void sendNextSNoESC(String string) {
        sendNextS(string, (byte) 1);
    }

    public void sendNextN(String text) {
        this.sendNextN(text, (byte) 0x24, this.id);
    }

    public void sendNextN(String text, byte type, int idd) {
        if (text.contains("#L")) {
            this.sendSimpleN(text, type, this.id);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x00, text, "00 01", type, (byte) 1, idd, false));
    }

    public void sendPrev(String text) {
        sendPrev(text, id);
    }

    public void sendPrev(String text, int id) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x00, text, "01 00", (byte) 0x00));
    }

    public void sendPrevS(String text, byte type) {
        sendPrevS(text, type, id);
    }

    public void sendPrevS(String text, byte type, int idd) {
        if (text.contains("#L")) {
            sendSimpleS(text, type);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x00, text, "01 00", type, idd));
    }

    public void sendPrevN(String text) {
        this.sendPrevN(text, (byte) 0x24, id);
    }

    public void sendPrevN(String text, byte type) {
        this.sendPrevN(text, type, this.id);
    }

    public void sendPrevN(String text, byte type, int idd) {
        if (text.contains("#L")) {
            this.sendSimpleN(text, type, idd);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x00, text, "01 00", type, (byte) 1, idd, false));
    }

    public void sendNextPrev(String text) {
        sendNextPrev(text, id);
    }

    public void sendNextPrev(String text, int id) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x00, text, "01 01", (byte) 0x00));
    }

    public void sendNextPrevNoESC(String string, int id) {
        sendNextPrevS(string, (byte) 1, id);
    }

    public void PlayerToNpc(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text, byte type) {
        sendNextPrevS(text, type, id);
    }

    public void sendNextPrevS(String text, byte type, int idd) {
        if (text.contains("#L")) {
            sendSimpleS(text, type);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x00, text, "01 01", type, idd));
    }

    public void sendNextPrevSNoESC(String string) {
        sendNextPrevS(string, (byte) 1);
    }

    public void sendNextPrevN(String text) {
        this.sendNextPrevS(text, (byte) 0x24);
    }

    public void sendNextPrevN(String text, byte type) {
        this.sendNextPrevN(text, type, this.id);
    }

    public void sendNextPrevN(String text, byte type, int idd) {
        if (text.contains("#L")) {
            this.sendSimpleN(text, type, idd);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x00, text, "01 01", type, (byte) 1, idd, false));
    }

    public void sendOk(String text) {
        sendOk(text, id);
    }

    public void sendOk(String text, int id) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x00, text, "00 00", (byte) 0x00));
    }

    public void sendOkN(String text) {
        sendOkN(text, id);
    }

    public void sendOkN(String text, int idd) {
        if (text.contains("#L")) {
            this.sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x00, text, "01 00", (byte) 0x24, (byte) 1, idd, false));
    }

    public void sendOkS(String text) {
        sendOkS(text, (byte) 0, id);
    }

    public void sendOkS(String text, byte type) {
        sendOkS(text, type, id);
    }

    public void sendOkS(String text, byte type, int idd) {
        if (text.contains("#L")) {
            sendSimpleS(text, type);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x00, text, "00 00", type, idd));
    }

    public void sendYesNo(String text) {
        sendYesNo(text, id);
    }

    public void sendYesNo(String text, int idd) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x03, text, "", (byte) 0x00));
    }

    public void sendYesNoS(String text, byte type) {
        sendYesNoS(text, type, id);
    }

    public void sendYesNoS(String text, byte type, int idd) {
        if (text.contains("#L")) {
            sendSimpleS(text, type);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x03, text, "", type, idd));
    }

    public void sendYesNoN(String text) {
        this.sendYesNoN(text, this.id);
    }

    public void sendYesNoN(String text, int idd) {
        if (text.contains("#L")) {
            this.sendSimpleN(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x03, text, "", (byte) 0x24, (byte) 1, idd, false));
    }

    public void sendAcceptDecline(String text) {
        askAcceptDecline(text);
    }

    public void sendAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text);
    }

    public void askAcceptDecline(String text) {
        askAcceptDecline(text, id);
    }

    public void askAcceptDecline(String text, int id) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        //V.114修改 以前 0x10
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x10, text, "", (byte) 0x00));
    }

    public void askAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text, id);
    }

    public void askAcceptDeclineNoESC(String text, int id) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        //V.114修改 以前 0x10
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x10, text, "", (byte) 0x01));
    }

    public void askMapSelection(String sel) {
        //V.114修改 以前0x12
        getClient().announce(NPCPacket.getMapSelection(id, (byte) 0x12, sel));
    }

    public void sendSimple(String text) {
        sendSimple(text, id);
    }

    public void sendSimple(String text, int id) {
        if (!text.contains("#L")) {
            sendNext(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(id, (byte) 0x06, text, "", (byte) 0));
    }

    public void sendSimpleS(String text, byte type) {
        sendSimpleS(text, type, id);
    }

    public void sendSimpleS(String text, byte type, int idd) {
        if (!text.contains("#L")) {
            sendNextS(text, type);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x06, text, "", type, idd));
    }

    public void sendSimpleN(String text) {
        this.sendSimpleN(text, (byte) 0x20, this.id);
    }

    public void sendSimpleN(String text, byte type, int idd) {
        if (!text.contains("#L")) {
            this.sendNextN(text, type, idd);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalk(idd, (byte) 0x06, text, "", type, (byte) 1, idd, false));
    }

    public void askAvatar(String text, int styles[], int card, boolean isSecond) {
        getClient().announce(NPCPacket.getNPCTalkStyle(id, text, styles, card, false, isSecond));
    }

    public void sendStyle(String text, int styles[], int card, boolean isSecond) {
        getClient().announce(NPCPacket.getNPCTalkStyle(id, text, styles, card, false, isSecond));
    }

    public void sendAStyle(String text, int styles[], int card) {
        getClient().announce(NPCPacket.getNPCTalkStyle(id, text, styles, card, true, false));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalkNum(id, (byte) 0x05, text, def, min, max));
    }

    public void sendGetText(String text) {
        sendGetText(text, id);
    }

    public void sendGetText(String text, int id) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getNPCTalkText(id, (byte) 0x04, text));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return getText;
    }

    public void sendPlayerOk(String text) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getPlayerTalk(id, (byte) 0x00, text, "00 00", (byte) 0x10));
    }

    public void sendPlayerOk(String text, byte type, int npcId) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getPlayerTalk(npcId, (byte) 0x00, text, "00 00", type, npcId));
    }

    public void sendPlayerPrev(String text, byte type, int npcId) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getPlayerTalk(npcId, (byte) 0x00, text, "01 00", type, npcId));
    }

    public void sendPlayerNext(String text) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getPlayerTalk(id, (byte) 0x00, text, "00 01", (byte) 0x12));
    }

    public void sendPlayerNext(String text, byte type, int npcId) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getPlayerTalk(npcId, (byte) 0x00, text, "00 01", type, npcId));
    }

    public void sendPlayerNextPrev(String text) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getPlayerTalk(id, (byte) 0x00, text, "01 01", (byte) 0x12));
    }

    public void sendPlayerNextPrev(String text, byte type, int npcId) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        getClient().announce(NPCPacket.getPlayerTalk(npcId, (byte) 0x00, text, "01 01", type, npcId));
    }

    /*
     * 复活宠物选择对话框
     * 未知正式启用
     * 1个宠物 宠物位置 9
     * Recv NPC_TALK [02B9] (65)
     * B9 02
     * 04
     * A6 BF 0F 00
     * 0C 00
     * 2C 00 C4 E3 CF EB C8 C3 C4 C4 D2 BB B8 F6 B3 E8 CE EF B8 B4 BB EE C4 D8 A3 BF C7 EB D1 A1 D4 F1 CF EB B8 B4 BB EE B5 C4 B3 E8 CE EF A1 AD
     * 01
     * 11 6E 3B 00 00 00 00 00 - 宠物的唯一ID
     * 09 宠物在背包的位置
     * ?.....,.你想让哪一个宠物复活呢？请选择想复活的宠物…..n;......
     */
    public void sendRevivePet(String text) {
        if (text.contains("#L")) {
            sendSimple(text);
        }
        //todo 0x0C
    }

    public void sendPlayerStart(String text) {
        if (text.contains("#L")) {
            sendSimple(text);
            return;
        }
        //V.114修改 以前0x10
        getClient().announce(NPCPacket.getPlayerTalk(id, (byte) 0x10, text, "", (byte) 0x10));
    }

    public void sendNext_(String text) {
        getClient().announce(NPCPacket.OnSay(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text, false, true));
    }

    public void sendPrev_(String text) {
        getClient().announce(NPCPacket.OnSay(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text, true, false));
    }

    public void sendNextPrev_(String text) {
        getClient().announce(NPCPacket.OnSay(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text, true, true));
    }

    public void sendOk_(String text) {
        getClient().announce(NPCPacket.OnSay(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text, false, false));
    }

    public void sendYesNo_(String text) {
        getClient().announce(NPCPacket.OnAskYesNo(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text));
    }

    public void sendAcceptDecline_(String text) {
        getClient().announce(NPCPacket.OnAskAccept(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text));
    }

    public void sendSimple_(String text) {
        getClient().announce(NPCPacket.OnAskMenu(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text));
    }

    public void sendSlideMenu(String sText, int nIndex, boolean bSlideDlgEX) {
        getClient().announce(NPCPacket.OnAskSlideMenu(NPCTalkOP.TEXT.getValue(), id, bSlideDlgEX, nIndex, sText));
    }

    public void sendSlideMenu(final int type, final String sel) {
        String[] arrstring = sel.split("#");
        if (arrstring.length < 3) {
            return;
        }
        getClient().announce(NPCPacket.getSlideMenu(NPCTalkOP.TEXT.getValue(), id, type, Integer.valueOf(arrstring[arrstring.length - 2]), sel));
    }

    public void askYesNo(String string) {
        sendYesNoS(string, (byte) 4);
    }

    public void askYesNoS(String string) {
        sendYesNoS(string, (byte) 3);
    }

    public void askYesNoE(String string) {
        sendYesNoS(string, (byte) 0);
    }

    public void askMenu(String text) {
        sendNextS(text, (byte) 4);
    }

    public void askMenuNoESC(String string, int id) {
        sendNextS(string, (byte) id, 4);
    }

    public String getDimensionalMirror(MapleCharacter player) {
        return MapleSlideMenu.SlideMenu0.getSelectionInfo(player, id);
    }

    public String getSlideMenuSelection(int type) {

        try {
            Method method = MapleSlideMenu.getSlideMenu(type).getMethod("getSelectionInfo", MapleCharacter.class, Integer.class);
            if (method != null) {
                return method.invoke(null, getPlayer(), id).toString();
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return getDimensionalMirror(getPlayer());
    }

    public int[] getSlideMenuDataIntegers(int type, int selection) {

        try {
            Method method = MapleSlideMenu.getSlideMenu(type).getMethod("getDataInteger", Integer.class);
            if (method != null) {
                return (int[]) method.invoke(null, selection);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return MapleSlideMenu.SlideMenu0.getDataInteger(selection);
    }

    public void sendStyle(String text, int styles[]) {
        getClient().announce(NPCPacket.OnAskAvatar(NPCTalkOP.TEXT.getValue(), id, text, styles));
    }

    public void sendGetNumber_(String text, int def, int min, int max) {
        getClient().announce(NPCPacket.OnAskNumber(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text, def, min, max));
    }

    public void sendGetText_(String text) {
        sendGetText(text, "", 0, 0);
    }

    public void sendGetText(String text, String def, int col, int line) {
        getClient().announce(NPCPacket.OnAskText(NPCTalkOP.TEXT.getValue(), id, (byte) 0, text, def, col, line));
    }

    public boolean canChangeHairFace(int id) {
        return getItemInfo().loadHairFace(id);
    }

    public void setHair(int hair) {
        if (!canChangeHairFace(hair)) {
            playerMessage(1, "该发型不存在，无法更换。\r\nID：" + hair);
            return;
        }
        getPlayer().setHair(hair);
        getPlayer().updateSingleStat(MapleStat.发型, hair);
        getPlayer().equipChanged();
    }

    public void setFace(int face) {
        if (!canChangeHairFace(face)) {
            playerMessage(1, "该脸型不存在，无法更换。\r\nID：" + face);
            return;
        }
        getPlayer().setFace(face);
        getPlayer().updateSingleStat(MapleStat.脸型, face);
        getPlayer().equipChanged();
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor((byte) color);
        getPlayer().updateSingleStat(MapleStat.皮肤, color);
        getPlayer().equipChanged();
    }

    public int[] getCanHair(int[] hairs) {
        List<Integer> canHair = new ArrayList<>();
        List<Integer> cantHair = new ArrayList<>();
        for (int hair : hairs) {
            if (canChangeHairFace(hair)) {
                canHair.add(hair);
            } else {
                cantHair.add(hair);
            }
        }
        if (cantHair.size() > 0 && getPlayer().isAdmin()) {
            StringBuilder sb = new StringBuilder("正在读取的发型中有");
            sb.append(cantHair.size()).append("个发型客户端不支持，已被系统清除：");
            for (int i = 0; i < cantHair.size(); i++) {
                sb.append(cantHair.get(i));
                if (i < cantHair.size() - 1) {
                    sb.append(",");
                }
            }
            playerMessage(sb.toString());
        }
        int[] getHair = new int[canHair.size()];
        for (int i = 0; i < canHair.size(); i++) {
            getHair[i] = canHair.get(i);
        }
        return getHair;
    }

    public int[] getCanFace(int[] faces) {
        List<Integer> canFace = new ArrayList<>();
        List<Integer> cantFace = new ArrayList<>();
        for (int face : faces) {
            if (canChangeHairFace(face)) {
                canFace.add(face);
            } else {
                cantFace.add(face);
            }
        }
        if (cantFace.size() > 0 && getPlayer().isAdmin()) {
            StringBuilder sb = new StringBuilder("正在读取的脸型中有");
            sb.append(cantFace.size()).append("个脸型客户端不支持，已被系统清除：");
            for (int i = 0; i < cantFace.size(); i++) {
                sb.append(cantFace.get(i));
                if (i < cantFace.size() - 1) {
                    sb.append(",");
                }
            }
            playerMessage(sb.toString());
        }
        int[] getFace = new int[canFace.size()];
        for (int i = 0; i < canFace.size(); i++) {
            getFace[i] = canFace.get(i);
        }
        return getFace;
    }

    public void setAndroidHair(int hair) {
        getPlayer().getAndroid().setHair(hair);
        getPlayer().getAndroid().saveToDb();
        getPlayer().setAndroid(getPlayer().getAndroid());
    }

    public void setAndroidFace(int face) {
        getPlayer().getAndroid().setFace(face);
        getPlayer().getAndroid().saveToDb();
        getPlayer().setAndroid(getPlayer().getAndroid());
    }

    public void setAndroidSkin(int skin) {
        getPlayer().getAndroid().setSkin(skin);
        getPlayer().getAndroid().saveToDb();
        getPlayer().setAndroid(getPlayer().getAndroid());
    }

    public int setRandomAvatarA(int ticket, int... args_all) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            getPlayer().getAndroid().setSkin(args);
        } else if (args < 30000) {
            while (!canChangeHairFace(args)) {
                args = args_all[Randomizer.nextInt(args_all.length)];
            }
            getPlayer().getAndroid().setFace(args);
        } else {
            while (!canChangeHairFace(args)) {
                args = args_all[Randomizer.nextInt(args_all.length)];
            }
            getPlayer().getAndroid().setHair(args);
        }
        getPlayer().getAndroid().saveToDb();
        getPlayer().setAndroid(getPlayer().getAndroid());
        return 1;
    }

    public int setAvatarA(int ticket, int args) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        if (args < 100) {
            getPlayer().getAndroid().setSkin(args);
        } else if (args < 30000) {
            getPlayer().getAndroid().setFace(args);
        } else {
            getPlayer().getAndroid().setHair(args);
        }
        getPlayer().getAndroid().saveToDb();
        getPlayer().setAndroid(getPlayer().getAndroid());
        return 1;
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        return setRandomAvatar(ticket, args_all, false);
    }

    public int setRandomAvatar(int ticket, int[] args_all, boolean isSecond) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            if (isSecond) {
                getPlayer().setSecondSkinColor((byte) args);
            } else {
                getPlayer().setSkinColor((byte) args);
                getPlayer().updateSingleStat(MapleStat.皮肤, args);
            }
        } else if (args < 30000) {
            while (!canChangeHairFace(args)) {
                args = args_all[Randomizer.nextInt(args_all.length)];
            }
            if (isSecond) {
                getPlayer().setSecondFace(args);
            } else {
                getPlayer().setFace(args);
                getPlayer().updateSingleStat(MapleStat.脸型, args);
            }
        } else {
            while (!canChangeHairFace(args)) {
                args = args_all[Randomizer.nextInt(args_all.length)];
            }
            if (isSecond) {
                getPlayer().setSecondHair(args);
            } else {
                getPlayer().setHair(args);
                getPlayer().updateSingleStat(MapleStat.发型, args);
            }
        }
        if (isSecond) {
            getClient().announce(MaplePacketCreator.DressUpInfoModified(getPlayer()));
        } else {
            getPlayer().equipChanged();
        }
        return 1;
    }

    public int setAvatar(int ticket, int args) {
        return setAvatar(ticket, args, false);
    }

    public int setAvatar(int ticket, int args, boolean isSecond) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        if (args < 100) {
            if (isSecond) {
                getPlayer().getSecondSkinColor();
            } else {
                getPlayer().setSkinColor((byte) args);
                getPlayer().updateSingleStat(MapleStat.皮肤, args);
            }
        } else if (args < 30000) {
            if (isSecond) {
                getPlayer().setSecondFace(args);
            } else {
                getPlayer().setFace(args);
                getPlayer().updateSingleStat(MapleStat.脸型, args);
            }
        } else if (isSecond) {
            getPlayer().setSecondHair(args);
        } else {
            getPlayer().setHair(args);
            getPlayer().updateSingleStat(MapleStat.发型, args);
        }
        if (isSecond) {
            getClient().announce(MaplePacketCreator.DressUpInfoModified(getPlayer()));
        } else {
            getPlayer().equipChanged();
        }
        return 1;
    }

    public void getAskMIXHair(String string) {
        getClient().announce(NPCPacket.getAskMIXHair(id, string));
    }

    public void getAskMIXHairNew(String string) {
        getClient().announce(NPCPacket.getAskMIXHairNew(id, string));
    }

    public void getAskCustomMIXHair(String string) {
        getClient().announce(NPCPacket.getAskCustomMIXHair(id, string));
    }

    public void sendStorage() {
        getPlayer().setConversation(4);
        getPlayer().getStorage().sendStorage(getClient(), id);
    }

    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(getClient());
    }

    public void openShopNPC(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(getClient(), this.id);
    }

    /*
     * 随机抽奖
     * 参数 道具的ID
     * 参数 道具的数量
     */
    public int gainGachaponItem(int id, int quantity) {
        return gainGachaponItem(id, quantity, getPlayer().getMap().getStreetName() + " - " + getPlayer().getMap().getMapName());
    }

    /*
     * 随机抽奖
     * 参数 道具的ID
     * 参数 道具的数量
     * 参数 获得装备的日志
     */
    public int gainGachaponItem(int id, int quantity, String msg) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        try {
            if (!ii.itemExists(id)) {
                return -1;
            }
            Item item = MapleInventoryManipulator.addbyId_Gachapon(getClient(), id, (short) quantity, "从 " + msg + " 中获得时间: " + DateUtil.getNowTime());
            if (item == null) {
                return -1;
            }
            byte rareness = GameConstants.gachaponRareItem(item.getItemId());
            if (rareness == 1 || rareness == 2 || rareness == 3) {
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.getGachaponMega(getPlayer().getName(), " : 从" + msg + "中获得{" + ii.getName(item.getItemId()) + "}！大家一起恭喜他（她）吧！！！！", item, rareness, getClient().getChannel()));
            }
            return item.getItemId();
        } catch (Exception e) {
            log.error("gainGachaponItem 错误", e);
        }
        return -1;
    }

    /*
     * NPC给玩家道具带公告
     * 参数 道具的ID
     * 参数 道具的数量
     * 参数 获得装备的日志
     * 参数 公告喇叭的类型[1-3]
     */
    public int gainGachaponItem(int id, int quantity, String msg, int rareness) {
        return gainGachaponItem(id, quantity, msg, rareness, false, 0);
    }

    /*
     * NPC给玩家道具带公告
     * 参数 道具的ID
     * 参数 道具的数量
     * 参数 获得装备的日志
     * 参数 公告喇叭的类型[1-3]
     * 参数 道具的使用时间
     */
    public int gainGachaponItem(int id, int quantity, String msg, int rareness, long period) {
        return gainGachaponItem(id, quantity, msg, rareness, false, period);
    }

    /*
     * NPC给玩家道具带公告
     * 参数 道具的ID
     * 参数 道具的数量
     * 参数 获得装备的日志
     * 参数 公告喇叭的类型[1-3]
     * 参数 是否NPC购买
     * 参数 道具的使用时间
     */
    public int gainGachaponItem(int id, int quantity, String msg, int rareness, boolean buy) {
        return gainGachaponItem(id, quantity, msg, rareness, buy, 0);
    }

    /*
     * NPC给玩家道具带公告
     * 参数 道具的ID
     * 参数 道具的数量
     * 参数 获得装备的日志
     * 参数 公告喇叭的类型[1-3]
     * 参数 是否NPC购买
     * 参数 道具的使用时间
     */
    public int gainGachaponItem(int id, int quantity, String msg, int rareness, boolean buy, long period) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        try {
            if (!ii.itemExists(id)) {
                return -1;
            }
            Item item = MapleInventoryManipulator.addbyId_Gachapon(getClient(), id, (short) quantity, "从 " + msg + " 中" + (buy ? "购买" : "获得") + "时间: " + DateUtil.getNowTime(), period);
            if (item == null) {
                return -1;
            }
            if (rareness == 1 || rareness == 2 || rareness == 3) {
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.getGachaponMega(getPlayer().getName(), " : 从" + msg + "中" + (buy ? "购买" : "获得") + "{" + ii.getName(item.getItemId()) + "}！大家一起恭喜他（她）吧！！！！", item, (byte) rareness, getClient().getChannel()));
            }
            return item.getItemId();
        } catch (Exception e) {
            log.error("gainGachaponItem 错误", e);
        }
        return -1;
    }

    public void changeJob(int jobId) {
        getPlayer().changeJob(jobId);
    }

    public boolean isValidJob(int jobId) {
        return JobConstants.getJobNameByIdNull(jobId) != null;
    }

    public String getJobNameById(int jobId) {
        return JobConstants.getJobNameByIdNull(jobId);
    }

    /**
     * 开始任务
     *
     * @param questId 任务ID
     */
    public void startQuest(int questId) {
        MapleQuest.getInstance(questId).start(getPlayer(), getNpc());
    }

    /**
     * 完成任务
     * @param questId 任务ID
     */
    public void completeQuest(int questId) {
        MapleQuest.getInstance(questId).complete(getPlayer(), getNpc());
    }

    /**
     * 放弃任务
     * @param questId 任务ID
     */
    public void forfeitQuest(int questId) {
        MapleQuest.getInstance(questId).forfeit(getPlayer());
    }

    /**
     * 强制开始任务
     * @param questId 任务ID
     */
    @Override
    public void forceStartQuest(int questId) {
        MapleQuest.getInstance(questId).forceStart(getPlayer(), getNpc(), null);
    }

    /**
     * 强制完成任务
     * @param questId
     */
    @Override
    public void forceCompleteQuest(int questId) {
        MapleQuest.getInstance(questId).forceComplete(getPlayer(), getNpc());
    }

    /*
     * 角色金币
     */
    public long getMeso() {
        return getPlayer().getMeso();
    }

    /*
     * 给角色AP点数
     */
    public void gainAp(int amount) {
        getPlayer().gainAp((short) amount);
    }

    /*
     * 增加角色的道具栏数量
     */
    public void expandInventory(byte type, int amt) {
        getPlayer().expandInventory(type, amt);
    }

    public void unequipEverything() {
        MapleInventory equipped = getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<Short> itemIds = new LinkedList<>();
        for (Item item : equipped.newList()) {
            itemIds.add(item.getPosition());
        }
        for (short ids : itemIds) {
            MapleInventoryManipulator.unequip(getC(), ids, equip.getNextFreeSlot());
        }
    }

    public void showEffect(boolean broadcast, String effect) {
        if (broadcast) {
            getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
        } else {
            getClient().announce(MaplePacketCreator.showEffect(effect));
        }
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
        } else {
            getClient().announce(MaplePacketCreator.playSound(sound));
        }
    }

    public void environmentChange(String env) {
        this.environmentChange(env, 2);
    }

    public void environmentChange(String env, int info) {
        environmentChange(false, env, info);
    }

    public void environmentChange(boolean broadcast, String env, int info) {
        if (broadcast) {
            getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(env, info));
        } else {
            getClient().announce(MaplePacketCreator.environmentChange(env, info));
        }
    }

    public void sendchangeMap(int mapid) {
        MapleMap map = getMap(mapid);
        getPlayer().changeMap(map, map.getPortal(0));
    }

    public void directionEffect(String data, int value, int x, int y, int z) {
        getClient().announce(UIPacket.getDirectionEffect(data, value, x, y, z));
    }

    public void sendRedLeaf(boolean viewonly, boolean autocheck) {
        if (autocheck) {
            viewonly = getPlayer().getFriendShipToAdd() == 0;
        }
        getClient().announce(MaplePacketCreator.sendRedLeaf(viewonly ? 0 : getPlayer().getFriendShipToAdd(), viewonly));
    }

    /*
     * 好友栏数量操作
     */
    public void updateBuddyCapacity(int capacity) {
        getPlayer().setBuddyCapacity((byte) capacity);
    }

    public int getBuddyCapacity() {
        return getPlayer().getBuddyCapacity();
    }

    /*
     * 组队操作相关
     */
    public int partyMembersInMap() {
        int inMap = 0;
        if (getPlayer().getParty() == null) {
            return inMap;
        }
        for (MapleCharacter chr : getPlayer().getMap().getCharactersThreadsafe()) {
            if (chr.getParty() != null && chr.getParty().getPartyId() == getPlayer().getParty().getPartyId()) {
                inMap++;
            }
        }
        return inMap;
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<>();
        for (MaplePartyCharacter partychr : getPlayer().getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter chr = channel.getPlayerStorage().getCharacterById(partychr.getId());
                if (chr != null) {
                    chars.add(chr);
                }
            }
        }
        return chars;
    }

    public void warpPartyWithExp(int mapId, int exp) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter partychr : getPlayer().getParty().getMembers()) {
            MapleCharacter chr = getClient().getChannelServer().getPlayerStorage().getCharacterByName(partychr.getName());
            if ((chr.getEventInstance() == null && getPlayer().getEventInstance() == null) || chr.getEventInstance() == getPlayer().getEventInstance()) {
                chr.changeMap(target, target.getPortal(0));
                chr.gainExp(exp, true, false, true);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            gainMeso(meso);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter partychr : getPlayer().getParty().getMembers()) {
            MapleCharacter chr = getClient().getChannelServer().getPlayerStorage().getCharacterByName(partychr.getName());
            if ((chr.getEventInstance() == null && getPlayer().getEventInstance() == null) || chr.getEventInstance() == getPlayer().getEventInstance()) {
                chr.changeMap(target, target.getPortal(0));
                chr.gainExp(exp, true, false, true);
                chr.gainMeso(meso, true);
            }
        }
    }

    /*
     * 获取远征队信息
     */
    public MapleSquad getSquad(String type) {
        return getClient().getChannelServer().getMapleSquad(type);
    }

    /*
     * 获取远征队伍是存在
     * 没有返回 -1
     * 存在返回 远征队伍的状态
     */
    public int getSquadAvailability(String type) {
        MapleSquad squad = getClient().getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        }
        return squad.getStatus();
    }

    /*
     * 注册新的远征队伍
     * 类型
     * 时间
     * 提示信息
     */
    public boolean registerSquad(String type, int minutes, String startText) {
        if (getClient().getChannelServer().getMapleSquad(type) == null) { //如果类型不为空
            /*
             * 新建远征队
             */
            MapleSquad squad = new MapleSquad(getClient().getChannel(), type, getPlayer(), minutes * 60 * 1000, startText);
            /*
             * 在当前频道中注册远征队
             */
            boolean ret = getClient().getChannelServer().addMapleSquad(squad, type);
            if (ret) { //如果在当前频道中注册成功
                MapleMap map = getPlayer().getMap();
                map.broadcastMessage(MaplePacketCreator.getClock(minutes * 60)); //在地图中显示远征队超时时间
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, getPlayer().getName() + startText)); //在地图中提示远征队广告
            } else {
                squad.clear();
            }
            return ret;
        }
        return false;
    }

    public boolean getSquadList(String type, byte type_) {
        try {
            MapleSquad squad = getClient().getChannelServer().getMapleSquad(type);
            if (squad == null) {
                return false;
            }
            if (type_ == 0 || type_ == 3) { // Normal viewing
                sendNext(squad.getSquadMemberString(type_));
            } else if (type_ == 1) { // Squad Leader banning, Check out banned participant
                sendSimple(squad.getSquadMemberString(type_));
            } else if (type_ == 2) {
                if (squad.getBannedMemberSize() > 0) {
                    sendSimple(squad.getSquadMemberString(type_));
                } else {
                    sendNext(squad.getSquadMemberString(type_));
                }
            }
            return true;
        } catch (NullPointerException ex) {
            log.error(ex);
            return false;
        }
    }

    /*
     * 检测是否为远征队队长
     */
    public byte isSquadLeader(String type) {
        MapleSquad squad = getClient().getChannelServer().getMapleSquad(type);
        if (squad != null) {
            if (squad.getLeader() != null && squad.getLeader().getId() == getPlayer().getId()) {
                return 1;
            } else {
                return 0;
            }
        }
        return -1;
    }

    public boolean reAdd(String eim, String squad) {
        EventInstanceManager eimz = getDisconnected(eim);
        MapleSquad squadz = getSquad(squad);
        if (eimz != null && squadz != null) {
            squadz.reAddMember(getPlayer());
            eimz.registerPlayer(getPlayer());
            return true;
        }
        return false;
    }

    /*
     * 将玩家设置为禁止加入远征队
     */
    public void banMember(String type, int pos) {
        MapleSquad squad = getClient().getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(pos);
        }
    }

    /*
     * 接受等待列表中的玩家为远征队成员
     */
    public void acceptMember(String type, int pos) {
        MapleSquad squad = getClient().getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.acceptMember(pos);
        }
    }

    /*
     * 添加或者删除远征队成员
     */
    public int addMember(String type, boolean join) {
        MapleSquad squad = getClient().getChannelServer().getMapleSquad(type);
        if (squad != null) {
            return squad.addMember(getPlayer(), join);
        }
        return -1;
    }

    /*
     * 检测是否为远征队成员
     */
    public byte isSquadMember(String type) {
        MapleSquad squad = getClient().getChannelServer().getMapleSquad(type);
        if (squad != null) {
            if (squad.containsMember(getClient().getPlayer())) {
                return 1;
            } else if (squad.isBanned(getClient().getPlayer())) {
                return 2;
            } else {
                return 0;
            }
        }
        return -1;
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    /*
     * 创建家族
     */
    public void genericGuildMessage(int code) {
        getClient().announce(GuildPacket.genericGuildMessage((byte) code));
    }

    /*
     * 解散家族
     */
    public void disbandGuild() {
        int gid = getPlayer().getGuildId();
        if (gid <= 0 || getPlayer().getGuildRank() != 1) {
            return;
        }
        WorldGuildService.getInstance().disbandGuild(gid);
    }

    public int getGuildCapacity() {
        return WorldGuildService.getInstance().getGuild(getPlayer()).getCapacity();
    }

    /*
     * 增加家族成员数
     */
    public void increaseGuildCapacity(boolean trueMax) {
        increaseGuildCapacity(trueMax, 50000000);
    }

    public void increaseGuildCapacity(boolean trueMax, int meso) {
        if (getPlayer().getMeso() < meso && !trueMax) {
            getClient().announce(MaplePacketCreator.serverNotice(1, "金币不足.要金币: " + meso));
            return;
        }
        int gid = getPlayer().getGuildId();
        if (gid <= 0) {
            return;
        }
        if (WorldGuildService.getInstance().increaseGuildCapacity(gid, trueMax)) {
            if (!trueMax) {
                getPlayer().gainMeso(-meso, true, true);
            } else {
                gainGP(-25000);
            }
        } else if (!trueMax) {
            sendNext("请检查家族成员是否到达上限. (最大人数: 100)");
        } else {
            sendNext("请检查家族成员是否到达上限, if you have the GP needed or if subtracting GP would decrease a guild level. (最大人数: 200)");
        }
    }

    /*
     * 荣耀之石
     */
    public void displayGuildRanks() {
        displayGuildRanks(false);
    }

    public void displayGuildRanks(boolean show) {
        getClient().announce(GuildPacket.showGuildRanks(id, MapleGuildRanking.getInstance().getRank(), show));
    }

    /*
     * 创建家族需要的金币
     */
    public int getCreateGuildCost() {
        return ServerConfig.CHANNEL_CREATEGUILDCOST;
    }

    public boolean removePlayerFromInstance() {
        if (getPlayer().getEventInstance() != null) {
            getPlayer().getEventInstance().removePlayer(getClient().getPlayer());
            return true;
        }
        return false;
    }

    public boolean isPlayerInstance() {
        return getPlayer().getEventInstance() != null;
    }

    /**
     * 修改装备属性
     *
     * @param slot
     * @param type
     * @param amount
     */
    public void changeStat(byte slot, int type, int amount) {
        Equip sel = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                sel.setStr((short) amount);
                break;
            case 1:
                sel.setDex((short) amount);
                break;
            case 2:
                sel.setInt((short) amount);
                break;
            case 3:
                sel.setLuk((short) amount);
                break;
            case 4:
                sel.setHp((short) amount);
                break;
            case 5:
                sel.setMp((short) amount);
                break;
            case 6:
                sel.setWatk((short) amount);
                break;
            case 7:
                sel.setMatk((short) amount);
                break;
            case 8:
                sel.setWdef((short) amount);
                break;
            case 9:
                sel.setMdef((short) amount);
                break;
            case 10:
                sel.setAcc((short) amount);
                break;
            case 11:
                sel.setAvoid((short) amount);
                break;
            case 12:
                sel.setHands((short) amount);
                break;
            case 13:
                sel.setSpeed((short) amount);
                break;
            case 14:
                sel.setJump((short) amount);
                break;
            case 15:
                sel.setUpgradeSlots((byte) amount);
                break;
            case 16:
                sel.setViciousHammer((byte) amount);
                break;
            case 17:
                sel.setLevel((byte) amount);
                break;
            case 18:
                sel.setState((byte) amount, false);
                break;
            case 19:
                sel.setEnhance((byte) amount);
                break;
            case 20:
                sel.setPotential1(amount);
                break;
            case 21:
                sel.setPotential2(amount);
                break;
            case 22:
                sel.setPotential3(amount);
                break;
            case 23:
                sel.setOwner(getText());
                break;
            case 24:
                sel.setBossDamage((short) amount);
                break;
            case 25:
                sel.setIgnorePDR((short) amount);
                break;
            case 26:
                sel.setTotalDamage((short) amount);
                break;
            case 27:
                sel.setAllStat((short) amount);
                break;
            default:
                break;
        }
        getPlayer().equipChanged();
        fakeRelog();
    }

    public void changeStat(byte slot, String type, int amount, boolean inpack) {
        Equip sel = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type.toUpperCase()) {
            case "STR":
                sel.setStr((short) amount);
                break;
            case "DEX":
                sel.setDex((short) amount);
                break;
            case "INT":
                sel.setInt((short) amount);
                break;
            case "LUK":
                sel.setLuk((short) amount);
                break;
            case "HP":
                sel.setHp((short) amount);
                break;
            case "MP":
                sel.setMp((short) amount);
                break;
            case "WATK":
                sel.setWatk((short) amount);
                break;
            case "MATK":
                sel.setMatk((short) amount);
                break;
            case "WDEF":
                sel.setWdef((short) amount);
                break;
            case "MDEF":
                sel.setMdef((short) amount);
                break;
            case "ACC":
                sel.setAcc((short) amount);
                break;
            case "AVOID":
                sel.setAvoid((short) amount);
                break;
            case "HANDS":
                sel.setHands((short) amount);
                break;
            case "SPEED":
                sel.setSpeed((short) amount);
                break;
            case "JUMP":
                sel.setJump((short) amount);
                break;
            case "UPGRADE":
                sel.setUpgradeSlots((byte) amount);
                break;
            case "VHAMMER":
                sel.setViciousHammer((byte) amount);
                break;
            case "LEVEL":
                sel.setLevel((byte) amount);
                break;
            case "STATE":
                sel.setState((byte) amount, false);
                break;
            case "ENHANCE":
                sel.setEnhance((byte) amount);
                break;
            case "POTENTIAL1":
                sel.setPotential1(amount);
                break;
            case "POTENTIAL2":
                sel.setPotential2(amount);
                break;
            case "POTENTIAL3":
                sel.setPotential3(amount);
                break;
            case "OWNER":
                sel.setOwner(getText());
                break;
            case "BOSSDAMAGE":
                sel.setBossDamage((short) amount);
                break;
            case "IGNOREPDR":
                sel.setIgnorePDR((short) amount);
                break;
            case "TOTALDAMAGE":
                sel.setTotalDamage((short) amount);
                break;
            case "ALLSTAT":
                sel.setAllStat((short) amount);
                break;
            default:
                break;
        }
        getPlayer().equipChanged();
        fakeRelog();
    }

    public void changePotentialStat(byte slot, int type, int amount) {
        Equip sel = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                if (amount == 0) {
                    sel.setPotential1(-5);
                } else if (amount == 1) {
                    sel.setPotential1(-6);
                } else if (amount == 2) {
                    sel.setPotential1(-7);
                } else if (amount == 3) {
                    sel.setPotential1(-8);
                }
                break;
            case 1:
                sel.setPotential1(amount);
                sel.setStateMsg(3);
                break;
            case 2:
                sel.setPotential2(amount);
                sel.setStateMsg(3);
                break;
            case 3:
                sel.setPotential3(amount);
                sel.setStateMsg(3);
                break;
            default:
                break;
        }
        getPlayer().equipChanged();
        fakeRelog();
    }

    public void openDuey() {
        getPlayer().setConversation(2);
        getClient().announce(MaplePacketCreator.sendDuey((byte) 0x09, null));
    }

    public void openMerchantItemStore() {
        getPlayer().setConversation(3);
        getClient().announce(PlayerShopPacket.merchItemStore((byte) 0x29)); //V.115.1修改
    }

    public void sendPVPWindow() {
        getClient().announce(UIPacket.sendPVPWindow(0));
        getClient().announce(MaplePacketCreator.sendPVPMaps());
    }

    public void sendPartyWindow() {
        getClient().announce(MaplePacketCreator.sendPartyWindow(id));
    }

    public void sendPartyWindow(int id) {
        getClient().announce(MaplePacketCreator.sendPartyWindow(id));
    }

    public void sendRepairWindow() {
        getClient().announce(MaplePacketCreator.sendRepairWindow(id));
    }

    public void sendProfessionWindow() {
        getClient().announce(MaplePacketCreator.sendProfessionWindow(0));
    }

    public void sendEventWindow() {
        getClient().announce(UIPacket.sendEventWindow(0));
    }

    public void sendLinkSkillWindow(int skillId) {
        if (hasSkill(skillId)) {
            getClient().announce(MaplePacketCreator.sendLinkSkillWindow(skillId));
        }
    }

    public void getMulungRanking() {
        getClient().announce(MaplePacketCreator.getMulungRanking((byte) 1));
    }

    public int getDojoPoints() {
        return dojo_getPts();
    }

    public int getDojoRecord() {
        return getPlayer().getIntNoRecord(GameConstants.DOJO_RECORD);
    }

    public void setDojoRecord(boolean reset) {
        setDojoRecord(reset, false, 0);
    }

    public void setDojoRecord(boolean reset, boolean take, int amount) {
        if (reset) {
            getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData("0");
            getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData("0");
        } else if (take) {
            getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData(String.valueOf(getPlayer().getIntRecord(GameConstants.DOJO_RECORD) - amount));
            getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(getPlayer().getIntRecord(GameConstants.DOJO_RECORD) - amount));
        } else {
            getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData(String.valueOf(getPlayer().getIntRecord(GameConstants.DOJO_RECORD) + 1));
        }
    }

    public boolean start_PyramidSubway(int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpStartPyramid(getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpStartSubway(getClient().getPlayer());
    }

    public boolean bonus_PyramidSubway(int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpBonusPyramid(getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpBonusSubway(getClient().getPlayer());
    }

    public short getKegs() {
        return getClient().getChannelServer().getFireWorks().getKegsPercentage();
    }

    public void giveKegs(int kegs) {
        getClient().getChannelServer().getFireWorks().giveKegs(getPlayer(), kegs);
    }

    public short getSunshines() {
        return getClient().getChannelServer().getFireWorks().getSunsPercentage();
    }

    public void addSunshines(int kegs) {
        getClient().getChannelServer().getFireWorks().giveSuns(getPlayer(), kegs);
    }

    public short getDecorations() {
        return getClient().getChannelServer().getFireWorks().getDecsPercentage();
    }

    public void addDecorations(int kegs) {
        try {
            getClient().getChannelServer().getFireWorks().giveDecs(getPlayer(), kegs);
        } catch (Exception e) {
            log.error("addDecorations 错误", e);
        }
    }

    public void maxStats() {
        List<Pair<MapleStat, Long>> statup = new ArrayList<>(8);

        getPlayer().getStat().str = (short) 32767;
        getPlayer().getStat().dex = (short) 32767;
        getPlayer().getStat().int_ = (short) 32767;
        getPlayer().getStat().luk = (short) 32767;

        getPlayer().getStat().maxhp = getPlayer().getMaxHpForSever();
        getPlayer().getStat().maxmp = getPlayer().getMaxMpForSever();
        getPlayer().getStat().setHp(getPlayer().getMaxHpForSever(), getClient().getPlayer());
        getPlayer().getStat().setMp(getPlayer().getMaxMpForSever(), getClient().getPlayer());

        statup.add(new Pair<>(MapleStat.力量, 32767L));
        statup.add(new Pair<>(MapleStat.敏捷, 32767L));
        statup.add(new Pair<>(MapleStat.运气, 32767L));
        statup.add(new Pair<>(MapleStat.智力, 32767L));
        statup.add(new Pair<>(MapleStat.HP, (long) getPlayer().getMaxHpForSever()));
        statup.add(new Pair<>(MapleStat.MAXHP, (long) getPlayer().getMaxHpForSever()));
        statup.add(new Pair<>(MapleStat.MP, (long) getPlayer().getMaxMpForSever()));
        statup.add(new Pair<>(MapleStat.MAXMP, (long) getPlayer().getMaxMpForSever()));
        getPlayer().getStat().recalcLocalStats(getClient().getPlayer());
        getClient().announce(MaplePacketCreator.updatePlayerStats(statup, getClient().getPlayer()));
    }

    public Triple<String, Map<Integer, String>, Long> getSpeedRun(String typ) {
        ExpeditionType types = ExpeditionType.valueOf(typ);
        if (SpeedRunner.getSpeedRunData(types) != null) {
            return SpeedRunner.getSpeedRunData(types);
        }
        return new Triple<>("", new HashMap<>(), 0L);
    }

    public boolean getSR(Triple<String, Map<Integer, String>, Long> ma, int sel) {
        if (ma.mid.get(sel) == null || ma.mid.get(sel).length() <= 0) {
            dispose();
            return false;
        }
        sendOk(ma.mid.get(sel));
        return true;
    }

    public Equip getEquip(int itemid) {
        return (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
    }

    public void setExpiration(Object statsSel, long expire) {
        if (statsSel instanceof Equip) {
            ((Equip) statsSel).setExpiration(System.currentTimeMillis() + (expire * 24 * 60 * 60 * 1000));
        }
    }

    public void setLock(Object statsSel) {
        if (statsSel instanceof Equip) {
            Equip eq = (Equip) statsSel;
            if (eq.getExpiration() == -1) {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.封印.getValue()));
            } else {
                eq.setFlag((byte) (eq.getFlag() | ItemFlag.不可交易.getValue()));
            }
        }
    }

    /*
     * 设置装备是否永久锁定
     */
    public void setItemLock() {
        setItemLock(true);
    }

    public void setItemLock(boolean lock) {
        setItemLock(1, lock);
    }

    public void setItemLock(int slot, boolean lock) {
        Item item = getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slot);
        if (item == null) {
            getPlayer().dropMessage(6, "装备栏的第[" + slot + "]个道具为空，操作失败。");
            return;
        }
        short flag = (short) ItemFlag.封印.getValue();
        if (lock) { //锁定装备
            if (ItemFlag.封印.check(item.getFlag())) {
                getPlayer().dropMessage(6, "装备栏的第[" + slot + "]个道具已经是锁定状态，无需进行此操作。");
                return;
            }
            item.addFlag(flag);
        } else { //解除装备锁定
            if (!ItemFlag.封印.check(item.getFlag())) {
                getPlayer().dropMessage(6, "装备栏的第[" + slot + "]个道具不是锁定状态，无需进行此操作。");
                return;
            }
            item.removeFlag(flag);
        }
        getPlayer().forceUpdateItem(item);
    }

    public boolean addFromDrop(Object statsSel) {
        if (statsSel instanceof Item) {
            Item it = (Item) statsSel;
            return MapleInventoryManipulator.checkSpace(getClient(), it.getItemId(), it.getQuantity(), it.getOwner()) && MapleInventoryManipulator.addFromDrop(getClient(), it, false);
        }
        return false;
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type) {
        return replaceItem(slot, invType, statsSel, offset, type, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type, boolean takeSlot) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        Item item = getPlayer().getInventory(inv).getItem((byte) slot);
        if (item == null || statsSel instanceof Item) {
            item = (Item) statsSel;
        }
        if (offset > 0) {
            if (inv != MapleInventoryType.EQUIP) {
                return false;
            }
            Equip eq = (Equip) item;
            if (takeSlot) {
                if (eq.getUpgradeSlots() < 1) {
                    return false;
                } else {
                    eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() - 1));
                }
                if (eq.getExpiration() == -1) {
                    eq.setFlag((byte) (eq.getFlag() | ItemFlag.封印.getValue()));
                } else {
                    eq.setFlag((byte) (eq.getFlag() | ItemFlag.不可交易.getValue()));
                }
            }
            if (type.equalsIgnoreCase("Slots")) {
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + offset));
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("Level")) {
                eq.setLevel((byte) (eq.getLevel() + offset));
            } else if (type.equalsIgnoreCase("Hammer")) {
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("STR")) {
                eq.setStr((short) (eq.getStr() + offset));
            } else if (type.equalsIgnoreCase("DEX")) {
                eq.setDex((short) (eq.getDex() + offset));
            } else if (type.equalsIgnoreCase("INT")) {
                eq.setInt((short) (eq.getInt() + offset));
            } else if (type.equalsIgnoreCase("LUK")) {
                eq.setLuk((short) (eq.getLuk() + offset));
            } else if (type.equalsIgnoreCase("HP")) {
                eq.setHp((short) (eq.getHp() + offset));
            } else if (type.equalsIgnoreCase("MP")) {
                eq.setMp((short) (eq.getMp() + offset));
            } else if (type.equalsIgnoreCase("WATK")) {
                eq.setWatk((short) (eq.getWatk() + offset));
            } else if (type.equalsIgnoreCase("MATK")) {
                eq.setMatk((short) (eq.getMatk() + offset));
            } else if (type.equalsIgnoreCase("WDEF")) {
                eq.setWdef((short) (eq.getWdef() + offset));
            } else if (type.equalsIgnoreCase("MDEF")) {
                eq.setMdef((short) (eq.getMdef() + offset));
            } else if (type.equalsIgnoreCase("ACC")) {
                eq.setAcc((short) (eq.getAcc() + offset));
            } else if (type.equalsIgnoreCase("Avoid")) {
                eq.setAvoid((short) (eq.getAvoid() + offset));
            } else if (type.equalsIgnoreCase("Hands")) {
                eq.setHands((short) (eq.getHands() + offset));
            } else if (type.equalsIgnoreCase("Speed")) {
                eq.setSpeed((short) (eq.getSpeed() + offset));
            } else if (type.equalsIgnoreCase("Jump")) {
                eq.setJump((short) (eq.getJump() + offset));
            } else if (type.equalsIgnoreCase("ItemEXP")) {
                eq.setItemEXP(eq.getItemEXP() + offset);
            } else if (type.equalsIgnoreCase("Expiration")) {
                eq.setExpiration(eq.getExpiration() + offset);
            } else if (type.equalsIgnoreCase("Flag")) {
                eq.setFlag((byte) (eq.getFlag() + offset));
            }
            item = eq.copy();
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), inv, (short) slot, item.getQuantity(), false);
        return MapleInventoryManipulator.addFromDrop(getClient(), item, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int upgradeSlots) {
        return replaceItem(slot, invType, statsSel, upgradeSlots, "Slots");
    }

    public boolean isCash(int itemId) {
        return MapleItemInformationProvider.getInstance().isCash(itemId);
    }

    public int getTotalStat(int itemId) {
        return MapleItemInformationProvider.getInstance().getTotalStat((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
    }

    public int getReqLevel(int itemId) {
        return MapleItemInformationProvider.getInstance().getReqLevel(itemId);
    }

    public MapleStatEffect getEffect(int buff) {
        return MapleItemInformationProvider.getInstance().getItemEffect(buff);
    }

    public void buffGuild(int buff, int duration, String msg) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getItemEffect(buff) != null && getPlayer().getGuildId() > 0) {
            MapleStatEffect mse = ii.getItemEffect(buff);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                    if (chr.getGuildId() == getPlayer().getGuildId()) {
                        mse.applyTo(chr, chr, true, null, duration);
                        chr.dropMessage(5, "Your guild has gotten a " + msg + " buff.");
                    }
                }
            }
        }
    }

    /*
     * 家族联盟
     * 创建1个家族联盟
     */
    public boolean createAlliance(String alliancename) {
        MapleParty pt = getPlayer().getParty();
        MapleCharacter otherChar = getClient().getChannelServer().getPlayerStorage().getCharacterById(pt.getMemberByIndex(1).getId());
        if (otherChar == null || otherChar.getId() == getPlayer().getId()) {
            return false;
        }
        try {
            return WorldAllianceService.getInstance().createAlliance(alliancename, getPlayer().getId(), otherChar.getId(), getPlayer().getGuildId(), otherChar.getGuildId());
        } catch (Exception re) {
            log.error("createAlliance 错误", re);
            return false;
        }
    }

    public boolean addCapacityToAlliance() {
        try {
            MapleGuild guild = WorldGuildService.getInstance().getGuild(getPlayer().getGuildId());
            if (guild != null && getPlayer().getGuildRank() == 1 && getPlayer().getAllianceRank() == 1) {
                if (WorldAllianceService.getInstance().getAllianceLeader(guild.getAllianceId()) == getPlayer().getId() && WorldAllianceService.getInstance().changeAllianceCapacity(guild.getAllianceId())) {
                    gainMeso(-MapleGuildAlliance.CHANGE_CAPACITY_COST);
                    return true;
                }
            }
        } catch (Exception re) {
            log.error("addCapacityToAlliance 错误", re);
        }
        return false;
    }

    /*
     * 解散家族联盟
     */
    public boolean disbandAlliance() {
        try {
            MapleGuild guild = WorldGuildService.getInstance().getGuild(getPlayer().getGuildId());
            if (guild != null && getPlayer().getGuildRank() == 1 && getPlayer().getAllianceRank() == 1) {
                if (WorldAllianceService.getInstance().getAllianceLeader(guild.getAllianceId()) == getPlayer().getId() && WorldAllianceService.getInstance().disbandAlliance(guild.getAllianceId())) {
                    return true;
                }
            }
        } catch (Exception re) {
            log.error("disbandAlliance 错误", re);
        }
        return false;
    }

    public boolean hasSkill(int skillid) {
        Skill theSkill = SkillFactory.getSkill(skillid);
        return theSkill != null && getPlayer().getSkillLevel(theSkill) > 0;
    }

    public void maxAllSkills() {
        HashMap<Integer, SkillEntry> sDate = new HashMap<>();
        for (Integer skillid : SkillFactory.getAllSkills().keySet()) {
            Skill skill = SkillFactory.getSkill(skillid);
            if (skill != null && SkillConstants.isApplicableSkill(skillid) && skillid < 90000000) { //no db/additionals/resistance skills
                sDate.put(skillid, new SkillEntry((byte) skill.getMaxLevel(), (byte) skill.getMaxLevel(), SkillFactory.getDefaultSExpiry(skill)));
            }
        }
        getPlayer().changeSkillsLevel(sDate);
        sDate.clear();
    }

    public void maxSkillsByJob() {
        List<Integer> skillIds = new ArrayList<>();
        HashMap<Integer, SkillEntry> sDate = new HashMap<>();
        for (Integer skillid : SkillFactory.getAllSkills().keySet()) {
            Skill skill = SkillFactory.getSkill(skillid);
            if (skill != null && skill.canBeLearnedBy(getPlayer().getJob()) && !JobConstants.is新手职业(skillid / 10000) && !skill.isSpecialSkill() && !skill.isHyperSkill()) {
                sDate.put(skillid, new SkillEntry((byte) skill.getMaxLevel(), (byte) skill.getMaxLevel(), SkillFactory.getDefaultSExpiry(skill)));
                skillIds.add(skillid);
            }
        }
        getPlayer().changeSkillsLevel(sDate);
        Collections.sort(skillIds);
//        if (getPlayer().isShowPacket()) {
//            String job = "Skill\\" + JobConstants.getJobNameById(getPlayer().getJob()) + ".txt";
//            for (Integer skillId : skillIds) {
//                for (Entry<Integer, SkillEntry> data : sDate.entrySet()) {
//                    if (data.getKey().getId() == skillId) {
//                        String txt = "public static final int " + data.getKey().getName() + " = " + data.getKey().getId() + "; //技能最大等级" + data.getKey().getMaxLevel();
//                        FileoutputUtil.log(job, txt, true);
//                    }
//                }
//            }
//        }
        sDate.clear();
        skillIds.clear();
    }

    public void clearSkills() {
        getPlayer().clearSkills();
    }

    public void maxHyperSkillsByJob() {
        List<Integer> skillIds = new ArrayList<>();
        HashMap<Integer, SkillEntry> sDate = new HashMap<>();
        for (Integer skillid : SkillFactory.getAllSkills().keySet()) {
            Skill skill = SkillFactory.getSkill(skillid);
            if (skill != null && skill.canBeLearnedBy(getPlayer().getJob()) && skill.isHyperSkill()) {
                sDate.put(skillid, new SkillEntry((byte) skill.getMaxLevel(), (byte) skill.getMaxLevel(), SkillFactory.getDefaultSExpiry(skill)));
                skillIds.add(skillid);
            }
        }
        getPlayer().changeSkillsLevel(sDate);
        Collections.sort(skillIds);
//        if (getPlayer().isShowPacket()) {
//            String job = "Skill\\" + JobConstants.getJobNameById(getPlayer().getJob()) + ".txt";
//            for (Integer skillId : skillIds) {
//                for (Entry<Integer, SkillEntry> data : sDate.entrySet()) {
//                    if (data.getKey().getId() == skillId) {
//                        String txt = "public static final int " + data.getKey().getName() + " = " + data.getKey().getId() + "; //技能最大等级" + data.getKey().getMaxLevel();
//                        FileoutputUtil.log(job, txt, true);
//                    }
//                }
//            }
//        }
        sDate.clear();
        skillIds.clear();
    }

    /*
     * 加满新手技能
     */
    public void maxBeginnerSkills() {
        List<Integer> skillIds = new ArrayList<>();
        HashMap<Integer, SkillEntry> sDate = new HashMap<>();
        for (Integer skil : SkillFactory.getAllSkills().keySet()) {
            Skill skill = SkillFactory.getSkill(skil);
            if (skill != null && skill.canBeLearnedBy(getPlayer().getJob()) && skill.isBeginnerSkill() && !skill.isSpecialSkill() && !skill.isHyperSkill()) {
                sDate.put(skil, new SkillEntry((byte) skill.getMaxLevel(), (byte) skill.getMaxLevel(), SkillFactory.getDefaultSExpiry(skill)));
                skillIds.add(skil);
            }
        }
        getPlayer().changeSkillsLevel(sDate);
        Collections.sort(skillIds);
        if (getPlayer().isShowPacket()) {
            Logger log = LogManager.getLogger("新手技能");
            for (Integer skillId : skillIds) {
                for (Entry<Integer, SkillEntry> data : sDate.entrySet()) {
                    Skill skill = SkillFactory.getSkill(data.getKey());
                    if (skill != null && data.getKey().equals(skillId)) {
                        log.info(JobConstants.getJobNameById(getPlayer().getJob()) + skill.getName() + " = " + data.getKey() + "; //技能最大等级" + skill.getMaxLevel());
                    }
                }
            }
        }
        sDate.clear();
        skillIds.clear();
    }

    public void resetStats(int str, int dex, int z, int luk) {
        getPlayer().resetStats(str, dex, z, luk);
    }

    public boolean dropItem(int slot, int invType, int quantity) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        return inv != null && MapleInventoryManipulator.drop(getClient(), inv, (short) slot, (short) quantity, true);
    }

    public boolean removeItem(int slot, int invType, int quantity) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        return inv != null && MapleInventoryManipulator.removeFromSlot(getClient(), inv, (short) slot, (short) quantity, true);
    }

    public List<Integer> getAllPotentialInfo() {
        List<Integer> list = new ArrayList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().keySet());
        Collections.sort(list);
        return list;
    }

    public List<Integer> getAllPotentialInfoSearch(String content) {
        List<Integer> list = new ArrayList<>();
        for (Entry<Integer, List<StructItemOption>> i : MapleItemInformationProvider.getInstance().getAllPotentialInfo().entrySet()) {
            for (StructItemOption ii : i.getValue()) {
                if (ii.toString().contains(content)) {
                    list.add(i.getKey());
                }
            }
        }
        Collections.sort(list);
        return list;
    }

    public String getPotentialInfo(int id) {
        List<StructItemOption> potInfo = MapleItemInformationProvider.getInstance().getPotentialInfo(id);
        StringBuilder builder = new StringBuilder("#b#e以下是潜能ID为 ");
        builder.append(id);
        builder.append(" 的信息#n#k\r\n\r\n");
        int minLevel = 1, maxLevel = 10;
        for (StructItemOption item : potInfo) {
            builder.append("#e等级范围 ");
            builder.append(minLevel);
            builder.append("~");
            builder.append(maxLevel);
            builder.append(": #n");
            builder.append(item.toString());
            minLevel += 10;
            maxLevel += 10;
            builder.append("\r\n");
        }
        return builder.toString();
    }

    public void sendRPS() {
        getClient().announce(MaplePacketCreator.getRPSMode((byte) 8, -1, -1, -1));
    }

    public void setQuestRecord(Object ch, int questid, String data) {
        ((MapleCharacter) ch).getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(data);
    }

    public final void doWeddingEffect(final Object ch) {
        final MapleCharacter chr = (MapleCharacter) ch;
        final MapleCharacter player = getPlayer();
        WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.yellowChat(player.getName() + ", 你愿意娶 " + chr.getName() + " 为妻吗？无论她将来是富有还是贫穷、或无论她将来身体健康或不适，你都愿意和她永远在一起吗？"));
        CloneTimer.getInstance().schedule(() -> {
            if (chr == null || player == null) {
                warpMap(700000000, 0);
            } else {
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.yellowChat(chr.getName() + ", 你愿意嫁给 " + player.getName() + " 吗？无论他将来是富有还是贫穷、或无论他将来身体健康或不适，你都愿意和他永远在一起吗？"));
            }
        }, 10000);
        CloneTimer.getInstance().schedule(() -> {
            if (chr == null || player == null) {
                if (player != null) {
                    setQuestRecord(player, 160001, "3");
                    setQuestRecord(player, 160002, "0");
                } else if (chr != null) {
                    setQuestRecord(chr, 160001, "3");
                    setQuestRecord(chr, 160002, "0");
                }
                warpMap(700000000, 0);
            } else {
                setQuestRecord(player, 160001, "2");
                setQuestRecord(chr, 160001, "2");
                chr.setMarriageId(player.getId());
                player.setMarriageId(chr.getId());
                sendNPCText("好，我以圣灵、圣父、圣子的名义宣布：" + player.getName() + " 和 " + chr.getName() + "结为夫妻。 希望你们在 " + chr.getClient().getChannelServer().getServerName() + " 游戏中玩的愉快!", 9201002);
                chr.getMap().startExtendedMapEffect("现在，新郎可以亲吻新娘了。 " + player.getName() + "!", 5120006);
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.yellowChat("好，我以圣灵、圣父、圣子的名义宣布：" + player.getName() + " 和 " + chr.getName() + "结为夫妻。 希望你们在 " + chr.getClient().getChannelServer().getServerName() + " 游戏中玩的愉快!"));
                if (chr.getGuildId() > 0) {
                    WorldGuildService.getInstance().guildPacket(chr.getGuildId(), MaplePacketCreator.sendMarriage(false, chr.getName()));
                }
                if (chr.getFamilyId() > 0) {
                    WorldFamilyService.getInstance().familyPacket(chr.getFamilyId(), MaplePacketCreator.sendMarriage(true, chr.getName()), chr.getId());
                }
                if (player.getGuildId() > 0) {
                    WorldGuildService.getInstance().guildPacket(player.getGuildId(), MaplePacketCreator.sendMarriage(false, player.getName()));
                }
                if (player.getFamilyId() > 0) {
                    WorldFamilyService.getInstance().familyPacket(player.getFamilyId(), MaplePacketCreator.sendMarriage(true, chr.getName()), player.getId());
                }
            }
        }, 20000); //10 sec 10 sec
    }

    public void putKey(int key, int type, int action) {
        getPlayer().changeKeybinding(key, (byte) type, action);
        getClient().announce(MaplePacketCreator.getKeymap(getPlayer()));
    }

    public void logDonator(String log, int previous_points) {
        StringBuilder logg = new StringBuilder();
        logg.append(MapleCharacterUtil.makeMapleReadable(getPlayer().getName()));
        logg.append(" [角色ID: ").append(getPlayer().getId()).append("] ");
        logg.append(" [账号: ").append(MapleCharacterUtil.makeMapleReadable(getClient().getAccountName())).append("] ");
        logg.append(log);
        logg.append(" [以前: ").append(previous_points).append("] [现在: ").append(getPlayer().getPoints()).append("]");
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO donorlog VALUES(DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, MapleCharacterUtil.makeMapleReadable(getClient().getAccountName()));
            ps.setInt(2, getClient().getAccID());
            ps.setString(3, MapleCharacterUtil.makeMapleReadable(getPlayer().getName()));
            ps.setInt(4, getPlayer().getId());
            ps.setString(5, log);
            ps.setString(6, DateUtil.getNowTime());
            ps.setInt(7, previous_points);
            ps.setInt(8, getPlayer().getPoints());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            NPCConversationManager.log.error(logg, e);
        }
    }

    public void doRing(String name, int itemid) {
        PlayersHandler.DoRing(getClient(), name, itemid);
    }

    public int getNaturalStats(int itemid, String it) {
        Map<String, Integer> eqStats = MapleItemInformationProvider.getInstance().getItemBaseInfo(itemid);
        if (eqStats != null && eqStats.containsKey(it)) {
            return eqStats.get(it);
        }
        return 0;
    }

    public boolean isEligibleName(String t) {
        return MapleCharacterUtil.canCreateChar(t, getPlayer().isGM()) && (!LoginInformationProvider.getInstance().isForbiddenName(t) || getPlayer().isGM());
    }

    public String checkDrop(int mobId) {
        //MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        if (!ranks.isEmpty() && ranks.size() > 0) {
            int num = 0, itemId, chance;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            for (MonsterDropEntry rank : ranks) {
                de = rank;
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                    itemId = de.itemId;
                    if (itemId == 0) {
                        continue;
                    }
                    if (num == 0) {
                        name.append("当前怪物 #o").append(mobId).append("# 的爆率为:\r\n");
                        name.append("--------------------------------------\r\n");
                        //outputWithLogging(mobId, "-- 怪物ID " + mobId);
                        //outputWithLogging(mobId, "-- 掉宝数量 " + ranks.size());
                        //outputWithLogging(mobId, "INSERT INTO drop_data (`dropperid`, `itemid`, `minimum_quantity`, `maximum_quantity`, `questid`, `chance`) VALUES");
                    }
                    //String lineMarker = (i == ranks.size() ? ";" : ",");
                    //outputWithLogging(mobId, "(" + mobId + ", " + itemId + ", " + de.minimum + ", " + de.maximum + ", " + de.questid + ", " + de.chance + ")" + lineMarker + " -- " + ii.getName(itemId) + ii.getReqLevel(itemId));
                    String namez = "#z" + itemId + "#";
                    if (itemId == 0) { //金币 物品ID为0就是金币道具
                        itemId = 4031041; //休咪的钱包 display sack of cash
                        namez = (de.minimum * getClient().getChannelServer().getMesoRate()) + " - " + (de.maximum * getClient().getChannelServer().getMesoRate()) + " 的金币";
                    }
                    chance = de.chance * getClient().getChannelServer().getDropRate();
                    if (getPlayer().isAdmin()) {
                        name.append(num + 1).append(") #v").append(itemId).append("#").append(namez).append(" - ").append(Integer.valueOf(chance >= 999999 ? 1000000 : chance).doubleValue() / 10000.0).append("%的爆率. ").append(de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接受任务: " + MapleQuest.getInstance(de.questid).getName()) : "").append("\r\n");
                    } else {
                        name.append(num + 1).append(") #v").append(itemId).append("#").append(namez).append(de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接受任务: " + MapleQuest.getInstance(de.questid).getName()) : "").append("\r\n");
                    }
                    num++;
                }
            }
            if (name.length() > 0) {
                return name.toString();
            }
        }
        return "没有找到这个怪物的爆率数据。";
    }

    public String checkMapDrop() {
        List<MonsterGlobalDropEntry> ranks = new ArrayList<>(MapleMonsterInformationProvider.getInstance().getGlobalDrop());
        int mapid = getPlayer().getMap().getId();
//        int cashServerRate = getClient().getChannelServer().getCashRate(); //点卷爆率
        int globalServerRate = getClient().getChannelServer().getDropgRate(); //特殊数据库道具爆率
        if (ranks != null && ranks.size() > 0) {
            int num = 0, itemId, chance;
            MonsterGlobalDropEntry de;
            StringBuilder name = new StringBuilder();
            for (MonsterGlobalDropEntry rank : ranks) {
                de = rank;
                if (de.continent < 0 || (de.continent < 10 && mapid / 100000000 == de.continent) || (de.continent < 100 && mapid / 10000000 == de.continent) || (de.continent < 1000 && mapid / 1000000 == de.continent)) {
                    itemId = de.itemId;
                    if (num == 0) {
                        name.append("当前地图 #r").append(mapid).append("#k - #m").append(mapid).append("# 的全局爆率为:");
                        name.append("\r\n--------------------------------------\r\n");
                    }
                    String names = "#z" + itemId + "#";
//                    if (itemId == 0 && cashServerRate != 0) {
//                        itemId = 4031041;
//                        names = (de.minimum * cashServerRate) + " - " + (de.maximum * cashServerRate) + " 的抵用卷";
//                    }
                    chance = de.chance * globalServerRate;
                    if (getPlayer().isAdmin()) {
                        name.append(num + 1).append(") #v").append(itemId).append("#").append(names).append(" - ").append(Integer.valueOf(chance >= 999999 ? 1000000 : chance).doubleValue() / 10000.0).append("%的爆率. ").append(de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接受任务: " + MapleQuest.getInstance(de.questid).getName()) : "").append("\r\n");
                    } else {
                        name.append(num + 1).append(") #v").append(itemId).append("#").append(names).append(de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("需要接受任务: " + MapleQuest.getInstance(de.questid).getName()) : "").append("\r\n");
                    }
                    num++;
                }
            }
            if (name.length() > 0) {
                return name.toString();
            }
        }
        return "当前地图没有设置全局爆率。";
    }

//    public void outputWithLogging(int mobId, String buff) {
//        String file = "drop_data\\" + mobId + ".sql";
//        FileoutputUtil.log(file, buff, true);
//    }

    public List<PokedexEntry> getAllPokedex() {
        return BattleConstants.getAllPokedex();
    }

    public String getLeftPadded(String in, char padchar, int length) {
        return StringUtil.getLeftPaddedStr(in, padchar, length);
    }

    public void preparePokemonBattle(List<Integer> npcTeam, int restrictedLevel) {
        int theId = MapleLifeFactory.getRandomNPC();
        PokemonBattle wild = new PokemonBattle(getPlayer(), npcTeam, theId, restrictedLevel);
        getPlayer().changeMap(wild.getMap(), wild.getMap().getPortal(0));
        getPlayer().setBattle(wild);
        wild.initiate(getPlayer(), MapleLifeFactory.getNPC(theId, getMapId()));
    }

    public List<Integer> makeTeam(int lowRange, int highRange, int neededLevel, int restrictedLevel) { //easy = 10 lvls below you to your lvl, normal = 5 lvls below you to 5 lvls above, hard = your lvl to 10 lvls above, hell = bosses that are lower than you
        // easy/norm/hard = min lvl 10, hell = min lvl 100
        List<Integer> ret = new ArrayList<>();
        int averageLevel = 0, numBattlers = 0;
        for (Battler b : getPlayer().getBattlers()) {
            if (b != null) {
                if (b.getLevel() > averageLevel) {
                    averageLevel = b.getLevel();
                }
                numBattlers++;
            }
        }
        boolean hell = lowRange == highRange;
        if (numBattlers < 3 || averageLevel < neededLevel) {
            return null;
        }
        if (averageLevel > restrictedLevel) {
            averageLevel = restrictedLevel; //cap it
        }
        List<PokedexEntry> pokeEntries = new ArrayList<>(getAllPokedex());
        Collections.shuffle(pokeEntries);
        while (ret.size() < numBattlers) {
            for (PokedexEntry d : pokeEntries) {
                if ((d.dummyBattler.getStats().isBoss() && hell) || (!d.dummyBattler.getStats().isBoss() && !hell)) {
                    if (!hell) {
                        if (d.dummyBattler.getLevel() <= (averageLevel + highRange) && d.dummyBattler.getLevel() >= (averageLevel + lowRange) && Randomizer.nextInt(numBattlers) == 0) {
                            ret.add(d.id);
                            if (ret.size() >= numBattlers) {
                                break;
                            }
                        }
                    } else if (d.dummyBattler.getFamily().type != MobExp.EASY && d.dummyBattler.getLevel() >= neededLevel && d.dummyBattler.getLevel() <= averageLevel && Randomizer.nextInt(numBattlers) == 0) {
                        ret.add(d.id);
                        if (ret.size() >= numBattlers) {
                            break;
                        }
                    }
                }
            }
        }
        return ret;
    }

    public BattleConstants.HoldItem[] getAllHoldItems() {
        return BattleConstants.HoldItem.values();
    }

    public void handleDivorce() {
        if (getPlayer().getMarriageId() <= 0) {
            sendNext("你还没结婚，怎么能离婚呢？");
            return;
        }
        int chz = WorldFindService.getInstance().findChannel(getPlayer().getMarriageId());
        MapleRing mRing = getPlayer().getMarriageRing();
        if (chz == -1) {  //sql queries
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                PreparedStatement ps = con.prepareStatement("UPDATE queststatus SET customData = ? WHERE characterid = ? AND (quest = ? OR quest = ?)");
                ps.setString(1, "0");
                ps.setInt(2, getPlayer().getMarriageId());
                ps.setInt(3, 160001);
                ps.setInt(4, 160002);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("UPDATE characters SET marriageid = ? WHERE id = ?");
                ps.setInt(1, 0);
                ps.setInt(2, getPlayer().getMarriageId());
                ps.executeUpdate();
                ps.close();
                if (mRing != null) {
                    ps = con.prepareStatement("DELETE FROM inventoryitems WHERE itemid = ? AND characterid = ?");
                    ps.setInt(1, mRing.getItemId());
                    ps.setInt(2, getPlayer().getMarriageId());
                    ps.executeUpdate();
                    ps.close();
                }
            } catch (SQLException e) {
                log.error(e);
                return;
            }
            if (mRing != null) {
                getPlayer().removeAll(mRing.getItemId(), true, true);
                MapleRing.removeRingFromDb(mRing.getRingId(), mRing.getPartnerRingId());
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.yellowChat("[系统公告] " + getPlayer().getName() + " 和 " + mRing.getPartnerName() + " 离婚了。"));
            }
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("离婚成功...");
            return;
        } else if (chz < -1) {
            sendNext("请确保你的伴侣是在线的.");
            return;
        }
        MapleCharacter cPlayer = ChannelServer.getInstance(chz).getPlayerStorage().getCharacterById(getPlayer().getMarriageId());
        if (cPlayer != null) {
            if (mRing != null) {
                cPlayer.removeAll(mRing.getItemId(), true, true);
                getPlayer().removeAll(mRing.getItemId(), true, true);
                MapleRing.removeRingFromDb(mRing.getRingId(), mRing.getPartnerRingId());
            }
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.yellowChat("[系统公告] " + getPlayer().getName() + " 和 " + cPlayer.getName() + " 离婚了。"));
            cPlayer.dropMessage(1, "你的伴侣和你离婚了.");
            cPlayer.setMarriageId(0);
            setQuestRecord(cPlayer, 160001, "0");
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(cPlayer, 160002, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("离婚成功...");
        } else {
            sendNext("出现了未知的错误...");
        }
    }

    public String getReadableMillis(long startMillis, long endMillis) {
        return StringUtil.getReadableMillis(startMillis, endMillis);
    }

    public boolean canCreateUltimate() {
        if (getPlayer().getLevel() < 120) {
            return false;
        }
        int jobId = getPlayer().getJob();
        return jobId == 1111 || jobId == 1112 || jobId == 1211 || jobId == 1212 || jobId == 1311 || jobId == 1312 || jobId == 1411 || jobId == 1412 || jobId == 1511 || jobId == 1512;
    }

    /*
     * 终极冒险岛家创建窗口
     */
    public void sendUltimateExplorer() {
        getClient().announce(MaplePacketCreator.ultimateExplorer());
    }

    public String getRankingInformation(int job) {
        StringBuilder sb = new StringBuilder();
        for (RankingInformation pi : RankingWorker.getRankingInfo(job)) {
            sb.append(pi.toString());
        }
        return sb.toString();
    }

    public String getPokemonRanking() {
        StringBuilder sb = new StringBuilder();
        for (PokemonInformation pi : RankingWorker.getPokemonInfo()) {
            sb.append(pi.toString());
        }
        return sb.toString();
    }

    public String getPokemonRanking_Caught() {
        StringBuilder sb = new StringBuilder();
        for (PokedexInformation pi : RankingWorker.getPokemonCaught()) {
            sb.append(pi.toString());
        }
        return sb.toString();
    }

    public String getPokemonRanking_Ratio() {
        StringBuilder sb = new StringBuilder();
        for (PokebattleInformation pi : RankingWorker.getPokemonRatio()) {
            sb.append(pi.toString());
        }
        return sb.toString();
    }

    public void sendPendant(boolean b) {
        getClient().announce(MaplePacketCreator.pendantSlot(b));
    }

    public Triple<Integer, Integer, Integer> getCompensation() {
        Triple<Integer, Integer, Integer> ret = null;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM compensationlog_confirmed WHERE chrname LIKE ?");
            ps.setString(1, getPlayer().getName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = new Triple<>(rs.getInt("value"), rs.getInt("taken"), rs.getInt("donor"));
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException e) {
            log.error(e);
            return ret;
        }
    }

    public boolean deleteCompensation(int taken) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE compensationlog_confirmed SET taken = ? WHERE chrname LIKE ?");
            ps.setInt(1, taken);
            ps.setString(2, getPlayer().getName());
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException e) {
            log.error(e);
            return false;
        }
    }

    public void testPacket(String testmsg) {
        getClient().announce(MaplePacketCreator.testPacket(testmsg));
    }

    public void testPacket(int op) {
        getClient().announce(FamilyPacket.sendFamilyMessage(op));
    }

    public void testPacket(String op, String msg) {
        getClient().announce(MaplePacketCreator.testPacket(op, msg));
    }

    public short getSpace(byte type) {
        return getPlayer().getSpace(type);
    }

    public boolean haveSpace(int type) {
        return getPlayer().haveSpace(type);
    }

    public boolean haveSpaceForId(int itemid) {
        return getPlayer().haveSpaceForId(itemid);
    }

    public int getMoney() {
        int money = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int cid = getPlayer().getId();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM bank WHERE charid=?");
            ps.setInt(1, cid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                money = rs.getInt("money");
            } else {
                PreparedStatement psu = con.prepareStatement("INSERT INTO bank (charid, money) VALUES (?, ?)");
                psu.setInt(1, cid);
                psu.setInt(2, 0);
                psu.executeUpdate();
                psu.close();
            }
            ps.close();
            rs.close();
        } catch (SQLException ex) {
            log.error("银行存款获取信息发生错误", ex);
        }
        return money;
    }

    public int addMoney(long money, int type) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int cid = getPlayer().getId();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM bank WHERE charid=?");
            ps.setLong(1, cid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (type == 1) {
                    if (money > rs.getLong("money")) {
                        return -1;
                    }
                }
                ps = con.prepareStatement("UPDATE bank SET money =money+ " + money + " WHERE charid = " + cid + "");
                return ps.executeUpdate();
            }
            ps.close();
            rs.close();
        } catch (SQLException ex) {
            log.error("银行存款添加数量发生错误", ex);
        }
        return 0;
    }

    /*
     * 刷新玩家信息
     */
    public void fakeRelog() {
        if (!getPlayer().isAlive() || getPlayer().getEventInstance() != null || FieldLimitType.ChannelSwitch.check(getPlayer().getMap().getFieldLimit())) {
            getPlayer().dropMessage(1, "刷新人物数据失败.");
            return;
        }
        getPlayer().dropMessage(5, "正在刷新人数据.请等待...");
        getPlayer().fakeRelog();
    }

    /*
     * 通过名字获取当前频道的的角色
     */
    public MapleCharacter getCharByName(String name) {
        try {
            return getClient().getChannelServer().getPlayerStorage().getCharacterByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * 获取角色的装备列表信息
     */
    public String EquipList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new LinkedList<>();
        for (Item item : equip.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    /*
     * 获取角色的消耗列表信息
     */
    public String UseList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory use = getPlayer().getInventory(MapleInventoryType.USE);
        List<String> stra = new LinkedList<>();
        for (Item item : use.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    /*
     * 获取角色的商城列表信息
     */
    public String CashList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory cash = getPlayer().getInventory(MapleInventoryType.CASH);
        List<String> stra = new LinkedList<>();
        for (Item item : cash.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    /*
     * 获取角色的其他列表信息
     */
    public String EtcList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory etc = getPlayer().getInventory(MapleInventoryType.ETC);
        List<String> stra = new LinkedList<>();
        for (Item item : etc.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    /*
     * 获取角色的设置列表信息
     */
    public String SetupList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory setup = getPlayer().getInventory(MapleInventoryType.SETUP);
        List<String> stra = new LinkedList<>();
        for (Item item : setup.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    /*
     * 删除全部指定道具ID的信息
     */
    public void deleteAll(int itemId) {
        MapleInventoryManipulator.removeAllById(getClient(), itemId, true);
    }

    public int useNebuliteGachapon() {
        try {
            if (getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1
                    || getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 1
                    || getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1
                    || getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 1
                    || getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() < 1) {
                return -1;
            }
            int grade = 0; // Default D
            int chance = Randomizer.nextInt(100); // cannot gacha S, only from alien cube.
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (chance < 1) { // Grade A
                grade = 3;
            } else if (chance < 5) { // Grade B
                grade = 2;
            } else if (chance < 35) { // Grade getClient()
                grade = 1;
            } else { // grade == 0
                grade = Randomizer.nextInt(100) < 25 ? 5 : 0; // 25% again to get premium ticket piece				
            }
            int newId = 0;
            if (grade == 5) {
                newId = 4420000;
            } else {
                List<StructItemOption> pots = new LinkedList<>(ii.getAllSocketInfo(grade).values());
                while (newId == 0) {
                    StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
                    if (pot != null) {
                        newId = pot.opID;
                    }
                }
            }
            Item item = MapleInventoryManipulator.addbyId_Gachapon(getClient(), newId, (short) 1, "从星岩中获得 时间: " + DateUtil.getNowTime());
            if (item == null) {
                return -1;
            }
            if (grade >= 2 && grade != 5) {
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.getGachaponMega(getPlayer().getName(), " : 从星岩中获得{" + ii.getName(item.getItemId()) + "}！大家一起恭喜他（她）吧！！！！", item, (byte) 2, getClient().getChannel()));
            }
            getClient().announce(MaplePacketCreator.getShowItemGain(newId, (short) 1, true));
            return item.getItemId();
        } catch (Exception e) {
            System.out.println("[Error] Failed to use Nebulite Gachapon. " + e);
        }
        return -1;
    }

    public void giveMountSkill(int itemId, int mountSkillId, long period) {
        giveMountSkill(itemId, mountSkillId, period, false);
    }

    public void giveMountSkill(int itemId, int mountSkillId, long period, boolean test) {
        if (mountSkillId > 0 && haveItem(itemId)) {
            if (test) {
                System.err.println("骑宠技能 - 1 " + mountSkillId + " LinkedMountItem: " + mountSkillId % 1000);
            }
            mountSkillId = mountSkillId > 80001000 ? mountSkillId : PlayerStats.getSkillByJob(mountSkillId, getPlayer().getJob());
            int fk = GameConstants.getMountItem(mountSkillId, getClient().getPlayer());
            if (test) {
                System.err.println("骑宠技能 - 2 " + mountSkillId + " 骑宠ID: " + fk);
            }
            if (fk > 0 && mountSkillId < 80001000) {
                for (int i = 80001001; i < 80001999; i++) {
                    Skill skill = SkillFactory.getSkill(i);
                    if (skill != null && GameConstants.getMountItem(skill.getId(), getClient().getPlayer()) == fk) {
                        mountSkillId = i;
                        break;
                    }
                }
            }
            if (test) {
                System.err.println("骑宠技能 - 3 " + mountSkillId + " 技能是否为空: " + (SkillFactory.getSkill(mountSkillId) == null) + " 骑宠: " + (GameConstants.getMountItem(mountSkillId, getClient().getPlayer()) == 0));
            }
            if (getPlayer().getSkillLevel(mountSkillId) > 0) {
                getPlayer().dropMessage(1, "您已经拥有了[" + SkillFactory.getSkill(mountSkillId).getName() + "]这个骑宠的技能，无法使用该道具。");
            } else if (SkillFactory.getSkill(mountSkillId) == null || GameConstants.getMountItem(mountSkillId, getClient().getPlayer()) == 0) {
                getPlayer().dropMessage(1, "暂时无法使用这个骑宠的技能.");
            } else if (period > 0) {
                gainItem(itemId, (short) -1);
                getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(mountSkillId), (byte) 1, (byte) 1, System.currentTimeMillis() + period * 24 * 60 * 60 * 1000);
                getPlayer().dropMessage(1, "恭喜您获得[" + SkillFactory.getSkill(mountSkillId).getName() + "]骑宠技能 " + period + " 权。");
            } else if (period == -1) {
                gainItem(itemId, (short) -1);
                getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(mountSkillId), (byte) 1, (byte) 1, -1);
                getPlayer().dropMessage(1, "恭喜您获得[" + SkillFactory.getSkill(mountSkillId).getName() + "]骑宠技能永久权。");
            }
        } else {
            getPlayer().dropMessage(1, "暂时无法使用这个骑宠的技能\r\n我的道具ID为: " + itemId);
        }
        getClient().announce(MaplePacketCreator.enableActions());
    }

    /*
     * 检测队伍是否能组队任务
     * 必须是所有队伍中的成员满足条件才可以开始任务
     */
    public boolean checkLevelAndItem(int minLevel, int maxLevel, int itemId) {
        return checkLevelAndItem(minLevel, maxLevel, itemId, 2);
    }

    public boolean checkLevelAndItem(int minLevel, int maxLevel, int itemId, int minSize) {
        MapleParty party = getPlayer().getParty();
        if (party == null || party.getLeader().getId() != getPlayer().getId()) {
            getPlayer().dropMessage(5, "您没有队伍 或者 不是队长..");
            return false;
        }
        int partySize = party.getMembers().size();
        if (partySize < minSize) {
            getPlayer().dropMessage(5, "队伍的人数成员不够 必须 " + minSize + " 人才可以开始组队任务，当前队伍人数: " + partySize);
            return false;
        }
        int chrSize = 0;
        for (MaplePartyCharacter partyPlayer : party.getMembers()) {
            MapleCharacter player = getPlayer().getMap().getCharacterById(partyPlayer.getId());
            if (player == null) {
                getPlayer().dropMessage(5, "队伍中的成员 " + partyPlayer.getName() + " 不在线 或者 不在同一地图.");
            } else if (player.getLevel() < minLevel || player.getLevel() > maxLevel) {
                getPlayer().dropMessage(5, "队伍中的成员 " + partyPlayer.getName() + " 等级不符合要求.等级限制: Lv." + minLevel + "～" + maxLevel);
            } else if (!player.haveItem(itemId)) {
                getPlayer().dropMessage(5, "队伍中的成员 " + partyPlayer.getName() + " 没有开始组队任务需要的道具.");
            } else {
                chrSize++;
            }
        }
        return partySize == chrSize;
    }

    /**
     * 改变角色内在能力的技能
     */
    public void changeInnerSkill(int skillId, int skillevel, int position, int rank) {
        changeInnerSkill(skillId, skillevel, position, rank, false);
    }

    public void changeInnerSkill(int skillId, int skillevel, int position, int rank, boolean replace) {
        if (replace || getPlayer().getInnerSkillIdByPos(position) <= 0) {
            getPlayer().changeInnerSkill(skillId, skillevel, (byte) position, (byte) rank);
        }
    }

    /**
     * 角色是否有变身效果
     *
     * @return true ? 有 : 没有
     */
    public boolean isMorphed() {
        boolean morph = false;

        Integer morphed = getPlayer().getBuffedValue(MapleBuffStat.变身效果);
        if (morphed != null) {
            morph = true;
        }
        return morph;
    }

    /**
     * 角色变身后的效果值ID
     *
     * @return 1=蘑菇，2=猪，3=外星人
     */
    public int getMorphValue() {
        try {
            return getPlayer().getBuffedValue(MapleBuffStat.变身效果);
        } catch (NullPointerException n) {
            return -1;
        }
    }

    public void addItemToMarket(int itemid, int quantity, int price) {
        this.getClient().getChannelServer().getMarket().addItem(itemid, quantity, price, getPlayer().getId());
    }

    public void removeItemFromMarket(int itemid, int quantity) {
        this.getClient().getChannelServer().getMarket().removeItem(itemid, quantity, getPlayer().getId());
    }

    public void buyItem(int itemId, int quantity, int price, int charId) {
        try {
            for (MarketEngine.ItemEntry ie : getClient().getChannelServer().getMarket().getItems()) {
                if (ie.getId() == itemId && ie.getPrice() == price && ie.getOwner() == charId) {
                    if (ie.getQuantity() < quantity) {
                        getClient().announce(MaplePacketCreator.serverNotice(1, "数量不足！"));
                        return;
                    }
                    if (ie.getQuantity() * ie.getPrice() > getPlayer().getMeso()) {
                        getClient().announce(MaplePacketCreator.serverNotice(1, "没有足够的金币！"));
                        return;
                    }
                    int cost = ie.getPrice() * ie.getQuantity();
                    getClient().getChannelServer().getMarket().removeItem(itemId, quantity, charId);
                    getPlayer().gainMeso(-cost, true, true);
                    gainItem(itemId, (short) quantity);
                    for (ChannelServer cs : ChannelServer.getAllInstances()) {
                        for (MapleCharacter mc : cs.getPlayerStorage().getAllCharacters()) {
                            if (mc.getId() == charId) {
                                mc.gainMeso(cost, false, true);
                                mc.getClient().announce(MaplePacketCreator.serverNotice(5, "[寄售系统] 你已获得 " + cost + " 金币。买家 " + getPlayer().getName() + "."));
                                return;
                            }
                        }
                    }
                    //OMG the other player was not found..
                    try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                        PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
                        ps.setInt(1, charId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            int meso = rs.getInt("meso");
                            int gain = meso + cost;
                            ps = con.prepareStatement("UPDATE characters SET meso = ? WHERE id = ?");
                            ps.setInt(1, gain);
                            ps.setInt(2, charId);
                            ps.executeUpdate();
                        }
                        ps.close();
                        rs.close();
                    } catch (SQLException ignored) {

                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showInventory(int type) {
        String send = "";
        MapleInventory invy = getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
        for (Item item : invy.list()) {
            send += "#L" + item.getPosition() + "##v" + item.getItemId() + "# 数量: #b" + item.getQuantity() + "#k#l\\r\\n";
        }
        sendSimple(send);
    }

    public String getInventorys(int type) {
        String send = "";
        MapleInventory invy = getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
        for (Item item : invy.list()) {
            send += "#L" + item.getPosition() + "##v" + item.getItemId() + "# 数量: #b" + item.getQuantity() + "#k#l\\r\\n";
        }
        return send;
    }

    public Item getItem(int slot, int type) {
        MapleInventory invy = getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
        for (Item item : invy.list()) {
            if (item.getPosition() == slot) {
                return item;
            }
        }
        return null;
    }

    public String getMarket() {
        MarketEngine me = getClient().getChannelServer().getMarket();
        String ret = "";
        int count = 0;
        for (ItemEntry ie : me.getItems()) {
            if (ie.getOwner() == getPlayer().getId()) {
                continue;
            }
            ret += "#L" + count + "##v"
                    + ie.getId()
                    + "# #b数量#k: "
                    + ie.getQuantity()
                    + " #b费用#k: "
                    + ie.getPrice() + " 金币"
                    + " #b 所有者: #k"
                    + me.getCharacterName(ie.getOwner())
                    + "#l\\r\\n";
            count++;
        }
        return ret;
    }

    public String getMarketRetrival() {
        MarketEngine me = getClient().getChannelServer().getMarket();
        String ret = "";
        int count = 0;
        for (ItemEntry ie : me.getItems()) {
            if (ie.getOwner() != getPlayer().getId()) {
                continue;
            }
            ret += "#L" + count + "##v"
                    + ie.getId()
                    + "# #b数量#k: "
                    + ie.getQuantity()
                    + " #b费用#k: "
                    + ie.getPrice() + " 金币"
                    + "#l\\r\\n";
            count++;
        }
        return ret;
    }

    public List<ItemEntry> getMyMarketItems() {
        List<ItemEntry> ret = new LinkedList<>();
        synchronized (getClient().getChannelServer().getMarket().getItems()) {
            for (ItemEntry ie : getClient().getChannelServer().getMarket().getItems()) {
                if (ie.getOwner() == getPlayer().getId()) {
                    ret.add(ie);
                }
            }
        }
        return ret;
    }

    public void retrieveMarketItem(int position) {
        List<ItemEntry> items = getMyMarketItems();
        ItemEntry ie = items.get(position);
        gainItem(ie.getId(), (short) ie.getQuantity());
        removeItemFromMarket(ie.getId(), ie.getQuantity());
    }

    public List<ItemEntry> getMarketItems() {
        List<ItemEntry> ret = new LinkedList<>();
        synchronized (getClient().getChannelServer().getMarket().getItems()) {
            for (ItemEntry ie : getClient().getChannelServer().getMarket().getItems()) {
                if (ie.getOwner() != getPlayer().getId()) {
                    ret.add(ie);
                }
            }
        }
        return ret;
    }

    public String getCharName(int id) {
        return getClient().getChannelServer().getMarket().getCharacterName(id);
    }

    public boolean changeitemxx(int invType, int slot, int str, int dex, int int_, int luk, int watk, int matk, int hc) {
        if (invType != 1) {
            return false;
        }
        Equip item = (Equip) getItem(slot, invType);
        item.setStr((short) str);
        item.setDex((short) dex);
        item.setInt((short) int_);
        item.setLuk((short) luk);
        item.setWatk((short) watk);
        item.setMatk((short) matk);
        return true;
    }
}
