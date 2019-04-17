package database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseException extends RuntimeException {

    private static final Logger log = LogManager.getLogger(DatabaseException.class.getName());
    private static final long serialVersionUID = -420103154764822555L;

    public DatabaseException(String msg) {
        super(msg);
    }

    public DatabaseException(Exception e) {
        super(e);
        log.error("数据库错误", e);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
