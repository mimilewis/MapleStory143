/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools.packet;

import client.MapleCharacter;
import handling.opcode.SendPacketOpcode;
import server.carnival.MapleCarnivalParty;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;


public class MonsterCarnivalPacket {

    public static byte[] startMonsterCarnival(final MapleCharacter chr, final int enemyavailable, final int enemytotal) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_START.getValue());
        final MapleCarnivalParty friendly = chr.getCarnivalParty();
        mplew.write(friendly.getTeam());
        mplew.writeInt(chr.getAvailableCP());
        mplew.writeInt(chr.getTotalCP());
        mplew.writeInt(friendly.getAvailableCP()); // ??
        mplew.writeInt(friendly.getTotalCP()); // ??
        mplew.write(0); // ??

        return mplew.getPacket();
    }

    public static byte[] playerDiedMessage(String name, int lostCP, int team) { //CPQ
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_DIED.getValue());
        mplew.write(team); //team
        mplew.writeMapleAsciiString(name);
        mplew.write(lostCP);

        return mplew.getPacket();
    }

    public static byte[] playerLeaveMessage(boolean leader, String name, int team) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_LEAVE.getValue());
        mplew.write(leader ? 7 : 0);
        mplew.write(team); // 0: red, 1: blue
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] CPUpdate(boolean party, int curCP, int totalCP, int team) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        mplew.writeInt(curCP);
        mplew.writeInt(totalCP);

        return mplew.getPacket();
    }

    public static byte[] showMCStats(int left, int right) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_STATS.getValue());
        mplew.writeInt(left);
        mplew.writeInt(right);

        return mplew.getPacket();
    }

    public static byte[] playerSummoned(String name, int tab, int number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        mplew.write(tab);
        mplew.write(number);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] showMCResult(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_RESULT.getValue());
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] showMCRanking(List<MapleCharacter> players) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_RANKING.getValue());
        mplew.writeShort(players.size());
        for (MapleCharacter i : players) {
            mplew.writeInt(i.getId());
            mplew.writeMapleAsciiString(i.getName());
            mplew.writeInt(10); // points
            mplew.write(0); // team
        }

        return mplew.getPacket();
    }

    public static byte[] startCPQ(byte team, int usedcp, int totalcp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_START.getValue());
        mplew.write(0); //team
        mplew.writeShort(0); //Obtained CP - Used CP
        mplew.writeShort(0); //Total Obtained CP
        mplew.writeShort(0); //Obtained CP - Used CP of the team
        mplew.writeShort(0); //Total Obtained CP of the team
        mplew.writeShort(0); //Obtained CP - Used CP of the team
        mplew.writeShort(0); //Total Obtained CP of the team
        mplew.writeShort(0); //Probably useless nexon shit
        mplew.writeLong(0); //Probably useless nexon shit
        return mplew.getPacket();
    }

    public static byte[] obtainCP() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
        mplew.writeShort(0); //Obtained CP - Used CP
        mplew.writeShort(0); //Total Obtained CP
        return mplew.getPacket();
    }

    public static byte[] obtainPartyCP() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
        mplew.write(0); //Team where the points are given to.
        mplew.writeShort(0); //Obtained CP - Used CP
        mplew.writeShort(0); //Total Obtained CP
        return mplew.getPacket();
    }

    public static byte[] CPQSummon() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        mplew.write(0); //Tab
        mplew.write(0); //Number of summon inside the tab
        mplew.writeMapleAsciiString(""); //Name of the player that summons
        return mplew.getPacket();
    }

    public static byte[] CPQDied() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
        mplew.write(0); //Team
        mplew.writeMapleAsciiString(""); //Name of the player that died
        mplew.write(0); //Lost CP
        return mplew.getPacket();
    }

    /**
     * Sends a CPQ Message
     * <p>
     * Possible values for <code>message</code>:<br>
     * 1: You don't have enough CP to continue. 2: You can no longer summon the
     * Monster. 3: You can no longer summon the being. 4: This being is already
     * summoned. 5: This request has failed due to an unknown error.
     *
     * @param message Displays a message inside Carnival PQ
     * @return
     */
    public static byte[] CPQMessage(byte message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_MESSAGE.getValue());
        mplew.write(message); //Message
        return mplew.getPacket();
    }

    public static byte[] leaveCPQ() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MONSTER_CARNIVAL_LEAVE.getValue());
        mplew.write(0); // Something?
        mplew.write(0); // Team
        mplew.writeMapleAsciiString(""); //Player name
        return mplew.getPacket();
    }
}
