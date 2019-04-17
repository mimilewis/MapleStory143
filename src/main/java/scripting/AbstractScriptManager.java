package scripting;

import client.MapleClient;
import configs.ServerConfig;
import handling.netty.MapleCustomEncryption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Matze
 */
public abstract class AbstractScriptManager {

    private static final Logger log = LogManager.getLogger(AbstractScriptManager.class.getName());

    private final ScriptEngineManager sem;

    protected AbstractScriptManager() {
        sem = new ScriptEngineManager();
    }

    protected Invocable getDefaultInvocable(String path) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(getClass().getResourceAsStream(path))) {
            ScriptEngine engine = sem.getEngineByName("nashorn");
            try {
                engine.eval(inputStreamReader);
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            return (Invocable) engine;
        } catch (IOException e) {
            log.error("加载默认脚本出错", e);
        }
        return null;
    }

    protected Invocable getInvocable(String path, MapleClient c) {
        return getInvocable(path, c, false);
    }

    protected Invocable getInvocable(String path, MapleClient c, boolean npc) {
        FileInputStream scriptFile_in = null;
        ByteArrayOutputStream out = null;
        try {
            path = ServerConfig.WORLD_SCRIPTSPATH + "/" + path;
            ScriptEngine engine = null;
            if (c != null) {
                engine = c.getScriptEngine(path);
            }
            if (engine == null) {
                File scriptFile = new File(path);
                if (!scriptFile.exists()) {
                    return null;
                }
                engine = sem.getEngineByName("nashorn");
                if (c != null) {
                    c.setScriptEngine(path, engine);
                }
                scriptFile_in = new FileInputStream(scriptFile);
                int fileLength = scriptFile_in.available();
                out = new ByteArrayOutputStream();
                byte[] buffer = new byte[fileLength];
                byte[] decrypt;
                scriptFile_in.read(buffer);
                out.write(buffer);
//                MapleCustomEncryption.encryptData(buffer);
//                System.err.println(new String(buffer, "GBK"));
//                FileOutputStream outs = null;
//                String file = "C:\\Users\\**\\Desktop\\1.js";
//                File outputFile = new File(file);
//                if (outputFile.getParentFile() != null) {
//                    outputFile.getParentFile().mkdirs();
//                }
//
//                outs = new FileOutputStream(file, true);
//                outs.write(buffer);
//                outs.close();

                String script = "";
                if (buffer.length > 0) {
                    //脚本解密
                    boolean isEncryption = buffer[0] == '#';
                    if (isEncryption) {
                        List<String> authList = new LinkedList<>();
                        decrypt = Arrays.copyOfRange(buffer, 1, fileLength);
                        MapleCustomEncryption.decryptData(decrypt);
                        script = new String(decrypt, "UTF-8");
                        String keystr = script.substring(0, script.indexOf("#"));
                        authList.addAll(Arrays.asList(keystr.split("\\|")));
                        script = script.substring(keystr.length() + 1, script.length() - 1);
                    } else {
                        script = new String(buffer, "UTF-8");
                    }
                }

                engine.eval(script);
//                engine.eval(new InputStreamReader(scriptFile_in, "gbk"));
            } else if (npc) {
                c.getPlayer().dropMessage(-1, "您当前已经和1个NPC对话了. 如果不是请输入 @ea 命令进行解卡。");
            }
            return (Invocable) engine;
        } catch (Exception e) {
            log.error("Error executing script. Path: " + path + "\r\nException " + e);
            return null;
        } finally {
            try {
                if (scriptFile_in != null) {
                    scriptFile_in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
                log.error("Error close script. Path: " + path + "\r\nException " + ignore);
            }
        }
    }
}
