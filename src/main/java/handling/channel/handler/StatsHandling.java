package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.PlayerStats;
import client.skills.Skill;
import client.skills.SkillEntry;
import client.skills.SkillFactory;
import configs.ServerConfig;
import constants.GameConstants;
import constants.JobConstants;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class StatsHandling {

    public static void DistributeAP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        List<Pair<MapleStat, Long>> statupdate = new ArrayList<>(2);
        c.announce(MaplePacketCreator.updatePlayerStats(statupdate, true, chr));
        chr.updateTick(slea.readInt());

        PlayerStats stat = chr.getStat();
        int job = chr.getJob();
        int statLimit = ServerConfig.CHANNEL_PLAYER_MAXAP;
        if (chr.getRemainingAp() > 0) {
            switch (slea.readInt()) {
                case 64: // 力量
                    if (stat.getStr() >= statLimit) {
                        return;
                    }
                    stat.setStr((short) (stat.getStr() + 1), chr);
                    statupdate.add(new Pair<>(MapleStat.力量, (long) stat.getStr()));
                    break;
                case 128: // 敏捷
                    if (stat.getDex() >= statLimit) {
                        return;
                    }
                    stat.setDex((short) (stat.getDex() + 1), chr);
                    statupdate.add(new Pair<>(MapleStat.敏捷, (long) stat.getDex()));
                    break;
                case 256: // 智力
                    if (stat.getInt() >= statLimit) {
                        return;
                    }
                    stat.setInt((short) (stat.getInt() + 1), chr);
                    statupdate.add(new Pair<>(MapleStat.智力, (long) stat.getInt()));
                    break;
                case 512: // 运气
                    if (stat.getLuk() >= statLimit) {
                        return;
                    }
                    stat.setLuk((short) (stat.getLuk() + 1), chr);
                    statupdate.add(new Pair<>(MapleStat.运气, (long) stat.getLuk()));
                    break;
                case 2048: // HP
                    int maxhp = stat.getMaxHp();
                    if (chr.getHpApUsed() >= 10000 || maxhp >= chr.getMaxHpForSever()) {
                        return;
                    }
                    if (JobConstants.is新手职业(job)) { // Beginner
                        maxhp += Randomizer.rand(8, 12);
                    } else if (JobConstants.is恶魔复仇者(job)) {
                        maxhp += 30;
                    } else if ((job >= 100 && job <= 132) || (job >= 3200 && job <= 3212) || (job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112) || (job >= 5100 && job <= 5112)) { // 战士
                        maxhp += Randomizer.rand(36, 42);
                    } else if ((job >= 200 && job <= 232) || (JobConstants.is龙神(job)) || (job >= 2700 && job <= 2712)) { // 法师
                        maxhp += Randomizer.rand(10, 20);
                    } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312) || (job >= 2400 && job <= 2412)) { // Bowman
                        maxhp += Randomizer.rand(16, 20);
                    } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) {
                        maxhp += Randomizer.rand(28, 32);
                    } else if ((job >= 500 && job <= 532) || (JobConstants.is龙的传人(job)) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
                        maxhp += Randomizer.rand(18, 22);
                    } else if (job >= 1200 && job <= 1212) { // Flame Wizard
                        maxhp += Randomizer.rand(15, 21);
                    } else if ((job >= 2000 && job <= 2112) || (job >= 11200 && job <= 11212)) { // 龙神和林之灵
                        maxhp += Randomizer.rand(38, 42);
                    } else if (job >= 10100 && job <= 10112) { // 神之子
                        maxhp += Randomizer.rand(48, 52);
                    } else { // GameMaster
                        maxhp += Randomizer.rand(18, 26);
                    }
                    maxhp = Math.min(chr.getMaxHpForSever(), Math.abs(maxhp));
                    chr.setHpApUsed((short) (chr.getHpApUsed() + 1));
                    stat.setMaxHp(maxhp, chr);
                    statupdate.add(new Pair<>(MapleStat.MAXHP, (long) maxhp));
                    break;
                case 8192: // MP
                    int maxmp = stat.getMaxMp();
                    if (chr.getHpApUsed() >= 10000 || stat.getMaxMp() >= chr.getMaxMpForSever()) {
                        return;
                    }
                    if (JobConstants.is新手职业(job)) { // 新手
                        maxmp += Randomizer.rand(6, 8);
                    } else if (JobConstants.isNotMpJob(job)) {  //恶魔和天使不能洗
                        return;
                    } else if ((job >= 200 && job <= 232) || (JobConstants.is龙神(job)) || (job >= 3200 && job <= 3212) || (job >= 1200 && job <= 1212) || (job >= 2700 && job <= 2712)) { // Magician
                        maxmp += Randomizer.rand(38, 40);
                    } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 500 && job <= 532) || (JobConstants.is龙的传人(job)) || (job >= 3200 && job <= 3212) || (job >= 3500 && job <= 3512) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 2300 && job <= 2312) || (job >= 2400 && job <= 2412)) { // Bowman
                        maxmp += Randomizer.rand(10, 12);
                    } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112) || (job >= 2000 && job <= 2112) || (job >= 5100 && job <= 5112)) { // Soul Master
                        maxmp += Randomizer.rand(6, 9);
                    } else { // GameMaster
                        maxmp += Randomizer.rand(6, 12);
                    }
                    maxmp = Math.min(chr.getMaxMpForSever(), Math.abs(maxmp));
                    chr.setHpApUsed((short) (chr.getHpApUsed() + 1));
                    stat.setMaxMp(maxmp, chr);
                    statupdate.add(new Pair<>(MapleStat.MAXMP, (long) maxmp));
                    break;
                default:
                    c.announce(MaplePacketCreator.enableActions());
                    return;
            }
            chr.setRemainingAp((short) (chr.getRemainingAp() - 1));
            statupdate.add(new Pair<>(MapleStat.AVAILABLEAP, (long) chr.getRemainingAp()));
            c.announce(MaplePacketCreator.updatePlayerStats(statupdate, true, chr));
        }
    }

    public static void DistributeSP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        // DE 00 A8 68 C9 09 EA 03 00 00 00 00 00 00 01 00 00 00
        c.getPlayer().updateTick(slea.readInt());
        int skillid = slea.readInt();
        byte amount = slea.available() > 0 ? slea.readByte() : 1;
        if (chr.isAdmin()) {
            chr.dropMessage(5, "开始加技能点 - 技能ID: " + skillid + " 等级: " + amount);
        }
        boolean isBeginnerSkill = false;
        int remainingSp;
        if (JobConstants.is新手职业(skillid / 10000)) {
            boolean resistance = skillid / 10000 == 3000 || skillid / 10000 == 3001;
            int snailsLevel = chr.getSkillLevel(SkillFactory.getSkill(((skillid / 10000) * 10000) + 1000));
            int recoveryLevel = chr.getSkillLevel(SkillFactory.getSkill(((skillid / 10000) * 10000) + 1001));
            int nimbleFeetLevel = chr.getSkillLevel(SkillFactory.getSkill(((skillid / 10000) * 10000) + (resistance ? 2 : 1002)));
            remainingSp = Math.min((chr.getLevel() - 1), resistance ? 9 : 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
            isBeginnerSkill = true;
        } else if (JobConstants.is新手职业(skillid / 10000)) {
            if (chr.isAdmin()) {
                chr.dropMessage(5, "加技能点错误 - 1");
            }
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else {
            if (JobConstants.is暗影双刀(chr.getJob())) {
                int skillbook = JobConstants.getSkillBookBySkill(skillid);
                if (skillbook == 0 || skillbook == 1) {
                    remainingSp = chr.getRemainingSp(0) + chr.getRemainingSp(1);
                } else if (skillbook == 2 || skillbook == 3) {
                    remainingSp = chr.getRemainingSp(2) + chr.getRemainingSp(3);
                } else {
                    remainingSp = chr.getRemainingSp(JobConstants.getSkillBookBySkill(skillid));
                }
            } else {
                remainingSp = chr.getRemainingSp(JobConstants.getSkillBookBySkill(skillid));
            }
        }
        Skill skill = SkillFactory.getSkill(skillid);
        if (skill == null) {
            if (chr.isAdmin()) {
                chr.dropMessage(5, "加技能点错误 - 技能为空 当前技能ID: " + skillid);
            }
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        for (Pair<String, Byte> ski : skill.getRequiredSkills()) {
            if (ski.left.equalsIgnoreCase("level")) { //需要的等级
                if (chr.getLevel() < ski.right) {
                    if (chr.isAdmin()) {
                        chr.dropMessage(5, "加技能点错误 - 技能要求等级: " + ski.right + " 当前角色等级: " + chr.getLevel());
                    }
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
            } else if (ski.left.equalsIgnoreCase("reqAmount")) { //需要投入的技能点数
                int reqAmount = chr.getBeastTamerSkillLevels(skillid);
                if (reqAmount < ski.right) {
                    if (chr.isAdmin()) {
                        chr.dropMessage(5, "加技能点错误 - 技能要求投入点数: " + ski.right + " 当前投入点数: " + reqAmount);
                    }
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
            } else { //需要前置技能的等级
                int left = Integer.parseInt(ski.left);
                if (chr.getSkillLevel(SkillFactory.getSkill(left)) < ski.right) {
                    if (chr.isAdmin()) {
                        chr.dropMessage(5, "加技能点错误 - 前置技能: " + left + " - " + SkillFactory.getSkillName(left) + " 的技能等级不足.");
                    }
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
            }
        }
        int maxlevel = skill.isFourthJob() ? chr.getMasterLevel(skill) : skill.getMaxLevel(); //技能的最大等级
        int curLevel = chr.getSkillLevel(skill); //当前技能的等级

        if (skill.isInvisible() && chr.getSkillLevel(skill) == 0) {
            if ((skill.isFourthJob() && chr.getMasterLevel(skill) == 0) || (!skill.isFourthJob() && maxlevel < 10 && !isBeginnerSkill && chr.getMasterLevel(skill) <= 0 && !JobConstants.is暗影双刀(chr.getJob()))) {
                if (chr.isAdmin()) {
                    chr.dropMessage(5, "加技能点错误 - 3 检测 -> isFourthJob : " + skill.isFourthJob() + " getMasterLevel: " + chr.getMasterLevel(skill) + " 当前技能最大等级: " + maxlevel);
                }
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        }
        for (int i : GameConstants.blockedSkills) {
            if (skill.getId() == i) {
                chr.dropMessage(1, "这个技能未修复，暂时无法加点.");
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (chr.isAdmin()) {
            chr.dropMessage(5, "开始加技能点 - 当前Sp: " + remainingSp + " 当前技能等级: " + curLevel + " 该技能最大等级: " + maxlevel + " 所加的等级: " + amount + " 是否为该职业技能: " + skill.canBeLearnedBy(chr.getJob()));
        }
        if ((remainingSp >= amount && curLevel + amount <= maxlevel) && skill.canBeLearnedBy(chr.getJob())) {
            if (!isBeginnerSkill) {
                int skillbook = JobConstants.getSkillBookBySkill(skillid);
                if (JobConstants.is暗影双刀(chr.getJob()) && skillbook < 4) {
                    int tempsp = amount;
                    skillbook = skillbook == 1 ? 0 : skillbook == 3 ? 2 : skillbook;
                    for (int i = skillbook; i < skillbook + 2; i++) {
                        if (chr.getRemainingSp(i) < tempsp) {
                            tempsp -= chr.getRemainingSp(i);
                            chr.setRemainingSp(0, i);
                        } else {
                            chr.setRemainingSp(chr.getRemainingSp(i) - tempsp, i);
                            break;
                        }
                    }
                } else {
                    chr.setRemainingSp(chr.getRemainingSp(skillbook) - amount, skillbook);
                }
            }
            chr.updateSingleStat(MapleStat.AVAILABLESP, 0);
            chr.changeSingleSkillLevel(skill, (byte) (curLevel + amount), chr.getMasterLevel(skill));
        } else {
            if (chr.isAdmin()) {
                chr.dropMessage(5, "加技能点错误 - SP点数不足够或者技能不是该角色的技能.");
            }
            c.announce(MaplePacketCreator.enableActions());
        }
        // 检测并开启名流爆击
        chr.AutoCelebrityCrit();
    }

    public static void AutoAssignAP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        /*
         * Send AUTO_ASSIGN_AP [8E] (34)
         * 8E 00 - 包头
         * 8F 82 2B 00 - 未知
         * 02 00 00 00 - 这个应该是有几个能力点要加
         * 80 00 00 00 00 00 00 00
         * 0E 00 00 00
         * 40 00 00 00 00 00 00 00
         * 2A 00 00 00
         * ?弬+.....?..........@.......*...
         */
        if (chr == null) {
            return;
        }
        chr.updateTick(slea.readInt());
        int autoSpSize = slea.readInt();
        if (slea.available() < autoSpSize * 12) {
            return;
        }
        int PrimaryStat = (int) slea.readLong();
        int amount = slea.readInt();
        int SecondaryStat = autoSpSize > 1 ? (int) slea.readLong() : 0;
        int amount2 = autoSpSize > 1 ? slea.readInt() : 0;
        if (amount < 0 || amount2 < 0) {
            return;
        }
        PlayerStats playerst = chr.getStat();
        boolean usedAp1 = true, usedAp2 = true;
        List<Pair<MapleStat, Long>> statupdate = new ArrayList<>(2);
//        c.announce(MaplePacketCreator.updatePlayerStats(statupdate, true, chr, true));
        int statLimit = ServerConfig.CHANNEL_PLAYER_MAXAP;
        if (chr.getRemainingAp() >= amount + amount2) {
            switch (PrimaryStat) {
                case 64: // 力量
                    if (playerst.getStr() + amount > statLimit) {
                        return;
                    }
                    playerst.setStr((short) (playerst.getStr() + amount), chr);
                    statupdate.add(new Pair<>(MapleStat.力量, (long) playerst.getStr()));
                    break;
                case 128: // 敏捷
                    if (playerst.getDex() + amount > statLimit) {
                        return;
                    }
                    playerst.setDex((short) (playerst.getDex() + amount), chr);
                    statupdate.add(new Pair<>(MapleStat.敏捷, (long) playerst.getDex()));
                    break;
                case 256: // 智力
                    if (playerst.getInt() + amount > statLimit) {
                        return;
                    }
                    playerst.setInt((short) (playerst.getInt() + amount), chr);
                    statupdate.add(new Pair<>(MapleStat.智力, (long) playerst.getInt()));
                    break;
                case 512: // 运气
                    if (playerst.getLuk() + amount > statLimit) {
                        return;
                    }
                    playerst.setLuk((short) (playerst.getLuk() + amount), chr);
                    statupdate.add(new Pair<>(MapleStat.运气, (long) playerst.getLuk()));
                    break;
                case 2048: //最大HP
                    int maxhp = playerst.getMaxHp();
                    if (chr.getHpApUsed() >= 10000 || maxhp >= chr.getMaxHpForSever() || !JobConstants.is恶魔复仇者(chr.getJob())) {
                        return;
                    }
                    maxhp += 30 * amount;
                    maxhp = Math.min(chr.getMaxHpForSever(), Math.abs(maxhp));
                    chr.setHpApUsed((short) (chr.getHpApUsed() + amount));
                    playerst.setMaxHp(maxhp, chr);
                    statupdate.add(new Pair<>(MapleStat.MAXHP, (long) playerst.getMaxHp()));
                    break;
                default:
                    usedAp1 = false;
                    break;
            }
            switch (SecondaryStat) {
                case 64: // 力量
                    if (playerst.getStr() + amount2 > statLimit) {
                        return;
                    }
                    playerst.setStr((short) (playerst.getStr() + amount2), chr);
                    statupdate.add(new Pair<>(MapleStat.力量, (long) playerst.getStr()));
                    break;
                case 128: // 敏捷
                    if (playerst.getDex() + amount2 > statLimit) {
                        return;
                    }
                    playerst.setDex((short) (playerst.getDex() + amount2), chr);
                    statupdate.add(new Pair<>(MapleStat.敏捷, (long) playerst.getDex()));
                    break;
                case 256: // 智力
                    if (playerst.getInt() + amount2 > statLimit) {
                        return;
                    }
                    playerst.setInt((short) (playerst.getInt() + amount2), chr);
                    statupdate.add(new Pair<>(MapleStat.智力, (long) playerst.getInt()));
                    break;
                case 512: // 运气
                    if (playerst.getLuk() + amount2 > statLimit) {
                        return;
                    }
                    playerst.setLuk((short) (playerst.getLuk() + amount2), chr);
                    statupdate.add(new Pair<>(MapleStat.运气, (long) playerst.getLuk()));
                    break;
                default:
                    usedAp2 = false;
                    break;
            }
            if ((!usedAp1 || !usedAp2) && chr.isAdmin()) {
                chr.dropMessage(5, "自动分配能力点 - 主要: " + usedAp1 + " 次要: " + usedAp2);
            }
            chr.setRemainingAp((short) (chr.getRemainingAp() - ((usedAp1 ? amount : 0) + (usedAp2 ? amount2 : 0))));
            statupdate.add(new Pair<>(MapleStat.AVAILABLEAP, (long) chr.getRemainingAp()));

            c.announce(MaplePacketCreator.updatePlayerStats(statupdate, true, chr, true));
        }
    }

    public static void DistributeHyperSP(int skillid, MapleClient c, MapleCharacter chr, boolean isStat) {
        Skill skill = SkillFactory.getSkill(skillid);
        if (skill != null && (skill.isHyperSkill() || skill.isHyperStat())) {
            if (!isStat) {
                if (chr.getLevel() >= skill.getReqLevel() && skill.canBeLearnedBy(chr.getJob()) && chr.getSkillLevel(skill) == 0) {
                    chr.changeSingleSkillLevel(skill, (byte) 1, (byte) skill.getMaxLevel());
                }
            } else if (chr.getSkillLevel(skill) < skill.getMaxLevel()) {
                if (skillid == 80000406 && !JobConstants.is恶魔猎手(chr.getJob())) { //DF
                    chr.dropMessage(1, "该技能只有恶魔猎手可以使用.");
                } else {
                    chr.changeSingleSkillLevel(skill, chr.getSkillLevel(skill) + 1, (byte) skill.getMaxLevel());
                }
            }
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    public static void ResetHyperSP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        chr.updateTick(slea.readInt());
        int amount = slea.readShort();
        if (amount > 0) {
            Map<Integer, SkillEntry> oldList = new HashMap<>(chr.getSkills());
            Map<Integer, SkillEntry> newList = new HashMap<>();
            for (Entry<Integer, SkillEntry> toRemove : oldList.entrySet()) {
                Skill skill = SkillFactory.getSkill(toRemove.getKey());
                if (skill != null && skill.isHyperSkill() && chr.getSkillLevel(toRemove.getKey()) == 1) {
                    if (skill.canBeLearnedBy(chr.getJob())) {
                        newList.put(toRemove.getKey(), new SkillEntry((byte) 0, toRemove.getValue().masterlevel, toRemove.getValue().expiration));
                    } else {
                        newList.put(toRemove.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
                    }
                }
            }
            if (!newList.isEmpty() && chr.getMeso() >= amount * 1000000) {
                chr.gainMeso(-amount * 1000000, true, true);
                chr.changeSkillsLevel(newList);
                chr.dropMessage(1, "超级技能初始化完成\r\n本次消费金币: " + amount * 1000000);
            } else {
                chr.dropMessage(1, "超级技能初始化失败，您的金币不足。本次需要金币: " + amount * 1000000);
            }
            oldList.clear();
            newList.clear();
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    public static void ResetHyperAP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        Map<Integer, SkillEntry> oldList = new HashMap<>(chr.getSkills());
        Map<Integer, SkillEntry> newList = new HashMap<>();
        for (Entry<Integer, SkillEntry> toRemove : oldList.entrySet()) {
            Skill skill = SkillFactory.getSkill(toRemove.getKey());
            if (skill != null && skill.isHyperStat() && chr.getSkillLevel(toRemove.getKey()) > 0) {
                newList.put(toRemove.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
            }
        }
        if (!newList.isEmpty() && chr.getMeso() >= 10000000) {
            chr.gainMeso(-10000000, true, true);
            chr.changeSkillsLevel(newList);
            chr.dropMessage(1, "超级属性点初始化完成\r\n本次消费金币: " + 10000000);
        } else {
            chr.dropMessage(1, "超级属性点初始化失败，您的金币不足。本次需要金币: " + 10000000);
        }
        oldList.clear();
        newList.clear();
        c.announce(MaplePacketCreator.enableActions());
    }
}
