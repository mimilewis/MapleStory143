/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package configs;


import constants.ServerConstants;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.config.ConfigurableProcessor;
import tools.config.PropertiesUtils;

import java.io.*;
import java.util.Collection;
import java.util.Properties;

/**
 * @author zisedk
 */
public class Config {

    private static final String dirpath = "./config";
    private static final Logger log = LogManager.getLogger(Config.class);
    private static final Properties[] props;

    static {
        try {
            props = PropertiesUtils.loadAllFromDirectory(dirpath);
        } catch (Exception e) {
            System.err.println("加载配置文件出现错误" + e.getMessage());
            throw new Error("加载配置文件出现错误.", e);
        }
    }

    /**
     * 加载配置文件
     */
    public static void load() {
        ConfigurableProcessor.process(ServerConfig.class, props);
        ConfigurableProcessor.process(MovementConfig.class, props);
        ConfigurableProcessor.process(CSInfoConfig.class, props);
        ConfigurableProcessor.process(FishingConfig.class, props);
        ConfigurableProcessor.process(NebuliteConfig.class, props);

        ServerConstants.loadBlockedMapFM();
    }

    public static String getProperty(String key, String defaultValue) {
        String ret = defaultValue;
        for (Properties prop : props) {
            if (prop.containsKey(key)) {
                ret = prop.getProperty(key);
            }
        }
        return ret;
    }

    public static void setProperty(String key, String value) {
        for (Properties prop : props) {
            if (prop.containsKey(key)) {
                prop.setProperty(key, value);
                changeFiles(key, value);
            }
        }
    }

    private static void changeFiles(String key, String value) {
        File root = new File(dirpath);
        try {
            Collection<File> files = FileUtils.listFiles(root, new String[]{"properties"}, false);
            for (File file : files) {
                if (file.isFile()) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "gbk"))) {
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith(key)) {
                                sb.append(key);
                                sb.append("=");
                                sb.append(value);
                                sb.append("\r\n");
                                continue;
                            }
                            sb.append(line);
                            sb.append("\r\n");
                        }
                        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "gbk"))) {
                            bw.write(sb.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
