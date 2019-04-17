package database;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.console.Start;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * All OdinMS servers maintain a Database Connection. This class therefore
 * "singletonices" the connection per process.
 *
 * @author Frz
 */
public class DatabaseConnection {

    public static final int RETURN_GENERATED_KEYS = 1;
    private static final Logger log = LogManager.getLogger(DatabaseConnection.class);
    private static final DruidDataSource druidDataSource = new DruidDataSource();
    private static final long connectionTimeOut = 300000; // 5 minutes 300000毫秒
    private static String dbUrl;
    private static DatabaseConnection databasePool = null;

    public static void init() {
        dbUrl = "jdbc:mysql://" + ServerConfig.DB_IP + ":" + ServerConfig.DB_PORT + "/" + ServerConfig.DB_NAME + "?autoReconnect=true&characterEncoding=GBK&zeroDateTimeBehavior=convertToNull";
        try {
            druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
            druidDataSource.setUrl(dbUrl);
            druidDataSource.setUsername(ServerConfig.DB_USER);
            druidDataSource.setPassword(ServerConfig.DB_PASSWORD);
            druidDataSource.setFilters("log4j2");
            druidDataSource.setInitialSize(ServerConfig.DB_INITIALPOOLSIZE);
            druidDataSource.setMinIdle(ServerConfig.DB_MINPOOLSIZE);
            druidDataSource.setMaxActive(ServerConfig.DB_MAXPOOLSIZE);
            druidDataSource.setMaxWait(60000);
            druidDataSource.setTimeBetweenEvictionRunsMillis(60000);
            druidDataSource.setMinEvictableIdleTimeMillis(300000);
            druidDataSource.setRemoveAbandoned(true);
            druidDataSource.setRemoveAbandonedTimeoutMillis(ServerConfig.DB_TIMEOUT);
            druidDataSource.setValidationQuery("SELECT 1 FROM dual");
            druidDataSource.setTestWhileIdle(true);
            druidDataSource.setTestOnBorrow(false);
            druidDataSource.setTestOnReturn(false);
            druidDataSource.setPoolPreparedStatements(true);
            druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        } catch (Exception e) {
            log.error("[数据库信息] 初始化失败", e);
            System.exit(0);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (null == databasePool) {
            databasePool = new DatabaseConnection();
        }
        return databasePool;
    }

    private static long getWaitTimeout(Connection con) {
        try (Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE 'wait_timeout'")) {
                if (rs.next()) {
                    return Math.max(1000, rs.getInt(2) * 1000 - 1000);
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            return -1;
        }
    }

    private static Connection connectToDB() {
        try {
            Connection localConnection = druidDataSource.getConnection();
            Long timeout = getWaitTimeout(localConnection);
            if (timeout == -1) {
                System.out.println("[数据库信息] 无法读取超时时间，using " + connectionTimeOut + " instead.");
            }
            return localConnection;
        } catch (SQLException e) {
            log.error("[数据库信息] 数据库连接失败，请确认MYSQL服务是否开启，数据库名、账号、密码是否配置正确", e);
            throw new DatabaseException(e);
        }
    }

    private static void colse() {
        druidDataSource.close();
    }

    public static void closeAll() throws SQLException {
        druidDataSource.close();
    }

    public DataBaseStatus TestConnection() {
        init();
        Connection localConnection = null;
        DataBaseStatus ret;
        try {
            localConnection = druidDataSource.getConnection();
            ret = DataBaseStatus.连接成功;
        } catch (Exception e) {
            Start.showMessage("连接数据库失败", "错误", 1);
            System.exit(0);
            ret = DataBaseStatus.连接失败;
        } finally {
            try {
                if (localConnection != null) {
                    localConnection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public DruidPooledConnection getConnection() throws SQLException {
        return druidDataSource.getConnection();
    }

    public enum DataBaseStatus {
        未初始化,
        连接成功,
        连接失败
    }

    public static class ConWrapper {

        private long lastAccessTime = 0;
        private Connection connection;

        public ConWrapper(Connection con) {
            this.connection = con;
        }

        public Connection getConnection() {
            if (expiredConnection()) {
                //System.out.println("[DB信息] 线程ID " + id + " 的SQL连接已经超时.重新连接...");
                try { // Assume that the connection is stale
                    connection.close();
                } catch (Throwable err) {
                    throw new DatabaseException("数据库错误", err);
                }
                this.connection = connectToDB();
            }
            lastAccessTime = System.currentTimeMillis(); // Record Access
            return this.connection;
        }

        /**
         * Returns whether this connection has expired
         *
         * @return
         */
        public boolean expiredConnection() {
            if (lastAccessTime == 0) {
                return false;
            }
            try {
                return System.currentTimeMillis() - lastAccessTime >= connectionTimeOut || connection.isClosed();
            } catch (Throwable ex) {
                return true;
            }
        }
    }
}
