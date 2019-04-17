/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleClient;
import client.MapleEnumClass;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import handling.opcode.NPCTalkOP;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.life.MapleHideNpc;
import server.life.MapleNPC;
import server.life.PlayerNPC;
import server.shop.MapleShop;
import server.shop.MapleShopResponse;
import tools.HexTool;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author admin
 */
public class NPCPacket {

    // InitialQuiz
    public static final int InitialQuizRes_Request = 0x0;
    public static final int InitialQuizRes_Fail = 0x1;
    // InitialSpeedQuiz
    public static final int TypeSpeedQuizNpc = 0x0;
    public static final int TypeSpeedQuizMob = 0x1;
    public static final int TypeSpeedQuizItem = 0x2;
    // SpeakerTypeID
    public static final int NoESC = 0x1;
    public static final int NpcReplacedByUser = 0x2;
    public static final int NpcReplayedByNpc = 0x4;
    public static final int FlipImage = 0x8;
    private static final Logger log = LogManager.getLogger(NPCPacket.class);

    public static byte[] sendNpcHide(List<MapleHideNpc> hide) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_HIDE.getValue());
        mplew.write(hide.size());
        for (MapleHideNpc h : hide) {
            mplew.writeInt(h.getNpcId());
        }
        return mplew.getPacket();
    }

    public static byte[] spawnNPC(MapleNPC life) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_NPC.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        mplew.write(0);
        mplew.write(life.getF() == 1 ? 0 : 1);
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(life.isHidden() || GameConstants.isHideNpc(life.getMapid(), life.getId()) ? 0 : 1); //
        mplew.writeInt(0); //未知 V.114 新增
        mplew.writeZeroBytes(5);
        mplew.writeInt(-1);
        mplew.writeZeroBytes(7);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] removeNPC(int objectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_NPC.getValue());
        mplew.writeInt(objectid);

        return mplew.getPacket();
    }

    public static byte[] removeNPCController(int objectid, boolean miniMap) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.writeBool(miniMap);
        mplew.writeInt(objectid);

        return mplew.getPacket();
    }

    public static byte[] spawnNPCRequestController(MapleNPC life, boolean MiniMap) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        mplew.writeShort(life.getF() == 1 ? 0 : 1);
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(MiniMap ? 1 : 0);
        mplew.writeInt(0); //未知 V.114 新增
        mplew.writeZeroBytes(5);
        mplew.writeInt(-1);
        mplew.writeZeroBytes(7);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] spawnPlayerNPC(PlayerNPC npc) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_NPC.getValue());
        mplew.write(npc.getF() == 1 ? 0 : 1);
        mplew.writeInt(npc.getId());
        mplew.writeMapleAsciiString(npc.getName());
        mplew.write(npc.getGender());
        mplew.write(npc.getSkin());
        mplew.writeInt(npc.getFace());
        mplew.writeInt(0); //job lol
        mplew.write(0);
        mplew.writeInt(npc.getHair());
        Map<Byte, Integer> equip = npc.getEquips();
        Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        for (Map.Entry<Byte, Integer> position : equip.entrySet()) {
            byte pos = (byte) (position.getKey() * -1);
            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, position.getValue());
            } else if (pos > 100 && pos != 111) { // don't ask. o.o
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, position.getValue());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, position.getValue());
            }
        }
        for (Map.Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        for (Map.Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        mplew.write(0xFF);
        Integer cWeapon = equip.get((byte) -111);
        mplew.writeInt(cWeapon != null ? cWeapon : 0);
        Integer Weapon = equip.get((byte) -111);
        mplew.writeInt(Weapon != null ? Weapon : 0);
        Integer subWeapon = equip.get((byte) -10);
        mplew.writeInt(subWeapon != null ? subWeapon : 0);
        mplew.writeBool(false);
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(npc.getPet(i));
        }
        mplew.writeZeroBytes(7);

        return mplew.getPacket();
    }

    public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type) {
        return getNPCTalk(npc, msgType, talk, endBytes, type, npc, false);
    }

    public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type, int diffNpc) {
        return getNPCTalk(npc, msgType, talk, endBytes, type, diffNpc, false);
    }

    public static byte[] getPlayerTalk(int npc, byte msgType, String talk, String endBytes, byte type) {
        return getNPCTalk(npc, msgType, talk, endBytes, type, npc, true);
    }

    public static byte[] getPlayerTalk(int npc, byte msgType, String talk, String endBytes, byte type, int diffNpc) {
        return getNPCTalk(npc, msgType, talk, endBytes, type, diffNpc, true);
    }

    public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type, int diffNpc, boolean player) {
        return getNPCTalk(npc, msgType, talk, endBytes, type, (byte) 0, diffNpc, true);
    }

    public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type, byte type2, int diffNpc, boolean player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(player ? 0x03 : 0x04);
        mplew.writeInt(npc);
        mplew.write(player ? 1 : 0); //V.112.1新增
        if (player) {
            mplew.writeInt(0);
        }
        mplew.write(msgType);
        mplew.writeShort(type); // mask; 1 = no ESC, 2 = playerspeaks, 4 = diff NPC 8 = something, ty KDMS
        mplew.write(type2);
        if ((type & 0x4) != 0) {
            mplew.writeInt(diffNpc);
        }
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));
        mplew.writeInt(0);

        return mplew.getPacket();

    }

    public static byte[] OnSay(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, String sText, boolean bPrev, boolean bNext) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.SAY.getMsgType());
        mplew.write(bParam);
        if ((bParam & 0x4) > 0) {
            mplew.writeInt(nSpeakerTemplateID);
        }
        mplew.writeMapleAsciiString(sText);
        mplew.writeBool(bPrev);
        mplew.writeBool(bNext);
        return mplew.getPacket();
    }

    public static byte[] OnSayImage(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, List<String> asPath) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.SAYIMAGE.getMsgType());
        mplew.write(bParam);
        mplew.write(asPath.size());
        for (String sPath : asPath) {
            mplew.writeMapleAsciiString(sPath);//CUtilDlgEx::AddImageList(v8, sPath); 
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskYesNo(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, String sText) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKYESNO.getMsgType());
        mplew.write(bParam);//(bParam & 0x6) 
        mplew.writeMapleAsciiString(sText);
        return mplew.getPacket();
    }

    public static byte[] OnAskAccept(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, String sText) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKACCEPT.getMsgType());
        mplew.write(bParam);
        mplew.writeMapleAsciiString(sText);
        return mplew.getPacket();
    }

    public static byte[] OnAskText(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, String sMsg, String sMsgDefault, int nLenMin, int nLenMax) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKTEXT.getMsgType());
        mplew.write(bParam);//(bParam & 0x6) 
        mplew.writeMapleAsciiString(sMsg);
        mplew.writeMapleAsciiString(sMsgDefault);
        mplew.writeShort(nLenMin);
        mplew.writeShort(nLenMax);
        return mplew.getPacket();
    }

    public static byte[] OnAskBoxText(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, String sMsg, String sMsgDefault, int nCol, int nLine) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKBOXTEXT.getMsgType());
        mplew.write(bParam);//(bParam & 0x6) 
        mplew.writeMapleAsciiString(sMsg);
        mplew.writeMapleAsciiString(sMsgDefault);
        mplew.writeShort(nCol);
        mplew.writeShort(nLine);
        return mplew.getPacket();
    }

    public static byte[] OnAskNumber(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, String sMsg, int nDef, int nMin, int nMax) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKNUMBER.getMsgType());
        mplew.write(bParam);//(bParam & 0x6) 
        mplew.writeMapleAsciiString(sMsg);
        mplew.writeInt(nDef);
        mplew.writeInt(nMin);
        mplew.writeInt(nMax);
        return mplew.getPacket();
    }

    public static byte[] OnAskMenu(int nSpeakerTypeID, int nSpeakerTemplateID, byte bParam, String sMsg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKMENU.getMsgType());
        mplew.write(bParam);//(bParam & 0x6) 
        mplew.writeMapleAsciiString(sMsg);
        return mplew.getPacket();
    }

    public static byte[] OnAskAvatar(int nSpeakerTypeID, int nSpeakerTemplateID, String sMsg, int[] anCanadite) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKAVATAR.getMsgType());
        mplew.write(0);
        mplew.writeMapleAsciiString(sMsg);
        mplew.write(anCanadite.length);
        for (int nCanadite : anCanadite) {
            mplew.writeInt(nCanadite);
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskMembershopAvatar(int nSpeakerTypeID, int nSpeakerTemplateID, String sMsg, int[] aCanadite) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKMEMBERSHOPAVATAR.getMsgType());
        mplew.write(0);
        mplew.writeMapleAsciiString(sMsg);
        mplew.write(aCanadite.length);
        for (int nCanadite : aCanadite) {
            mplew.writeInt(nCanadite);
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskPet(int nSpeakerTypeID, int nSpeakerTemplateID, String sMsg, List<MaplePet> apPet) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKPET.getMsgType());
        mplew.write(0);
        mplew.writeMapleAsciiString(sMsg);
        mplew.write(apPet.size());
        for (MaplePet pPet : apPet) {
            if (pPet != null) {
                mplew.writeLong(pPet.getUniqueId());
                mplew.write(pPet.getSummonedValue());
            }
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskPetAll(int nSpeakerTypeID, int nSpeakerTemplateID, String sMsg, List<MaplePet> apPet, boolean bExceptionExist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKPETALL.getMsgType());
        mplew.write(0);
        mplew.writeMapleAsciiString(sMsg);
        mplew.write(apPet.size());
        mplew.writeBool(bExceptionExist);
        for (MaplePet pPet : apPet) {
            if (pPet != null) {
                mplew.writeLong(pPet.getUniqueId());
                mplew.write(pPet.getSummonedValue());
            }
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskQuiz(int nSpeakerTypeID, int nSpeakerTemplateID, int nResCode, String sTitle, String sProblemText, String sHintText, int nMinInput, int nMaxInput, int tRemainInitialQuiz) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKQUIZ.getMsgType());
        mplew.write(0);
        mplew.write(nResCode);
        if (nResCode == InitialQuizRes_Request) {
            mplew.writeMapleAsciiString(sTitle);
            mplew.writeMapleAsciiString(sProblemText);
            mplew.writeMapleAsciiString(sHintText);
            mplew.writeShort(nMinInput);
            mplew.writeShort(nMaxInput);
            mplew.writeInt(tRemainInitialQuiz);
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskSpeedQuiz(int nSpeakerTypeID, int nSpeakerTemplateID, int nResCode, int nType, int dwAnswer, int nCorrect, int nRemain, int tRemainInitialQuiz) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKSPEEDQUIZ.getMsgType());
        mplew.write(0);
        mplew.write(nResCode);
        if (nResCode == InitialQuizRes_Request) {
            mplew.writeInt(nType);
            mplew.writeInt(dwAnswer);
            mplew.writeInt(nCorrect);
            mplew.writeInt(nRemain);
            mplew.writeInt(tRemainInitialQuiz);
        }
        return mplew.getPacket();
    }

    public static byte[] OnAskSlideMenu(int nSpeakerTypeID, int nSpeakerTemplateID, boolean bSlideDlgEX, int nIndex, String sMsg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(nSpeakerTemplateID);
        mplew.writeShort(MapleEnumClass.ScriptMessageType.ASKSLIDEMENU.getMsgType());
        mplew.write(0);
        mplew.writeInt(bSlideDlgEX ? 1 : 0);
        mplew.writeInt(nIndex);
        mplew.writeMapleAsciiString(sMsg);
        return mplew.getPacket();
    }

    public static byte[] getSlideMenu(int nSpeakerTypeID, int npcid, int type, int lasticon, String sel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(nSpeakerTypeID);
        mplew.writeInt(npcid);
        mplew.write(0);
        mplew.writeShort(19);
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(type);
        mplew.writeInt(type == 0 ? lasticon : 0);
        mplew.writeMapleAsciiString(sel);

        return mplew.getPacket();
    }


    public static byte[] getMapSelection(int npcid, byte msgType, String sel) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(0x04);
        mplew.writeInt(npcid);
        mplew.write(0); //V.112.1新增
        mplew.writeShort(msgType); //V.114修改 以前 0x11 现在 0x10
        mplew.writeShort(0); //V.119.1新增
        mplew.writeInt(npcid == 3000012 ? 5 : npcid == 9010000 ? 3 : npcid == 2083006 ? 1 : 0);
        mplew.writeInt(npcid == 9010022 ? 1 : 0);
        mplew.writeMapleAsciiString(sel);

        return mplew.getPacket();
    }

    public static byte[] getNPCTalkStyle(int npc, String talk, int styles[], int card, boolean android, boolean isSecond) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(0x04);
        mplew.writeInt(npc);
        mplew.write(0); //V.112.1新增
        mplew.writeShort(android ? 0x0B : 0x0A);  //V.114修改以前 android 0x0B 角色 0x0A
        mplew.writeShort(0); //V.119.1新增
        if (!android) {
            mplew.writeShort(isSecond ? 1 : 0); //V.114 修改 以前1个 0
        }
        mplew.writeMapleAsciiString(talk);
        mplew.write(styles.length);
        for (int style : styles) {
            mplew.writeInt(style);
        }
        mplew.write(0);
        mplew.writeInt(card);

        return mplew.getPacket();
    }

    public static byte[] getNPCTalkNum(int npc, byte msgType, String talk, int def, int min, int max) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(0x04);
        mplew.writeInt(npc);
        mplew.write(0); //V.112.1新增
        mplew.writeShort(msgType);
        mplew.writeShort(0);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);

        return mplew.getPacket();
    }

    public static byte[] getNPCTalkText(int npc, byte msgType, String talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(0x04);
        mplew.writeInt(npc);
        mplew.write(0); //V.112.1新增
        mplew.writeShort(msgType);
        mplew.writeShort(0); //V.119.1新增
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(0x00);
        mplew.writeInt(0x00);

        return mplew.getPacket();
    }

    public static byte[] getNPCTalkText(byte type, byte mode, int npcid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(MapleEnumClass.ScriptMessageType.ASKTEXT.getMsgType());
        mplew.writeInt(0);
        mplew.write(1);
        mplew.writeInt(npcid);
        mplew.writeShort(24);
        mplew.write(1);
        mplew.write(0);
        mplew.writeShort(1);
        mplew.writeLong(0);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    /**
     * 显示向导提示
     *
     * @param data
     * @return
     */
    public static byte[] getEvanTutorial(String data) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(0); //NpcID
        mplew.write(0);
        mplew.writeShort(2);
        mplew.writeShort(1);
        mplew.write(1);
        mplew.writeMapleAsciiString(data);

        return mplew.getPacket();
    }

    public static byte[] getAskMIXHair(int npc, String data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        a(mplew, (byte) 3, npc, (byte) NPCTalkOP.MIX_HAIR.getValue(), (byte) 0, (byte) 0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);
        mplew.writeShort(1);
        mplew.writeMapleAsciiString(data);
        int n3 = 8;
        mplew.write(n3);
        for (int i2 = 0; i2 < n3; ++i2) {
            mplew.writeInt(i2);
        }
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] getAskMIXHairNew(int n2, String string) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        a(mplew, (byte) 3, n2, (byte) NPCTalkOP.MIX_HAIR_NEW.getValue(), (byte) 0, (byte) 0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(2);
        mplew.writeMapleAsciiString(string);
        int n3 = 3;
        mplew.write(n3);
        if (n3 == 3) {
            mplew.writeInt(1);
            mplew.writeInt(1);
            mplew.writeInt(1);
        } else {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] getAskCustomMIXHair(int n2, String string) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        a(mplew, (byte) 3, n2, (byte) NPCTalkOP.MIXED_HAIR.getValue(), (byte) 0, (byte) 0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(string);

        return mplew.getPacket();
    }

    public static void a(MaplePacketLittleEndianWriter mplew, byte by2, int n2, byte by3, byte by4, byte by5) {
        a(mplew, by2, n2, false, 0, by3, by4, by5);
    }

    public static void a(MaplePacketLittleEndianWriter mplew, byte by2, int n2, boolean bl2, int n3, byte by3, byte by4, byte by5) {
        mplew.writeShort(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(by2);
        mplew.writeInt(n2);
        mplew.writeBool(bl2);
        if (bl2) {
            mplew.writeInt(n3);
        }
        mplew.write(by3);
        mplew.write(by4);
        mplew.write(by5);
    }

    /*
     * 打开1个商店
     */
    public static byte[] getNPCShop(int shopId, MapleShop shop, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
        mplew.write(0);
        PacketHelper.addShopInfo(mplew, shop, c, shopId);

        return mplew.getPacket();
    }

    /*
     * 商店操作提示
     */
    public static byte[] confirmShopTransaction(MapleShopResponse code, MapleShop shop, MapleClient c, int indexBought) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        mplew.write(code.getValue());
        switch (code) {
            case 购买道具完成: //购买道具 [9B 02] [00] [00 00] 购买回购栏里面的道具 [9B 02] [00] [01] [00 00 00 00]
            case 背包空间不够: //请确认是不是你的背包的空间不够。[9B 02] [04] [00 00]
                mplew.write(indexBought >= 0 ? 1 : 0); //是否回购栏的道具
                if (indexBought >= 0) {
                    mplew.writeShort(indexBought); //道具在回购栏的位置 默认从 0 开始
                    mplew.writeInt(0);
                } else {
                    mplew.write(0); //[9B 02] [00] [00] [01] [85 84 1E 00 = 2000005 物品ID] 达到购买上限
                }
                mplew.writeInt(0);
                break;
            case 卖出道具完成: //卖出道具
                PacketHelper.addShopInfo(mplew, shop, c, shop.getNpcId());
                break;
            case 充值飞镖完成: //充值飞镖和子弹 V.112修改 以前 0x0A
            case 充值金币不够: //充值飞镖和子弹提示金币不足 V.112修改 以前0x0C
                break;
            case 购买回购出错: //贩卖价格比购买价格高.无法购买。
                mplew.write(0);
                mplew.write(0);
                break;
            default:
                System.err.println("未知商店买卖操作: " + code);
                break;
        }

        return mplew.getPacket();
    }

    /*
     * 仓库取出
     */
    public static byte[] takeOutStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x09);
        mplew.write(slots);
        mplew.writeLong(type.getBitfieldEncoding());
        mplew.write(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item);
        }

        return mplew.getPacket();
    }

    /*
     * 取回道具
     * 0x0A = 请确认是不是你的背包空间不够。
     * 0x0B = 金币不足
     * 保存道具
     * 0x10 = 金币不足
     * 0x11 = 仓库已满
     */
    public static byte[] getStorageError(byte op) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(op);

        return mplew.getPacket();
    }

    /*
     * 仓库存入道具
     */
    public static byte[] storeStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x0D);
        mplew.write(slots);
        mplew.writeLong(type.getBitfieldEncoding());
        mplew.write(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item);
        }

        return mplew.getPacket();
    }

    /*
     * 仓库道具排序
     */
    public static byte[] arrangeStorage(byte slots, Collection<Item> items, boolean changed) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x0F);
        mplew.write(slots);
        mplew.writeLong(0x7C); //4 | 8 | 10 | 20 | 40
        /*
         * 排序仓库应该是
         * 装备一种
         * 消耗一种
         * 其他一种
         * 设置一种
         * 商城一种
         */
        mplew.write(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item);
        }
        mplew.writeInt(0x00);

        return mplew.getPacket();
    }

    /*
     * 仓库保存金币
     */
    public static byte[] mesoStorage(byte slots, long meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x13);
        mplew.write(slots);
        mplew.writeLong(0x02);
        mplew.writeLong(meso);

        return mplew.getPacket();
    }

    /*
     * 打开仓库
     */
    public static byte[] getStorage(int npcId, byte slots, Collection<Item> items, long meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x16);
        mplew.writeInt(npcId);
        mplew.write(slots);
        mplew.writeLong(0x7E);
        mplew.writeLong(meso);
        /*
         * 打开仓库应该是
         * 装备一种
         * 消耗一种
         * 其他一种
         * 设置一种
         * 商城一种
         */
        mplew.write(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item);
        }
        mplew.writeInt(0x00);

        return mplew.getPacket();
    }

    public static byte[] setNPCSpecialAction(int npcoid, String string) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_SPECIAL_ACTION.getValue());
        mplew.writeInt(npcoid);
        mplew.writeMapleAsciiString(string);
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] updateNPCSpecialAction(int npcoid, int value, int x, int y) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FORCE_MOVE_BY_SCRIPT.getValue());
        mplew.writeInt(npcoid);
        mplew.writeInt(value);
        mplew.writeInt(x);
        mplew.writeInt(y);

        return mplew.getPacket();
    }

    public static byte[] moveNpc(MaplePacketLittleEndianWriter mplew) {
        mplew.writeInt(0);
        mplew.writePos(new Point(0, 0));
        mplew.writeInt(0);
        PacketHelper.serializeMovementList(mplew, new ArrayList<>());
        return mplew.getPacket();
    }
}
