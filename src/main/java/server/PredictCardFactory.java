/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Randomizer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author PlayDK
 */
public class PredictCardFactory {

    private static final PredictCardFactory instance = new PredictCardFactory();
    protected final MapleDataProvider etcData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));
    protected final Map<Integer, PredictCard> predictCard = new HashMap<>();
    protected final Map<Integer, PredictCardComment> predictCardComment = new HashMap<>();

    public static PredictCardFactory getInstance() {
        return instance;
    }

    public void initialize() {
        if (!predictCard.isEmpty() || !predictCardComment.isEmpty()) {
            return;
        }
        MapleData infoData = etcData.getData("PredictCard.img");
        PredictCard card;
        for (MapleData cardDat : infoData) {
            if (cardDat.getName().equals("comment")) {
                continue;
            }
            card = new PredictCard();
            card.name = MapleDataTool.getString("name", cardDat, "");
            card.comment = MapleDataTool.getString("comment", cardDat, "");
            predictCard.put(Integer.parseInt(cardDat.getName()), card);
        }
        PredictCardComment comment;
        MapleData commentData = infoData.getChildByPath("comment");
        for (MapleData commentDat : commentData) {
            comment = new PredictCardComment();
            comment.worldmsg0 = MapleDataTool.getString("0", commentDat, "");
            comment.worldmsg1 = MapleDataTool.getString("1", commentDat, "");
            comment.score = MapleDataTool.getIntConvert("score", commentDat, 0);
            comment.effectType = MapleDataTool.getIntConvert("effectType", commentDat, 0);
            predictCardComment.put(Integer.parseInt(commentDat.getName()), comment);
        }
    }

    public PredictCard getPredictCard(int id) {
        if (!predictCard.containsKey(id)) {
            return null;
        }
        return predictCard.get(id);
    }

    public PredictCardComment getPredictCardComment(int id) {
        if (!predictCardComment.containsKey(id)) {
            return null;
        }
        return predictCardComment.get(id);
    }

    public PredictCardComment RandomCardComment() {
        return getPredictCardComment(Randomizer.nextInt(predictCardComment.size()));
    }

    public int getCardCommentSize() {
        return predictCardComment.size();
    }

    public static class PredictCard {

        public String name, comment;
    }

    public static class PredictCardComment {

        public int score, effectType;
        public String worldmsg0, worldmsg1;
    }
}
