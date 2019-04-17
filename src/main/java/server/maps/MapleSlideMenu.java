package server.maps;

import client.MapleCharacter;

public class MapleSlideMenu {

    public static Class<?> getSlideMenu(int id) {
        final Class<?>[] slideMenus = new Class<?>[]{SlideMenu0.class,
                SlideMenu1.class, SlideMenu2.class, SlideMenu3.class,
                SlideMenu4.class, SlideMenu5.class, SlideMenu6.class};
        return slideMenus[id];
    }

    public static class SlideMenu0 {

        public static String getSelectionInfo(MapleCharacter player, Integer npc) {
            StringBuilder mapselect = new StringBuilder();
            for (DimensionalMirror mirror : DimensionalMirror.values()) {
                if (player.getLevel() >= mirror.getMinLevel() && player.getLevel() <= mirror.getMaxLevel() && mirror.isShow()) {
                    if ((player.getQuestStatus(mirror.getRequieredQuest()) >= mirror.getRequieredQuestState()) || mirror.getRequieredQuest() == 0) {
                        if (mirror != DimensionalMirror.DEFAULT) {
                            mapselect.append("#").append(mirror.getId()).append("#").append(mirror.getName());
                        }
                    }
                }
            }
            if ((mapselect.length() == 0) || mapselect.toString().equals("")) {
                mapselect = new StringBuilder("#-1#没有可以通过次元之镜移动的地方。");
            }
            if (npc == 9201231) {
                mapselect = new StringBuilder("#87# Crack in the Dimensional Mirror");
            }
            return mapselect.toString();
        }

        public static int[] getDataInteger(Integer id) {
            DimensionalMirror mirror = DimensionalMirror.getById(id);
            return new int[]{mirror.getMap(), mirror.getPortal()};
        }

        public enum DimensionalMirror {
            PQ0(0, "阿里安特竞技场", 682020000, 3, 20, 30, 0, 0, false),
            PQ1(1, "武陵道场", 925020000, 4, 120, 255, 0, 0, true),
            PQ2(2, "怪物嘉年华", 980000000, 4, 30, 50, 0, 0, false),
            PQ3(3, "怪物嘉年华", 980030000, 4, 50, 255, 0, 0, true),
            PQ4(4, "雾海", 923020000, 0, 120, 255, 0, 0, true),
            PQ5(5, "奈特的金字塔", 926010000, 4, 60, 109, 0, 0, true),
            PQ6(6, "废都广场", 910320000, 2, 25, 30, 0, 0, false),
            PQ7(7, "幸福村", 209000000, 0, 13, 255, 0, 0, false),
            PQ8(8, "黄金寺院2", 950100000, 9, 110, 255, 0, 0, false),
            PQ9(9, "10周年大赏纪念冒险岛公园", 706040000, 0, 30, 255, 0, 0, true),
            PQ10(10, "第一次同行", 910340700, 0, 50, 255, 0, 0, false),
            PQ11(11, "玩具城组队任务", 221023300, 2, 50, 255, 0, 0, false),
            PQ12(12, "毒雾深林", 300030100, 1, 70, 255, 0, 0, false),
            PQ13(13, "女神的痕迹", 200080101, 1, 70, 255, 0, 0, false),
            PQ14(14, "海盗船组队任务", 251010404, 2, 70, 255, 0, 0, false),
            PQ15(15, "罗密欧和朱丽叶", 261000021, 5, 70, 255, 0, 0, false),
            PQ16(16, "侏儒怪帝王的复活", 211000002, 0, 120, 255, 0, 0, false),
            PQ17(17, "御龙魔", 240080000, 2, 120, 255, 0, 0, false),
            PQ18(18, "活动地图", 0, 0, 10, 255, 0, 0, false),
            PQ19(19, "万圣节的树", 682000000, 0, 120, 255, 0, 0, false),
            PQ20(20, "活动地图2", 0, 0, 10, 255, 0, 0, false),
            PQ21(21, "陷入危险的坎特", 923040000, 0, 120, 255, 0, 0, false),
            PQ22(22, "逃脱", 921160000, 0, 120, 255, 0, 0, false),
            PQ23(23, "冰骑士的诅咒", 932000000, 0, 20, 255, 0, 0, false),
            PQ24(24, "活动地图3", 0, 0, 10, 255, 0, 0, false),
            PQ25(25, "冒险岛联盟会议场", 913050010, 0, 75, 255, 0, 0, true),
            PQ26(26, "万圣节的树2", 682000700, 0, 10, 255, 0, 0, false),
            PQ27(27, "阿斯旺解放战", 262010000, 0, 40, 255, 0, 0, true),
            PQ28(28, "黄金寺院", 950100000, 9, 105, 255, 0, 0, false),
            PQ29(29, "休彼得曼的画廊", 0, 0, 50, 120, 0, 0, false),
            PQ30(30, "大乱斗", 960000000, 0, 30, 255, 0, 0, true),
            PQ31(31, "古代神社", 800000000, 0, 10, 255, 0, 0, true),
            PQ32(32, "进化系统研究所", 957000000, 0, 105, 255, 1802, 1, true),
            PQ33(33, "次元入侵", 940020000, 0, 140, 255, 0, 0, true),
            PQ34(34, "休彼德蔓的客房(初级)", 910002000, 2, 50, 255, 0, 0, false),
            PQ35(35, "休彼德蔓的客房(中级)", 910002010, 2, 70, 255, 0, 0, false),
            PQ36(36, "休彼德蔓的客房(高级)", 910002020, 2, 10, 255, 0, 0, false),
            PQ37(37, "唐云", 0, 0, 70, 255, 0, 0, false),
            PQ38(38, "克林逊森林城堡", 301000000, 0, 130, 255, 0, 0, true),
            PQ39(39, "次元图书馆", 302000000, 0, 100, 125, 0, 0, true),
            PQ40(40, "组队任务综合入场", 910002000, 0, 50, 255, 0, 0, true),
            PQ41(41, "世界综合组队任务", 708001000, 0, 70, 255, 0, 0, true),
            PQ42(42, "鲁塔比斯", 105200000, 0, 125, 255, 0, 0, true),
            PQ43(43, "起源之塔", 992000000, 0, 100, 255, 0, 0, true),
            PQ44(44, "好友故事", 0, 0, 100, 255, 0, 0, false),
            PQ45(45, "怪物公园", 951000000, 0, 75, 255, 0, 0, true),
            PQ46(46, "鬼魂公园", 956100000, 0, 125, 255, 0, 0, true),
            PQ47(47, "霸王乌鲁斯", 970072200, 0, 100, 255, 0, 0, true),
            PQ48(48, "简单希纳斯", 0, 0, 100, 255, 0, 0, false),
            PQ200(200, "外星访客", 861000000, 0, 200, 255, 0, 0, true),
            PQ201(201, "变了的水下世界", 860000000, 6, 200, 255, 0, 0, true),
            PQ510(510, "维拉森特", 0, 0, 0, 255, 0, 0, false),
            PQ525(525, "未知1", 0, 0, 0, 255, 0, 0, false),
            PQ526(526, "未知2", 0, 0, 0, 255, 0, 0, false),
            PQ800(800, "西式婚礼", 680000000, 0, 0, 255, 0, 0, true),
            PQ801(801, "海外旅游", 950000000, 0, 0, 255, 0, 0, true),
            PQ802(802, "中式婚礼2", 700000000, 0, 0, 255, 0, 0, false),
            PQ803(803, "新叶城", 600000000, 0, 0, 255, 0, 0, true),
            PQ804(804, "中式婚礼", 700000000, 0, 0, 255, 0, 0, false),
            PQ805(805, "VIP休息室", 704000000, 0, 0, 255, 0, 0, false),
            PQ806(806, "未知4", 0, 0, 0, 255, 0, 0, false),
            PQ807(807, "天空庭院", 706020100, 0, 13, 255, 0, 0, true),
            PQ808(808, "未知6", 0, 0, 0, 255, 0, 0, false),
            PQ809(809, "光荣大厅", 710000000, 0, 0, 255, 0, 0, true),
            PQ810(810, "未知7", 0, 0, 0, 255, 0, 0, false),
            PQ811(811, "未知8", 0, 0, 0, 255, 0, 0, false),
            PQ812(812, "未知9", 0, 0, 0, 255, 0, 0, false),
            PQ813(813, "未知10", 0, 0, 0, 255, 0, 0, false),
            PQ814(814, "未知10", 0, 0, 0, 255, 0, 0, false),
            DEFAULT(Integer.MAX_VALUE, "Default Map", 999999999, 0, 0, 0, 0, 0, false);

            private final int id;
            private final int map;
            private final int minLevel;
            private final int maxLevel;
            private final int requieredQuest;
            private final int requieredQuestState;
            private final String name;
            private final boolean show;
            private final int portal;

            DimensionalMirror(int id, String name, int map, int portal, int minLevel, int maxLevel, int requieredQuest, int requieredQuestState, boolean show) {
                this.id = id;
                this.name = name;
                this.map = map;
                this.portal = portal;
                this.minLevel = minLevel;
                this.maxLevel = maxLevel;
                this.requieredQuest = requieredQuest;
                this.requieredQuestState = requieredQuestState;
                this.show = show;
            }

            public static DimensionalMirror getById(int id) {
                for (DimensionalMirror mirror : DimensionalMirror.values()) {
                    if (mirror.getId() != id) continue;
                    return mirror;
                }
                return DEFAULT;
            }

            public int getId() {
                return this.id;
            }

            public String getName() {
                return this.name;
            }

            public int getMap() {
                return map;
            }

            public int getMinLevel() {
                return minLevel;
            }

            public int getMaxLevel() {
                return maxLevel;
            }

            public int getRequieredQuest() {
                return requieredQuest;
            }

            public int getRequieredQuestState() {
                return requieredQuestState;
            }

            public boolean isShow() {
                return show;
            }

            public int getPortal() {
                return portal;
            }
        }

    }

    public static class SlideMenu1 {

        public static String getSelectionInfo(MapleCharacter player, Integer npc) {
            String string = "";
            for (TimeGate gate : TimeGate.values()) {
                if (player.getQuestStatus(gate.getRequieredQuest()) != gate.getRequieredQuestState() && gate.getRequieredQuest() != 0)
                    continue;
                string = string + "#" + gate.getId() + "#" + gate.getName();
            }
            if (string.isEmpty()) {
                string = "#-1# There are no locations you can move to.";
            }
            return string;
        }

        public static int[] getDataInteger(Integer id) {
            TimeGate gate = TimeGate.kz(id);
            int[] arrn = new int[2];
            arrn[0] = gate != null ? gate.getMap() : TimeGate.YEAR_2099.getMap();
            arrn[1] = 0;
            return arrn;
        }

        public enum TimeGate {
            YEAR_2021(1, "Year 2021, Average Town", 0, 0, 0, 0),
            YEAR_2099(2, "Year 2099, Midnight Harbor", 0, 0, 0, 0),
            YEAR_2215(3, "Year 2215, Bombed City Center", 0, 0, 0, 0),
            YEAR_2216(4, "Year 2216, Ruined City", 0, 0, 0, 0),
            YEAR_2230(5, "Year 2230, Dangerous Tower", 0, 0, 0, 0),
            YEAR_2503(6, "Year 2503, Air Battleship Hermes", 0, 0, 0, 0);

            private final int id, map, portal, requieredQuest, requieredQuestState;
            private final String name;

            TimeGate(int id, String name, int map, int portal, int requieredQuest, int requieredQuestState) {
                this.id = id;
                this.name = name;
                this.map = map;
                this.requieredQuest = requieredQuest;
                this.requieredQuestState = requieredQuestState;
                this.portal = portal;
            }

            public static TimeGate kz(int n2) {
                for (TimeGate timeGate2 : TimeGate.values()) {
                    if (timeGate2.getId() != n2) continue;
                    return timeGate2;
                }
                return null;
            }

            public int getId() {
                return this.id;
            }

            public String getName() {
                return this.name;
            }

            public int getMap() {
                return map;
            }

            public int getPortal() {
                return portal;
            }

            public int getRequieredQuest() {
                return requieredQuest;
            }

            public int getRequieredQuestState() {
                return requieredQuestState;
            }
        }

    }

    public static class SlideMenu2 {
        public static String getSelectionInfo(MapleCharacter player, Integer npc) {
            String string = "";
            for (a a2 : a.values()) {
                string = string + "#" + a2.getId() + "#" + a2.getName();
            }
            if ("".equals(string)) {
                string = "#-1# There are no locations you can move to.";
            }
            return string;
        }

        public static int[] getDataInteger(Integer n2) {
            a a2 = a.kA(n2);
            int[] arrn = new int[2];
            arrn[0] = a2 != null ? a2.jy() : -1;
            arrn[1] = a2 != null ? a2.jz() : -1;
            return arrn;
        }

        public enum a {
            bsV(0, "射手村", 100000000, 0),
            bsW(1, "魔法密林", 101000000, 0),
            bsX(2, "勇士部落", 102000000, 0),
            bsY(3, "废弃都市", 103000000, 0),
            bsZ(4, "明珠港", 104000000, 0),
            bta(5, "林中之城", 105000000, 0),
            btb(6, "诺特勒斯号", 120000100, 0),
            btc(7, "圣地", 130000000, 0),
            btd(8, "里恩", 140000000, 0),
            bte(9, "天空之城", 200000000, 0),
            btf(10, "冰封雪域", 211000000, 0),
            btg(11, "玩具城", 220000000, 0),
            bth(14, "水下世界", 230000000, 0),
            bti(15, "神木村", 240000000, 0),
            btj(16, "武陵", 250000000, 0),
            btk(17, "百草堂", 251000000, 0),
            btl(18, "阿里安特", 260000000, 0),
            btm(19, "玛加提亚", 261000000, 0),
            btn(20, "埃德尔斯坦", 310000000, 0);

            private final int id;
            private final int bsJ;
            private final int bsM;
            private final String name;

            a(int n3, String string2, int n4, int n5) {
                this.id = n3;
                this.name = string2;
                this.bsJ = n4;
                this.bsM = n5;
            }

            public static a kA(int n2) {
                for (a a2 : a.values()) {
                    if (a2.getId() != n2) continue;
                    return a2;
                }
                return null;
            }

            public int getId() {
                return this.id;
            }

            public String getName() {
                return this.name;
            }

            public int jy() {
                return this.bsJ;
            }

            public int jz() {
                return this.bsM;
            }
        }

    }

    public static class SlideMenu3 {
        public static String getSelectionInfo(MapleCharacter player, Integer npc) {
            String string = "";
            for (a a2 : a.values()) {
                string = string + "#" + a2.getId() + "#" + a2.getName();
            }
            if ("".equals(string)) {
                string = "#-1# There are no locations you can move to.";
            }
            return string;
        }

        public static int[] getDataInteger(Integer n2) {
            a a2 = a.kB(n2);
            int[] arrn = new int[2];
            arrn[0] = a2 != null ? a2.jy() : -1;
            arrn[1] = a2 != null ? a2.jz() : -1;
            return arrn;
        }

        public enum a {
            btp(0, "射手村", 100000000, 0),
            btq(1, "魔法密林", 101000000, 0),
            btr(2, "勇士部落", 102000000, 0),
            bts(3, "废弃都市", 103000000, 0),
            btt(4, "明珠港", 104000000, 0),
            btu(5, "林中之城", 105000000, 0),
            btv(6, "诺特勒斯号", 120000100, 0),
            btw(7, "圣地", 130000000, 0),
            btx(8, "里恩", 140000000, 0),
            bty(9, "天空之城", 200000000, 0),
            btz(10, "冰封雪域", 211000000, 0),
            btA(11, "玩具城", 220000000, 0),
            btB(14, "水下世界", 230000000, 0),
            btC(15, "神木村", 240000000, 0),
            btD(16, "武陵", 250000000, 0),
            btE(17, "百草堂", 251000000, 0),
            btF(18, "阿里安特", 260000000, 0),
            btG(19, "玛加提亚", 261000000, 0),
            btH(20, "埃德尔斯坦", 310000000, 0),
            btI(21, "埃欧雷", 101050000, 0),
            btJ(22, "万神殿", 400000000, 0);

            private final int id;
            private final int bsJ;
            private final int bsM;
            private final String name;

            a(int n3, String string2, int n4, int n5) {
                this.id = n3;
                this.name = string2;
                this.bsJ = n4;
                this.bsM = n5;
            }

            public static a kB(int n2) {
                for (a a2 : a.values()) {
                    if (a2.getId() != n2) continue;
                    return a2;
                }
                return null;
            }

            public int getId() {
                return this.id;
            }

            public String getName() {
                return this.name;
            }

            public int jy() {
                return this.bsJ;
            }

            public int jz() {
                return this.bsM;
            }
        }

    }

    public static class SlideMenu4 {
        public static String getSelectionInfo(MapleCharacter player, Integer npc) {
            String buffselect = "";
            for (BuffMenu buff : BuffMenu.values()) {
                buffselect = buffselect + "#" + buff.getId() + "# " + buff.getName();
            }
            return buffselect;
        }

        public static int[] getDataInteger(Integer n2) {
            BuffMenu buff = BuffMenu.getById(n2);
            int[] arrn = new int[2];
            arrn[0] = buff != null ? buff.jD() : BuffMenu.BUFF0.jD();
            arrn[1] = 0;
            return arrn;
        }

        public enum BuffMenu {
            BUFF0(0, "Recover 50% HP", 2022855),
            BUFF1(1, "Recover 100% HP", 2022856),
            BUFF2(2, "MaxHP + 10000 (Duration: 10 min)", 2022857),
            BUFF3(3, "Weapon/Magic ATT + 30 (Duration: 10 min)", 2022858),
            BUFF4(4, "Weapon/Magic ATT + 60 (Duration: 10 min)", 2022859),
            BUFF5(5, "Weapon/Magic DEF + 2500 (Duration: 10 min)", 2022860),
            BUFF6(6, "Weapon/Magic DEF + 4000 (Duration: 10 min)", 2022861),
            BUFF7(7, "Accuracy/Avoidability + 2000 (Duration: 10 min)", 2022862),
            BUFF8(8, "Speed/Jump MAX (Duration: 10 min)", 2022863),
            BUFF9(9, "Attack Speed + 1 (Duration: 10 min)", 2022864);

            private final int id, buff;
            private final String name;

            BuffMenu(int id, String name, int buff) {
                this.id = id;
                this.name = name;
                this.buff = buff;
            }

            public static BuffMenu getById(int n2) {
                for (BuffMenu buff : BuffMenu.values()) {
                    if (buff.getId() != n2) continue;
                    return buff;
                }
                return null;
            }

            public int getId() {
                return this.id;
            }

            public String getName() {
                return this.name;
            }

            public int jD() {
                return this.buff;
            }
        }

    }

    public static class SlideMenu5 {
        public static String getSelectionInfo(MapleCharacter player, Integer npc) {
            String string = "";
            for (TownTeleport townTeleport2 : TownTeleport.values()) {
                string = string + "#" + townTeleport2.getId() + "#" + townTeleport2.getName();
            }
            if ("".equals(string)) {
                string = "#-1# There are no locations you can move to.";
            }
            return string;
        }

        public static int[] getDataInteger(Integer id) {
            TownTeleport teleport = TownTeleport.kD(id);
            int[] arrn = new int[2];
            arrn[0] = teleport != null ? teleport.getMap() : -1;
            arrn[1] = teleport != null ? teleport.getPortal() : -1;
            return arrn;
        }

        public enum TownTeleport {
            btX(0, "六岔路口", 104020000, 2),
            btY(1, "射手村", 100000000, 0),
            btZ(2, "魔法密林", 101000000, 0),
            bua(3, "勇士部落", 102000000, 0),
            bub(4, "废弃都市", 103000000, 0),
            buc(5, "明珠港", 104000000, 0),
            bud(6, "林中之城", 105000000, 0),
            bue(7, "诺特勒斯号", 120000100, 0),
            buf(8, "圣地", 130000000, 0),
            bug(9, "里恩", 140000000, 0),
            buh(10, "天空之城", 200000000, 0),
            bui(11, "冰封雪域", 211000000, 0),
            buj(12, "玩具城", 220000000, 0),
            buk(15, "水下世界", 230000000, 0),
            bul(16, "神木村", 240000000, 0),
            bum(17, "武陵", 250000000, 0),
            bun(18, "百草堂", 251000000, 0),
            buo(19, "阿里安特", 260000000, 0),
            bup(20, "玛加提亚", 261000000, 0),
            buq(21, "埃德尔斯坦", 310000000, 0),
            bur(22, "埃欧雷", 101050000, 0),
            bus(24, "万神殿", 400000000, 0);

            private final int id, map, portal;
            private final String name;

            TownTeleport(int id, String name, int map, int portal) {
                this.id = id;
                this.name = name;
                this.map = map;
                this.portal = portal;
            }

            public static TownTeleport kD(int n2) {
                for (TownTeleport townTeleport2 : TownTeleport.values()) {
                    if (townTeleport2.getId() != n2) continue;
                    return townTeleport2;
                }
                return null;
            }

            public int getId() {
                return this.id;
            }

            public String getName() {
                return this.name;
            }

            public int getMap() {
                return map;
            }

            public int getPortal() {
                return portal;
            }
        }

    }

    public static class SlideMenu6 {
        public static String getSelectionInfo(MapleCharacter player, Integer npc) {
            String string = "";
            for (a a2 : a.values()) {
                string = string + "#" + a2.getId() + "#" + a2.getName();
            }
            if ("".equals(string)) {
                string = "#-1# There are no locations you can move to.";
            }
            return string;
        }

        public static int[] getDataInteger(Integer n2) {
            a a2 = a.kE(n2);
            int[] arrn = new int[2];
            arrn[0] = a2 != null ? a2.jy() : -1;
            arrn[1] = a2 != null ? a2.jz() : -1;
            return arrn;
        }

        public enum a {
            buu(0, "射手村", 100000000, 0),
            buv(1, "魔法密林", 101000000, 0),
            buw(2, "勇士部落", 102000000, 0),
            bux(3, "废弃都市", 103000000, 0),
            buy(4, "明珠港", 104000000, 0),
            buz(5, "林中之城", 105000000, 0),
            buA(6, "诺特勒斯号", 120000100, 0),
            buB(7, "圣地", 130000000, 0),
            buC(8, "里恩", 140000000, 0),
            buD(9, "天空之城", 200000000, 0),
            buE(10, "冰封雪域", 211000000, 0),
            buF(11, "玩具城", 220000000, 0),
            buG(14, "水下世界", 230000000, 17),
            buH(15, "神木村", 240000000, 0),
            buI(16, "武陵", 250000000, 0),
            buJ(17, "百草堂", 251000000, 0),
            buK(18, "阿里安特", 260000000, 0),
            buL(19, "玛加提亚", 261000000, 0),
            buM(20, "埃德尔斯坦", 310000000, 0),
            buN(21, "埃欧雷", 101050000, 0),
            buO(22, "万神殿", 400000000, 0);

            private final int id;
            private final int bsJ;
            private final int bsM;
            private final String name;

            a(int n3, String string2, int n4, int n5) {
                this.id = n3;
                this.name = string2;
                this.bsJ = n4;
                this.bsM = n5;
            }

            public static a kE(int n2) {
                for (a a2 : a.values()) {
                    if (a2.getId() != n2) continue;
                    return a2;
                }
                return null;
            }

            public int getId() {
                return this.id;
            }

            public String getName() {
                return this.name;
            }

            public int jy() {
                return this.bsJ;
            }

            public int jz() {
                return this.bsM;
            }
        }

    }
}
