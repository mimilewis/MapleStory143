package handling.opcode;

import handling.ExternalCodeTableGetter;
import handling.WritableIntValueHolder;

import java.io.*;
import java.util.Properties;

public enum RecvPacketOpcode implements WritableIntValueHolder {
    PONG(0x94, false),
    CHAT_SERVER_PONG(0xe, false),
    CHAT_SERVER_REQUEST(0x1, false),
    GUILD_CHAT(0x13, false),
    BUDDY_CHAT(0x14, false),
    CLIENT_AUTH(0x66, false),
    CLIENT_HELLO(0x67, false),
    LOGIN_PASSWORD(0x69, false),
    PLAYER_LOGGEDIN(0x6e, false),
    LOGIN_AUTHKEY(0x73, false),
    CLIENT_ERROR(0x85, false),
    CLIENT_FAIL(0x96, false),
    RSA_KEY(0xb5, false),

    CREATE_ULTIMATE(0x21),
    STRANGE_DATA(0x28),
    CHARLIST_REQUEST(0x6a),
    CHAR_SELECT(0x6f),
    CHECK_CHAR_NAME(0x74),
    SERVERLIST_REQUEST(0x75),
    CREATE_CHAR(0x7d),
    DELETE_CHAR(0x80),
    PART_TIME_JOB(0x8e),
    CHARACTER_CARDS(0x8f),
    REQUEST_CONNECTION(0x99),
    LICENSE_REQUEST(0x9d),
    SET_GENDER(0x9e),
    CHAR_SELECTED(0x9f),
    UPDATE_CHARSLOT(0xa0),
    SET_CHAR_CARDS(0xab),
    SET_ACC_CASH(0xac),
    QUICK_BUY_CS_ITEM(0xad),
    SERVERSTATUS_REQUEST(0xb4),
    CREATE_CHAR_REQUEST(0xb8),
    SEAL_FAMILIAR(0xb9),
    CHANGE_MAP(0xbe),
    CHANGE_CHANNEL(0xbf),
    ENTER_CASH_SHOP(0xc4),
    MOVE_PLAYER(0xca),
    CANCEL_CHAIR(0xcb),
    USE_CHAIR(0xcc),
    CLOSE_RANGE_ATTACK(0xcf),
    RANGED_ATTACK(0xd0),
    MAGIC_ATTACK(0xd1),
    PASSIVE_ENERGY(0xd2),
    UNK004B(0x7ffe),
    CLUSTER_ATTACK(0xd3),
    TAKE_DAMAGE(0xd5),
    GENERAL_CHAT(0xd6),
    CLOSE_CHALKBOARD(0xd7),
    FACE_EXPRESSION(0xd8),
    FACE_ANDROID(0xd9),
    USE_ITEM_EFFECT(0xda),
    WHEEL_OF_FORTUNE(0xdb),
    USE_TITLE_EFFECT(0xdc),
    USE_ACTIVATE_DAMAGE_SKIN(0xdd),
    USE_ACTIVATE_DAMAGE_SKIN_PREMIUM(0xde),
    NPC_TALK(0xe6),
    REMOTE_STORE(0xe7),
    NPC_TALK_MORE(0xe8),
    NPC_SHOP(0xe9),
    STORAGE(0xea),
    USE_HIRED_MERCHANT(0xeb),
    MERCH_ITEM_STORE(0xec),
    DUEY_ACTION(0xed),
    MECH_CANCEL(0xee),
    EXTRA_ATTACK(0xf0),
    SHOW_FIREWALL_REQUEST(0xf1),
    USE_HOLY_FOUNTAIN(0xf2),
    OWL(0xf4),
    OWL_WARP(0xf5),
    ITEM_GATHER(0xf8),
    ITEM_SORT(0xf9),
    ITEM_MOVE(0xfa),
    MOVE_BAG(0xfb),
    SWITCH_BAG(0xfc),
    USE_ITEM(0xff),
    CANCEL_ITEM_EFFECT(0x100),
    USE_SUMMON_BAG(0x102),
    PET_FOOD(0x103),
    USE_MOUNT_FOOD(0x104),
    USE_SCRIPTED_NPC_ITEM(0x105),
    USE_RECIPE(0x106),
    USE_CASH_ITEM(0x107),
    USE_ADDITIONAL_ADD_ITEM(0x108),
    ALLOW_PET_LOOT(0x109),
    ALLOW_PET_AOTO_EAT(0x10a),
    USE_OPTION_CHANGE_REQUEST(0x10b),
    USE_CATCH_ITEM(0x10d),
    USE_SKILL_BOOK(0x10e),
    USE_SP_RESET(0x10f),
    USE_AP_RESET(0x110),
    USE_OWL_MINERVA(0x118),
    USE_TELE_ROCK(0x119),
    USE_RETURN_SCROLL(0x11a),
    USE_UPGRADE_SCROLL(0x11c),
    USE_FLAG_SCROLL(0x11d),
    USE_EQUIP_SCROLL(0x11e),
    USE_EXITEM_UPGRADE(0x11f),
    USE_POTENTIAL_SCROLL(0x122),
    USE_POTENTIAL_ADD_SCROLL(0x123),
    USE_ADDITIONAL_ITEM(0x124),
    USE_SOULS_SCROLL(0x126),
    USE_SOUL_MARBLE(0x127),
    USE_MIRACLE_CUBE(0x128),
    USE_ENCHANTING(0x129),
    USE_BAG(0x12c),
    USE_MAGNIFY_GLASS(0x12d),
    USE_CRAFTED_CUBE(0x12e),
    DISTRIBUTE_AP(0x135),
    AUTO_ASSIGN_AP(0x136),
    HEAL_OVER_TIME(0x138),
    DEL_TEACH_SKILL(0x13b),
    SET_TEACH_SKILL(0x13c),
    DISTRIBUTE_SP(0x13d),
    SPECIAL_MOVE(0x13e),
    CANCEL_BUFF(0x13f),
    SKILL_EFFECT(0x141),
    MESO_DROP(0x7ffe),
    SUPER_CANNON_REQUEST(0x143),
    GIVE_FAME(0x145),
    CHAR_INFO_REQUEST(0x147),
    SPAWN_PET(0x148),
    PET_AUTO_BUFF(0x149),
    CANCEL_DEBUFF(0x14a),
    CHANGE_MAP_SPECIAL(0x14b),
    USE_INNER_PORTAL(0x14d),
    TROCK_ADD_MAP(0x14f),
    LIE_DETECTOR(0x150),
    LIE_DETECTOR_SKILL(0x151),
    LIE_DETECTOR_REFRESH(0x152),
    REPORT(0x153),
    QUEST_ACTION(0x155),
    REISSUE_MEDAL(0x156),
    MOVE_ENERGY(0x158),
    SPECIAL_ATTACK(0x159),
    SKILL_MACRO(0x15d),
    REWARD_ITEM(0x15f),
    ITEM_MAKER(0x160),
    REPAIR_ALL(0x163),
    REPAIR(0x164),
    SOLOMON(0x7ffe),
    GACH_EXP(0x7ffe),
    FOLLOW_REQUEST(0x167),
    FOLLOW_REPLY(0x168),
    AUTO_FOLLOW_REPLY(0x16b),
    PROFESSION_INFO(0x16c),
    USE_POT(0x16d),
    CLEAR_POT(0x16e),
    FEED_POT(0x16f),
    CURE_POT(0x170),
    REWARD_POT(0x171),
    USE_COSMETIC(0x173),
    DF_COMBO(0x174),
    USER_TRUMP_SKILL_ACTION_REQUEST(0x176),
    DOT_HEAL_HP_REQUEST(0x177),
    USE_REDUCER(0x17a),
    USE_REDUCER_PRESTIGE(0x17b),
    PVP_RESPAWN(0x7ffe),
    CHANGE_ZERO_LOOK(0x18a),
    CHANGE_ZERO_LOOK_END(0x18c),
    PARTYCHAT(0x194),
    WHISPER(0x196),
    MESSENGER(0x197),
    PLAYER_INTERACTION(0x198),
    PARTY_OPERATION(0x199),
    DENY_PARTY_REQUEST(0x19a),
    ALLOW_PARTY_INVITE(0x19b),
    EXPEDITION_OPERATION(0x19c),
    EXPEDITION_LISTING(0x19d),
    GUILD_OPERATION(0x19f),
    DENY_GUILD_REQUEST(0x1a0),
    GUILD_APPLY(0x1a1),
    ACCEPT_GUILD_APPLY(0x1a3),
    DENY_GUILD_APPLY(0x1a4),
    ADMIN_COMMAND(0x1a5),
    BUDDYLIST_MODIFY(0x1a9),
    NOTE_ACTION(0x1aa),
    RPS_GAME(0x1ad),
    USE_DOOR(0x1ae),
    USE_MECH_DOOR(0x1b0),
    CHANGE_KEYMAP(0x1b2),
    RING_ACTION(0x1b7),
    ARAN_COMBO(0x1ca),
    LOST_ARAN_COMBO(0x1cb),
    CRAFT_DONE(0x1d3),
    CRAFT_EFFECT(0x1d4),
    CRAFT_MAKE(0x1d5),
    MICRO_BUFF_END_TIME(0x1d8),
    CHANGE_MARKET_MAP(0x1da),
    MEMORY_SKILL_CHOOSE(0x1db),
    MEMORY_SKILL_CHANGE(0x1dc),
    MEMORY_SKILL_OBTAIN(0x1dd),
    BUY_CROSS_ITEM(0x1e6),
    USE_TEMPEST_BLADES(0x1e8),
    DISTRIBUTE_HYPER_SP(0x1ef),
    RESET_HYPER_SP(0x1f0),
    DISTRIBUTE_HYPER_AP(0x1f1),
    RESET_HYPER_AP(0x1f2),
    CHANGE_PLAYER(0x201),
    UNKNOWN_168(0x203),
    LIE_DETECTOR_RESPONSE(0x20c),
    USER_HOWLING_STORM_STACK(0x212),
    ADD_ATTACK_RESET(0x224),
    FAMILIAR_OPERATION(0x232),
    SOUL_MODE(0x230),
    USE_TOWERCHAIR_SETTING(0x238),
    VMATRIX_MAKE_REQUEST(0x242),
    VMATRIX_HELP_REQUEST(0x243),
    MOVE_PET(0x249),
    PET_CHAT(0x24a),
    PET_COMMAND(0x24b),
    PET_LOOT(0x24c),
    PET_AUTO_POT(0x24d),
    PET_EXCEPTION_LIST(0x24e),
    PET_AOTO_EAT(0x24f),
    MOVE_LITTLEWHITE(0x253),
    MOVE_SUMMON(0x25a),
    SUMMON_ATTACK(0x25b),
    DAMAGE_SUMMON(0x25c),
    SUB_SUMMON(0x25d),
    REMOVE_SUMMON(0x25e),
    MOVE_DRAGON(0x264),
    DRAGON_FLY(0x265),
    MOVE_ANDROID(0x268),
    SUB_LITTLEWHITE(0x26d),
    QUICK_SLOT(0x26f),
    PLAYER_VIEW_RANGE(0x273),
    SYSTEM_PROCESS_LIST(0x276),
    CHANGE_POTENTIAL_BOX(0x278),
    CHANGE_POTENTIAL_WP(0x279),
    CHANGE_POTENTIAL(0x27a),
    SHOW_LOVE_RANK(0x27c),
    SPAWN_ARROWS_TURRET(0x286),
    USE_GROWTH_HELPER_REQUEST(0x29c),
    WARLOCK_MAGIC_ATTACK(0x2a1),
    ENTER_STARTPLANET(0x7ffe),
    TRACK_FLAMES(0x2d0),
    SELECT_JAGUAR(0x2d5),
    GIVE_KSPSYCHIC(0x2ec),
    ATTACK_KSPSYCHIC(0x2ed),
    CANCEL_KSPSYCHIC(0x2ee),
    GIVE_KSULTIMATE(0x2f0),
    ATTACK_KSULTIMATE(0x2f1),
    MIST_KSULTIMAT(0x2f2),
    CANCEL_KSULTIMATE(0x2f3),
    SIGNIN_OPERATION(0x2fe),
    MULTI_SKILL_ATTACK_REQUEST(0x30b),
    MULTI_SKILL_CHARGE_REQUEST(0x30f),
    USE_NEBULITE(0x311),
    USE_ALIEN_SOCKET(0x7ffe),
    USE_ALIEN_SOCKET_RESPONSE(0x7ffe),
    USE_NEBULITE_FUSION(0x7ffe),
    POTION_POT_USE(0x314),
    POTION_POT_ADD(0x315),
    POTION_POT_MODE(0x316),
    POTION_POT_INCR(0x317),
    USE_SPECIAL_ITEM(0x31f),
    APPLY_HYUNCUBE(0x324),
    CALL_FRIENDS(0x325),
    BBS_OPERATION(0x335),
    SELECT_CHAIR(0x339),
    TRANSFORM_PLAYER(0x33d),
    OPEN_AVATAR_RANDOM_BOX(0x33e),
    ENTER_MTS(0x33f),
    FISHING(0x344),
    USE_TREASUER_CHEST(0x346),
    SHIKONGJUAN(0x34f),
    SET_CHAR_CASH(0x354),
    OPEN_WORLDMAP(0x356),
    SAVE_DAMSKIN(0x35c),
    CHANGE_DAMSKIN(0x35d),
    DELETE_DAMSKIN(0x35e),
    MOVE_LIFE(0x371),
    AUTO_AGGRO(0x372),
    FRIENDLY_DAMAGE(0x375),
    MONSTER_BOMB(0x377),
    MONSTER_BOMB_COLLISION_GROUP(0x378),
    MONSTER_SPECIAL_SKILL(0x37f),
    NPC_ACTION(0x38b),
    ITEM_PICKUP(0x391),
    DAMAGE_REACTOR(0x394),
    TOUCH_REACTOR(0x395),
    MAKE_EXTRACTOR(0x3a1),
    SNOW_BALL_HIT(0x3a5),
    PLAYER_UPDATE(0x3b5),
    PARTY_SEARCH_START(0x3b8),
    PARTY_SEARCH_STOP(0x3b9),
    START_HARVEST(0x3bd),
    STOP_HARVEST(0x3bf),
    QUICK_MOVE(0x3c1),
    USE_RUNE(0x3c2),
    USE_RUNE_SKILL_REQ(0x3c3),
    OBTACLE_ATOM_COLLISION(0x3c4),
    DEMIANOBJECT_MAKE_ENTER_ACK(0x43a),
    DEMIANOBJECT_NODE_END(0x43b),
    DEMIANOBJECT_ERR_RECREATE(0x43c),
    CS_UPDATE(0x452),
    BUY_CS_ITEM(0x453),
    COUPON_CODE(0x454),
    SEND_CS_GIFI(0x456),
    USE_HAMMER(0x473),
    HAMMER_RESPONSE(0x475),
    BATTLE_STATISTICS(0x478),
    HIDDEN_TAIL_ADN_EAR(0x7ffe),
    EFFECT_SWITCH(0x49a),
    PAM_SONG(0x7ffe),
    TAP_JOY_RESPONSE(0x48f),
    TAP_JOY_DONE(0x490),
    TAP_JOY_NEXT_STAGE(0x491),

    UNKNOWN;

    static {
        reloadValues();
    }

    private int code = -2;
    private boolean CheckState;

    RecvPacketOpcode() {
        this.CheckState = true;
    }

    RecvPacketOpcode(int code) {
        this.code = code;
    }

    RecvPacketOpcode(int code, boolean CheckState) {
        this.CheckState = CheckState;
        this.code = code;

    }

    public static String getNamebyID(int val) {
        for (RecvPacketOpcode op : RecvPacketOpcode.values()) {
            if (op.getValue() == val) {
                return op.name();
            }
        }
        return "UNKNOWN";
    }

    public static RecvPacketOpcode getByType(int type) {
        for (RecvPacketOpcode l : RecvPacketOpcode.values()) {
            if (l.getValue() == type) {
                return l;
            }
        }
        return UNKNOWN;
    }

    public static Properties getDefaultProperties() throws IOException {
        Properties props = new Properties();
        FileInputStream fileInputStream = new FileInputStream("properties/recvops.properties");
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "GBK");
        BufferedReader buff = new BufferedReader(inputStreamReader);
        props.load(buff);
        buff.close();
        inputStreamReader.close();
        fileInputStream.close();
        return props;
    }

    public static void reloadValues() {
        try {
            File file = new File("properties/recvops.properties");
            if (file.exists()) {
                ExternalCodeTableGetter.populateValues(getDefaultProperties(), values());
            }
        } catch (IOException e) {
            throw new RuntimeException("加载 recvops.properties 文件出现错误", e);
        }
    }

    @Override
    public short getValue() {
        return (short) code;
    }

    @Override
    public void setValue(short code) {
        this.code = code;
    }

    public boolean NeedsChecking() {
        return CheckState;
    }
}
