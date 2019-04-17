/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import client.CardData;
import com.alibaba.druid.pool.DruidPooledConnection;
import constants.SkillConstants;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;
import tools.Triple;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

/**
 * 角色卡系统
 *
 * @author PlayDK
 */
public class CharacterCardFactory {

    private static final Logger log = LogManager.getLogger();
    private static final CharacterCardFactory instance = new CharacterCardFactory();
    protected final MapleDataProvider etcData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));
    protected final Map<Integer, Integer> cardEffects = new HashMap<>();
    protected final Map<Integer, List<Integer>> uniqueEffects = new HashMap<>();

    public static CharacterCardFactory getInstance() {
        return instance;
    }

    public void initialize() {
        MapleData data = etcData.getData("CharacterCard.img");
        for (MapleData Card : data.getChildByPath("Card")) {
            int skillId = MapleDataTool.getIntConvert("skillID", Card, 0);
            if (skillId > 0) {
                cardEffects.put(Integer.parseInt(Card.getName()), skillId);
            }
        }
        for (MapleData Deck : data.getChildByPath("Deck")) {
            boolean uniqueEffect = MapleDataTool.getIntConvert("uniqueEffect", Deck, 0) > 0;
            int skillId = MapleDataTool.getIntConvert("skillID", Deck, 0);
            if (uniqueEffect) {
                List<Integer> ids = new ArrayList<>();
                for (MapleData reqCardID : Deck.getChildByPath("reqCardID")) {
                    ids.add(MapleDataTool.getIntConvert(reqCardID));
                }
                if (skillId > 0 && !ids.isEmpty()) {
                    uniqueEffects.put(skillId, ids);
                }
            }
        }
    }

    public Triple<Integer, Integer, Integer> getCardSkill(int job, int level) {
        int skillid = cardEffects.get(job / 10);
        if (skillid <= 0) {
            return null;
        }
        //角色卡ID 技能ID 技能等级
        return new Triple<>(skillid - 71000000, skillid, SkillConstants.getCardSkillLevel(level));
    }

    public List<Integer> getUniqueSkills(List<Integer> special) {
        List<Integer> ret = new LinkedList<>();
        for (Entry<Integer, List<Integer>> m : uniqueEffects.entrySet()) {
            if (m.getValue().contains(special.get(0)) && m.getValue().contains(special.get(1)) && m.getValue().contains(special.get(2))) {
                ret.add(m.getKey());
            }
        }
        return ret;
    }

    public boolean isUniqueEffects(int skillId) {
        return uniqueEffects.containsKey(skillId);
    }

    public int getRankSkill(int level) {
        return (SkillConstants.getCardSkillLevel(level) + 71001099);
    }

    public boolean canHaveCard(int level, int job) {
        return level >= 30 && cardEffects.get(job / 10) != null;
    }

    public Map<Integer, CardData> loadCharacterCards(int accId, int serverId) {
        Map<Integer, CardData> cards = new LinkedHashMap<>();
        Map<Integer, Pair<Short, Short>> inf = loadCharactersInfo(accId, serverId);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM `character_cards` WHERE `accid` = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    int deck1 = 0, deck2 = 3, deck3 = 6;
                    while (rs.next()) {
                        int chrId = rs.getInt("characterid");
                        Pair<Short, Short> x = inf.get(chrId);
                        if (x == null) {
                            continue;
                        }
                        // 检测职业 和 等级
                        if (!canHaveCard(x.getLeft(), x.getRight())) {
                            continue;
                        }
                        int position = rs.getInt("position");
                        if (position < 4) {
                            deck1++;
                            cards.put(deck1, new CardData(chrId, x.getLeft(), x.getRight()));
                            //System.out.println("第1组: " + deck1 + " id: " + chrId + " 职业: " + x.getLeft().shortValue() + " 等级: " + x.getRight().shortValue());
                        } else if (position > 3 && position < 7) {
                            deck2++;
                            cards.put(deck2, new CardData(chrId, x.getLeft(), x.getRight()));
                            //System.out.println("第2组: " + deck2 + " id: " + chrId + " 职业: " + x.getLeft().shortValue() + " 等级: " + x.getRight().shortValue());
                        } else {
                            deck3++;
                            cards.put(deck3, new CardData(chrId, x.getLeft(), x.getRight()));
                            //System.out.println("第3组: " + deck3 + " id: " + chrId + " 职业: " + x.getLeft().shortValue() + " 等级: " + x.getRight().shortValue());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to load character cards. Reason: ", e);
        }
        for (int i = 1; i <= 9; i++) {
            cards.computeIfAbsent(i, k -> new CardData(0, (short) 0, (short) 0));
        }
        return cards;
    }

    public Map<Integer, Pair<Short, Short>> loadCharactersInfo(int accId, int serverId) {
        Map<Integer, Pair<Short, Short>> chars = new LinkedHashMap<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, level, job FROM characters WHERE accountid = ? AND world = ?")) {
                ps.setInt(1, accId);
                ps.setInt(2, serverId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        chars.put(rs.getInt("id"), new Pair<>(rs.getShort("level"), rs.getShort("job")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("error loading characters info. reason: " + e.toString());
        }
        return chars;
    }
}
