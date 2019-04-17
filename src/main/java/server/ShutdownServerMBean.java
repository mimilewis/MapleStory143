package server;

/**
 * @author RM
 */
public interface ShutdownServerMBean extends Runnable {

    void shutdown();
}
