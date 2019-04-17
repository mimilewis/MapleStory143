package server.console.groups.boss;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

public class BossManagePanelTest {

//    @Before
//    public void setUp() throws Exception {
//        BossManagePanel.load();
//    }

    @Test
    public static void main(String[] arge) throws JsonProcessingException {
        BossManagePanel bossManagePanel = new BossManagePanel(null);
        bossManagePanel.pack();
        bossManagePanel.setLocationRelativeTo(null);
        bossManagePanel.setVisible(true);
//        Map<String, Map<String, String>> testMapToJson = new HashMap<>();
//        Map<String, String> submap = new HashMap<>();
//        submap.put("是否开放", "true");
//        submap.put("是否使用自定义", "true");
//        testMapToJson.put("扎昆", submap);
//        testMapToJson.put("黑龙", submap);
//        testMapToJson.put("品克缤", submap);
//        System.out.println(JsonUtil.getMapperInstance().writeValueAsString(testMapToJson));
    }

}