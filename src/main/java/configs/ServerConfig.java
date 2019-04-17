package configs;

import tools.config.Property;

public final class ServerConfig {

    // 调试设置
    /**
     * 是否开启调试模式
     */
    @Property(key = "debug", defaultValue = "false")
    public static boolean DEBUG_MODE;
    /**
     * 是否仅允许GM登陆
     */
    @Property(key = "world.server.onlyadmin", defaultValue = "false")
    public static boolean WORLD_ONLYADMIN;
    /**
     * 脚本路径
     */
    @Property(key = "world.server.scriptspath", defaultValue = "scripts")
    public static String WORLD_SCRIPTSPATH;


    // 登录服务器设置
    /**
     * 服务器版本号
     */
    @Property(key = "login.server.version", defaultValue = "138")
    public static short LOGIN_MAPLE_VERSION;
    /**
     * 服务器补丁版本号
     */
    @Property(key = "login.server.patch", defaultValue = "1")
    public static String LOGIN_MAPLE_PATCH;
    /**
     * 服务器区域类型
     */
    @Property(key = "login.server.type", defaultValue = "4")
    public static byte LOGIN_MAPLE_TYPE;
    /**
     * 服务器名称
     */
    @Property(key = "login.server.name", defaultValue = "冒险岛Online")
    public static String LOGIN_SERVERNAME;
    /**
     * 频道选择界面公告
     */
    @Property(key = "login.server.eventmessage", defaultValue = "本服务端由工作室隆重出品")
    public static String LOGIN_EVENTMESSAGE;
    /**
     * 游戏置顶公告
     */
    @Property(key = "login.server.message", defaultValue = "欢迎来到冒险岛Online，祝你游戏愉快！")
    public static String LOGIN_SERVERMESSAGE;
    /**
     * 服务器显示区服名称：蓝蜗牛、白雪人等
     */
    @Property(key = "login.server.flag", defaultValue = "0")
    public static byte LOGIN_SERVERFLAG;
    /**
     * 服务器运行状态
     */
    @Property(key = "login.server.status", defaultValue = "2")
    public static byte LOGIN_SERVERSTATUS;
    /**
     * 最大在线人数
     */
    @Property(key = "login.server.userlimit", defaultValue = "500")
    public static int LOGIN_USERLIMIT;
    /**
     * 默认最大在线人数
     */
    @Property(key = "login.server.defaultuserlimit", defaultValue = "500")
    public static int LOGIN_DEFAULTUSERLIMIT;
    /**
     * 是否使用SHA-1加密
     */
    @Property(key = "login.server.usesha1hash", defaultValue = "true")
    public static boolean LOGIN_USESHA1HASH;
    /**
     * 启用删除角色的按钮
     */
    @Property(key = "login.server.deletecharacter", defaultValue = "true")
    public static boolean LOGIN_DELETECHARACTER;

    // 其他设置

    /**
     * 检测玩家能力值
     */
    @Property(key = "world.checkplayersp", defaultValue = "false")
    public static boolean WORLD_CHECKPLAYERSP;
    /**
     * 检测玩家点券
     */
    @Property(key = "world.checkplayernx", defaultValue = "false")
    public static boolean WORLD_CHECKPLAYERNX;
    /**
     * 检测复制装备
     */
    @Property(key = "world.checkplayerequip", defaultValue = "false")
    public static boolean WORLD_CHECKPLAYEREQUIP;
    /**
     * 禁止释放技能
     */
    @Property(key = "world.banallskill", defaultValue = "false")
    public static boolean WORLD_BANALLSKILL;
    /**
     * 禁止获得经验
     */
    @Property(key = "world.bangainexp", defaultValue = "false")
    public static boolean WORLD_BANGAINEXP;
    /**
     * 禁止所有交易
     */
    @Property(key = "world.bantrade", defaultValue = "false")
    public static boolean WORLD_BANTRADE;
    /**
     * 禁止怪物爆物
     */
    @Property(key = "world.bandropitem", defaultValue = "false")
    public static boolean WORLD_BANDROPITEM;
    /**
     * 玩家人气小于0禁止穿戴装备
     */
    @Property(key = "world.equipcheckfame", defaultValue = "false")
    public static boolean WORLD_EQUIPCHECKFAME;


    // 游戏连接设置
    /**
     * 服务器本地IP设置
     */
    @Property(key = "world.interface", defaultValue = "221.231.130.70")
    public static String WORLD_INTERFACE;
    /**
     * 登录端口
     */
    @Property(key = "login.server.port", defaultValue = "8484")
    public static short LOGIN_PORT;
    /**
     * 商城端口
     */
    @Property(key = "cash.server.port", defaultValue = "8787")
    public static short CASH_PORT;
    /**
     * 聊天端口
     */
    @Property(key = "chat.server.port", defaultValue = "8283")
    public static short CHAT_PORT;
    /**
     * 拍卖端口
     */
    @Property(key = "auction.server.port", defaultValue = "8700")
    public static short AUCTION_PORT;
    /**
     * 频道端口
     */
    @Property(key = "channel.server.ports", defaultValue = "5")
    public static int CHANNEL_PORTS;


    // 游戏倍率设置
    /**
     * 经验倍率设置
     */
    @Property(key = "channel.rate.exp", defaultValue = "10")
    public static int CHANNEL_RATE_EXP;
    /**
     * 金币倍率设置
     */
    @Property(key = "channel.rate.meso", defaultValue = "10")
    public static int CHANNEL_RATE_MESO;
    /**
     * 爆率设置
     */
    @Property(key = "channel.rate.drop", defaultValue = "10")
    public static int CHANNEL_RATE_DROP;
    /**
     * 特殊道具全局爆率
     */
    @Property(key = "channel.rate.globaldrop", defaultValue = "10")
    public static int CHANNEL_RATE_GLOBALDROP;
    /**
     * 专业技能经验倍数
     */
    @Property(key = "channel.rate.trait", defaultValue = "1")
    public static int CHANNEL_RATE_TRAIT;
    /**
     * 潜能等级改变几率 1~1000
     */
    @Property(key = "channel.rate.potentiallevel", defaultValue = "4")
    public static int CHANNEL_RATE_POTENTIALLEVEL;


    // 玩家参数设置
    /**
     * 最大能力值上限
     */
    @Property(key = "channel.player.maxap", defaultValue = "30000")
    public static short CHANNEL_PLAYER_MAXAP;
    /**
     * 最大HP上限
     */
    @Property(key = "channel.player.maxhp", defaultValue = "500000")
    public static int CHANNEL_PLAYER_MAXHP;
    /**
     * 最大MP上限
     */
    @Property(key = "channel.player.maxmp", defaultValue = "500000")
    public static int CHANNEL_PLAYER_MAXMP;
    /**
     * 最高等级
     */
    @Property(key = "channel.player.maxlevel", defaultValue = "250")
    public static int CHANNEL_PLAYER_MAXLEVEL;
    /**
     * 持有金币上限
     */
    @Property(key = "channel.player.maxmeso", defaultValue = "10000000000")
    public static long CHANNEL_PLAYER_MAXMESO;
    /**
     * 新手出生地图
     */
    @Property(key = "channel.player.beginnermap", defaultValue = "50000")
    public static int CHANNEL_PLAYER_BEGINNERMAP;
    /**
     * 最大可创建角色数量
     */
    @Property(key = "channel.player.maxcharacters", defaultValue = "6")
    public static byte CHANNEL_PLAYER_MAXCHARACTERS;
    /**
     * 每日免费复活次数
     */
    @Property(key = "channel.player.resufreecount", defaultValue = "5")
    public static int CHANNEL_PLAYER_RESUFREECOUNT;
    /**
     * 付费复活所需金币
     */
    @Property(key = "channel.player.resuneedmeso", defaultValue = "1000000")
    public static int CHANNEL_PLAYER_RESUNEEDMESO;
    /**
     * 突破之石上限
     */
    @Property(key = "channel.player.limitbreak", defaultValue = "5000000")
    public static int CHANNEL_PLAYER_LIMITBREAK;


    // 游戏参数设置
    /**
     * 在线时间刷新间隔（单位：分钟）
     */
    @Property(key = "world.refreshonline", defaultValue = "10")
    public static int WORLD_REFRESHONLINE;
    /**
     * 排行榜刷新时间间隔（单位：分钟）
     */
    @Property(key = "world.refreshrank", defaultValue = "120")
    public static int WORLD_REFRESHRANK;
    /**
     * 开启自动封号
     */
    @Property(key = "world.autoban", defaultValue = "true")
    public static boolean WORLD_AUTOBAN;
    /**
     * 禁止使用的技能列表
     */
    @Property(key = "world.blockskills", defaultValue = "")
    public static String WORLD_BLOCKSKILLS;
    /**
     * 禁止开放的职业
     */
    @Property(key = "world.closejobs", defaultValue = "")
    public static String WORLD_CLOSEJOBS;
    /**
     * 隐藏的NPC
     */
    @Property(key = "world.hidenpcs", defaultValue = "")
    public static String WORLD_HIDENPCS;
    /**
     * 商城按钮打开的NPC
     */
    @Property(key = "channel.server.enternpc_cashshop", defaultValue = "")
    public static String CHANNEL_ENTERNPC_CASHSHOP;
    /**
     * 拍卖按钮打开的NPC
     */
    @Property(key = "channel.server.enternpc_mts", defaultValue = "9900004")
    public static String CHANNEL_ENTERNPC_MTS;
    /**
     * 是否开启PVP
     */
    @Property(key = "channel.server.openpvp", defaultValue = "false")
    public static boolean CHANNEL_OPENPVP;
    /**
     * PVP地图列表
     */
    @Property(key = "channel.server.pvpmaps", defaultValue = "910000018")
    public static String CHANNEL_PVPMAPS;
    /**
     * 开启自由市场聊天信息用小黑板显示
     */
    @Property(key = "channel.server.chalkboard", defaultValue = "true")
    public static boolean CHANNEL_CHALKBOARD;
    /**
     * 创建家族所需金币
     */
    @Property(key = "channel.server.createguildcost", defaultValue = "5000000")
    public static int CHANNEL_CREATEGUILDCOST;
    /**
     * 是否开启商城道具可用抵用券购买
     */
    @Property(key = "channel.server.enablepointsbuy", defaultValue = "true")
    public static boolean CHANNEL_ENABLEPOINTSBUY;
    /**
     * 是否开启角色DEBUFF
     */
    @Property(key = "channel.server.applyplayerdebuff", defaultValue = "false")
    public static boolean CHANNEL_APPLYPLAYERDEBUFF;
    /**
     * 是否开启怪物BUFF(怪物)
     */
    @Property(key = "channel.server.applymonsterstatus", defaultValue = "false")
    public static boolean CHANNEL_APPLYMONSTERSTATUS;
    /**
     * 事件脚本设置
     */
    @Property(key = "channel.server.events", defaultValue = "")
    public static String CHANNEL_EVENTS;


    // 怪物参数设置
    /**
     * 怪物刷新时间(秒)
     */
    @Property(key = "channel.monster.refresh", defaultValue = "15")
    public static int CHANNEL_MONSTER_REFRESH;
    /**
     * 地图最大怪物数量
     */
    @Property(key = "channel.monster.maxcount", defaultValue = "15")
    public static int CHANNEL_MONSTER_MAXCOUNT;
    /**
     * 打怪获得点券(为0则关闭)
     */
    @Property(key = "channel.monster.givepoint", defaultValue = "0")
    public static int CHANNEL_MONSTER_GIVEPOINT;

    // 在线奖励设置
    /**
     * 是否开启在线奖励
     */
    @Property(key = "channel.reward.isopen", defaultValue = "false")
    public static boolean CHANNEL_REWARD_ISOPEN;
    /**
     * 奖励间隔时间(秒)
     */
    @Property(key = "channel.reward.refreshtime", defaultValue = "60")
    public static int CHANNEL_REWARD_REFRESHTIME;
    /**
     * 获得经验数量
     */
    @Property(key = "channel.reward.exp", defaultValue = "1000")
    public static int CHANNEL_REWARD_EXP;
    /**
     * 获得点券数量
     */
    @Property(key = "channel.reward.acash", defaultValue = "1")
    public static int CHANNEL_REWARD_ACASH;
    /**
     * 获得抵用券数量
     */
    @Property(key = "channel.reward.mpoint", defaultValue = "1")
    public static int CHANNEL_REWARD_MPOINT;
    /**
     * 获得金币数量
     */
    @Property(key = "channel.reward.meso", defaultValue = "1")
    public static int CHANNEL_REWARD_MESO;
    /**
     * 获得积分数量
     */
    @Property(key = "channel.reward.integral", defaultValue = "1")
    public static int CHANNEL_REWARD_INTEGRAL;
    /**
     * 获得元宝数量
     */
    @Property(key = "channel.reward.rmb", defaultValue = "1")
    public static int CHANNEL_REWARD_RMB;

    // 数据库设置
    @Property(key = "db.ip", defaultValue = "localhost")
    public static String DB_IP;

    @Property(key = "db.port", defaultValue = "3306")
    public static String DB_PORT;

    @Property(key = "db.name", defaultValue = "qhms")
    public static String DB_NAME;

    @Property(key = "db.user", defaultValue = "root")
    public static String DB_USER;

    @Property(key = "db.password", defaultValue = "root")
    public static String DB_PASSWORD;

    //    @Property(key = "db.timeout", defaultValue = "1800000")
    public static int DB_TIMEOUT = 300000;

    //    @Property(key = "db.MinPoolSize", defaultValue = "20")
    public static int DB_MINPOOLSIZE = 20;

    //    @Property(key = "db.InitialPoolSize", defaultValue = "30")
    public static int DB_INITIALPOOLSIZE = 30;

    //    @Property(key = "db.MaxPoolSize", defaultValue = "400")
    public static int DB_MAXPOOLSIZE = 1000;

    @Property(key = "db.setuppath", defaultValue = "D:\\MySQL\\MySQL Server 5.6")
    public static String DB_SETUPPATH;

    @Property(key = "db.backuppath", defaultValue = "D:\\数据库备份")
    public static String DB_BACKUPPATH;

    @Property(key = "db.autobackuptime", defaultValue = "120")
    public static int DB_AUTOBACKUPTIME;
}
