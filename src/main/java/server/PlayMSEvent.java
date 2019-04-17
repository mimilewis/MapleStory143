/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 设定所有端内的定时任务
 *
 * @author PlayDK
 */
public class PlayMSEvent {

    private static final Logger log = LogManager.getLogger(PlayMSEvent.class.getName());

    public static void start() {
        log.info("所有定时活动已经启动完毕...");
    }
}
