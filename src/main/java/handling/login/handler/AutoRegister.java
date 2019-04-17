package handling.login.handler;

import client.LoginCrypto;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AutoRegister {

    public static boolean createAccount(String login, String pwd) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, login);
                ps.setString(2, LoginCrypto.hexSha1(pwd));
                ps.setString(3, "autoregister@mail.com");
                ps.setString(4, "0000-00-00");
                ps.setString(5, "00-00-00-00-00-00");
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException ex) {
            System.out.println("Ex:" + ex);
        }
        return false;
    }
}
