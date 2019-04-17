package handling;

import client.MapleClient;
import client.MapleEnumClass;
import configs.ServerConfig;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.cashshop.handler.BuyCashItemHandler;
import handling.cashshop.handler.CashShopOperation;
import handling.cashshop.handler.CouponCodeHandler;
import handling.channel.ChannelServer;
import handling.channel.handler.*;
import handling.chat.ChatServer;
import handling.login.LoginServer;
import handling.login.handler.*;
import handling.netty.MaplePacketDecoder;
import handling.opcode.CashShopOpcode;
import handling.opcode.RecvPacketOpcode;
import handling.opcode.SendPacketOpcode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.*;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.LoginPacket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapleServerHandler extends ChannelInboundHandlerAdapter {

    public static final List<String> BlockIPList = new ArrayList<>();
    public static final Logger AllPacketLog = LogManager.getLogger("AllPackets");
    public static final Logger BuffPacketLog = LogManager.getLogger("BuffPackets");
    public static final Logger AttackPakcetLog = LogManager.getLogger("AttackPackets");
    public static final boolean preventIpAttack = true;
    private static final Logger log = LogManager.getLogger(MapleServerHandler.class.getName());
    private static boolean show = false;
    private final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<>();
    private int world = -1, channel = -1;
    private ServerType type = null;

    public MapleServerHandler(int world, int channel, ServerType type) {
        this.world = world;
        this.channel = channel;
        this.type = type;
    }

    public static void handlePacket(RecvPacketOpcode header, SeekableLittleEndianAccessor slea, MapleClient c, ServerType type) throws Exception {
        switch (header) {
            case PONG: // 心跳包
                c.pongReceived();
                break;
            case CHAT_SERVER_PONG://聊天服务器Pong
                c.chatServerPongReceived();
                break;
            case STRANGE_DATA:
                // Does nothing for now, HackShield's heartbeat
                break;
            case CLIENT_AUTH:
                slea.skip(1);
                int clientfile = slea.readInt();
                int checkfile;
                checkfile = ((clientfile >> 5) << 5) + (((((clientfile & 0x1F) >> 3) ^ 2) << 3) + (7 - (clientfile & 7)));
                checkfile |= ((clientfile >> 7) << 7);
                checkfile -= 2;
                //c.announce(MaplePacketCreator.getClientAuthentication(checkfile));
                break;
            case CLIENT_ERROR: // 客户端发送过来的错误信息
                ClientErrorLogHandler.handlePacket(slea, c);
                break;
            case CLIENT_FAIL: // 客户端失败错误信息
                PacketErrorHandler.handlePacket(slea, c);
                break;
            case REQUEST_CONNECTION:
                c.announce(LoginPacket.addConnection());
                break;
            case CHAT_SERVER_REQUEST:
                ChatHandler.EnterChatServer(slea, c);//好友/家族聊天服务器验证
                break;
            case GUILD_CHAT:
                ChatHandler.GuildChat(slea, c); //家族聊天
                break;
            case BUDDY_CHAT:
                ChatHandler.BuddyChat(slea, c);  //好友聊天
                break;
            case LOGIN_PASSWORD: // 登陆账号密码
                LoginPasswordHandler.handlePacket(slea, c);
                break;
            case LICENSE_REQUEST: // 许可协议
                LicenseRequestHandler.handlePacket(slea, c);
                break;
            case SET_GENDER: // 选择性别
                SetGenderHandler.handlePacket(slea, c);
                break;
            case CHARACTER_CARDS: //更新选择的角色卡
                UpdateCharCards.handlePacket(slea, c);
                break;
            case SET_CHAR_CARDS: //显示角色卡界面 也就是获取角色卡的数量
                ShowCharCards.handlePacket(slea, c);
                break;
            case SET_ACC_CASH: //获取帐号点卷信息
                ShowAccCash.handlePacket(slea, c);
                break;
//            case SEND_ENCRYPTED:
//                if (c.isLocalhost()) {
//                    LoginPasswordHandler.handlePacket(slea, c);
//                } else {
//                    c.announce(LoginPacket.getCustomEncryption());
//                }
//                break;
//            case CLIENT_START:
//            case CLIENT_FAILED:
//                c.announce(LoginPacket.getCustomEncryption());
//                break;
//            case VIEW_SERVERLIST:
//                if (slea.readByte() == 0) {
//                    ServerlistRequestHandler.handlePacket(c, false);
//                }
//                break;
//            case REDISPLAY_SERVERLIST:
            case SERVERLIST_REQUEST: // 请求服务器列表
                ServerlistRequestHandler.handlePacket(c, true);
                break;
            case CLIENT_HELLO: // 连接到服务端
                MapLoginHandler.handlePacket(slea, c, type);
                break;
            case CHARLIST_REQUEST: //请求人物列表
                CharlistRequestHandler.handlePacket(slea, c);
                break;
            case SERVERSTATUS_REQUEST: // 服务器状态
                ServerStatusRequestHandler.handlePacket(c);
                break;
            case CHECK_CHAR_NAME: // 检查人物名字
                CheckCharNameHandler.handlePacket(slea, c);
                break;
            case CREATE_CHAR: // 创建人物
                CreateCharHandler.handlePacket(slea, c);
                break;
            case CREATE_ULTIMATE: // 创建终极冒险家
                CreateUltimateHandler.handlePacket(slea, c);
                break;
            case DELETE_CHAR: // 删除角色
                DeleteCharHandler.handlePacket(slea, c);
                break;
            case PART_TIME_JOB:
                PartJobHandler.handlePacket(slea, c);
                break;
            case CHAR_SELECT:   // 开始游戏
            case CHAR_SELECTED: // 创建角色后直接进入
                CharSelectedHandler.handlePacket(slea, c);
                break;
            case UPDATE_CHARSLOT:
                UpdatePlayerSlots.handlePacket(slea);
                break;
            case RSA_KEY:
                c.announce(LoginPacket.getLoginAUTH());
                if (ServerConfig.WORLD_ONLYADMIN) {
                    for (int i = 1; i <= 2; i++) {
                        MapleEnumClass.AuthReply login = c.login("admin" + i, "admin", false, false);
                        if (login == MapleEnumClass.AuthReply.GAME_LOGIN_SUCCESSFUL) {
                            c.setTempInfo("admin" + i, "admin", false);
                            LoginPasswordHandler.Login(c);
                            break;
                        }
                    }
                }
                break;
            case CREATE_CHAR_REQUEST:
                CreateCharHandler.getCreatCharAuth(slea, c);
                break;
            /*
             * --------------------------------------------------------------------------------------
             * END OF LOGIN SERVER
             * --------------------------------------------------------------------------------------
             */
            case SEAL_FAMILIAR:
                PlayerHandler.sealFamiliar(slea, c, c.getPlayer());
                break;
            case CHANGE_CHANNEL: // 更换频道
                InterServerHandler.ChangeChannel(slea, c, c.getPlayer());
                break;
            case PLAYER_LOGGEDIN: // 登陆请求
                slea.readInt(); //V.114新增 [00 00 00 00]
                int playerid = slea.readInt();
                //后面是MAC地址
                InterServerHandler.Loggedin(slea, playerid, c, type);
                break;
            case ENTER_CASH_SHOP: // 进入商城
                InterServerHandler.enterCS(c, c.getPlayer(), false);
                break;
            case ENTER_MTS: // 进入拍卖
                InterServerHandler.EnterMTS(c, c.getPlayer());
                break;
            case CHANGE_PLAYER: //切换角色
                InterServerHandler.ChangePlayer(slea, c);
                break;
            case LOGIN_AUTHKEY:
                LoginPasswordHandler.handlerAuthKey(slea, c);
                break;
            case MOVE_PLAYER: // 人物移动
                PlayerHandler.MovePlayer(slea, c, c.getPlayer());
                break;
            case CHAR_INFO_REQUEST:
                slea.readInt();
                PlayerHandler.CharInfoRequest(slea.readInt(), c, c.getPlayer());
                break;
            case CLOSE_RANGE_ATTACK: //近距离攻击
            case RANGED_ATTACK: //远距离攻击
            case MAGIC_ATTACK:  //魔法攻击
            case SUMMON_ATTACK: //召唤兽攻击
            case PASSIVE_ENERGY://被动近距离攻击
            case CLUSTER_ATTACK:
                PlayerHandler.attackProcessing(slea, c, header);
                break;
            case SPECIAL_MOVE:
                PlayerHandler.SpecialMove(slea, c, c.getPlayer());
                break;
            case PROFESSION_INFO:
                ItemMakerHandler.ProfessionInfo(slea, c, c.getPlayer());
                break;
            case CRAFT_DONE:
                ItemMakerHandler.CraftComplete(slea, c, c.getPlayer());
                break;
            case CRAFT_MAKE:
                ItemMakerHandler.CraftMake(slea, c, c.getPlayer());
                break;
            case CRAFT_EFFECT:
                ItemMakerHandler.CraftEffect(slea, c, c.getPlayer());
                break;
            case START_HARVEST:
                ItemMakerHandler.StartHarvest(slea, c, c.getPlayer());
                break;
            case STOP_HARVEST:
                ItemMakerHandler.StopHarvest(slea, c, c.getPlayer());
                break;
            case MAKE_EXTRACTOR: //分解机
                ItemMakerHandler.MakeExtractor(slea, c, c.getPlayer());
                break;
            case USE_ENCHANTING:
                ItemScrollHandler.UseEquipEnchanting(slea, c, c.getPlayer());
                break;
            case USE_BAG:
                ItemMakerHandler.UseBag(slea, c, c.getPlayer());
                break;
            case USE_RECIPE:
                ItemMakerHandler.UseRecipe(slea, c, c.getPlayer());
                break;
            case USE_NEBULITE:
                InventoryHandler.UseNebulite(slea, c, c.getPlayer());
                break;
            case MOVE_ANDROID:
                PlayerHandler.MoveAndroid(slea, c, c.getPlayer());
                break;
            case SUB_LITTLEWHITE:
                SummonHandler.SubLittleWhite(slea, c.getPlayer());
                break;
            case FACE_EXPRESSION:
                PlayerHandler.ChangeEmotion(slea.readInt(), c.getPlayer());
                break;
            case FACE_ANDROID:
                PlayerHandler.ChangeAndroidEmotion(slea.readInt(), c.getPlayer());
                break;
            case TAKE_DAMAGE:
                TakeDamageHandler.TakeDamage(slea, c, c.getPlayer());
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.Heal(slea, c.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.CancelBuffHandler(slea.readInt(), c.getPlayer());
                break;
            case MECH_CANCEL:
                PlayerHandler.CancelMech(slea, c.getPlayer());
                break;
            case USE_HOLY_FOUNTAIN: //使用神圣源泉
                PlayersHandler.UseHolyFountain(slea, c, c.getPlayer());
                break;
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.CancelItemEffect(slea.readInt(), c.getPlayer());
                break;
            case USE_CHAIR:
                PlayerHandler.UseChair(slea, c, c.getPlayer());
                break;
            case CANCEL_CHAIR:
                PlayerHandler.CancelChair(slea.readShort(), c, c.getPlayer());
                break;
            case USE_ITEM_EFFECT: //使用商城道具效果
                PlayerHandler.UseItemEffect(slea, c, c.getPlayer());
                break;
            case WHEEL_OF_FORTUNE: //使用未知效果 ------------------- 不确定
                break; //whatever
            case USE_TITLE_EFFECT: //使用称号道具
                PlayerHandler.UseTitleEffect(slea.readInt(), c, c.getPlayer());
                break;
            case MICRO_BUFF_END_TIME:
                PlayerHandler.MicroBuffEndTime(slea, c.getPlayer());
                break;
            case USE_ACTIVATE_DAMAGE_SKIN:  //使用未知效果 ------------------- 不确定
                PlayerHandler.UseActivateDamageSkin(slea, c.getPlayer());
                break;
            case USE_ACTIVATE_DAMAGE_SKIN_PREMIUM:
                PlayerHandler.UseActivateDamageSkinPremium(slea, c.getPlayer());
                break;
            case DEMIANOBJECT_MAKE_ENTER_ACK:
                PlayerHandler.DemianObjectMakeEnterAck(slea, c.getPlayer());
                break;
            case DEMIANOBJECT_NODE_END:
                PlayerHandler.DemianObjectNodeEnd(slea, c.getPlayer());
                break;
            case DEMIANOBJECT_ERR_RECREATE:
                break;
            case SKILL_EFFECT:
                PlayerHandler.SkillEffect(slea, c.getPlayer());
                break;
            case SUPER_CANNON_REQUEST:
                PlayerHandler.UserSupserCannotRequest(slea, c.getPlayer());
                break;
            case QUICK_SLOT:
                PlayerHandler.QuickSlot(slea, c.getPlayer());
                break;
            case MESO_DROP:
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.DropMeso(slea.readInt(), c.getPlayer());
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.ChangeKeymap(slea, c.getPlayer());
                break;
            case CHANGE_MAP:
                if (type == ServerType.商城服务器) {
                    CashShopOperation.LeaveCS(slea, c, c.getPlayer());
                } else {
                    PlayerHandler.ChangeMap(slea, c, c.getPlayer());
                }
                break;
            case CHANGE_MAP_SPECIAL:
                slea.skip(1);
                PlayerHandler.ChangeMapSpecial(slea.readMapleAsciiString(), c, c.getPlayer());
                break;
            case USE_INNER_PORTAL:
                slea.skip(1);
                PlayerHandler.InnerPortal(slea, c, c.getPlayer());
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.TrockAddMap(slea, c, c.getPlayer());
                break;
            case LIE_DETECTOR: //玩家使用道具进行测谎
                //PlayersHandler.LieDetector(slea, c, c.getPlayer(), true);
                break;
            case LIE_DETECTOR_SKILL: //管理员使用技能进行测谎
                PlayersHandler.LieDetector(slea, c, c.getPlayer(), false);
                break;
            case LIE_DETECTOR_RESPONSE: //确定输入的验证码
                PlayersHandler.LieDetectorResponse(slea, c);
                break;
            case LIE_DETECTOR_REFRESH: //测谎仪图片刷新
                PlayersHandler.LieDetectorRefresh(slea, c);
                break;
            case USER_HOWLING_STORM_STACK:
                PlayerHandler.UserHowlingStormStack(slea, c.getPlayer());
                break;
            case ADD_ATTACK_RESET:
                break;
            case ARAN_COMBO: //增加战神连击点数
                PlayerHandler.AranCombo(c, c.getPlayer(), 5);
                break;
            case LOST_ARAN_COMBO: //减少战神连击点数
                PlayerHandler.AranCombo(c, c.getPlayer(), -10);
                break;
            case SPECIAL_ATTACK:
                PlayerHandler.specialAttack(slea, c, c.getPlayer());
                break;
            case SKILL_MACRO:
                PlayerHandler.ChangeSkillMacro(slea, c.getPlayer());
                break;
            case GIVE_FAME:
                PlayersHandler.GiveFame(slea, c, c.getPlayer());
                break;
            case TRANSFORM_PLAYER:
                PlayersHandler.TransformPlayer(slea, c, c.getPlayer());
                break;
            case NOTE_ACTION:
                PlayersHandler.Note(slea, c.getPlayer());
                break;
            case USE_DOOR:
                PlayersHandler.UseDoor(slea, c.getPlayer());
                break;
            case USE_MECH_DOOR:
                PlayersHandler.UseMechDoor(slea, c.getPlayer());
                break;
            case DAMAGE_REACTOR:
                PlayersHandler.HitReactor(slea, c);
                break;
            case TOUCH_REACTOR:
                PlayersHandler.TouchReactor(slea, c);
                break;
            case USE_RUNE:
                PlayersHandler.UseRune(slea, c.getPlayer());
                break;
            case USE_RUNE_SKILL_REQ:
                PlayersHandler.UseRuneSkillReq(slea, c.getPlayer());
                break;
            case CLOSE_CHALKBOARD:
                c.getPlayer().setChalkboard(null);
                break;
            case ITEM_SORT:
                InventoryHandler.ItemSort(slea, c);
                break;
            case ITEM_GATHER:
                InventoryHandler.ItemGather(slea, c);
                break;
            case ITEM_MOVE:
                InventoryHandler.ItemMove(slea, c);
                break;
            case MOVE_BAG:
                InventoryHandler.MoveBag(slea, c);
                break;
            case SWITCH_BAG:
                InventoryHandler.SwitchBag(slea, c);
                break;
            case ITEM_MAKER:
                ItemMakerHandler.ItemMaker(slea, c);
                break;
            case ITEM_PICKUP:
                InventoryHandler.Pickup_Player(slea, c, c.getPlayer());
                break;
            case USE_CASH_ITEM: //使用商城道具
                UseCashItemHandler.handlePacket(slea, c, c.getPlayer());
                break;
            case USE_ADDITIONAL_ADD_ITEM:
                InventoryHandler.UseAdditionalAddItem(slea, c, c.getPlayer());
                break;
            case USE_ITEM: //使用物品道具
                InventoryHandler.UseItem(slea, c, c.getPlayer());
                break;
            case USE_COSMETIC: //使用理发卷
                InventoryHandler.UseCosmetic(slea, c, c.getPlayer());
                break;
            case USE_REDUCER: //使用还原器
                InventoryHandler.UseReducer(slea, c, c.getPlayer());
                break;
            case USE_REDUCER_PRESTIGE: //使用声望重置能力
                InventoryHandler.UseReducerPrestige(slea, c, c.getPlayer());
                break;
            case USE_MAGNIFY_GLASS:
                InventoryHandler.UseMagnify(slea, c, c.getPlayer());
                break;
            case USE_CRAFTED_CUBE:
                InventoryHandler.applyBlackCube(slea, c, c.getPlayer());
                break;
            case USE_SCRIPTED_NPC_ITEM:
                InventoryHandler.UseScriptedNPCItem(slea, c, c.getPlayer());
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.UseReturnScroll(slea, c, c.getPlayer());
                break;
            case USE_UPGRADE_SCROLL:
            case USE_EXITEM_UPGRADE:
                ItemScrollHandler.handlePacket(slea, c, c.getPlayer(), false);
                break;
            case USE_FLAG_SCROLL: // 这个是改变装备的状态 比如使用防暴卷什么的 但是防暴卷在 道具栏使用就检测不到
                ItemScrollHandler.handlePacket(slea, c, c.getPlayer(), true);
                break;
            case USE_POTENTIAL_SCROLL:
            case USE_POTENTIAL_ADD_SCROLL:
            case USE_EQUIP_SCROLL:
                ItemScrollHandler.handlePacket(slea, c, c.getPlayer(), false);
                break;
            case USE_ADDITIONAL_ITEM: //使用潜能附加印章
                InventoryHandler.UseAdditionalItem(slea, c, c.getPlayer());
                break;
            case USE_SOULS_SCROLL:
                InventoryHandler.UseSoulEnchanter(slea, c, c.getPlayer());
                break;
            case USE_SOUL_MARBLE:
                InventoryHandler.UseSoulScroll(slea, c, c.getPlayer());
                break;
            case USE_MIRACLE_CUBE:
                InventoryHandler.UseMiracleCube(slea, c.getPlayer());
                break;
            case USE_SUMMON_BAG:
                InventoryHandler.UseSummonBag(slea, c, c.getPlayer());
                break;
            case USE_TREASUER_CHEST:
                InventoryHandler.UseTreasureChest(slea, c, c.getPlayer());
                break;
            case USE_SKILL_BOOK: //使用技能书
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseSkillBook((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case USE_SP_RESET: //使用SP初始化卷轴
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseSpReset((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case USE_AP_RESET: //使用AP初始化卷轴
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseApReset((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case USE_CATCH_ITEM:
                InventoryHandler.UseCatchItem(slea, c, c.getPlayer());
                break;
            case USE_MOUNT_FOOD:
                InventoryHandler.UseMountFood(slea, c, c.getPlayer());
                break;
            case REWARD_ITEM:
                InventoryHandler.UseRewardItem((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case BUY_CROSS_ITEM: //购买十字猎人商店道具
                InventoryHandler.BuyCrossHunterItem(slea, c, c.getPlayer());
                break;
            case MOVE_LIFE:
                MobHandler.MoveMonster(slea, c, c.getPlayer());
                break;
            case AUTO_AGGRO:
                MobHandler.AutoAggro(slea.readInt(), c.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.FriendlyDamage(slea, c.getPlayer());
                break;
            case MONSTER_BOMB:
                MobHandler.MonsterBomb(slea.readInt(), c.getPlayer());
                break;
            case MONSTER_BOMB_COLLISION_GROUP:
                break;
            case MONSTER_SPECIAL_SKILL:
                MobHandler.MonsterSpecialSkill(slea, c.getPlayer());
                break;
            case NPC_SHOP:
                NPCHandler.NPCShop(slea, c, c.getPlayer());
                break;
            case NPC_TALK:
                NPCHandler.NPCTalk(slea, c, c.getPlayer());
                break;
            case NPC_TALK_MORE:
                NPCHandler.NPCMoreTalk(slea, c);
                break;
            case NPC_ACTION:
                NPCHandler.NPCAnimation(slea, c);
                break;
            case QUEST_ACTION: //任务操作
                NPCHandler.QuestAction(slea, c, c.getPlayer());
                break;
            case REISSUE_MEDAL: //重新领取勋章
                PlayerHandler.ReIssueMedal(slea, c, c.getPlayer());
                break;
            case STORAGE: //仓库操作
                NPCHandler.Storage(slea, c, c.getPlayer());
                break;
            case GENERAL_CHAT:
                if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
                    c.getPlayer().updateTick(slea.readInt());
                    ChatHandler.GeneralChat(slea.readMapleAsciiString(), slea.readByte(), c, c.getPlayer());
                }
                break;
            case PARTYCHAT:
                ChatHandler.Others(slea, c, c.getPlayer());
                break;
            case WHISPER:
                ChatHandler.Whisper_Find(slea, c);
                break;
            case MESSENGER:
                ChatHandler.Messenger(slea, c);
                break;
            case SHOW_LOVE_RANK:
                ChatHandler.ShowLoveRank(slea, c);
                break;
            case SPAWN_ARROWS_TURRET:
                PlayerHandler.SpawnArrowsTurret(slea, c, c.getPlayer());
                break;
            case USE_GROWTH_HELPER_REQUEST:
                PlayerHandler.UserGrowthHelperRequest(slea, c, c.getPlayer());
                break;
            case WARLOCK_MAGIC_ATTACK:
                PlayerHandler.WarLockMagicDamage(slea, c, c.getPlayer());
                break;
            case ENTER_STARTPLANET:
                InterServerHandler.EnterMTS(c, c.getPlayer());
                break;
            case TRACK_FLAMES:
                PlayerHandler.showTrackFlames(slea, c, c.getPlayer());
                break;
            case AUTO_ASSIGN_AP:
                StatsHandling.AutoAssignAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_AP:
                StatsHandling.DistributeAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_SP: //分配普通技能点
                StatsHandling.DistributeSP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_HYPER_AP:
                c.getPlayer().updateTick(slea.readInt());
                StatsHandling.DistributeHyperSP(slea.readInt(), c, c.getPlayer(), true);
                break;
            case DISTRIBUTE_HYPER_SP: //分配超级技能点
                c.getPlayer().updateTick(slea.readInt());
                StatsHandling.DistributeHyperSP(slea.readInt(), c, c.getPlayer(), false);
                break;
            case RESET_HYPER_SP: //初始化超级技能点
                StatsHandling.ResetHyperSP(slea, c, c.getPlayer());
                break;
            case RESET_HYPER_AP:
                StatsHandling.ResetHyperAP(slea, c, c.getPlayer());
                break;
            case PLAYER_INTERACTION:
                PlayerInteractionHandler.PlayerInteraction(slea, c, c.getPlayer());
                break;
            case GUILD_OPERATION:
                GuildHandler.Guild(slea, c);
                break;
            case DENY_GUILD_REQUEST:
                slea.skip(1);
                GuildHandler.DenyGuildRequest(slea.readMapleAsciiString(), c);
                break;
            case GUILD_APPLY:
                GuildHandler.GuildApply(slea, c);
                break;
            case ACCEPT_GUILD_APPLY:
                GuildHandler.AcceptGuildApply(slea, c);
                break;
            case DENY_GUILD_APPLY:
                GuildHandler.DenyGuildApply(slea, c);
                break;
//            case ALLIANCE_OPERATION:
//                AllianceHandler.HandleAlliance(slea, c, false);
//                break;
//            case DENY_ALLIANCE_REQUEST:
//                AllianceHandler.HandleAlliance(slea, c, true);
//                break;
            case QUICK_MOVE: //快速移动
                NPCHandler.OpenQuickMoveNpc(slea, c);
                break;
            case BBS_OPERATION:
                BBSHandler.BBSOperation(slea, c);
                break;
            case SELECT_JAGUAR:
                PlayerHandler.selectJaguar(slea, c, c.getPlayer());
                break;
            case GIVE_KSPSYCHIC:
                PlayerHandler.showKSPsychicGrabHanlder(slea, c, c.getPlayer());
                break;
            case ATTACK_KSPSYCHIC:
                PlayerHandler.showKSPsychicAttackHanlder(slea, c, c.getPlayer());
                break;
            case CANCEL_KSPSYCHIC:
                PlayerHandler.showKSPsychicReleaseHanlder(slea, c, c.getPlayer());
                break;
            case GIVE_KSULTIMATE:
                PlayerHandler.showGiveKSUltimate(slea, c, c.getPlayer());
                break;
            case ATTACK_KSULTIMATE:
                PlayerHandler.showAttackKSUltimate(slea, c, c.getPlayer());
                break;
            case MIST_KSULTIMAT:
                PlayerHandler.showKSMonsterEffect(slea, c, c.getPlayer());
                break;
            case CANCEL_KSULTIMATE:
                PlayerHandler.showCancelKSUltimate(slea, c, c.getPlayer());
                break;
            case PARTY_OPERATION:
                PartyHandler.PartyOperation(slea, c);
                break;
            case DENY_PARTY_REQUEST:
                PartyHandler.DenyPartyRequest(slea, c);
                break;
            case ALLOW_PARTY_INVITE:
                PartyHandler.AllowPartyInvite(slea, c);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.BuddyOperation(slea, c);
                break;
//            case CYGNUS_SUMMON:
//                UserInterfaceHandler.CygnusSummon_NPCRequest(c);
//                break;
//            case SHIP_OBJECT:
//                UserInterfaceHandler.ShipObjectRequest(slea.readInt(), c);
//                break;
            case BUY_CS_ITEM: //购买商城道具
                BuyCashItemHandler.BuyCashItem(slea, c, c.getPlayer());
                break;
            case COUPON_CODE: //用兑换码兑换商城道具
                CouponCodeHandler.handlePacket(slea, c, c.getPlayer());
                break;
            case CS_UPDATE: //刷新点卷
                CashShopOperation.CSUpdate(c);
                break;
            case SEND_CS_GIFI: //商城送礼
                BuyCashItemHandler.商城送礼(slea, c, c.getPlayer());
                break;
            case OPEN_AVATAR_RANDOM_BOX:
                break;
//            case TOUCHING_MTS:
//                MTSOperation.MTSUpdate(MTSStorage.getInstance().getCart(c.getPlayer().getId()), c);
//                break;
//            case MTS_TAB:
//                MTSOperation.MTSOperation(slea, c);
//                break;
            case USE_POT:
                ItemMakerHandler.UsePot(slea, c);
                break;
            case CLEAR_POT:
                ItemMakerHandler.ClearPot(slea, c);
                break;
            case FEED_POT:
                ItemMakerHandler.FeedPot(slea, c);
                break;
            case CURE_POT:
                ItemMakerHandler.CurePot(slea, c);
                break;
            case REWARD_POT:
                ItemMakerHandler.RewardPot(slea, c);
                break;
            case MOVE_LITTLEWHITE:
                SummonHandler.MoveLittleWhite(slea, c.getPlayer());
                break;
            case DAMAGE_SUMMON:
                SummonHandler.DamageSummon(slea, c.getPlayer());
                break;
            case MOVE_SUMMON:
                SummonHandler.MoveSummon(slea, c.getPlayer());
                break;
            case MOVE_DRAGON: //龙龙移动
                SummonHandler.MoveDragon(slea, c.getPlayer());
                break;
            case DRAGON_FLY: //龙飞行
                SummonHandler.DragonFly(slea, c.getPlayer());
                break;
            case SUB_SUMMON:
                SummonHandler.SubSummon(slea, c.getPlayer());
                break;
            case REMOVE_SUMMON:
                SummonHandler.RemoveSummon(slea, c);
                break;
            case SPAWN_PET:
                PetHandler.SpawnPet(slea, c, c.getPlayer());
                break;
            case PET_AUTO_BUFF:
                PetHandler.Pet_AutoBuff(slea, c, c.getPlayer());
                break;
            case MOVE_PET:
                PetHandler.MovePet(slea, c.getPlayer());
                break;
            case PET_CHAT:
                PetHandler.PetChat(slea, c, c.getPlayer());
                break;
            case PET_COMMAND:
                PetHandler.PetCommand(slea, c, c.getPlayer());
                break;
            case PET_FOOD:
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            case PET_LOOT:
                InventoryHandler.Pickup_Pet(slea, c, c.getPlayer());
                break;
            case PET_AUTO_POT:
                PetHandler.Pet_AutoPotion(slea, c, c.getPlayer());
                break;
            case PET_EXCEPTION_LIST:
                PetHandler.PetExcludeItems(slea, c, c.getPlayer());
                break;
            case PET_AOTO_EAT:
                slea.skip(4);
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            case ALLOW_PET_LOOT: //是否允许宠物捡取道具
                PetHandler.AllowPetLoot(slea, c, c.getPlayer());
                break;
            case ALLOW_PET_AOTO_EAT: //是否允许宠物自动喂食
                PetHandler.AllowPetAutoEat(slea, c, c.getPlayer());
                break;
            case USE_OPTION_CHANGE_REQUEST:
                UseHammerHandler.GoldHammerResponse(slea, c);
                break;
            case DUEY_ACTION:
                DueyHandler.DueyOperation(slea, c);
                break;
            case USE_HIRED_MERCHANT:
                HiredMerchantHandler.UseHiredMerchant(c, true);
                break;
            case MERCH_ITEM_STORE:
                HiredMerchantHandler.MerchantItemStore(slea, c);
                break;
            case FISHING:
                HiredFisherHandler.INSTANCE.UseHiredFisher(slea, c.getPlayer());
                break;
            case CANCEL_DEBUFF:
                // Ignore for now
                break;
            case REPAIR:
                NPCHandler.repair(slea, c);
                break;
            case REPAIR_ALL:
                NPCHandler.repairAll(c);
                break;
            case OWL:
                InventoryHandler.Owl(slea, c);
                break;
            case OWL_WARP:
                InventoryHandler.OwlWarp(slea, c);
                break;
            case USE_OWL_MINERVA:
                InventoryHandler.OwlMinerva(slea, c);
                break;
            case RPS_GAME:
                NPCHandler.RPSGame(slea, c);
                break;
//            case UPDATE_QUEST:
//                NPCHandler.UpdateQuest(slea, c);
//                break;
//            case USE_ITEM_QUEST:
//                NPCHandler.UseItemQuest(slea, c);
//                break;
            case FOLLOW_REQUEST:
                PlayersHandler.FollowRequest(slea, c);
                break;
            case AUTO_FOLLOW_REPLY:
            case FOLLOW_REPLY:
                PlayersHandler.FollowReply(slea, c);
                break;
            case RING_ACTION:
                PlayersHandler.RingAction(slea, c);
                break;
//            case REQUEST_FAMILY:
//                FamilyHandler.RequestFamily(slea, c);
//                break;
//            case OPEN_FAMILY:
//                FamilyHandler.OpenFamily(slea, c);
//                break;
//            case FAMILY_OPERATION:
//                FamilyHandler.FamilyOperation(slea, c);
//                break;
//            case DELETE_JUNIOR:
//                FamilyHandler.DeleteJunior(slea, c);
//                break;
//            case DELETE_SENIOR:
//                FamilyHandler.DeleteSenior(slea, c);
//                break;
//            case USE_FAMILY:
//                FamilyHandler.UseFamily(slea, c);
//                break;
//            case FAMILY_PRECEPT:
//                FamilyHandler.FamilyPrecept(slea, c);
//                break;
//            case FAMILY_SUMMON:
//                FamilyHandler.FamilySummon(slea, c);
//                break;
//            case ACCEPT_FAMILY:
//                FamilyHandler.AcceptFamily(slea, c);
//                break;
            case SOLOMON:
                PlayersHandler.Solomon(slea, c);
                break;
            case GACH_EXP:
                PlayersHandler.GachExp(slea, c);
                break;
            case PARTY_SEARCH_START:
                PartyHandler.MemberSearch(slea, c);
                break;
            case PARTY_SEARCH_STOP:
                PartyHandler.PartySearch(slea, c);
                break;
            case EXPEDITION_LISTING:
                PartyHandler.PartyListing(slea, c);
                break;
            case EXPEDITION_OPERATION:
                PartyHandler.Expedition(slea, c);
                break;
            case USE_TELE_ROCK:
                InventoryHandler.TeleRock(slea, c);
                break;
            case PAM_SONG:
                InventoryHandler.PamSong(slea, c);
                break;
            case REPORT:
                PlayersHandler.Report(slea, c);
                break;
            case REMOTE_STORE: //雇佣商人遥控器
                HiredMerchantHandler.RemoteStore(slea, c);
                break;
            case SHIKONGJUAN: //超时空券
                PlayerHandler.UseChronosphere(slea, c, c.getPlayer());
                break;
            case PLAYER_UPDATE: //玩家数据更新
                PlayerHandler.PlayerUpdate(c, c.getPlayer());
                break;
            case CHANGE_MARKET_MAP:
                PlayerHandler.ChangeMarketMap(slea, c, c.getPlayer());
                break;
            case DEL_TEACH_SKILL:
                PlayerHandler.DelTeachSkill(slea, c, c.getPlayer());
                break;
            case SET_TEACH_SKILL: //传授技能
                PlayerHandler.SetTeachSkill(slea, c, c.getPlayer());
                break;
            case SET_CHAR_CASH:
                PlayerHandler.showPlayerCash(slea, c);
                break;
            case OPEN_WORLDMAP:
                c.announce(MaplePacketCreator.openWorldMap());
                break;
            case SAVE_DAMSKIN:
                PlayerHandler.SaveDamageSkin(slea, c.getPlayer());
                break;
            case CHANGE_DAMSKIN:
                PlayerHandler.ChangeDamageSkin(slea, c.getPlayer());
                break;
            case DELETE_DAMSKIN:
                PlayerHandler.DeleteDamageSkin(slea, c.getPlayer());
                break;
            case USE_HAMMER: //使用金锤子
                UseHammerHandler.UseHammer(slea, c);
                break;
            case HAMMER_RESPONSE: //使用金锤子
                UseHammerHandler.HammerResponse(slea, c);
                break;
            case BATTLE_STATISTICS:
                PlayerHandler.startBattleStatistics(slea, c);
                break;
            case MEMORY_SKILL_CHOOSE:  //装备复制技能
                PhantomMemorySkill.MemorySkillChoose(slea, c);
                break;
            case MEMORY_SKILL_CHANGE: //删除选择技能
                PhantomMemorySkill.MemorySkillChange(slea, c);
                break;
            case MEMORY_SKILL_OBTAIN: //封印之瞳
                PhantomMemorySkill.MemorySkillObtain(slea, c);
                break;
            case PLAYER_VIEW_RANGE:
                byte by2 = slea.readByte();
                if (by2 == 1) {
                    c.sendEnableActions();
                }
                break;
            case CHANGE_POTENTIAL: // 神之子更改武器潜能
                ItemScrollHandler.ChangeWeaponPotential(slea, c, c.getPlayer());
                break;
            case CHANGE_POTENTIAL_WP:
                ItemScrollHandler.ChangeWeaponPotential_WP(slea, c, c.getPlayer());
                break;
            case USE_TEMPEST_BLADES: //使用剑刃之壁
                PlayerHandler.useTempestBlades(slea, c, c.getPlayer());
                break;
            case UNKNOWN_168:
                /*
                 * 68 01
                 * 09 00 31 32 33 31 32 33 31 32 33 - 角色名字
                 */
                c.announce(MaplePacketCreator.sendUnkPacket1FC());
                break;
            case QUICK_BUY_CS_ITEM: //快速扩充和购买商城道具
                PlayerHandler.quickBuyCashShopItem(slea, c, c.getPlayer());
                break;
            case SYSTEM_PROCESS_LIST:
                SystemProcess.SystemProcess(slea, c, c.getPlayer());
                break;
            case FAMILIAR_OPERATION:
                PlayerHandler.useFamiliarCard(slea, c, c.getPlayer());
                break;
            case SOUL_MODE:
                PlayerHandler.updateSoulEffect(slea, c, c.getPlayer());
                break;
            case USE_TOWERCHAIR_SETTING:
                PlayerHandler.UseTowerChairSetting(slea, c, c.getPlayer());
                break;
            case VMATRIX_MAKE_REQUEST:
                PlayerHandler.VCoreOperation(slea, c.getPlayer());
                break;
            case VMATRIX_HELP_REQUEST:
                PlayerHandler.VmatrixHelpRequest(slea, c.getPlayer());
                break;
            case SIGNIN_OPERATION:
                PlayerHandler.openSigin(slea, c, c.getPlayer());
                break;
            case MULTI_SKILL_ATTACK_REQUEST:
                PlayerHandler.MulitSkillAttackRequest(slea, c.getPlayer());
                break;
            case MULTI_SKILL_CHARGE_REQUEST:
                PlayerHandler.MultiSkillChargeRequest(slea, c.getPlayer());
                break;
            case POTION_POT_USE:
                PotionPotHandler.PotionPotUse(slea, c, c.getPlayer());
                break;
            case POTION_POT_ADD:
                PotionPotHandler.PotionPotAdd(slea, c, c.getPlayer());
                break;
            case POTION_POT_MODE:
                PotionPotHandler.PotionPotMode(slea, c, c.getPlayer());
                break;
            case POTION_POT_INCR:
                PotionPotHandler.PotionPotIncr(slea, c, c.getPlayer());
                break;
            case APPLY_HYUNCUBE:
                InventoryHandler.applyHyunCube(slea, c, c.getPlayer());
                break;
            case CHANGE_ZERO_LOOK:
                PlayerHandler.changeZeroLook(slea, c, c.getPlayer(), false);
                break;
            case CHANGE_ZERO_LOOK_END:
                PlayerHandler.changeZeroLook(slea, c, c.getPlayer(), true);
                break;
            case EXTRA_ATTACK:
                PlayerHandler.ExtraAttack(slea, c, c.getPlayer());
                break;
            case SHOW_FIREWALL_REQUEST:
                PlayerHandler.spawnSpecial(slea, c, c.getPlayer());
                break;
            case MOVE_ENERGY:
                PlayerHandler.MoveEnergyBall(slea, c);
                break;
            case HIDDEN_TAIL_ADN_EAR:
                if (c.getPlayer() != null) {
                    c.getPlayer().hiddenTailAndEar(slea.readInt());
                }
                break;
            case DF_COMBO:
                PlayerHandler.absorbingDF(slea, c, c.getPlayer());
                break;
            case USER_TRUMP_SKILL_ACTION_REQUEST:
                PlayerHandler.UserTrumpSkillActionRequest(slea, c.getPlayer());
                break;
            case DOT_HEAL_HP_REQUEST:
                PlayerHandler.DotHealHPRequest(c.getPlayer());
                break;
            case EFFECT_SWITCH:
                PlayerHandler.effectSwitch(slea, c);
                break;
            case CALL_FRIENDS:
                PlayerHandler.callFriends(slea, c, c.getPlayer());
                break;
            case SELECT_CHAIR:
                PlayerHandler.selectChair(slea, c, c.getPlayer());
                break;
            case TAP_JOY_RESPONSE:
                UseCashItemHandler.TapJoyResponse(slea, c.getPlayer());
                break;
            case TAP_JOY_DONE:
                UseCashItemHandler.TapJoyDone(slea, c.getPlayer());
                break;
            case TAP_JOY_NEXT_STAGE:
                UseCashItemHandler.TapJoyNextStage(slea, c.getPlayer());
                break;
            default:
                System.out.println("[未处理封包] Recv " + header.toString() + " [" + HexTool.getOpcodeToString(header.getValue()) + "]");
                break;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 开始检测IP
        String address = ctx.channel().remoteAddress().toString().split(":")[0];
        if (BlockIPList.contains(address)) { //是否在禁止的IP列表
            ctx.channel().close();
            return;
        }
        if (!address.equals("/127.0.0.1")) { // 过滤本地IP  && (type == ServerType.登录服务器 || type == ServerType.聊天服务器 || type == ServerType.频道服务器)
            Pair<Long, Byte> track = tracker.get(address);
            byte count;
            if (track == null) {
                count = 1;
            } else {
                count = track.right;
                long difference = System.currentTimeMillis() - track.left;
                if (difference < 2000) { //同一个IP地址连接时间检测 当前为2秒
                    count++;
                } else if (difference > 20000) { //清除连接次数的时间 当前为20秒
                    count = 1;
                }
                if (preventIpAttack && count > 5) { // 单个IP的连接上限 达到多少次就禁止连接
                    BlockIPList.add(address);
                    tracker.remove(address); // Cleanup
                    ctx.channel().close();
                    return;
                }
            }
            tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
            // 结束IP检测.
        }

        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) { //如果频道是关闭的就断开连接
                ctx.channel().close();
                return;
            }
        } else if (type == ServerType.商城服务器) {
            if (CashShopServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else if (type == ServerType.登录服务器) {
            if (LoginServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else if (type == ServerType.聊天服务器) {
            if (ChatServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        }

        byte ivRecv[] = {70, 114, 122, 82};
        byte ivSend[] = {82, 48, 120, 115};
        ivRecv[3] = (byte) (Math.random() * 255);
        ivSend[3] = (byte) (Math.random() * 255);
        MapleAESOFB sendCypher = new MapleAESOFB(ivSend, (short) (0xFFFF - ServerConfig.LOGIN_MAPLE_VERSION));
        MapleAESOFB recvCypher = new MapleAESOFB(ivRecv, ServerConfig.LOGIN_MAPLE_VERSION);
        MapleClient client = new MapleClient(sendCypher, recvCypher, ctx.channel());
        client.setSessionId(Randomizer.nextLong());
        client.setChannel(channel);
        client.setWorld(world);

        ctx.channel().writeAndFlush(LoginPacket.getHello(ServerConfig.LOGIN_MAPLE_VERSION, ivSend, ivRecv, type));

        ctx.channel().attr(MapleClient.CLIENT_KEY).set(client);

        if (ServerConstants.isShowPacket()) {
            RecvPacketOpcode.reloadValues();
            SendPacketOpcode.reloadValues();
            CashShopOpcode.reloadValues();
        }

        StringBuilder sb = new StringBuilder();
        if (channel > -1) {
            sb.append("[Channel Server] Channel ").append(channel).append(" : ");
        } else if (type == ServerType.商城服务器) {
            sb.append("[Cash Server] ");
        } else if (type == ServerType.聊天服务器) {
            sb.append("[Chat Server]");
        } else {
            sb.append("[Login Server] ");
        }
        sb.append("IoSession opened ").append(address);
        System.out.println(sb.toString());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (client != null) {
            try {
                client.disconnect(true, ServerType.商城服务器.equals(type));
            } catch (Throwable t) {
                log.error("连接异常关闭", t);
            } finally {
                ctx.channel().attr(MapleClient.CLIENT_KEY).set(null);
                ctx.channel().attr(MaplePacketDecoder.DECODER_STATE_KEY).set(null);
                ctx.channel().close();
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream((byte[]) msg));
        if (slea.available() < 2) {
            return;
        }
        MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (client == null || !client.isReceiving()) {
            return;
        }
        short packetId = slea.readShort();
        for (RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == packetId) {
                if (recv.NeedsChecking() && !client.isLoggedIn()) {
                    break;
                }
                try {
                    if (client.getPlayer() != null && client.isMonitored() && ServerConfig.DEBUG_MODE) {
                        try (FileWriter fw = new FileWriter(new File("日志/MonitorLogs/" + client.getPlayer().getName() + "_log.txt"), true)) {
                            fw.write(String.valueOf(recv) + " (" + Integer.toHexString(packetId) + ") Handled: \r\n" + slea.toString() + "\r\n");
                            fw.flush();
                        }
                    }
                    handlePacket(recv, slea, client, type);
                } catch (Exception e) {
                    log.error("封包: " + lookupRecv(packetId) + "\r\n" + slea.toString(true), e);
                }
                return;
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (client != null && type == ServerType.频道服务器) {
            client.sendPing();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();
            if (client != null && client.getPlayer() != null) {
                client.getPlayer().saveToCache();
                log.error("Exception caught by: " + client.getPlayer().getName(), cause);
            }
        }
    }

    private String lookupRecv(short header) {
        for (RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header) {
                return recv.name();
            }
        }
        return "UNKNOWN";
    }
}
