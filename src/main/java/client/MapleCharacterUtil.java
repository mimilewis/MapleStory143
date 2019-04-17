package client;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import tools.Pair;
import tools.Triple;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class MapleCharacterUtil {

    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger(MapleCharacterUtil.class);
    private static final Pattern namePattern = Pattern.compile("^(?!_)(?!.*?_$)[a-zA-Z0-9_\u4e00-\u9fa5]+$");
    private static final Pattern petPattern = Pattern.compile("^(?!_)(?!.*?_$)[a-zA-Z0-9_\u4e00-\u9fa5]+$");

    public static boolean canCreateChar(String name, boolean gm) {
        return !(getIdByName(name) != -1 || !isEligibleCharName(name, gm));
    }

    public static boolean canChangePetName(String name) {
        return !(name.getBytes().length < 4 || name.getBytes().length > 12) && petPattern.matcher(name).matches();
    }

    public static boolean isEligibleCharName(String name, boolean gm) {
        if (name.getBytes().length > 12) {
            return false;
        }
        if (gm) {
            return true;
        }
        return name.getBytes().length >= 4 && namePattern.matcher(name).matches();
    }

    public static String makeMapleReadable(String in) {
        String wui = in.replace('I', 'i');
        wui = wui.replace('l', 'L');
        wui = wui.replace("rn", "Rn");
        wui = wui.replace("vv", "Vv");
        wui = wui.replace("VV", "Vv");
        return wui;
    }

    /*
     * 角色名字获取角色ID
     */
    public static int getIdByName(String name) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            int id = rs.getInt("id");
            rs.close();
            ps.close();

            return id;
        } catch (SQLException e) {
            log.error("error 'getIdByName' " + e);
        }
        return -1;
    }

    /*
     * 从角色ID获取角色名字和帐号ID
     */
    public static Pair<String, Integer> getNameById(int chrId, int world) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ? AND world = ?");
            ps.setInt(1, chrId);
            ps.setInt(2, world);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            Pair<String, Integer> id = new Pair<>(rs.getString("name"), rs.getInt("accountid"));
            rs.close();
            ps.close();
            return id;
        } catch (Exception e) {
            log.error("error 'getInfoByName' " + e);
        }
        return null;
    }

    public static boolean PromptPoll(int accountid) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean prompt = false;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            ps = con.prepareStatement("SELECT * FROM game_poll_reply WHERE AccountId = ?");
            ps.setInt(1, accountid);
            rs = ps.executeQuery();
            prompt = !rs.next();
        } catch (SQLException ignored) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {
            }
        }
        return prompt;
    }

    public static boolean SetPoll(int accountid, int selection) {
        if (!PromptPoll(accountid)) { // Hacking OR spamming the db.
            return false;
        }
        PreparedStatement ps = null;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            ps = con.prepareStatement("INSERT INTO game_poll_reply (AccountId, SelectAns) VALUES (?, ?)");
            ps.setInt(1, accountid);
            ps.setInt(2, selection);

            ps.execute();
        } catch (SQLException ignored) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ignored) {
            }
        }
        return true;
    }

    /*
     * -2 = 出现未知的错误
     * -1 = 没有找到账号信息
     * 0 = 未设置二级密码.
     * 1 = 输入的二级密码错误
     * 2 = 二级密码修改成功
     */
    public static int Change_SecondPassword(int accid, String password, String newpassword) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * from accounts where id = ?");
            ps.setInt(1, accid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            String secondPassword = rs.getString("2ndpassword");
            String salt2 = rs.getString("salt2");
            if (secondPassword != null && salt2 != null) {
                secondPassword = LoginCrypto.rand_r(secondPassword);
            } else if (secondPassword == null && salt2 == null) {
                rs.close();
                ps.close();
                return 0;
            }
            if (!check_ifPasswordEquals(secondPassword, password, salt2)) {
                rs.close();
                ps.close();
                return 1;
            }
            rs.close();
            ps.close();
            String SHA1hashedsecond;
            try {
                SHA1hashedsecond = LoginCryptoLegacy.encodeSHA1(newpassword);
            } catch (Exception e) {
                return -2;
            }
            ps = con.prepareStatement("UPDATE accounts set 2ndpassword = ?, salt2 = ? where id = ?");
            ps.setString(1, SHA1hashedsecond);
            ps.setString(2, null);
            ps.setInt(3, accid);
            if (!ps.execute()) {
                ps.close();
                return 2;
            }
            ps.close();
            return -2;
        } catch (SQLException e) {
            log.error("修改二级密码发生错误" + e);
            return -2;
        }
    }

    private static boolean check_ifPasswordEquals(String passhash, String pwd, String salt) {
        // Check if the passwords are correct here. :B
        if (LoginCryptoLegacy.isLegacyPassword(passhash) && LoginCryptoLegacy.checkPassword(pwd, passhash)) {
            // Check if a password upgrade is needed.
            return true;
        } else if (salt == null && LoginCrypto.checkSha1Hash(passhash, pwd)) {
            return true;
        } else if (LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt)) {
            return true;
        }
        return false;
    }

    //id accountid gender
    public static Triple<Integer, Integer, Integer> getInfoByName(String name, int world) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name = ? AND world = ?");
            ps.setString(1, name);
            ps.setInt(2, world);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            Triple<Integer, Integer, Integer> id = new Triple<>(rs.getInt("id"), rs.getInt("accountid"), rs.getInt("gender"));
            rs.close();
            ps.close();
            return id;
        } catch (Exception e) {
            log.error("error 'getInfoByName' " + e);
        }
        return null;
    }

    public static void setNXCodeUsed(String name, String code) throws SQLException {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE nxcode SET `user` = ?, `valid` = 0, time = CURRENT_TIMESTAMP() WHERE code = ?");
            ps.setString(1, name);
            ps.setString(2, code);
            ps.execute();
            ps.close();
        }
    }

    public static void sendNote(String to, String name, String msg, int fame) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`, `gift`) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, to);
            ps.setString(2, name);
            ps.setString(3, msg);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, fame);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            log.error("Unable to send note" + e);
        }
    }

    public static Triple<Boolean, Integer, Integer> getNXCodeInfo(String code) throws SQLException {
        Triple<Boolean, Integer, Integer> ret = null;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT `valid`, `type`, `item` FROM nxcode WHERE code = ?");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = new Triple<>(rs.getInt("valid") > 0, rs.getInt("type"), rs.getInt("item"));
            }
            rs.close();
            ps.close();
            return ret;
        }
    }

    public static void addToItemSearch(int itemId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM itemsearch WHERE itemid = ?");
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("count");
                PreparedStatement psu = con.prepareStatement("UPDATE itemsearch SET count = ? WHERE itemid = ?");
                psu.setInt(1, count + 1);
                psu.setInt(2, itemId);
                psu.executeUpdate();
                psu.close();
            } else {
                PreparedStatement psi = con.prepareStatement("INSERT INTO itemsearch (itemid, count, itemName) VALUES (?, ?, ?)");
                psi.setInt(1, itemId);
                psi.setInt(2, 1);
                psi.setString(3, ii.getName(itemId));
                psi.executeUpdate();
                psi.close();
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error(e);

        }
    }

    public static Pair<Integer, Integer> getCashByAccId(int AccId) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ? ");
            ps.setInt(1, AccId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            Pair<Integer, Integer> id = new Pair<>(rs.getInt("ACash"), rs.getInt("mPoints"));
            rs.close();
            ps.close();
            return id;
        } catch (Exception e) {
            log.error("error 'getInfoByName' " + e);
        }
        return null;
    }
}
