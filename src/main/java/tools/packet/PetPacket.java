package tools.packet;

import client.MapleCharacter;
import client.MapleStat;
import client.inventory.MaplePet;
import handling.opcode.EffectOpcode;
import handling.opcode.SendPacketOpcode;
import server.movement.LifeMovementFragment;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.List;

public class PetPacket {

    public static byte[] showPetPickUpMsg(boolean canPickup, int pets) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_PICKUP_MSG.getValue());
        mplew.write(canPickup ? 1 : 0);
        mplew.write(pets);

        return mplew.getPacket();
    }

    public static byte[] showPetAutoEatMsg() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_AUTO_EAT_MSG.getValue());

        return mplew.getPacket();
    }

    public static byte[] showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger) {
        return showPet(chr, pet, remove, hunger, false);
    }

    public static byte[] showPet(MapleCharacter chr, MaplePet pet, boolean remove, boolean hunger, boolean show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(show ? SendPacketOpcode.SHOW_PET.getValue() : SendPacketOpcode.SPAWN_PET.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getPetIndex(pet));
        mplew.write(remove ? 0 : 1);
        /*
         * 0 = 手动召回
         * 1 = 宠物饥饿度为0自动回去
         * 2 = 宠物时间到期
         */
        mplew.write(hunger ? 1 : 0);
        if (!remove) {
            addPetInfo(mplew, chr, pet, false);
        }

        return mplew.getPacket();
    }

    public static void addPetInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, MaplePet pet, boolean showpet) {
        if (showpet) {
            mplew.write(1);
            mplew.writeInt(chr.getPetIndex(pet));
        }
        mplew.writeInt(pet.getPetItemId());  //宠物ID
        mplew.writeMapleAsciiString(pet.getName()); //宠物名字
        mplew.writeLong(pet.getUniqueId()); //宠物的SQL唯一ID
        mplew.writePos(pet.getPos()); //宠物的坐标
        mplew.write(pet.getStance()); //姿势
        mplew.writeShort(pet.getFh());
        mplew.writeInt(-1); //T071新增
        mplew.writeInt(0x64); //V.109新增 未知
    }

    public static byte[] movePet(int chrId, int slot, Point startPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_PET.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(slot);
        mplew.writeInt(0); //V.112新增
        mplew.writePos(startPos);
        mplew.writeInt(0);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static byte[] petChat(int chaId, short act, String text, byte slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_CHAT.getValue());
        mplew.writeInt(chaId);
        mplew.writeInt(slot);
        mplew.writeShort(act);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    public static byte[] commandResponse(int chrId, byte command, byte slot, boolean success, boolean food) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_COMMAND.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(slot);
        mplew.write(food ? 2 : 1);
        mplew.write(command);
        if (food) {
            mplew.writeInt(0); //T071修改为 Int
        } else {
            mplew.writeShort(success ? 1 : 0);  //T071修改为 byte
        }
        return mplew.getPacket();
    }

    public static byte[] showPetLevelUp(MapleCharacter chr, byte index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(EffectOpcode.UserEffect_Pet.getValue());
        mplew.write(0);
        mplew.writeInt(index);

        return mplew.getPacket();
    }

    public static byte[] loadExceptionList(MapleCharacter chr, MaplePet pet) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_EXCEPTION_LIST.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getPetIndex(pet));
        mplew.writeLong(pet.getUniqueId());
        List<Integer> excluded = pet.getExcluded();
        mplew.write(excluded.size());
        for (Integer anExcluded : excluded) {
            mplew.writeInt(anExcluded);
        }

        return mplew.getPacket();
    }

    public static byte[] petStatUpdate(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        mplew.write(0);
        mplew.writeLong(MapleStat.宠物.getValue());
        MaplePet[] pets = chr.getSpawnPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null) {
                mplew.writeLong(pets[i].getUniqueId());
            } else {
                mplew.writeLong(0);
            }
        }
        mplew.write(0);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] changePetColor(MapleCharacter player, MaplePet pet) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_COLOR_CHANGE.getValue());
        mplew.writeInt(player.getId());
        mplew.writeInt(player.getPetIndex(pet));
        mplew.writeInt(pet.getColor());

        return mplew.getPacket();
    }
}
