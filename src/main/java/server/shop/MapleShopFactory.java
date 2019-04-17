package server.shop;

import com.alibaba.druid.pool.DruidPooledConnection;
import constants.ItemConstants;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MapleShopFactory {

    private static final Logger log = LogManager.getLogger(MapleShopFactory.class.getName());
    private static final Set<Integer> rechargeableItems = new LinkedHashSet<>();
    private static final Set<Integer> blockedItems = new LinkedHashSet<>();
    private static final MapleShopFactory instance = new MapleShopFactory();

    static {
        rechargeableItems.add(2070000); //海星镖
        rechargeableItems.add(2070001); //回旋镖
        rechargeableItems.add(2070002); //黑色利刃
        rechargeableItems.add(2070003); //雪花镖
        rechargeableItems.add(2070004); //黑色刺
        rechargeableItems.add(2070005); //金钱镖
        rechargeableItems.add(2070006); //齿轮镖
        rechargeableItems.add(2070007); //月牙镖
        rechargeableItems.add(2070008); //小雪球
        rechargeableItems.add(2070009); //木制陀螺
        rechargeableItems.add(2070010); //冰菱
        rechargeableItems.add(2070011); //枫叶镖
        rechargeableItems.add(2070012); //纸飞机
        rechargeableItems.add(2070013); //橘子
        rechargeableItems.add(2070015); //初学者标
        rechargeableItems.add(2070016); //雪球
        rechargeableItems.add(2070019); //高科技电光镖
        rechargeableItems.add(2070020); //鞭炮
        rechargeableItems.add(2070021); //蛋糕镖
        rechargeableItems.add(2070023); //火焰飞镖
        rechargeableItems.add(2070024); //无限飞镖

        rechargeableItems.add(2330000); //子弹
        rechargeableItems.add(2330001); //手枪弹
        rechargeableItems.add(2330002); //铜头子弹
        rechargeableItems.add(2330003); //银子弹
        rechargeableItems.add(2330004); //高爆弹
        rechargeableItems.add(2330005); //穿甲弹
        rechargeableItems.add(2330006); //新手专用弹
        rechargeableItems.add(2330007); //高科技穿甲弹
        rechargeableItems.add(2330008); //钢铁子弹


        blockedItems.add(4170023); //花生
        blockedItems.add(4170024); //冰方块
        blockedItems.add(4170025); //英雄的钥匙
        blockedItems.add(4170028); //正义之锤
        blockedItems.add(4170029); //神秘茶袋
        blockedItems.add(4170031); //明亮的镜子
        blockedItems.add(4170032); //祖母绿之镜
        blockedItems.add(4170033); //导游妮妮之镜
    }

    private final Map<Integer, MapleShop> shops = new TreeMap<>();
    private final Map<Integer, Integer> shopIDs = new TreeMap<>();

    public static MapleShopFactory getInstance() {
        return instance;
    }

    public void loadShopData() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM shops")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        shopIDs.put(rs.getInt("shopid"), rs.getInt("npcid"));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("加载NPC商店错误", e);
        }

        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Map.Entry<Integer, Integer> entry : shopIDs.entrySet()) {
            // 添加商店数据
            MapleShop shop = new MapleShop(entry.getKey(), entry.getValue());
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC")) {
                    ps.setInt(1, entry.getKey());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            List<Integer> recharges = new ArrayList<>(rechargeableItems);
                            int position = 1;
                            while (rs.next()) {
                                if (!ii.itemExists(rs.getInt("itemid")) || blockedItems.contains(rs.getInt("itemid"))) {
                                    continue;
                                }
                                if (ItemConstants.is飞镖道具(rs.getInt("itemid")) || ItemConstants.is子弹道具(rs.getInt("itemid"))) {
                                    MapleShopItem starItem = new MapleShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("reqitem"), rs.getInt("reqitemq"), rs.getInt("pointtype"), rs.getInt("period"), rs.getInt("state"), rs.getInt("category"), rs.getInt("minLevel"), position, true);
                                    shop.addItem(starItem);
                                    if (rechargeableItems.contains(starItem.getItemId())) {
                                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                                    }
                                } else {
                                    shop.addItem(new MapleShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("reqitem"), rs.getInt("reqitemq"), rs.getInt("pointtype"), rs.getInt("period"), rs.getInt("state"), rs.getInt("category"), rs.getInt("minLevel"), position, false));
                                }
                                position++;
                            }
                            for (Integer recharge : recharges) {
                                shop.addItem(new MapleShopItem((short) 1, recharge, 0, 0, 0, 0, 0, 0, 0, 0, 0, true));
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("添加商店数据错误", e);
            }
            shops.put(entry.getKey(), shop);
        }
        log.trace("商店数据加载完成.");
    }

    public void clear() {
        shopIDs.clear();
        shops.clear();
        loadShopData();
    }

    public MapleShop getShop(int shopId) {
        return loadShop(shopId);
    }

    public MapleShop getShopForNPC(int npcId) {
        if (!shopIDs.values().contains(npcId)) {
            return null;
        }
        int shopId = 0;
        for (Map.Entry<Integer, Integer> entry : shopIDs.entrySet()) {
            if (npcId == entry.getValue()) {
                shopId = entry.getKey();
            }
        }
        return loadShop(shopId);
    }

    private MapleShop loadShop(int id) {
        return shops.get(id);
    }

    public Map<Integer, MapleShop> getAllShop() {
        return shops;
    }
}
