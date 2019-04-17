package handling.channel.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import handling.world.World;
import handling.world.WorldFamilyService;
import handling.world.WorldFindService;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyCharacter;
import server.maps.FieldLimitType;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianAccessor;
import tools.packet.FamilyPacket;

import java.util.List;

public class FamilyHandler {

    public static void RequestFamily(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (chr != null) {
            c.announce(FamilyPacket.getFamilyPedigree(chr));
        }
    }

    /*
     * 打开学院
     */
    public static void OpenFamily(LittleEndianAccessor slea, MapleClient c) {
        c.announce(FamilyPacket.getFamilyInfo(c.getPlayer()));
    }

    /*
     * 使用学院状态
     */
    public static void UseFamily(LittleEndianAccessor slea, MapleClient c) {
        int type = slea.readInt();
        if (MapleFamilyBuff.values().length <= type) {
            return;
        }
        MapleFamilyBuff entry = MapleFamilyBuff.values()[type];
        boolean success = c.getPlayer().getFamilyId() > 0 && c.getPlayer().canUseFamilyBuff(entry) && c.getPlayer().getCurrentRep() > entry.rep;
        if (!success) {
            return;
        }
        MapleCharacter victim = null;
        switch (entry) {
            case 瞬移: //teleport: need add check for if not a safe place
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                if (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) || c.getPlayer().isInBlockedMap()) {
                    c.getPlayer().dropMessage(5, "Summons failed. Your current location or state does not allow a summons.");
                    success = false;
                } else if (victim == null || (victim.isGM() && !c.getPlayer().isGM())) {
                    c.getPlayer().dropMessage(1, "Invalid name or you are not on the same channel.");
                    success = false;
                } else if (victim.getFamilyId() == c.getPlayer().getFamilyId() && !FieldLimitType.VipRock.check(victim.getMap().getFieldLimit()) && victim.getId() != c.getPlayer().getId() && !victim.isInBlockedMap()) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().getPortal(0));
                } else {
                    c.getPlayer().dropMessage(5, "Summons failed. Your current location or state does not allow a summons.");
                    success = false;
                }
                break;
            case 召唤: // TODO give a check to the player being forced somewhere else..
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                if (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) || c.getPlayer().isInBlockedMap()) {
                    c.getPlayer().dropMessage(5, "Summons failed. Your current location or state does not allow a summons.");
                } else if (victim == null || (victim.isGM() && !c.getPlayer().isGM())) {
                    c.getPlayer().dropMessage(1, "Invalid name or you are not on the same channel.");
                } else if (victim.getTeleportName().length() > 0) {
                    c.getPlayer().dropMessage(1, "Another character has requested to summon this character. Please try again later.");
                } else if (victim.getFamilyId() == c.getPlayer().getFamilyId() && !FieldLimitType.VipRock.check(victim.getMap().getFieldLimit()) && victim.getId() != c.getPlayer().getId() && !victim.isInBlockedMap()) {
                    victim.getClient().announce(FamilyPacket.familySummonRequest(c.getPlayer().getName(), c.getPlayer().getMap().getMapName()));
                    victim.setTeleportName(c.getPlayer().getName());
                } else {
                    c.getPlayer().dropMessage(5, "Summons failed. Your current location or state does not allow a summons.");
                }
                return; //RETURN not break
            case 爆率15分钟: // drop rate + 50% 15 min
            case 经验15分钟: // exp rate + 50% 15 min
            case 爆率30分钟: // drop rate + 100% 15 min
            case 经验30分钟: // exp rate + 100% 15 min
                //case Drop_15_15:
                //case Drop_15_30:
                //c.announce(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
                entry.applyTo(c.getPlayer());
                break;
            case 团结: // 6 family members in pedigree online Drop Rate & Exp Rate + 100% 30 minutes
                final MapleFamily fam = WorldFamilyService.getInstance().getFamily(c.getPlayer().getFamilyId());
                List<MapleFamilyCharacter> chrs = fam.getMFC(c.getPlayer().getId()).getOnlineJuniors(fam);
                if (chrs.size() < 7) {
                    success = false;
                } else {
                    for (MapleFamilyCharacter chrz : chrs) {
                        int chr = WorldFindService.getInstance().findChannel(chrz.getId());
                        if (chr == -1) {
                            continue; //STOP WTF?! take reps though..
                        }
                        MapleCharacter chrr = World.getStorage(chr).getCharacterById(chrz.getId());
                        entry.applyTo(chrr);
                        //chrr.getClient().announce(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
                    }
                }
                break;
            /*
             * case EXP_Party:
             * case Drop_Party_12: // drop rate + 100% party 30 min
             * case Drop_Party_15: // exp rate + 100% party 30 min
             * entry.applyTo(c.getPlayer());
             * //c.announce(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
             * if (c.getPlayer().getParty() != null) {
             * for (MaplePartyCharacter mpc : c.getPlayer().getParty().getMembers()) {
             * if (mpc.getId() != c.getPlayer().getId()) {
             * MapleCharacter chr = c.getPlayer().getMap().getCharacterById(mpc.getId());
             * if (chr != null) {
             * entry.applyTo(chr);
             * //chr.send(FamilyPacket.familyBuff(entry.type, type, entry.effect, entry.duration*60000));
             * }
             * }
             * }
             * }
             * break;
             */
        }
        if (success) { //again
            c.getPlayer().setCurrentRep(c.getPlayer().getCurrentRep() - entry.rep);
            c.announce(FamilyPacket.changeRep(-entry.rep, c.getPlayer().getName()));
            c.getPlayer().useFamilyBuff(entry);
        } else {
            c.getPlayer().dropMessage(5, "发生未知错误。");
        }
    }

    /*
     * 学院操作
     */
    public static void FamilyOperation(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer() == null) {
            return;
        }
        MapleCharacter addChr = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (addChr == null) {
            c.announce(FamilyPacket.sendFamilyMessage(0x41)); //角色不在线，或角色名不正确。
        } else if (addChr.getFamilyId() == c.getPlayer().getFamilyId() && addChr.getFamilyId() > 0) {
            c.announce(FamilyPacket.sendFamilyMessage(0x42)); //是同一冒险学院。
        } else if (addChr.getMapId() != c.getPlayer().getMapId()) {
            c.announce(FamilyPacket.sendFamilyMessage(0x45)); //只有在同一地图中的角色才能登录为同学
        } else if (addChr.getSeniorId() != 0) {
            c.announce(FamilyPacket.sendFamilyMessage(0x46)); //已经是其他角色的同学
        } else if (addChr.getLevel() >= c.getPlayer().getLevel()) {
            c.announce(FamilyPacket.sendFamilyMessage(0x47)); //只能将比自己等级低的角色登录为同学
        } else if (addChr.getLevel() < c.getPlayer().getLevel() - 20) {
            c.announce(FamilyPacket.sendFamilyMessage(0x48)); //等级差异超过20，无法登录为同学。
            //} else if (c.getPlayer().getFamilyId() != 0 && c.getPlayer().getFamily().getGens() >= 1000) {
            //	c.getPlayer().dropMessage(5, "Your family cannot extend more than 1000 generations from above and below.");
        } else if (addChr.getLevel() < 10) {
            c.getPlayer().dropMessage(1, "被邀请的角色等级必须大于10级.");
        } else if (c.getPlayer().getJunior1() > 0 && c.getPlayer().getJunior2() > 0) {
            c.getPlayer().dropMessage(1, "你已经有2位同学，无法继续邀请.");
        } else if (c.getPlayer().isGM() || !addChr.isGM()) {
            addChr.send(FamilyPacket.sendFamilyInvite(c.getPlayer().getId(), c.getPlayer().getLevel(), c.getPlayer().getJob(), c.getPlayer().getName()));
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    public static void FamilyPrecept(LittleEndianAccessor slea, MapleClient c) {
        MapleFamily fam = WorldFamilyService.getInstance().getFamily(c.getPlayer().getFamilyId());
        if (fam == null || fam.getLeaderId() != c.getPlayer().getId()) {
            return;
        }
        fam.setNotice(slea.readMapleAsciiString());
    }

    /*
     * 召唤学院同学
     */
    public static void FamilySummon(LittleEndianAccessor slea, MapleClient c) {
        MapleFamilyBuff cost = MapleFamilyBuff.召唤;
        MapleCharacter tt = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
        if (c.getPlayer().getFamilyId() > 0 && tt != null && tt.getFamilyId() == c.getPlayer().getFamilyId() && !FieldLimitType.VipRock.check(tt.getMap().getFieldLimit())
                && !FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()) && tt.canUseFamilyBuff(cost)
                && c.getPlayer().getTeleportName().equals(tt.getName()) && tt.getCurrentRep() > cost.rep && !c.getPlayer().isInBlockedMap() && !tt.isInBlockedMap()) {
            //whew lots of checks
            boolean accepted = slea.readByte() > 0;
            if (accepted) {
                c.getPlayer().changeMap(tt.getMap(), tt.getMap().getPortal(0));
                tt.setCurrentRep(tt.getCurrentRep() - cost.rep);
                tt.getClient().announce(FamilyPacket.changeRep(-cost.rep, tt.getName()));
                tt.useFamilyBuff(cost);
            } else {
                tt.dropMessage(5, "召唤玩家失败，您当前的位置或状态不容许召唤学院同学。");
            }
        } else {
            c.getPlayer().dropMessage(5, "召唤玩家失败，您当前的位置或状态不容许召唤学院同学。");
        }
        c.getPlayer().setTeleportName("");
    }

    public static void DeleteJunior(LittleEndianAccessor slea, MapleClient c) {
        int juniorid = slea.readInt();
        if (c.getPlayer().getFamilyId() <= 0 || juniorid <= 0 || (c.getPlayer().getJunior1() != juniorid && c.getPlayer().getJunior2() != juniorid)) {
            return;
        }
        //junior is not required to be online.
        MapleFamily fam = WorldFamilyService.getInstance().getFamily(c.getPlayer().getFamilyId());
        MapleFamilyCharacter other = fam.getMFC(juniorid);
        if (other == null) {
            return;
        }
        MapleFamilyCharacter oth = c.getPlayer().getMFC();
        boolean junior2 = oth.getJunior2() == juniorid;
        if (junior2) {
            oth.setJunior2(0);
        } else {
            oth.setJunior1(0);
        }
        c.getPlayer().saveFamilyStatus();
        other.setSeniorId(0);
        //if (!other.isOnline()) {
        MapleFamily.setOfflineFamilyStatus(other.getFamilyId(), other.getSeniorId(), other.getJunior1(), other.getJunior2(), other.getCurrentRep(), other.getTotalRep(), other.getId());
        //}
        MapleCharacterUtil.sendNote(other.getName(), c.getPlayer().getName(), c.getPlayer().getName() + "宣布诀别，冒险学院关系已断绝。", 0);
        MapleCharacter receiver = c.getChannelServer().getPlayerStorage().getCharacterByName(other.getName());
        if (receiver != null) {
            receiver.showNote();
        }
        if (!fam.splitFamily(juniorid, other)) { //juniorid splits to make their own family. function should handle the rest
            if (!junior2) {
                fam.resetDescendants();
            }
            fam.resetPedigree();
        }
        c.announce(FamilyPacket.sendFamilyMessage(0x01, other.getId())); // 已和(null)诀别。冒险学院关系已经结束。
        c.announce(MaplePacketCreator.enableActions());
    }

    public static void DeleteSenior(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getFamilyId() <= 0 || c.getPlayer().getSeniorId() <= 0) {
            return;
        }
        //not required to be online
        MapleFamily fam = WorldFamilyService.getInstance().getFamily(c.getPlayer().getFamilyId()); //this is old family
        MapleFamilyCharacter mgc = fam.getMFC(c.getPlayer().getSeniorId());
        MapleFamilyCharacter mgc_ = c.getPlayer().getMFC();
        mgc_.setSeniorId(0);
        boolean junior2 = mgc.getJunior2() == c.getPlayer().getId();
        if (junior2) {
            mgc.setJunior2(0);
        } else {
            mgc.setJunior1(0);
        }
        //if (!mgc.isOnline()) {
        MapleFamily.setOfflineFamilyStatus(mgc.getFamilyId(), mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
        //}
        c.getPlayer().saveFamilyStatus();
        MapleCharacterUtil.sendNote(mgc.getName(), c.getPlayer().getName(), c.getPlayer().getName() + "宣布诀别，冒险学院关系已断绝。", 0);
        MapleCharacter receiver = c.getChannelServer().getPlayerStorage().getCharacterByName(mgc.getName());
        if (receiver != null) {
            receiver.showNote();
        }
        if (!fam.splitFamily(c.getPlayer().getId(), mgc_)) { //now, we're the family leader
            if (!junior2) {
                fam.resetDescendants();
            }
            fam.resetPedigree();
        }
        c.announce(FamilyPacket.sendFamilyMessage(0x01, mgc.getId())); // 已和(null)诀别。冒险学院关系已经结束。
        c.announce(MaplePacketCreator.enableActions());
    }

    public static void AcceptFamily(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter inviter = c.getPlayer().getMap().getCharacterById(slea.readInt());
        if (inviter != null && c.getPlayer().getSeniorId() == 0 && (c.getPlayer().isGM() || !inviter.isHidden()) && inviter.getLevel() - 20 <= c.getPlayer().getLevel() && inviter.getLevel() >= 10 && inviter.getName().equals(slea.readMapleAsciiString()) && inviter.getNoJuniors() < 2 /*
                 * && inviter.getFamily().getGens() < 1000
                 */ && c.getPlayer().getLevel() >= 10) {
            boolean accepted = slea.readByte() > 0;
            inviter.getClient().announce(FamilyPacket.sendFamilyJoinResponse(accepted, c.getPlayer().getName()));
            if (accepted) {
                //c.announce(FamilyPacket.sendFamilyMessage(0));
                c.announce(FamilyPacket.getSeniorMessage(inviter.getName()));
                int old = c.getPlayer().getMFC() == null ? 0 : c.getPlayer().getMFC().getFamilyId();
                int oldj1 = c.getPlayer().getMFC() == null ? 0 : c.getPlayer().getMFC().getJunior1();
                int oldj2 = c.getPlayer().getMFC() == null ? 0 : c.getPlayer().getMFC().getJunior2();
                if (inviter.getFamilyId() > 0 && WorldFamilyService.getInstance().getFamily(inviter.getFamilyId()) != null) {
                    MapleFamily fam = WorldFamilyService.getInstance().getFamily(inviter.getFamilyId());
                    //if old isn't null, don't set the familyid yet, mergeFamily will take care of it
                    c.getPlayer().setFamily(old <= 0 ? inviter.getFamilyId() : old, inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2);
                    MapleFamilyCharacter mf = inviter.getMFC();
                    if (mf.getJunior1() > 0) {
                        mf.setJunior2(c.getPlayer().getId());
                    } else {
                        mf.setJunior1(c.getPlayer().getId());
                    }
                    inviter.saveFamilyStatus();
                    if (old > 0 && WorldFamilyService.getInstance().getFamily(old) != null) { //has junior
                        MapleFamily.mergeFamily(fam, WorldFamilyService.getInstance().getFamily(old));
                    } else {
                        c.getPlayer().setFamily(inviter.getFamilyId(), inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2);
                        fam.setOnline(c.getPlayer().getId(), true, c.getChannel());
                        c.getPlayer().saveFamilyStatus();
                    }
                    if (fam != null) {
                        if (inviter.getNoJuniors() == 1 || old > 0) {//just got their first junior whoopee
                            fam.resetDescendants();
                        }
                        fam.resetPedigree(); //is this necessary?
                    }
                } else {
                    int id = MapleFamily.createFamily(inviter.getId());
                    if (id > 0) {
                        //before loading the family, set sql
                        MapleFamily.setOfflineFamilyStatus(id, 0, c.getPlayer().getId(), 0, inviter.getCurrentRep(), inviter.getTotalRep(), inviter.getId());
                        MapleFamily.setOfflineFamilyStatus(id, inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2, c.getPlayer().getCurrentRep(), c.getPlayer().getTotalRep(), c.getPlayer().getId());
                        inviter.setFamily(id, 0, c.getPlayer().getId(), 0); //load the family
                        inviter.finishAchievement(36);
                        c.getPlayer().setFamily(id, inviter.getId(), oldj1 <= 0 ? 0 : oldj1, oldj2 <= 0 ? 0 : oldj2);
                        MapleFamily fam = WorldFamilyService.getInstance().getFamily(id);
                        fam.setOnline(inviter.getId(), true, inviter.getClient().getChannel());
                        if (old > 0 && WorldFamilyService.getInstance().getFamily(old) != null) { //has junior
                            MapleFamily.mergeFamily(fam, WorldFamilyService.getInstance().getFamily(old));
                        } else {
                            fam.setOnline(c.getPlayer().getId(), true, c.getChannel());
                        }
                        fam.resetDescendants();
                        fam.resetPedigree();
                    }
                }
                c.announce(FamilyPacket.getFamilyInfo(c.getPlayer()));
            }
        }
    }
}
