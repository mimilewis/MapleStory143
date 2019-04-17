/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import constants.SkillConstants;
import server.CharacterCardFactory;
import tools.Pair;
import tools.Triple;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author PlayDK
 *         角色卡系统
 */
public class MapleCharacterCards {

    private final List<Pair<Integer, Integer>> skills = new ArrayList<>();  // 技能id, 技能等级
    private Map<Integer, CardData> cards = new LinkedHashMap<>();  // order

    public Map<Integer, CardData> getCards() {
        return cards;
    }

    public void setCards(Map<Integer, CardData> cads) {
        cards = cads;
    }

    public List<Pair<Integer, Integer>> getCardEffects() {
        return skills;
    }

    public void calculateEffects() {
        skills.clear(); // 重置角色卡所有技能
        int deck1amount = 0, deck2amount = 0, deck3amount = 0, reqRank1 = 0, reqRank2 = 0, reqRank3 = 0;
        List<Integer> cardSkillIds1 = new LinkedList<>();
        List<Integer> cardSkillIds2 = new LinkedList<>();
        List<Integer> cardSkillIds3 = new LinkedList<>();
        CharacterCardFactory cardFactory = CharacterCardFactory.getInstance();
        for (Entry<Integer, CardData> cardInfo : cards.entrySet()) {
            if (cardInfo.getValue().chrId > 0) { // 检测角色ID大于0才有
                //角色卡ID 技能ID 技能等级
                Triple<Integer, Integer, Integer> skillData = cardFactory.getCardSkill(cardInfo.getValue().job, cardInfo.getValue().level);
                if (cardInfo.getKey() < 4) { //卡片的位置也就是第1个卡片组合
                    if (skillData != null) {
                        cardSkillIds1.add(skillData.getLeft());
                        skills.add(new Pair<>(skillData.getMid(), skillData.getRight())); //添加技能 [ID] [等级]
                    }
                    deck1amount++;
                    if (reqRank1 == 0 || reqRank1 > cardInfo.getValue().level) {
                        reqRank1 = cardInfo.getValue().level; // take lowest
                    }
                } else if (cardInfo.getKey() > 3 && cardInfo.getKey() < 7) {
                    if (skillData != null) {
                        cardSkillIds2.add(skillData.getLeft());
                        skills.add(new Pair<>(skillData.getMid(), skillData.getRight())); //添加技能 [ID] [等级]
                    }
                    deck2amount++;
                    if (reqRank2 == 0 || reqRank2 > cardInfo.getValue().level) {
                        reqRank2 = cardInfo.getValue().level; // take lowest
                    }
                } else {
                    if (skillData != null) {
                        cardSkillIds3.add(skillData.getLeft());
                        skills.add(new Pair<>(skillData.getMid(), skillData.getRight())); //添加技能 [ID] [等级]
                    }
                    deck3amount++;
                    if (reqRank3 == 0 || reqRank3 > cardInfo.getValue().level) {
                        reqRank3 = cardInfo.getValue().level; // take lowest
                    }
                }
            }
        }

        if (deck1amount == 3 && cardSkillIds1.size() == 3) { //组合技能 需要有3张卡片且是在同1组才有
            List<Integer> uid = cardFactory.getUniqueSkills(cardSkillIds1); //获取组合卡片的技能ID
            for (int i : uid) {
                skills.add(new Pair<>(i, SkillConstants.getCardSkillLevel(reqRank1))); // 组合卡片
            }
            skills.add(new Pair<>(cardFactory.getRankSkill(reqRank1), 1));
        }
        if (deck2amount == 3 && cardSkillIds2.size() == 3) {
            List<Integer> uid = cardFactory.getUniqueSkills(cardSkillIds2);
            for (int i : uid) {
                skills.add(new Pair<>(i, SkillConstants.getCardSkillLevel(reqRank2)));
            }
            skills.add(new Pair<>(cardFactory.getRankSkill(reqRank2), 1));
        }
        if (deck3amount == 3 && cardSkillIds3.size() == 3) {
            List<Integer> uid = cardFactory.getUniqueSkills(cardSkillIds3);
            for (int i : uid) {
                skills.add(new Pair<>(i, SkillConstants.getCardSkillLevel(reqRank3)));
            }
            skills.add(new Pair<>(cardFactory.getRankSkill(reqRank3), 1));
        }
    }

    public void recalcLocalStats(MapleCharacter chr) {
        int pos = -1;
        for (Entry<Integer, CardData> x : cards.entrySet()) {
            if (x.getValue().chrId == chr.getId()) {
                pos = x.getKey();
                break;
            }
        }
        if (pos != -1) {
            if (!CharacterCardFactory.getInstance().canHaveCard(chr.getLevel(), chr.getJob())) {
                cards.remove(pos); // we don't need to reset pos as its not needed
            } else {
                cards.put(pos, new CardData(chr.getId(), chr.getLevel(), chr.getJob())); // override old
            }
        }
        calculateEffects(); // recalculate, just incase 
    }

    public void loadCards(MapleClient c, boolean channelserver) {
        cards = CharacterCardFactory.getInstance().loadCharacterCards(c.getAccID(), c.getWorld());
        if (channelserver) {
            calculateEffects();
        }
    }

    public void connectData(MaplePacketLittleEndianWriter mplew) {
        if (cards.isEmpty()) {  // we don't show for new characters 
            mplew.writeZeroBytes(9 * 9);
            return;
        }
        int poss = 0;
        for (CardData i : cards.values()) {
            poss++;
            if (poss > 9) {
                return;
            }
            mplew.writeInt(i.chrId);
            mplew.write(i.level);
            mplew.writeInt(i.job);
        }
    }
}
