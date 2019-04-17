package handling.world;

import client.ClientRedirector;
import client.MapleClient;
import configs.ServerConfig;
import handling.Auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import server.shops.HiredFisher;
import server.shops.HiredMerchant;
import tools.CollectionUtil;

import java.util.*;

public class World {

    public static void init() {
        WorldFindService.getInstance();
        WorldBroadcastService.getInstance();
        WorldPartyService.getInstance();
        WorldAllianceService.getInstance();
        WorldBuddyService.getInstance();
        WorldFamilyService.getInstance();
        WorldGuildService.getInstance();
        WorldMessengerService.getInstance();
    }

    public static String getStatus() {
        StringBuilder ret = new StringBuilder();
        int totalUsers = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            ret.append("频道 ");
            ret.append(cs.getChannel());
            ret.append(": ");
            int channelUsers = cs.getConnectedClients();
            totalUsers += channelUsers;
            ret.append(channelUsers);
            ret.append(" 玩家\n");
        }
        ret.append("总计在线: ");
        ret.append(totalUsers);
        ret.append("\n");
        return ret.toString();
    }

    public static Map<Integer, Integer> getConnected() {
        Map<Integer, Integer> ret = new LinkedHashMap<>();
        int total = 0;
        for (ChannelServer ch : ChannelServer.getAllInstances()) {
            int chOnline = ch.getConnectedClients();
            ret.put(ch.getChannel(), chOnline);
            total += chOnline;
        }
        int csOnline = CashShopServer.getConnectedClients();
        ret.put(-10, csOnline); //-10 是商城 -20 是拍卖
        total += csOnline;
        //最后将总数加入 0 表示世界服务器总计在线
        ret.put(0, total);
        return ret;
    }

    public static List<CheaterData> getCheaters() {
        List<CheaterData> allCheaters = new ArrayList<>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            allCheaters.addAll(cs.getCheaters());
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    public static List<CheaterData> getReports() {
        List<CheaterData> allCheaters = new ArrayList<>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            allCheaters.addAll(cs.getReports());
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    public static boolean isConnected(String charName) {
        return WorldFindService.getInstance().findChannel(charName) > 0;
    }

    public static void toggleMegaphoneMuteState() {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.toggleMegaphoneMuteState();
        }
    }

    public static void ChannelChange_Data(CharacterTransfer Data, int characterid, int toChannel) {
        getStorage(toChannel).registerPendingPlayer(Data, characterid);
    }

    public static boolean isCharacterListConnected(List<String> charNames) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (String name : charNames) {
                if (cserv.isConnected(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getAllowLoginTip(List<String> charNames) {
        StringBuilder ret = new StringBuilder("账号下其他角色在游戏: ");
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            for (String name : charNames) {
                if (cserv.isConnected(name)) {
                    ret.append(name);
                    ret.append(" ");
                }
            }
        }
        return ret.toString();
    }

    /*
     * 检测账号是否开过雇佣商店
     */
    public static boolean hasMerchant(int accountID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            if (cs.containsMerchant(accountID)) {
                return true;
            }
        }
        return false;
    }

    /*
     * 检测账号下的玩家是否开过雇佣商店
     */
    public static boolean hasMerchant(int accountID, int characterID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            if (cs.containsMerchant(accountID, characterID)) {
                return true;
            }
        }
        return false;
    }

    /*
     * 获取账号下的玩家的雇佣商店信息
     * 返回 雇佣商店
     */
    public static HiredMerchant getMerchant(int accountID, int characterID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            if (cs.containsMerchant(accountID, characterID)) {
                return cs.getHiredMerchants(accountID, characterID);
            }
        }
        return null;
    }

    public static PlayerStorage getStorage(int channel) {
        if (channel == -20) {
            return AuctionServer.getPlayerStorage();
        } else if (channel == -10) {
            return CashShopServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(channel).getPlayerStorage();
    }

    public static boolean isChannelAvailable(int ch) {
        return !(ChannelServer.getInstance(ch) == null || ChannelServer.getInstance(ch).getPlayerStorage() == null) && ChannelServer.getInstance(ch).getPlayerStorage().getConnectedClients() < ServerConfig.LOGIN_USERLIMIT;
    }

    public static boolean hasFisher(int accountID, int characterID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            if (cs.containsFisher(accountID, characterID)) {
                return true;
            }
        }
        return false;
    }

    public static HiredFisher getFisher(int accountID, int characterID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            if (cs.containsFisher(accountID, characterID)) {
                return cs.getHiredFisher(accountID, characterID);
            }
        }
        return null;
    }

    public static class Redirector {

        private static final Map<String, ClientRedirector> redirectors = new HashMap<>();

        public static String addRedirector(MapleClient c) {
            for (Map.Entry<String, ClientRedirector> redirector : redirectors.entrySet()) {
                if (redirector.getValue().getAccount().equals(c.getAccountName())) {
                    redirectors.remove(redirector.getKey());
                    break;
                }
            }
            Random random = new Random();
            String code = "";
            for (int i = 0; i < 200; i++) {
                switch (random.nextInt(10)) {
                    case 0:
                        code += "_";
                        break;
                    case 1:
                        code += "~";
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        int temp = random.nextInt(2) == 0 ? 0x41 : 0x61;
                        code += (char) (random.nextInt(0x1A) + temp);
                        break;
                    default:
                        code += String.valueOf(random.nextInt(10));
                }
            }
            redirectors.put(code, new ClientRedirector(c.getAccountName(), c.getWorld(), c.getChannel(), false));
            return code;
        }

        public static Map<String, ClientRedirector> getRedirectors() {
            return redirectors;
        }
    }

    public static class Client {

        private static final ArrayList<MapleClient> clients = new ArrayList<>();

        public static void addClient(MapleClient c) {
            if (!clients.contains(c)) {
                clients.add(c);
            }
        }

        public static boolean removeClient(MapleClient c) {
            return clients.remove(c);
        }

        public static ArrayList<MapleClient> getClients() {
            return clients;
        }
    }
}
