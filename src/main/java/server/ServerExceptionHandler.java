package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RemoteException;
import java.sql.SQLException;


public class ServerExceptionHandler {

    private static final Logger log = LogManager.getLogger(ServerExceptionHandler.class);

    public static void HandlerRemoteException(RemoteException exception) {
        log.error("异常类型 RemoteException：", exception);
    }

    public static void HandlerSqlException(SQLException exception) {
        log.error("异常类型 SQLException：", exception);
    }

    public static void HandlerException(Exception exception) {
        log.error("异常类型 " + exception.getClass().getName() + "：", exception);
    }
}