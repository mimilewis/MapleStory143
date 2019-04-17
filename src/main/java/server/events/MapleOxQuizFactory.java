package server.events;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import tools.Pair;
import tools.Randomizer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapleOxQuizFactory {

    private static final MapleOxQuizFactory instance = new MapleOxQuizFactory();
    private final Map<Pair<Integer, Integer>, MapleOxQuizEntry> questionCache = new HashMap<>();

    public MapleOxQuizFactory() {
        initialize();
    }

    public static MapleOxQuizFactory getInstance() {
        return instance;
    }

    public Entry<Pair<Integer, Integer>, MapleOxQuizEntry> grabRandomQuestion() {
        int size = questionCache.size();
        while (true) {
            for (Entry<Pair<Integer, Integer>, MapleOxQuizEntry> oxquiz : questionCache.entrySet()) {
                if (Randomizer.nextInt(size) == 0) {
                    return oxquiz;
                }
            }
        }
    }

    private void initialize() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM wz_oxdata");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                questionCache.put(new Pair<>(rs.getInt("questionset"), rs.getInt("questionid")), get(rs));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MapleOxQuizEntry get(ResultSet rs) throws SQLException {
        return new MapleOxQuizEntry(rs.getString("question"), rs.getString("display"), getAnswerByText(rs.getString("answer")), rs.getInt("questionset"), rs.getInt("questionid"));
    }

    private int getAnswerByText(String text) {
        if (text.equalsIgnoreCase("x")) {
            return 0;
        } else if (text.equalsIgnoreCase("o")) {
            return 1;
        } else {
            return -1;
        }
    }

    public static class MapleOxQuizEntry {

        private final String question;
        private final String answerText;
        private final int answer;
        private final int questionset;
        private final int questionid;

        public MapleOxQuizEntry(String question, String answerText, int answer, int questionset, int questionid) {
            this.question = question;
            this.answerText = answerText;
            this.answer = answer;
            this.questionset = questionset;
            this.questionid = questionid;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswerText() {
            return answerText;
        }

        public int getAnswer() {
            return answer;
        }

        public int getQuestionSet() {
            return questionset;
        }

        public int getQuestionId() {
            return questionid;
        }
    }
}
