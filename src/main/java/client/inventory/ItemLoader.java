package client.inventory;

import com.alibaba.druid.pool.DruidPooledConnection;
import constants.ItemConstants;
import database.DatabaseConnection;
import server.MapleItemInformationProvider;
import tools.Pair;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum ItemLoader {

    装备道具(0, false),
    仓库道具(1, true),
    现金道具(2, true),
    雇佣道具(5, false),
    送货道具(6, false),
    拍卖道具(8, false),
    MTS_TRANSFER(9, false),
    钓鱼道具(10, false);

    private final int value;
    private final boolean account;

    ItemLoader(int value, boolean account) {
        this.value = value;
        this.account = account;
    }

    public int getValue() {
        return value;
    }

    //does not need connection con to be auto commit
    public Map<Long, Pair<Item, MapleInventoryType>> loadItems(boolean login, int id) throws SQLException {
        Map<Long, Pair<Item, MapleInventoryType>> items = new LinkedHashMap<>();

        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING(`inventoryitemid`) LEFT JOIN `nirvanaflame` USING(`inventoryitemid`) LEFT JOIN `familiarcard` USING(`inventoryitemid`) WHERE `type` = ? AND `");
        query.append(account ? "accountid" : "characterid");
        query.append("` = ?");

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                if (login) {
                    query.append(" AND `inventorytype` = ");
                    query.append(MapleInventoryType.EQUIPPED.getType());
                }

                ps.setInt(1, value);
                ps.setInt(2, id);

                try (ResultSet rs = ps.executeQuery()) {
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    while (rs.next()) {
                        if (!ii.itemExists(rs.getInt("itemid"))) { //没有存在的道具就跳过
                            continue;
                        }
                        MapleInventoryType mit = MapleInventoryType.getByType(rs.getByte("inventorytype"));

                        if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                            Equip equip = new Equip(rs.getInt("itemid"), rs.getShort("position"), rs.getInt("uniqueid"), rs.getShort("flag"), rs.getShort("espos"));
                            if (!login && equip.getPosition() != -55) { //monsterbook
                                equip.setQuantity((short) 1);
                                equip.setInventoryId(rs.getLong("inventoryitemid"));
                                equip.setOwner(rs.getString("owner"));
                                equip.setExpiration(rs.getLong("expiredate"));
                                equip.setEquipOnlyId(rs.getInt("equipOnlyId")); //设置装备道具的唯一ID
                                equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                                equip.setLevel(rs.getByte("level"));
                                equip.setStr(rs.getShort("str"));
                                equip.setDex(rs.getShort("dex"));
                                equip.setInt(rs.getShort("int"));
                                equip.setLuk(rs.getShort("luk"));
                                equip.setHp(rs.getShort("hp"));
                                equip.setMp(rs.getShort("mp"));
                                equip.setWatk(rs.getShort("watk"));
                                equip.setMatk(rs.getShort("matk"));
                                equip.setWdef(rs.getShort("wdef"));
                                equip.setMdef(rs.getShort("mdef"));
                                equip.setAcc(rs.getShort("acc"));
                                equip.setAvoid(rs.getShort("avoid"));
                                equip.setHands(rs.getShort("hands"));
                                equip.setSpeed(rs.getShort("speed"));
                                equip.setJump(rs.getShort("jump"));
                                equip.setViciousHammer(rs.getByte("ViciousHammer"));
                                equip.setItemEXP(rs.getLong("itemEXP"));
                                equip.setGMLog(rs.getString("GM_Log"));
                                equip.setDurability(rs.getInt("durability"));
                                equip.setState(rs.getByte("state"), false);
                                equip.setEnhance(rs.getByte("enhance"));
                                equip.setPotential1(rs.getInt("potential1"));
                                equip.setPotential2(rs.getInt("potential2"));
                                equip.setPotential3(rs.getInt("potential3"));
                                equip.setState(rs.getByte("addState"), true);
                                equip.setPotential4(rs.getInt("potential4"));
                                equip.setPotential5(rs.getInt("potential5"));
                                equip.setPotential6(rs.getInt("potential6"));
                                equip.setGiftFrom(rs.getString("sender"));
                                equip.setIncSkill(rs.getInt("incSkill"));
                                equip.setPVPDamage(rs.getShort("pvpDamage"));
                                equip.setCharmEXP(rs.getShort("charmEXP"));
                                equip.setStateMsg(rs.getInt("statemsg")); //潜能等级提示设置
                                equip.setSocket1(rs.getInt("itemSlot1")); //镶嵌宝石1
                                equip.setSocket2(rs.getInt("itemSlot2")); //镶嵌宝石2
                                equip.setSocket3(rs.getInt("itemSlot3")); //镶嵌宝石3
                                equip.setItemSkin(rs.getInt("itemSkin")); //道具合成后的外形
                                equip.setLimitBreak(rs.getInt("limitBreak")); //武器攻击突破上限
                                //新增装备属性
                                equip.setEnhanctBuff(rs.getShort("enhanctBuff"));
                                equip.setReqLevel(rs.getShort("reqLevel"));
                                equip.setYggdrasilWisdom(rs.getShort("yggdrasilWisdom"));
                                equip.setFinalStrike(rs.getShort("finalStrike") > 0);
                                equip.setBossDamage(rs.getShort("bossDamage"));
                                equip.setIgnorePDR(rs.getShort("ignorePDR"));
                                //新增装备特殊属性
                                equip.setTotalDamage(rs.getShort("totalDamage"));
                                equip.setAllStat(rs.getShort("allStat"));
                                equip.setKarmaCount(rs.getShort("karmaCount"));
                                equip.setFire(rs.getLong("fire"));
                                //漩涡装备属性
                                equip.setSealedLevel(equip.isSealedEquip() ? (byte) Math.max(1, rs.getByte("sealedlevel")) : 0);
                                equip.setSealedExp(rs.getLong("sealedExp"));
                                //灵魂武器属性
                                equip.setSoulName(rs.getShort("soulname"));
                                equip.setSoulEnchanter(rs.getShort("soulenchanter"));
                                equip.setSoulPotential(rs.getShort("soulpotential"));
                                equip.setSoulSkill(rs.getInt("soulskill"));
                                equip.setARC(rs.getShort("arc"));
                                equip.setARCExp(rs.getInt("arcexp"));
                                equip.setARCLevel(rs.getShort("arclevel"));
                                //涅槃火焰
                                equip.getNirvanaFlame().setNstr(rs.getInt("nstr"));
                                equip.getNirvanaFlame().setNdex(rs.getInt("ndex"));
                                equip.getNirvanaFlame().setNint(rs.getInt("nint"));
                                equip.getNirvanaFlame().setNluk(rs.getInt("nluk"));
                                equip.getNirvanaFlame().setNhp(rs.getInt("nhp"));
                                equip.getNirvanaFlame().setNmp(rs.getInt("nmp"));
                                equip.getNirvanaFlame().setNwatk(rs.getInt("nwatk"));
                                equip.getNirvanaFlame().setNmatk(rs.getInt("nmatk"));
                                equip.getNirvanaFlame().setNwdef(rs.getInt("nwdef"));
                                equip.getNirvanaFlame().setNmdef(rs.getInt("nmdef"));
                                equip.getNirvanaFlame().setNacc(rs.getInt("nacc"));
                                equip.getNirvanaFlame().setNavoid(rs.getInt("navoid"));
                                equip.getNirvanaFlame().setNhands(rs.getInt("nhands"));
                                equip.getNirvanaFlame().setNspeed(rs.getInt("nspeed"));
                                equip.getNirvanaFlame().setNjump(rs.getInt("njump"));
                                equip.getNirvanaFlame().setNbossDamage(rs.getInt("nbossDamage"));
                                equip.getNirvanaFlame().setNignorePDR(rs.getInt("nignorePDR"));
                                equip.getNirvanaFlame().setNtotalDamage(rs.getInt("ntotalDamage"));
                                equip.getNirvanaFlame().setNallStat(rs.getInt("nallStat"));

                                if (equip.getReqLevel() <= 0) {
                                    equip.setReqLevel((short) MapleItemInformationProvider.getInstance().getReqLevel(equip.getItemId()));
                                }
                        /*
                         * 如果装备的魅力小于0 就重新加载默认的魅力
                         */
                                if (equip.getCharmEXP() < 0) {
                                    equip.setCharmEXP(((Equip) ii.getEquipById(equip.getItemId())).getCharmEXP());
                                }
                        /*
                         * 装备特殊的潜能属性
                         */
                                if (equip.getBossDamage() <= 0 && ii.getBossDamageRate(equip.getItemId()) > 0) {
                                    equip.setBossDamage((short) ii.getBossDamageRate(equip.getItemId()));
                                }
                                if (equip.getIgnorePDR() <= 0 && ii.getIgnoreMobDmageRate(equip.getItemId()) > 0) {
                                    equip.setIgnorePDR((short) ii.getIgnoreMobDmageRate(equip.getItemId()));
                                }
                                if (equip.getTotalDamage() <= 0 && ii.getTotalDamage(equip.getItemId()) > 0) {
                                    equip.setTotalDamage((short) ii.getTotalDamage(equip.getItemId()));
                                }
                                if (equip.getPotential1() == 0 && ii.getOption(equip.getItemId(), 1) > 0) {
                                    equip.setPotential1(ii.getOption(equip.getItemId(), 1));
                                }
                                if (equip.getPotential2() == 0 && ii.getOption(equip.getItemId(), 2) > 0) {
                                    equip.setPotential2(ii.getOption(equip.getItemId(), 2));
                                }
                                if (equip.getPotential3() == 0 && ii.getOption(equip.getItemId(), 3) > 0) {
                                    equip.setPotential3(ii.getOption(equip.getItemId(), 3));
                                }
                        /*
                         * 如果道具合成后的外形ID大于 0 且 物品数据中没有这个道具就设置外形为 0
                         */
                                if (equip.getItemSkin() > 0 && !ii.itemExists(equip.getItemSkin())) {
                                    equip.setItemSkin(0);
                                }
                                if (equip.getUniqueId() > -1) {
                                    if (ItemConstants.isEffectRing(rs.getInt("itemid"))) {
                                        MapleRing ring = MapleRing.loadFromDb(equip.getUniqueId(), mit.equals(MapleInventoryType.EQUIPPED));
                                        if (ring != null) {
                                            equip.setRing(ring);
                                        }
                                    } else if (equip.getItemId() / 10000 == 166) {
                                        MapleAndroid android = MapleAndroid.loadFromDb(equip.getItemId(), equip.getUniqueId());
                                        if (android != null) {
                                            equip.setAndroid(android);
                                        }
                                    }
                                }
                                if (equip.hasSetOnlyId()) {
                                    equip.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                                }
                                equip.initAllState();
                            }
                            items.put(rs.getLong("inventoryitemid"), new Pair<>(equip.copy(), mit));
                        } else {
                            Item item = new Item(rs.getInt("itemid"), rs.getShort("position"), rs.getShort("quantity"), rs.getShort("flag"), rs.getInt("uniqueid"), rs.getShort("espos"));
                            item.setOwner(rs.getString("owner"));
                            item.setInventoryId(rs.getLong("inventoryitemid"));
                            item.setExpiration(rs.getLong("expiredate"));
                            item.setGMLog(rs.getString("GM_Log"));
                            item.setGiftFrom(rs.getString("sender"));
//                        item.setFamiliarid(rs.getInt("familiarid"));
                            if (ItemConstants.getFamiliarByItemID(item.getItemId()) > 0) {
                                item.setFamiliarCard(new FamiliarCard(rs.getShort("skill"), rs.getByte("level"), rs.getByte("grade"), rs.getInt("option1"), rs.getInt("option2"), rs.getInt("option3")));
                            }
                            if (ItemConstants.isPet(item.getItemId())) {
                                if (item.getUniqueId() > -1) {
                                    MaplePet pet = MaplePet.loadFromDb(item.getItemId(), item.getUniqueId(), item.getPosition());
                                    if (pet != null) {
                                        item.setPet(pet);
                                    }
                                } else {
                                    item.setPet(MaplePet.createPet(item.getItemId(), MapleInventoryIdentifier.getInstance()));
                                }
                            }
                            items.put(rs.getLong("inventoryitemid"), new Pair<>(item.copy(), mit));
                        }
                    }
                }
            }
        }
        return items;
    }

    public void saveItems(DruidPooledConnection con, List<Pair<Item, MapleInventoryType>> items, int id) throws SQLException {
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        PreparedStatement psn = null;
        PreparedStatement psf = null;
        boolean needclose = false;
        try {
            if (con == null) {
                needclose = true;
                con = DatabaseConnection.getInstance().getConnection();
            }
            String query = String.format("DELETE FROM `inventoryitems` WHERE `type` = ? AND `%s` = ?", account ? "accountid" : "characterid");

            ps = con.prepareStatement(query);
            ps.setInt(1, value);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
            if (items == null) {
                return;
            }
            ps = con.prepareStatement("INSERT INTO `inventoryitems` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO `inventoryequipment` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            psn = con.prepareStatement("INSERT INTO `nirvanaflame` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            psf = con.prepareStatement("INSERT INTO `familiarcard` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)");

            for (Pair<Item, MapleInventoryType> pair : items) {
                Item item = pair.getLeft();
                MapleInventoryType mit = pair.getRight();
                ps.setInt(1, value);
                ps.setString(2, account ? null : String.valueOf(id));
                ps.setString(3, account ? String.valueOf(id) : null);
                ps.setInt(4, item.getItemId());
                ps.setInt(5, mit.getType());
                ps.setInt(6, item.getPosition());
                ps.setInt(7, item.getQuantity());
                ps.setString(8, item.getOwner());
                ps.setString(9, item.getGMLog());
                if (item.getPet() != null) {
                    ps.setInt(10, Math.max(item.getUniqueId(), item.getPet().getUniqueId()));
                } else {
                    ps.setInt(10, item.getUniqueId());
                }
                ps.setShort(11, item.getFlag());
                ps.setLong(12, item.getExpiration());
                ps.setString(13, item.getGiftFrom());
                ps.setInt(14, item.getEquipOnlyId()); //新增增加的装备道具的唯一SQLid
                ps.setInt(15, item.getESPos()); //新增增加的装备道具的唯一SQLid
                ps.executeUpdate();

                if (item.getFamiliarCard() != null && ItemConstants.getFamiliarByItemID(item.getItemId()) > 0) {
                    int i = 0;
                    ResultSet rs = ps.getGeneratedKeys();
                    if (!rs.next()) {
                        throw new RuntimeException("[saveItems] 保存道具失败.");
                    }
                    psf.setLong(++i, rs.getLong(1));
                    rs.close();
                    FamiliarCard fc = item.getFamiliarCard();
                    psf.setByte(++i, fc.getLevel());
                    psf.setByte(++i, fc.getGrade());
                    psf.setShort(++i, fc.getSkill());
                    psf.setInt(++i, fc.getOption1());
                    psf.setInt(++i, fc.getOption2());
                    psf.setInt(++i, fc.getOption3());
                    psf.executeUpdate();
                }

                if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                    int i = 0;
                    Equip equip;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new RuntimeException("[saveItems] 保存道具失败.");
                        }
                        pse.setLong(++i, rs.getLong(1));
                    }

                    equip = (Equip) item;
                    pse.setInt(++i, equip.getUpgradeSlots());
                    pse.setInt(++i, equip.getLevel());
                    pse.setInt(++i, equip.getStr() - equip.getNirvanaFlame().getNstr());
                    pse.setInt(++i, equip.getDex() - equip.getNirvanaFlame().getNdex());
                    pse.setInt(++i, equip.getInt() - equip.getNirvanaFlame().getNint());
                    pse.setInt(++i, equip.getLuk() - equip.getNirvanaFlame().getNluk());
                    pse.setInt(++i, equip.getHp() - equip.getNirvanaFlame().getNhp());
                    pse.setInt(++i, equip.getMp() - equip.getNirvanaFlame().getNmp());
                    pse.setInt(++i, equip.getWatk() - equip.getNirvanaFlame().getNwatk());
                    pse.setInt(++i, equip.getMatk() - equip.getNirvanaFlame().getNmatk());
                    pse.setInt(++i, equip.getWdef() - equip.getNirvanaFlame().getNwdef());
                    pse.setInt(++i, equip.getMdef() - equip.getNirvanaFlame().getNmdef());
                    pse.setInt(++i, equip.getAcc() - equip.getNirvanaFlame().getNacc());
                    pse.setInt(++i, equip.getAvoid() - equip.getNirvanaFlame().getNavoid());
                    pse.setInt(++i, equip.getHands() - equip.getNirvanaFlame().getNhands());
                    pse.setInt(++i, equip.getSpeed() - equip.getNirvanaFlame().getNspeed());
                    pse.setInt(++i, equip.getJump() - equip.getNirvanaFlame().getNjump());
                    pse.setInt(++i, equip.getViciousHammer());
                    pse.setLong(++i, equip.getItemEXP());
                    pse.setInt(++i, equip.getDurability());
                    pse.setByte(++i, equip.getState(false));
                    pse.setByte(++i, equip.getEnhance());
                    pse.setInt(++i, equip.getPotential1());
                    pse.setInt(++i, equip.getPotential2());
                    pse.setInt(++i, equip.getPotential3());
                    pse.setByte(++i, equip.getState(true));
                    pse.setInt(++i, equip.getPotential4());
                    pse.setInt(++i, equip.getPotential5());
                    pse.setInt(++i, equip.getPotential6());
                    pse.setInt(++i, equip.getIncSkill());
                    pse.setShort(++i, equip.getCharmEXP());
                    pse.setShort(++i, equip.getPVPDamage());
                    pse.setInt(++i, equip.getStateMsg()); //星级提示次数
                    pse.setInt(++i, equip.getSocket1()); //镶嵌宝石1
                    pse.setInt(++i, equip.getSocket2()); //镶嵌宝石2
                    pse.setInt(++i, equip.getSocket3()); //镶嵌宝石3
                    pse.setInt(++i, equip.getItemSkin()); //道具合成后的外观
                    pse.setInt(++i, equip.getLimitBreak()); //武器攻击突破上限
                    //新增装备属性字段
                    pse.setInt(++i, equip.getEnhanctBuff());
                    pse.setInt(++i, equip.getReqLevel());
                    pse.setInt(++i, equip.getYggdrasilWisdom());
                    pse.setInt(++i, (equip.getFinalStrike() ? 1 : 0));
                    pse.setInt(++i, equip.getBossDamage() - equip.getNirvanaFlame().getNbossDamage());
                    pse.setInt(++i, equip.getIgnorePDR() - equip.getNirvanaFlame().getNignorePDR());
                    pse.setInt(++i, equip.getTotalDamage() - equip.getNirvanaFlame().getNtotalDamage());
                    pse.setInt(++i, equip.getAllStat() - equip.getNirvanaFlame().getNallStat());
                    pse.setInt(++i, equip.getKarmaCount());
                    pse.setLong(++i, equip.getFire());
                    pse.setInt(++i, equip.getSealedLevel());
                    pse.setLong(++i, equip.getSealedExp());
                    pse.setShort(++i, equip.getSoulName());
                    pse.setShort(++i, equip.getSoulEnchanter());
                    pse.setShort(++i, equip.getSoulPotential());
                    pse.setInt(++i, equip.getSoulSkill());
                    pse.setShort(++i, equip.getARC());
                    pse.setInt(++i, equip.getARCExp());
                    pse.setInt(++i, equip.getARCLevel());
                    pse.executeUpdate();

                    i = 0;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new RuntimeException("[saveItems] 保存道具失败.");
                        }
                        psn.setLong(++i, rs.getLong(1));
                    }
                    psn.setInt(++i, equip.getNirvanaFlame().getNstr());
                    psn.setInt(++i, equip.getNirvanaFlame().getNdex());
                    psn.setInt(++i, equip.getNirvanaFlame().getNint());
                    psn.setInt(++i, equip.getNirvanaFlame().getNluk());
                    psn.setInt(++i, equip.getNirvanaFlame().getNhp());
                    psn.setInt(++i, equip.getNirvanaFlame().getNmp());
                    psn.setInt(++i, equip.getNirvanaFlame().getNwatk());
                    psn.setInt(++i, equip.getNirvanaFlame().getNmatk());
                    psn.setInt(++i, equip.getNirvanaFlame().getNwdef());
                    psn.setInt(++i, equip.getNirvanaFlame().getNmdef());
                    psn.setInt(++i, equip.getNirvanaFlame().getNacc());
                    psn.setInt(++i, equip.getNirvanaFlame().getNavoid());
                    psn.setInt(++i, equip.getNirvanaFlame().getNhands());
                    psn.setInt(++i, equip.getNirvanaFlame().getNspeed());
                    psn.setInt(++i, equip.getNirvanaFlame().getNjump());
                    psn.setInt(++i, equip.getNirvanaFlame().getNbossDamage());
                    psn.setInt(++i, equip.getNirvanaFlame().getNignorePDR());
                    psn.setInt(++i, equip.getNirvanaFlame().getNtotalDamage());
                    psn.setInt(++i, equip.getNirvanaFlame().getNallStat());
                    psn.executeUpdate();
                }
            }
        } finally {
            if (ps != null) {
                ps.close();
            }
            if (pse != null) {
                pse.close();
            }
            if (psf != null) {
                psf.close();
            }
            if (needclose && con != null) {
                con.close();
            }
        }
    }
}