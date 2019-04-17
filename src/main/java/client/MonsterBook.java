package client;

import client.inventory.Equip;
import com.alibaba.druid.pool.DruidPooledConnection;
import constants.GameConstants;
import database.DatabaseConnection;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public final class MonsterBook implements Serializable {

    private static final long serialVersionUID = 7179541993413738569L;
    private final int level = 0;
    private final List<Integer> cardItems = new ArrayList<>();
    private final Map<Integer, Pair<Integer, Boolean>> sets = new HashMap<>();
    private boolean changed = false;
    private int currentSet = -1;
    private int setScore;
    private int finishedSets;
    private Map<Integer, Integer> cards;

    public MonsterBook() {

    }

    public MonsterBook(Map<Integer, Integer> cards, MapleCharacter chr) {
        this.cards = cards;
        calculateItem();
        calculateScore();

        MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.CURRENT_SET));
        if (stat != null && stat.getCustomData() != null) {
            currentSet = Integer.parseInt(stat.getCustomData());
            if (!sets.containsKey(currentSet) || !sets.get(currentSet).right) {
                currentSet = -1;
            }
        }
    }

    public static MonsterBook loadCards(int charid, MapleCharacter chr) throws SQLException {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            final PreparedStatement ps = con.prepareStatement("SELECT * FROM monsterbook WHERE charid = ? ORDER BY cardid ASC");
            ps.setInt(1, charid);
            final ResultSet rs = ps.executeQuery();
            Map<Integer, Integer> cards = new LinkedHashMap<>();

            while (rs.next()) {
                cards.put(rs.getInt("cardid"), rs.getInt("level"));
            }
            rs.close();
            ps.close();
            return new MonsterBook(cards, chr);
        }
    }

    public byte calculateScore() {
        //        sets.clear();
//        int oldLevel = level, oldSetScore = setScore;
//        setScore = 0;
//        finishedSets = 0;
//        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
//        for (int i : cardItems) {
//            //we need the card id but we store the mob id lol
//            Integer x = ii.getSetId(i);
//            if (x != null && x.intValue() > 0) {
//                Triple<Integer, List<Integer>, List<Integer>> set = ii.getMonsterBookInfo(x);
//                if (set != null) {
//                    if (!sets.containsKey(x)) {
//                        sets.put(x, new Pair<>(1, Boolean.FALSE));
//                    } else {
//                        sets.get(x).left++;
//                    }
//                    if (sets.get(x).left == set.mid.size()) {
//                        sets.get(x).right = Boolean.TRUE;
//                        setScore += set.left;
//                        if (currentSet == -1) {
//                            currentSet = x;
//                            returnval = 2;
//                        }
//                        finishedSets++;
//                    }
//                }
//            }
//        }
//        level = 10;
//        for (byte i = 0; i < 10; i++) {
//            if (GameConstants.getSetExpNeededForLevel(i) > setScore) {
//                level = i;
//                break;
//            }
//        }
//        if (level > oldLevel) {
//            returnval = 2;
//        } else if (setScore > oldSetScore) {
//            returnval = 1;
//        }
        return (byte) 0;
    }

    public void writeCharInfoPacket(MaplePacketLittleEndianWriter mplew) {
        //cid, then the character's level
        List<Integer> cardSize = new ArrayList<>(10); //0 = total, 1-9 = card types..
        for (int i = 0; i < 10; i++) {
            cardSize.add(0);
        }
        for (int x : cardItems) {
            cardSize.set(0, cardSize.get(0) + 1);
            cardSize.set(((x / 1000) % 10) + 1, cardSize.get(((x / 1000) % 10) + 1) + 1);
        }
        for (int i : cardSize) {
            mplew.writeInt(i);
        }
        mplew.writeInt(setScore);
        mplew.writeInt(currentSet);
        mplew.writeInt(finishedSets);
    }

    public void writeFinished(MaplePacketLittleEndianWriter mplew) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        mplew.write(1);
        mplew.writeShort(cardItems.size());
        List<Integer> mbList = new ArrayList<>(ii.getMonsterBookList());
        Collections.sort(mbList);
        int fullCards = (mbList.size() / 8) + (mbList.size() % 8 > 0 ? 1 : 0);
        mplew.writeShort(fullCards); //which cards of all you have; more efficient than writing each card

        for (int i = 0; i < fullCards; i++) {
            int currentMask = 0x1, maskToWrite = 0;
            for (int y = (i * 8); y < ((i * 8) + 8); y++) {
                if (mbList.size() <= y) {
                    break;
                }
                if (cardItems.contains(mbList.get(y))) {
                    maskToWrite |= currentMask;
                }
                currentMask *= 2;
            }
            mplew.write(maskToWrite);
        }

        int fullSize = (cardItems.size() / 2) + (cardItems.size() % 2 > 0 ? 1 : 0);
        mplew.writeShort(fullSize); //i honestly don't know the point of this, is it to signify yes/no if you have the card or not?... which is already done...?
        for (int i = 0; i < fullSize; i++) {
            mplew.write(i == (cardItems.size() / 2) ? 1 : 0x11);
        }
    }

    public void writeUnfinished(MaplePacketLittleEndianWriter mplew) {
        mplew.write(0);
        mplew.writeShort(cardItems.size());
        for (int i : cardItems) {
            mplew.writeShort(i % 10000);
            mplew.write(1); //whether you have the card or not? idk
        }
    }

    public void calculateItem() {
        cardItems.clear();
        for (Entry<Integer, Integer> s : cards.entrySet()) {
            addCardItem(s.getKey(), s.getValue());
        }
    }

    public void addCardItem(int key, int value) {
        if (value >= 2) {
            Integer x = MapleItemInformationProvider.getInstance().getItemIdByMob(key);
            if (x != null && x > 0) {
                cardItems.add(x);
            }
        }
    }

    public void modifyBook(Equip eq) {
//        eq.setStr((short) level);
//        eq.setDex((short) level);
//        eq.setInt((short) level);
//        eq.setLuk((short) level);
//        eq.setPotential1((short) 0);
//        eq.setPotential2((short) 0);
//        eq.setPotential3((short) 0);
//        if (currentSet > -1) {
//            final Triple<Integer, List<Integer>, List<Integer>> set = MapleItemInformationProvider.getInstance().getMonsterBookInfo(currentSet);
//            if (set != null) {
//                for (int i = 0; i < set.right.size(); i++) {
//                    if (i == 0) {
//                        eq.setPotential1(set.right.get(i).shortValue());
//                    } else if (i == 1) {
//                        eq.setPotential2(set.right.get(i).shortValue());
//                    } else if (i == 2) {
//                        eq.setPotential3(set.right.get(i).shortValue());
//                        break;
//                    }
//                }
//            } else {
//                currentSet = -1;
//            }
//        }
    }

    public int getSetScore() {
        return setScore;
    }

    public int getLevel() {
        return level;
    }

    public int getSet() {
        return currentSet;
    }

    public boolean changeSet(int c) {
        if (sets.containsKey(c) && sets.get(c).right) {
            this.currentSet = c;
            return true;
        }
        return false;
    }

    public void changed() {
        changed = true;
    }

    public Map<Integer, Integer> getCards() {
        return cards;
    }

    public int getSeen() {
        return cards.size();
    }

    public int getCaught() {
        int ret = 0;
        for (int i : cards.values()) {
            if (i >= 2) {
                ret++;
            }
        }
        return ret;
    }

    public int getLevelByCard(int cardid) {
        return cards.get(cardid) == null ? 0 : cards.get(cardid);
    }

    public void saveCards(DruidPooledConnection con, int charid) throws SQLException {
        if (!changed) {
            return;
        }
        PreparedStatement ps = con.prepareStatement("DELETE FROM monsterbook WHERE charid = ?");
        ps.setInt(1, charid);
        ps.execute();
        ps.close();
        changed = false;
        if (cards.isEmpty()) {
            return;
        }

        boolean first = true;
        StringBuilder query = new StringBuilder();

        for (Entry<Integer, Integer> all : cards.entrySet()) {
            if (first) {
                first = false;
                query.append("INSERT INTO monsterbook VALUES (DEFAULT,");
            } else {
                query.append(",(DEFAULT,");
            }
            query.append(charid);
            query.append(",");
            query.append(all.getKey()); // Card ID
            query.append(",");
            query.append(all.getValue()); // Card level
            query.append(")");
        }
        ps = con.prepareStatement(query.toString());
        ps.execute();
        ps.close();
    }

    public boolean monsterCaught(MapleClient c, int cardid, String cardname) {
        if (!cards.containsKey(cardid) || cards.get(cardid) < 2) {
            changed = true;
            c.getPlayer().dropMessage(-6, "Book entry updated - " + cardname);
            //c.announce(MaplePacketCreator.showSpecialEffect(16));
            cards.put(cardid, 2);
            return true;
        }
        return false;
    }

    public void monsterSeen(MapleClient c, int cardid, String cardname) {
        if (cards.containsKey(cardid)) {
            return;
        }
        changed = true;
        // New card
        c.getPlayer().dropMessage(-6, "New book entry - " + cardname);
        cards.put(cardid, 1);
        //c.announce(MaplePacketCreator.showSpecialEffect(16));
    }
}
