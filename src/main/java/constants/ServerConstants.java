package constants;

import configs.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.PropertyTool;
import tools.Triple;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ServerConstants {

    //    public static final short SERVER_VERSION = 142;
//    public static final String SERVER_PATCH = "1";
    public static final int MIN_MTS = 150; //lowest amount an item can be, GMS = 110
    public static final int MTS_BASE = 0; //+amount to everything, GMS = 500, MSEA = 1000
    public static final int MTS_TAX = 5; //+% to everything, GMS = 10
    public static final int MTS_MESO = 2500; //拍卖中的手续费用
    //master login is only used in GMS: fake account for localhost only
    //master and master2 is to bypass all accounts passwords only if you are under the IPs below
    public static final List<String> localhostIP = new LinkedList<>(), vpnIp = new LinkedList<>();
    public static final int MAXIMUM_CONNECTIONS = 1000;
    public static final boolean GMS = false;
    // IP地址 221.231.130.70
    public static final byte[] NEXON_IP = new byte[]{(byte) 0xDD, (byte) 0xE7, (byte) 0x82, (byte) 0x46}; //singapore
    // job.length = 23
    public static final String[] JOB_NAMELIST = {"反抗者", "冒险家", "骑士团", "战神", "龙神", "双弩精灵", "恶魔", "幻影", "暗影双刀", "米哈尔", "夜光法师", "狂龙战士", "爆莉萌天使", "火炮手", "尖兵", "神之子", "隐月", "品克缤", "超能力者", "林之灵", "龙的传人", "剑豪", "阴阳师"};
    public static final Triple<String, Integer, Boolean>[] backgrounds = new Triple[]{};
    private static final Logger log = LogManager.getLogger(ServerConstants.class.getName());
    // 封包配置文件
    private static final Map<String, Boolean> blockedOpcodes = new HashMap<>();
    private static final Map<String, Boolean> blockedMapFM = new HashMap<>(); // 禁止显示给其他角色的技能
    public static boolean TESPIA = false; // true = uses GMS test server, for MSEA it does nothing though
    public static String HOST = "221.231.130.70";
    //Inject a DLL that hooks SetupDiGetClassDevsExA and returns 0.
    // Start of Poll
    public static boolean PollEnabled = false;
    public static String Poll_Question = "Are you mudkiz?";
    public static String[] Poll_Answers = {"test1", "test2", "test3"};
    private static boolean showGMMessage = false;

    static {
        localhostIP.add("221.231.130.70");
        for (int i = 0; i < 256; i++) {
            vpnIp.add("221.231.130." + i);
        }
        for (int i = 0; i < 256; i++) {
            vpnIp.add("17.1.1." + i);
        }
        for (int i = 0; i < 256; i++) {
            vpnIp.add("17.1.2." + i);
        }
    }

    public static boolean isIPLocalhost(String sessionIP) {
        return localhostIP.contains(sessionIP.replace("/", ""));
    }

    public static boolean isVpn(String sessionIP) {
        return vpnIp.contains(sessionIP.replace("/", ""));
    }

    /**
     * 加载禁止使用FM命令的地图列表
     */
    public static void loadBlockedMapFM() {
        blockedMapFM.clear();
        Properties settings = new Properties();
        try {
            FileInputStream fis = new FileInputStream("config\\blockMapFM.ini");
            settings.load(fis);
            fis.close();
        } catch (IOException ex) {
            log.error("加载 blockMapFM.ini 配置出错", ex);
        }
        PropertyTool propTool = new PropertyTool(settings);
        for (Map.Entry<Object, Object> entry : settings.entrySet()) {
            String property = (String) entry.getKey();
            blockedMapFM.put(property, propTool.getSettingInt(property, 0) > 0);
        }
    }

    /**
     * 检测这个技能是否禁止显示
     */
    public static boolean isBlockedMapFM(int skillId) {
        if (blockedMapFM.containsKey(String.valueOf(skillId))) {
            return blockedMapFM.get(String.valueOf(skillId));
        }
        return false;
    }

    public static boolean isShowPacket() {
        return ServerConfig.DEBUG_MODE;
    }

    public static boolean isShowGMMessage() {
        return showGMMessage;
    }

    public static void setShowGMMessage(boolean b) {
        showGMMessage = b;
    }

    public static boolean isPvpMap(int mapid) {
        return ServerConfig.CHANNEL_PVPMAPS.indexOf(mapid) != -1;
    }

    public static int getMTSNpcID() {
        return Integer.valueOf(ServerConfig.CHANNEL_ENTERNPC_MTS.split("_")[0]);
    }

    public static String getMTSNpcID_Mode() {
        return ServerConfig.CHANNEL_ENTERNPC_MTS.contains("_") ? ServerConfig.CHANNEL_ENTERNPC_MTS.split("_")[1] : null;
    }

    public static int getCSNpcID() {
        if (ServerConfig.CHANNEL_ENTERNPC_CASHSHOP.isEmpty()) {
            return 0;
        }
        return Integer.valueOf(ServerConfig.CHANNEL_ENTERNPC_CASHSHOP.split("_")[0]);
    }

    public static String getCSNpcID_Mode() {
        return ServerConfig.CHANNEL_ENTERNPC_CASHSHOP.contains("_") ? ServerConfig.CHANNEL_ENTERNPC_CASHSHOP.split("_")[1] : null;
    }

    public static boolean isOpenJob(String jobname) {
        return !ServerConfig.WORLD_CLOSEJOBS.contains(jobname);
    }

    public enum MapleServerType {

        UNKNOWN(-1),
        JAPAN(3),
        CHINA(4),
        TEST(5),
        TAIWAN(6),
        SEA(7),
        GLOBAL(8),
        BRAZIL(9);
        final byte type;

        MapleServerType(int type) {
            this.type = (byte) type;
        }

        public static MapleServerType getByType(byte type) {
            for (MapleServerType l : MapleServerType.values()) {
                if (l.getType() == type) {
                    return l;
                }
            }
            return UNKNOWN;
        }

        public byte getType() {
            return type;
        }
    }

    public enum MapleStatusInfo {

        获得道具(0x00),
        更新任务状态(0x01),
        商城道具到期(0x03),
        获得经验(0x04),
        获得SP(0x05),
        获得人气(0x06),
        获得金币(0x07),
        获得家族点(0x08),
        获得贡献度(0x09),
        显示消耗品描述(0x0A),
        非商城道具到期(0x0B),
        系统红字公告(0x0C),
        更新任务信息(0x0E),
        更新任务信息2(0x0F),
        技能到期(0x13),
        获得倾向熟练度(0x14),
        超过今天可获得倾向熟练度(0x15),
        移除机器人心脏(0x17),
        休息后恢复了疲劳度(0x18),
        系统灰字公告(0x1A),
        配偶提示(0x1B),
        神之子不能获得金币(0x23),
        获得WP(0x24),
        不能获得更多的WP(0x25),
        连续击杀(0x26),;

        final byte type;

        MapleStatusInfo(int type) {
            this.type = (byte) type;
        }

        public byte getType() {
            return type;
        }
    }


    public enum MapleServerName {
        蓝蜗牛,
        蘑菇仔,
        绿水灵,
        漂漂猪,
        小青蛇,
        红螃蟹,
        大海龟,
        章鱼怪,
        顽皮猴,
        星精灵,
        胖企鹅,
        白雪人,
        石头人,
        紫色猫,
        大灰狼,
        小白兔,
        喷火龙,
        火野猪,
        青鳄鱼,
        花蘑菇,
        祖母绿(0x1E),
        黑水晶,
        水晶钻,
        黄水晶,
        蓝水晶,
        紫水晶,
        海蓝石,
        蛋白石,
        石榴石,
        月石川,
        星石申,
        黄金粤,
        黑珍珠,
        猫眼石,
        火星石,
        玛丽亚(0x3C),
        阿勒斯,
        薇薇安,
        月妙,
        七星剑(0x5A),
        明珠港(0x78),
        童话村,
        玩具城,
        里恩,
        飞花院,
        埃德尔,
        斯坦,
        匠人街,
        神木村,
        埃欧雷,
        阿斯旺,
        万神殿,
        镜世界;

        private final int value;

        MapleServerName() {
            this(Counter.nextvalue);
        }

        MapleServerName(int value) {
            this.value = value;
            Counter.nextvalue = value + 1;
        }

        public static int getValueByOrdinal(int ordinal) {
            if (ordinal > values().length || ordinal < 0) {
                ordinal = 0;
            }
            return values()[ordinal].getValue();
        }

        public int getValue() {
            return value;
        }

        private static class Counter {
            private static int nextvalue = 0;
        }
    }

    public enum MapleServerStatus {
        无,
        活动,
        新服,
        热门
    }
}
