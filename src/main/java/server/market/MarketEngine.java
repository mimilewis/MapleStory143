/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor. Then suck a dick
 */
package server.market;

import client.MapleCharacter;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.channel.ChannelServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MarketEngine {

    private final List<ItemEntry> items = new LinkedList<>();
    private final Map<Integer, String> names = new LinkedHashMap<>();

    public void addItem(int itemId, int quantity, int price, int charid) {
        synchronized (items) {
            for (ItemEntry ie : items) {
                if (ie.getId() == itemId && ie.getOwner() == charid && ie.getPrice() == price) {
                    ie.setQuantity(ie.getQuantity() + quantity);
                    return;
                }
            }
        }
        ItemEntry ie = new ItemEntry();
        ie.setId(itemId);
        ie.setQuantity(quantity);
        ie.setOwner(charid);
        ie.setPrice(price);
        synchronized (items) {
            items.add(ie);
        }
    }

    public void removeItem(int itemId, int quantity, int charid) {
        synchronized (items) {
            for (int i = 0; i < items.size(); i++) {
                ItemEntry ie = items.get(i);
                if (ie.getOwner() == charid && ie.getId() == itemId && ie.getQuantity() >= quantity) {
                    if (ie.getQuantity() == quantity) {
                        items.remove(items.indexOf(ie));
                    } else {
                        ie.setQuantity(ie.getQuantity() - quantity);
                    }
                }
            }
        }
    }

    public ItemEntry getItem(int position) {
        return items.get(position);
    }

    public List<ItemEntry> getItems() {
        return items;
    }

    public String getCharacterName(int charId) {
        if (names.get(charId) != null) {
            return names.get(charId);
        }

        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter mc : cs.getPlayerStorage().getAllCharacters()) {
                if (mc.getId() == charId) {
                    names.put(charId, mc.getName());
                    return mc.getName();
                }
            }
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                names.put(charId, name);
                return name;
            }
        } catch (SQLException e) {
            return "SQL Error fixmepl0x";
        }
        return "No user";
    }

    @Override
    public String toString() {
        String ret = "";
        synchronized (items) {
            for (ItemEntry ie : items) {
                ret += "#v" + ie.getId() +
                        "# 价格: #b" + ie.getPrice() + "#k" +
                        "卖家: #b" + getCharacterName(ie.getOwner()) + "#k" +
                        "\\r\\n";
            }
        }
        return ret;
    }

    public static class ItemEntry {

        private int quantity;
        private int id;
        private int price;
        private int owner;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice(int price) {
            this.price = price;
        }

        public int getOwner() {
            return owner;
        }

        public void setOwner(int owner) {
            this.owner = owner;
        }
    }
}
