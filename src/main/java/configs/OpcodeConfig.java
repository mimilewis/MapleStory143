package configs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OpcodeConfig {

    private static final Logger log = LogManager.getLogger();
    private static final List<String> blockops = new ArrayList<>();

    public static void load() {
        final File file = new File("./config/opcode.properties");
        if (!file.exists()) {
            log.error("读取opcode配置文件失败");
            return;
        }
        Timer.WorldTimer.getInstance().register(new Runnable() {
            @Override
            public synchronized void run() {
                try {
                    blockops.clear();
                    Properties props = new Properties();
                    FileInputStream fileInputStream = new FileInputStream(file);
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "GBK");
                    BufferedReader buff = new BufferedReader(inputStreamReader);
                    props.load(buff);
                    buff.close();
                    inputStreamReader.close();
                    fileInputStream.close();

                    String sendops = props.getProperty("sendops"), recvops = props.getProperty("recvops");

                    for (String s : sendops.split(",")) {
                        blockops.add("S_" + s);
                    }

                    for (String s : recvops.split(",")) {
                        blockops.add("R_" + s);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, 1000);
    }

    public static boolean isblock(String ops, boolean issend) {
        return blockops.contains(issend ? "S_" + ops : "R_" + ops);
    }
}
