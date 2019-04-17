package server.cashshop;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.*;
import server.cashshop.CashItemInfo.CashModInfo;
import tools.Pair;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CashItemFactory {

    private static final Logger log = LogManager.getLogger(CashItemFactory.class.getName());
    private final static int[] bestItems = new int[]{30200045, 50000080, 30200066, 50400016, 30100092};
    private static final CashItemFactory instance = new CashItemFactory();
    private final List<CashItemCategory> categories = new LinkedList<>();
    private final Map<Integer, CashModInfo> itemMods = new HashMap<>();
    private final Map<Integer, CashItemForSql> menuItems = new HashMap<>();
    private final Map<Integer, CashItemForSql> categoryItems = new HashMap<>();
    private final Map<Integer, CashItemInfo> itemStats = new HashMap<>(); //商城道具状态
    private final Map<Integer, Integer> idLookup = new HashMap<>(); //商城道具的SN集合
    private final Map<Integer, CashItemInfo> oldItemStats = new HashMap<>(); //老版本的商城道具状态
    private final Map<Integer, Integer> oldIdLookup = new HashMap<>(); //老版本的商城道具的SN集合
    private final Map<Integer, List<Integer>> itemPackage = new HashMap<>(); //礼包信息
    private final Map<Integer, List<Pair<Integer, Integer>>> openBox = new HashMap<>(); //箱子道具物品
    private final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Etc.wz"));
    private final MapleData commodities = data.getData("Commodity.img");
    private final List<Integer> blockRefundableItemId = new LinkedList<>(); //禁止使用回购的道具 也就是有些道具有多个SN信息 而每个SN下的价格又不一样
    private final Map<Integer, Byte> baseNewItems = new HashMap<>();

    public static CashItemFactory getInstance() {
        return instance;
    }

    public void initialize() {
        blockRefundableItemId.clear();
        int onSaleSize = 0;
        Map<Integer, Integer> fixId = new HashMap<>(); //检测WZ中是否有重复价格的道具 [SN] [itemId]
        //加载商城道具
        for (MapleData field : commodities.getChildren()) {
            int SN = MapleDataTool.getIntConvert("SN", field, 0);
            int itemId = MapleDataTool.getIntConvert("ItemId", field, 0);
            int count = MapleDataTool.getIntConvert("Count", field, 1);
            int price = MapleDataTool.getIntConvert("Price", field, 0);
            int meso = MapleDataTool.getIntConvert("Meso", field, 0);
            int originalPrice = MapleDataTool.getIntConvert("originalPrice", field, 0);
            int period = MapleDataTool.getIntConvert("Period", field, 0);
            int gender = MapleDataTool.getIntConvert("Gender", field, 2);
            byte csClass = (byte) MapleDataTool.getIntConvert("Class", field, 0);
            byte priority = (byte) MapleDataTool.getIntConvert("Priority", field, 0);
            int termStart = MapleDataTool.getIntConvert("termStart", field, 0);
            int termEnd = MapleDataTool.getIntConvert("termEnd", field, 0);
            boolean onSale = MapleDataTool.getIntConvert("OnSale", field, 0) > 0 || isOnSalePackage(SN); //道具是否出售
            boolean bonus = MapleDataTool.getIntConvert("Bonus", field, 0) > 0; //是否有奖金红利？
            boolean refundable = MapleDataTool.getIntConvert("Refundable", field, 0) == 0; //道具是否可以回购
            boolean discount = MapleDataTool.getIntConvert("discount", field, 0) > 0; //是否打折出售
            if (onSale) {
                onSaleSize++;
            }
            CashItemInfo stats = new CashItemInfo(itemId, count, price, originalPrice, meso, SN, period, gender, csClass, priority, termStart, termEnd, onSale, bonus, refundable, discount);
            if (SN > 0) {
                itemStats.put(SN, stats);
                if (idLookup.containsKey(itemId)) {
                    fixId.put(SN, itemId);
                    blockRefundableItemId.add(itemId);
                }
                idLookup.put(itemId, SN);
            }
        }
        log.info("共加载 " + itemStats.size() + " 个商城道具 有 " + onSaleSize + " 个道具处于出售状态...");
        log.info("其中有 " + fixId.size() + " 重复价格的道具和 " + blockRefundableItemId.size() + " 个禁止换购的道具.");
        //加载商城礼包的信息
        MapleData packageData = data.getData("CashPackage.img");
        for (MapleData root : packageData.getChildren()) {
            if (root.getChildByPath("SN") == null) {
                continue;
            }
            List<Integer> packageItems = new ArrayList<>();
            for (MapleData dat : root.getChildByPath("SN").getChildren()) {
                packageItems.add(MapleDataTool.getIntConvert(dat));
            }
            itemPackage.put(Integer.parseInt(root.getName()), packageItems);
        }
        log.info("共加载 " + itemPackage.size() + " 个商城礼包...");
        //加载老的商城道具信息
        onSaleSize = 0;
        MapleDataDirectoryEntry root = data.getRoot();
        for (MapleDataEntry topData : root.getFiles()) {
            if (topData.getName().startsWith("OldCommodity")) {
                MapleData Commodity = data.getData(topData.getName());
                for (MapleData field : Commodity.getChildren()) {
                    int SN = MapleDataTool.getIntConvert("SN", field, 0);
                    int itemId = MapleDataTool.getIntConvert("ItemId", field, 0);
                    int count = MapleDataTool.getIntConvert("Count", field, 1);
                    int price = MapleDataTool.getIntConvert("Price", field, 0);
                    int meso = MapleDataTool.getIntConvert("Meso", field, 0);
                    int originalPrice = MapleDataTool.getIntConvert("originalPrice", field, 0);
                    int period = MapleDataTool.getIntConvert("Period", field, 0);
                    int gender = MapleDataTool.getIntConvert("Gender", field, 2);
                    byte csClass = (byte) MapleDataTool.getIntConvert("Class", field, 0);
                    byte priority = (byte) MapleDataTool.getIntConvert("Priority", field, 0);
                    int termStart = MapleDataTool.getIntConvert("termStart", field, 0);
                    int termEnd = MapleDataTool.getIntConvert("termEnd", field, 0);
                    boolean onSale = MapleDataTool.getIntConvert("OnSale", field, 0) > 0 || isOnSalePackage(SN); //道具是否出售
                    boolean bonus = MapleDataTool.getIntConvert("Bonus", field, 0) >= 0; //是否有奖金红利？
                    boolean refundable = MapleDataTool.getIntConvert("Refundable", field, 0) == 0; //道具是否可以回购
                    boolean discount = MapleDataTool.getIntConvert("discount", field, 0) >= 0; //是否打折出售
                    if (onSale) {
                        onSaleSize++;
                    }
                    CashItemInfo stats = new CashItemInfo(itemId, count, price, originalPrice, meso, SN, period, gender, csClass, priority, termStart, termEnd, onSale, bonus, refundable, discount);
                    if (SN > 0) {
                        oldItemStats.put(SN, stats);
                        oldIdLookup.put(itemId, SN);
                    }
                }
            }
        }
        log.info("共加载 " + oldItemStats.size() + " 个老的商城道具 有 " + onSaleSize + " 个道具处于出售状态...");

        loadMoifiedItemInfo();
        loadRandomItemInfo();

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_categories"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CashItemCategory cat = new CashItemCategory(rs.getInt("categoryid"), rs.getString("name"), rs.getInt("parent"), rs.getInt("flag"), rs.getInt("sold"));
                    categories.add(cat);
                }
            } catch (SQLException e) {
                log.error("Failed to load cash shop categories. ", e);
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_menuitems"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CashItemForSql item = new CashItemForSql(rs.getInt("category"), rs.getInt("subcategory"), rs.getInt("parent"), rs.getString("image"), rs.getInt("sn"), rs.getInt("itemid"), rs.getInt("flag"), rs.getInt("price"), rs.getInt("discountPrice"), rs.getInt("quantity"), rs.getInt("expire"), rs.getInt("gender"), rs.getInt("likes"));
                    menuItems.put(item.getSN(), item);
                }
            } catch (SQLException e) {
                log.error("Failed to load cash shop menuitems. ", e);
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_items"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CashItemForSql item = new CashItemForSql(rs.getInt("category"), rs.getInt("subcategory"), rs.getInt("parent"), rs.getString("image"), rs.getInt("sn"), rs.getInt("itemid"), rs.getInt("flag"), rs.getInt("price"), rs.getInt("discountPrice"), rs.getInt("quantity"), rs.getInt("expire"), rs.getInt("gender"), rs.getInt("likes"));
                    categoryItems.put(item.getSN(), item);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to load cash shop items. ", e);
        }
    }

    public boolean isOnSalePackage(int snId) {
        return snId >= 170200002 && snId <= 170200013;
    }

    public void loadMoifiedItemInfo() {
        itemMods.clear();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_modified_items"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CashModInfo ret = new CashModInfo(
                            rs.getInt("serial"),
                            rs.getInt("discount_price"),
                            rs.getInt("mark"),
                            rs.getInt("showup") > 0,
                            rs.getInt("itemid"),
                            rs.getInt("priority"),
                            rs.getInt("package") > 0,
                            rs.getInt("period"),
                            rs.getInt("gender"),
                            rs.getInt("count"),
                            rs.getInt("meso"),
                            rs.getInt("csClass"),
                            rs.getInt("termStart"),
                            rs.getInt("termEnd"),
                            rs.getInt("extra_flags"),
                            rs.getInt("fameLimit"),
                            rs.getInt("levelLimit"),
                            rs.getInt("categories"),
                            rs.getByte("bast_new") > 0);
                    itemMods.put(ret.getSn(), ret);
                    final CashItemInfo cc = itemStats.get(ret.getSn());
                    if (cc != null) {
                        ret.toCItem(cc); //init
                        if (ret.isBase_new()) {
                            baseNewItems.put(ret.getSn(), cc.getCsClass());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("cashshop_modified_items_error: ", e);
        }
        log.info("共加载了 " + itemMods.size() + " 个商品");
    }

    public void loadRandomItemInfo() {
        openBox.clear();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_randombox"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<Pair<Integer, Integer>> boxItems = openBox.computeIfAbsent(rs.getInt("randboxID"), integer -> new ArrayList<>());
                    boxItems.add(new Pair<>(rs.getInt("itemSN"), rs.getInt("count")));
                }
            }
        } catch (SQLException e) {
            log.error("加载商城随机箱子的信息出错.", e);
        }

        log.info("共加载 " + openBox.size() + " 个商城随机箱子的信息...");
    }

    public CashItemInfo getSimpleItem(int sn) {
        return itemStats.get(sn);
    }

    public Map<Integer, CashItemInfo> getAllItem() {
        return itemStats;
    }

    public boolean isBlockRefundableItemId(int itemId) {
        return blockRefundableItemId.contains(itemId);
    }

    public CashModInfo getModInfo(int sn) {
        return itemMods.get(sn);
    }

    public Map<Integer, CashModInfo> getAllModInfo() {
        return itemMods;
    }

    public Map<Integer, Byte> getAllBaseNewInfo() {
        return baseNewItems;
    }

    public CashItemInfo getItem(int sn) {
        return getItem(sn, true);
    }

    public CashItemInfo getItem(int sn, boolean checkSale) {
        CashItemInfo stats = itemStats.get(sn);
        CashModInfo z = getModInfo(sn);
        if (z != null && z.isShowUp()) {
            return z.toCItem(stats);
        }
        if (stats == null) {
            return null;
        }
        return checkSale && !stats.onSale() ? null : stats;
    }

    public CashItemForSql getMenuItem(int sn) {
        for (CashItemForSql ci : getMenuItems()) {
            if (ci.getSN() == sn) {
                return ci;
            }
        }
        return null;
    }

    public CashItemForSql getAllItem(int sn) {
        for (CashItemForSql ci : getAllItems()) {
            if (ci.getSN() == sn) {
                return ci;
            }
        }
        return null;
    }

    public List<Integer> getPackageItems(int itemId) {
        return itemPackage.get(itemId);
    }

    /*
     * 随机箱子道具
     */
    public Map<Integer, List<Pair<Integer, Integer>>> getRandomItemInfo() {
        return openBox;
    }

    public boolean hasRandomItem(int itemId) {
        return openBox.containsKey(itemId);
    }

    public List<Pair<Integer, Integer>> getRandomItem(int itemId) {
        return openBox.get(itemId);
    }

    public int[] getBestItems() {
        return bestItems;
    }

    public int getLinkItemId(int itemId) {
        switch (itemId) {
            case 5000029: //宝贝龙
            case 5000030: //绿龙
            case 5000032: //蓝龙
            case 5000033: //黑龙
            case 5000035: //红龙
                return 5000028; //进化龙
            case 5000048: //娃娃机器人
            case 5000049: //机器人(蓝色)
            case 5000050: //机器人(红色)
            case 5000051: //机器人(绿色)
            case 5000052: //机器人(金色)
                return 5000047; //罗伯
        }
        return itemId;
    }

    public int getSnFromId(int itemId) {
        if (idLookup.containsKey(itemId)) {
            return idLookup.get(itemId);
        }
        return 0;
    }

    public List<CashItemCategory> getCategories() {
        return categories;
    }

    public List<CashItemForSql> getMenuItems(int type) {
        List<CashItemForSql> items = new LinkedList<>();
        for (CashItemForSql ci : menuItems.values()) {
            if (ci.getSubCategory() / 10000 == type) {
                items.add(ci);
            }
        }
        return items;
    }

    public List<CashItemForSql> getMenuItems() {
        List<CashItemForSql> items = new LinkedList<>();
        for (CashItemForSql ci : menuItems.values()) {
            items.add(ci);
        }
        return items;
    }

    public List<CashItemForSql> getAllItems(int type) {
        List<CashItemForSql> items = new LinkedList<>();
        for (CashItemForSql ci : categoryItems.values()) {
            if (ci.getSubCategory() / 10000 == type) {
                items.add(ci);
            }
        }
        return items;
    }

    public List<CashItemForSql> getAllItems() {
        List<CashItemForSql> items = new LinkedList<>();
        for (CashItemForSql ci : categoryItems.values()) {
            items.add(ci);
        }
        return items;
    }

    public List<CashItemForSql> getCategoryItems(int subcategory) {
        List<CashItemForSql> items = new LinkedList<>();
        for (CashItemForSql ci : categoryItems.values()) {
            if (ci.getSubCategory() == subcategory) {
                items.add(ci);
            }
        }
        return items;
    }
}
