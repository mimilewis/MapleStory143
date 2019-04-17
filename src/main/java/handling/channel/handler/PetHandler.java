package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleDisease;
import client.inventory.*;
import client.skills.Skill;
import client.skills.SkillFactory;
import configs.ServerConfig;
import constants.GameConstants;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.maps.FieldLimitType;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import tools.packet.EffectPacket;
import tools.packet.PetPacket;

import java.awt.*;
import java.util.List;

public class PetHandler {

    /*
     * 召唤宠物
     */
    public static void SpawnPet(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        //[9A 00] [B8 19 35 01] [05] [00]
        chr.updateTick(slea.readInt());
        chr.spawnPet(slea.readByte(), slea.readByte() > 0);
    }

    /*
     * 宠物自动加BUFF
     */
    public static void Pet_AutoBuff(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int petid = slea.readInt();
        MaplePet pet = chr.getSpawnPet(petid);
        if (chr.getMap() == null || pet == null) {
            return;
        }
        int skillId = slea.readInt();
        Skill buffId = SkillFactory.getSkill(skillId);
        if (chr.getSkillLevel(buffId) > 0 || skillId == 0) {
            pet.setBuffSkill(skillId);
            chr.petUpdateStats(pet, true);
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    /*
     * 宠物自动喝药
     */
    public static void Pet_AutoPotion(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(1);
        if (chr == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        chr.updateTick(slea.readInt());
        short slot = slea.readShort();
        if (chr == null || !chr.isAlive() || chr.getMapId() == 749040100 || chr.getMap() == null || chr.hasDisease(MapleDisease.无法使用药水)) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != slea.readInt()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            chr.dropMessage(5, "暂时无法使用道具.");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }
        } else {
            c.announce(MaplePacketCreator.enableActions());
        }
    }

    public static void PetExcludeItems(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        /*
         * FF 00
         * 00 00 00 00
         * 01
         * 63 BF 0F 00
         */
        int petSlot = slea.readInt();
        MaplePet pet = chr.getSpawnPet(petSlot);
        if (pet == null || !PetFlag.PET_IGNORE_PICKUP.check(pet.getFlags())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        pet.clearExcluded(); //清除以前的过滤
        byte amount = slea.readByte(); //有多少个过滤的道具ID
        for (int i = 0; i < amount; i++) {
            pet.addExcluded(i, slea.readInt());
        }
    }

    /*
     * 宠物说话
     */
    public static void PetChat(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        /*
         * FB 00
         * 00 00 00 00
         * 40 62 BB 00
         * 01 13
         * 06 00 DF C6 DF C6 DF C6
         */
        if (slea.available() < 12) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        int petid = slea.readInt();
        slea.readInt();
        if (chr == null || chr.getMap() == null || chr.getSpawnPet(petid) == null) {
            return;
        }
        short act = slea.readShort();
        String text = slea.readMapleAsciiString();
        if (text.length() < 1) {
            //FileoutputUtil.log(FileoutputUtil.宠物说话, "玩家宠物说话为空 - 操作: " + act + " 宠物ID: " + chr.getSpawnPet(petid).getPetItemId(), true);
            return;
        }
        chr.getMap().broadcastMessage(chr, PetPacket.petChat(chr.getId(), act, text, (byte) petid), true);
    }

    /*
     * 使用宠物命令
     */
    public static void PetCommand(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        /*
         * FC 00
         * 00 00 00 00
         * 00
         * 0C
         */
        int petId = slea.readInt();
        MaplePet pet = null;
        pet = chr.getSpawnPet((byte) petId);
        slea.readByte(); //always 0?
        if (pet == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        byte command = slea.readByte();
        PetCommand petCommand = PetDataFactory.getPetCommand(pet.getPetItemId(), command);
        if (petCommand == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        byte petIndex = chr.getPetIndex(pet);
        boolean success = false;
        if (Randomizer.nextInt(99) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + (petCommand.getIncrease() * ServerConfig.CHANNEL_RATE_TRAIT);
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.announce(EffectPacket.showOwnPetLevelUp(petIndex));
                    chr.getMap().broadcastMessage(EffectPacket.showPetLevelUp(chr.getId(), petIndex));
                }
                chr.petUpdateStats(pet, true);
            }
        }
        chr.getMap().broadcastMessage(PetPacket.commandResponse(chr.getId(), (byte) petCommand.getCommand(), petIndex, success, false));
    }

    /*
     * 使用宠物食品
     */
    public static void PetFood(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        /*
         * 74 00
         * 04 3A 41 01
         * 06 00 - slot
         * 40 59 20 00 - itemId
         */
        if (chr == null || chr.getMap() == null) {
            return;
        }
        int previousFullness = 100;
        byte petslot = 0;
        MaplePet[] pets = chr.getSpawnPets();
        for (byte i = 0; i < 3; i++) {
            if (pets[i] != null && pets[i].getFullness() < previousFullness) {
                petslot = i;
                break;
            }
        }
        MaplePet pet = chr.getSpawnPet(petslot);
        chr.updateTick(slea.readInt());
        short slot = slea.readShort();
        int itemId = slea.readInt();
        Item petFood = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (pet == null || petFood == null || petFood.getItemId() != itemId || petFood.getQuantity() <= 0 || itemId / 10000 != 212) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        boolean gainCloseness = false;
        if (Randomizer.nextInt(101) > 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);
            byte index = chr.getPetIndex(pet);
            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + 1;
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.announce(EffectPacket.showOwnPetLevelUp(index));
                    chr.getMap().broadcastMessage(EffectPacket.showPetLevelUp(chr.getId(), index));
                }
            }
            chr.petUpdateStats(pet, true);
            chr.getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(chr.getId(), (byte) 1, index, true, true), true);
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - 1;
                if (newCloseness < 0) {
                    newCloseness = 0;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness < GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() - 1);
                }
                chr.dropMessage(5, "您的宠物的饥饿感是满值，如果继续使用将会有50%的几率减少1点亲密度。");
            }
            chr.petUpdateStats(pet, true);
            chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 1, chr.getPetIndex(pet), false, true), true);
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, true, false);
        c.announce(MaplePacketCreator.enableActions());
    }

    /*
     * 宠物移动
     */
    public static void MovePet(LittleEndianAccessor slea, MapleCharacter chr) {
        int petSlot = slea.readInt();
        slea.skip(1); //[01] V.103 新增
        slea.skip(4); //[00 00 00 00] V.112 新增
        Point startPos = slea.readPos(); //开始的坐标
        slea.skip(4); //未知
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);
        if (res != null && chr != null && !res.isEmpty() && chr.getMap() != null) { // map crash hack
//            if (slea.available() != 8) {
//                System.out.println("slea.available != 8 (宠物移动出错) 剩余封包长度: " + slea.available());
//                FileoutputUtil.log(FileoutputUtil.Movement_Log, "slea.available != 8 (宠物移动出错) 封包: " + slea.toString(true));
//                return;
//            }
            MaplePet pet = chr.getSpawnPet(petSlot);
            if (pet == null) {
                return;
            }
            chr.getSpawnPet(chr.getPetIndex(pet)).updatePosition(res);
            chr.getMap().broadcastMessage(chr, PetPacket.movePet(chr.getId(), petSlot, startPos, res), false);
        }
    }

    public static void AllowPetLoot(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        slea.skip(4);
        int data = slea.readShort();
        if (data > 0) {
            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.ALLOW_PET_LOOT)).setCustomData(String.valueOf(data));
        } else {
            chr.getQuestRemove(MapleQuest.getInstance(GameConstants.ALLOW_PET_LOOT));
        }
        MaplePet[] pet = c.getPlayer().getSpawnPets();
        for (int i = 0; i < 3; i++) {
            if (pet[i] != null && pet[i].getSummoned()) {
                pet[i].setCanPickup(data > 0);
                chr.petUpdateStats(pet[i], true);
            }
        }
        c.announce(PetPacket.showPetPickUpMsg(data > 0, 1));
    }

    public static void AllowPetAutoEat(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        slea.skip(4); //
        slea.skip(4); // [00 08 00 00] 宠物是否有这个状态
        boolean data = slea.readByte() > 0;
        chr.updateInfoQuest(GameConstants.宠物自动喂食, data ? "autoEat=1" : "autoEat=0");
        c.announce(PetPacket.showPetAutoEatMsg());
    }
}
