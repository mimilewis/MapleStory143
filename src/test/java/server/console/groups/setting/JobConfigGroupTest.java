package server.console.groups.setting;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 免责声明：本模拟器源代码下载自ragezone.com，仅用于技术研究学习，无任何商业行为。
 */
public class JobConfigGroupTest {

    @Test
    public void splitString() throws Exception {
        String desc = "HP消耗#hpRCon%，每秒HP消耗#y，恢复道具和技能的恢复量限定为最大HP的#w%\\n根据最大HP对比所消耗的HP的#u%，最终伤害增加#x%，以一定周期将魔族之血洒在地上，#s秒内最多#mobCount个敌人造成伤害，每隔一定周期以#damage%的伤害发起#attackCount次攻击\\n如果当前HP低于最大HP的#q2%，将不消耗HP，也不会洒出魔族之血\\n再次使用时禁用技能\\n冷却时间#z秒";
//        String[] split = desc.split("#");
//        List<String> ret = new ArrayList<>();
//        for (String s : split) {
//            StringBuilder temp = new StringBuilder();
//            for (char c : s.toCharArray()) {
//                if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
//                    temp.append(c);
//                }
//            }
//            if (temp.length() != 0) {
//                ret.add(temp.toString());
//            }
//        }

        boolean find = false;
        List<String> ret = new ArrayList<>();
        StringBuilder temp = new StringBuilder();
        for (char c : desc.toCharArray()) {
            if (c == '#') {
                find = true;
                continue;
            }
            if (find) {
                if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9')) {
                    find = false;
                    ret.add(temp.toString());
                    temp = new StringBuilder();
                } else {
                    temp.append(c);
                }
            }
        }
        System.out.println(ret);
    }
}