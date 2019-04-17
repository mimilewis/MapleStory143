package tools.packet;

import client.MapleCharacter;
import client.MonsterFamiliar;
import handling.opcode.SendPacketOpcode;
import server.movement.LifeMovementFragment;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FamiliarPacket {

    private static final int mask = 0x74000000;

    public static byte[] showFamiliar(int cid, Map<Integer, MonsterFamiliar> familiars) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        mplew.writeInt(cid);
        mplew.write(FamiliarOP.加载数据.ordinal());
        mplew.write(1);
        mplew.writeShort(0);
        mplew.write(0x74);
        mplew.writeShort(3);
        mplew.write(2);
        mplew.writeInt(familiars.size());
        mplew.writeInt(familiars.size());
        int i = 0;
        familiars.forEach((integer, monsterFamiliar) -> {
            mplew.write(i * 2);
            mplew.writeLong(monsterFamiliar.getObjectId());
            mplew.write(0x02);
            mplew.write(0x50);
            mplew.write(0x20);
            mplew.write(0x64);
            mplew.writeInt(monsterFamiliar.getFamiliar()); //9
            mplew.writeZeroBytes(14);
            mplew.write(-2);
            mplew.writeShort(1);
            mplew.writeShort(0); // skillid 牧羊冲击
            mplew.writeShort(131);
            mplew.writeInt(0);
            mplew.writeShort(1);
            mplew.writeShort(0); //潜能1
            mplew.writeShort(0); //潜能2
            mplew.writeShort(0); //潜能3
            mplew.writeShort(0);
            mplew.writeHexString("20 64 8C FE 66 15");
        });


        return mplew.getPacket();
    }

    public static byte[] addFamiliarCard(MapleCharacter player, MonsterFamiliar mf) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        mplew.writeInt(player.getId());
        treeMap.put(2, 1);
        writeFamiliarData(mplew, mf, player, 3, mask | 1, treeMap, true);

        return mplew.getPacket();
    }

    public static byte[] updateFamiliarCard(MapleCharacter player, Map<Integer, Integer> mfs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        mplew.writeInt(player.getId());
        mplew.write(2);
        mplew.writeInt(mask | 1);
        mplew.write(2);
        mplew.write(mfs.size() * 2);
        for (Integer integer : mfs.values()) {
            mplew.writeShort(integer);
        }

        return mplew.getPacket();
    }

    public static byte[] showAllFamiliar(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        Map<Integer, Integer> hashMap = new HashMap<>();
        mplew.writeInt(player.getId());
        hashMap.put(2, player.getFamiliars().size());
        writeFamiliarData(mplew, null, player, 3, mask | 1, hashMap, false);

        return mplew.getPacket();
    }

    public static byte[] bl(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        mplew.writeInt(player.getId());
        mplew.write(2);
        mplew.writeInt(mask | 1);
        mplew.write(3);

        return mplew.getPacket();
    }

    public static byte[] showFamiliarCard(MapleCharacter player, Map<Integer, Integer> cards) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        mplew.writeInt(player.getId());
        writeFamiliarData(mplew, null, player, 2, mask | 1, cards, false);
        writeFamiliarData(mplew, null, player, 4, mask, cards, false);

        return mplew.getPacket();
    }

    public static byte[] H(MapleCharacter player, int n2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        mplew.writeInt(player.getId());
        treeMap.put(4, n2);
        writeFamiliarData(mplew, null, player, 3, mask | 1, treeMap, false);

        return mplew.getPacket();
    }

    public static byte[] attackFamiliar(int cid, int oid, Map<Integer, Integer> map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        mplew.writeInt(cid);
        mplew.write(2);
        mplew.writeInt(oid);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.write(map.size() * 2);
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            mplew.writeInt(entry.getKey());
            mplew.write(2);
            mplew.writeInt(entry.getValue());
        }

        return mplew.getPacket();
    }

    public static byte[] moveFamiliar(int n2, Point point, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        MaplePacketLittleEndianWriter mplew2 = new MaplePacketLittleEndianWriter();
        PacketHelper.serializeMovementList(mplew2, moves);
        byte[] arrby = mplew2.getPacket();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());
        mplew.writeInt(n2);
        mplew.write(2);
        mplew.writeInt(1946157058);
        mplew.write(0);
        int n3 = arrby.length + 13;
        if (n3 < 64) {
            mplew.write(n3 * 2);
        } else {
            int n4 = n3 * 4;
            do {
                boolean bl2 = n4 + ((byte) (n4 & 255)) == n3 * 4;
                if (bl2) {
                    if (n3 % 64 != 0) break;
                    n4 |= 128;
                    break;
                }
                ++n4;
            } while (true);
            mplew.writeShort(n4);
        }
        mplew.writeInt(0);
        mplew.writePos(point);
        mplew.writeInt(0);
        mplew.write(arrby);
        mplew.write(0);
        return mplew.getPacket();
    }


    public static byte[] spawnFamiliar(MonsterFamiliar familiar, boolean bl2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());

        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        mplew.writeInt(familiar.getCharacterid());
        treeMap.put(0, mask | 2);
        writeFamiliarData(mplew, familiar, null, 0, mask | 2, null, true);
        writeFamiliarData(mplew, familiar, null, 3, mask | 1, treeMap, false);


        return mplew.getPacket();
    }

    public static byte[] removeFamiliar(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());
        mplew.writeInt(cid);
        treeMap.put(0, 0);
        writeFamiliarData(mplew, null, null, 1, mask | 2, null, true);
        writeFamiliarData(mplew, null, null, 3, mask | 1, treeMap, false);
        return mplew.getPacket();
    }

    public static byte[] updateFamiliar(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        mplew.writeShort(SendPacketOpcode.FAMILIAR_OPERATION.getValue());
        mplew.writeInt(player.getId());
        int n2 = mask;
        if (player.getSummonedFamiliar() != null) {
            treeMap.put(0, n2 |= 2);
            writeFamiliarData(mplew, player.getSummonedFamiliar(), player, 0, n2, null, true);
        }
        if ((n2 & 2) != 0) {
            n2 -= 2;
        }
        treeMap.put(2, player.getFamiliars().size());
        treeMap.put(4, 0);
        treeMap.put(6, 3);
        writeFamiliarData(mplew, null, player, 3, n2 | 1, treeMap, false);
        return mplew.getPacket();
    }

    public static void writeFamiliarData(MaplePacketLittleEndianWriter mplew, MonsterFamiliar mf, MapleCharacter player, int mode, int unk_mask, Map<Integer, Integer> map, boolean unk) {
        mplew.write(mode);
        switch (mode) {
            case 0: {
                mplew.writeHexString("24 31 7B 25");
                mplew.writeInt(unk_mask);
                mf.writePacket(mplew, unk);
                mplew.write(0);
                mplew.write(0);
                mplew.writeInt(2000);
                mplew.writeInt(2000);
                break;
            }
            case 1: {
                mplew.writeInt(unk_mask);
                break;
            }
            case 2: {
                mplew.writeInt(unk_mask);
                if ((unk_mask & 1) == 1) break;
                mplew.write(0);
                break;
            }
            case 3: {
                mplew.writeInt(unk_mask);
                mplew.writeShort(map.size());
                for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                    writeFamiliarCard(mplew, entry.getKey(), entry.getValue(), player, mf);
                }
                break;
            }
            case 4: {
                mplew.write(map.size() * 2);
                for (Map.Entry entry : map.entrySet()) {
                    mplew.writeInt((Integer) entry.getKey());
                    mplew.write((Integer) entry.getValue());
                    mplew.writeInt(0);
                }
                break;
            }
        }
    }

    public static void writeFamiliarCard(MaplePacketLittleEndianWriter mplew, int mode, int n3, MapleCharacter player, MonsterFamiliar mf) {
        mplew.write(mode);
        switch (mode) {
            case 0: {
                if ((n3 & 1) != 0) {
                    --n3;
                }
                mplew.writeInt(n3 | 2);
                break;
            }
            case 2: {
                mplew.writeInt(player.getFamiliars().size());
                if (player.getFamiliars().size() <= 0) break;
                mplew.writeInt(n3);
                if (n3 != 1 || player.getFamiliars().size() == n3) {
                    int n4 = 0;
                    for (Map.Entry<Integer, MonsterFamiliar> entry : player.getFamiliars().entrySet()) {
                        MonsterFamiliar value = entry.getValue();
                        value.setIndex(n4);
                        mplew.write(n4 * 2);
                        value.writePacket(mplew, true);
                        ++n4;
                    }
                    break;
                }
                mplew.write(mf.getIndex() * 2);
                mf.writePacket(mplew, true);
                break;
            }
            case 4: {
                mplew.writeShort(n3);
                break;
            }
            case 6: {
                mplew.writeInt(3);
                mplew.writeInt(n3);
                if (n3 == 3) {
                    for (int i2 = 0; i2 < 3; ++i2) {
                        mplew.write(i2);
                        mplew.writeShort(0);
                    }
                    break;
                }
                if (n3 != 1) break;
                mplew.write(0);
                mplew.writeShort(0);
            }
        }
    }

    public static byte[] writeWarpToMap(MapleCharacter player, boolean bl2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        int n2 = player.getSummonedFamiliar() != null ? 1 : 0;
        int n3 = 2 | n2;
        mplew.writeInt(n3);
        if (player.getSummonedFamiliar() != null) {
            mplew.writeInt(0x257B3124);
            mplew.writeInt(mask | 2);
            mplew.writeInt(0x44);
            player.getSummonedFamiliar().writePacket(mplew, true);
            mplew.writeHexString("BD 01 C6 03");
            mplew.writeInt(2000);
            mplew.writeInt(2000);
        }
        mplew.writeInt(1392187010);
        mplew.writeInt(mask | 1);
        mplew.writeInt(!bl2 && player.getFamiliars().size() > 0 ? player.getFamiliars().size() * 56 + (player.getFamiliars().size() > 0 ? 14 : 8) : 8);
        n3 = !bl2 && n2 > 0 ? mask | 2 : 0;
        mplew.writeInt(n3);
        if (!bl2 && player.getFamiliars().size() > 0) {
            mplew.write(player.getFamiliars().size() * 2);
            for (MonsterFamiliar ai2 : player.getFamiliars().values()) {
                ai2.writePacket(mplew, false);
            }
            mplew.writeShort(2);
            mplew.write(6);
            mplew.writeShort(0);
        }
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.writeInt(1980835063);
        mplew.writeInt(mask);
        mplew.writeInt(8);
        mplew.writeInt(mask | 1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public enum FamiliarOP {
        召唤怪怪,
        移除怪怪,
        显示卡牌,
        加载数据,
    }
//
//
//    public static byte[] removeFamiliar(int cid) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.SPAWN_FAMILIAR.getValue());
//        mplew.writeInt(cid);
//        mplew.writeShort(0);
//        mplew.write(0);
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] spawnFamiliar(MonsterFamiliar mf, boolean spawn, boolean respawn) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(respawn ? SendPacketOpcode.SPAWN_FAMILIAR_2.getValue() : SendPacketOpcode.SPAWN_FAMILIAR.getValue());
//        mplew.writeInt(mf.getCharacterId());
//        mplew.write(spawn ? 1 : 0);
//        mplew.write(respawn ? 1 : 0);
//        mplew.write(0);
//        if (spawn) {
//            mplew.writeInt(mf.getFamiliar());
//            mplew.writeInt(mf.getFatigue());
//            mplew.writeInt(mf.getVitality() * 300); // max fatigue
//            mplew.writeMapleAsciiString(mf.getName());
//            mplew.writePos(mf.getTruePosition());
//            mplew.write(mf.getStance());
//            mplew.writeShort(mf.getFh());
//        }
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] moveFamiliar(int cid, Point startPos, java.util.List<LifeMovementFragment> moves) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.MOVE_FAMILIAR.getValue());
//        mplew.writeInt(cid);
//        mplew.write(0);
//        mplew.writePos(startPos);
//        mplew.writeInt(0);
//        PacketHelper.serializeMovementList(mplew, moves);
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] touchFamiliar(int cid, byte unk, int objectid, int type, int delay, int damage) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.TOUCH_FAMILIAR.getValue());
//        mplew.writeInt(cid);
//        mplew.write(0);
//        mplew.write(unk);
//        mplew.writeInt(objectid);
//        mplew.writeInt(type);
//        mplew.writeInt(delay);
//        mplew.writeInt(damage);
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] familiarAttack(int cid, byte unk, java.util.List<Triple<Integer, Integer, java.util.List<Integer>>> attackPair) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.ATTACK_FAMILIAR.getValue());
//        mplew.writeInt(cid);
//        mplew.write(0);// familiar id?
//        mplew.write(unk);
//        mplew.write(attackPair.size());
//        for (Triple<Integer, Integer, java.util.List<Integer>> s : attackPair) {
//            mplew.writeInt(s.left);
//            mplew.write(s.mid);
//            mplew.write(s.right.size());
//            for (int damage : s.right) {
//                mplew.writeInt(damage);
//            }
//        }
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] renameFamiliar(MonsterFamiliar mf) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.RENAME_FAMILIAR.getValue());
//        mplew.writeInt(mf.getCharacterId());
//        mplew.write(0);
//        mplew.writeInt(mf.getFamiliar());
//        mplew.writeMapleAsciiString(mf.getName());
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] updateFamiliar(MonsterFamiliar mf) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.UPDATE_FAMILIAR.getValue());
//        mplew.writeInt(mf.getCharacterId());
//        mplew.writeInt(mf.getFamiliar());
//        mplew.writeInt(mf.getFatigue());
//        mplew.writeLong(PacketHelper.getTime(mf.getVitality() >= 3 ? System.currentTimeMillis() : -2L));
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] registerFamiliar(MonsterFamiliar mf) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.REGISTER_FAMILIAR.getValue());
//        mplew.writeLong(mf.getId());
//        mf.writePacket(mplew, false);
//        mplew.write(mf.getVitality() >= 3 ? 1 : 0);
//        mplew.write(0);
//
//        return mplew.getPacket();
//    }
}
