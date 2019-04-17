/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.map;

import client.MapleClient;
import configs.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author PlayDK
 */
public class MapScriptManager {

    private static final Logger log = LogManager.getLogger(MapScriptManager.class.getName());
    private static final MapScriptManager instance = new MapScriptManager();
    private static final ScriptEngineFactory sef = new ScriptEngineManager().getEngineByName("nashorn").getFactory();
    private final Map<String, MapScript> scripts = new HashMap<>();

    public synchronized static MapScriptManager getInstance() {
        return instance;
    }

    public void getMapScript(MapleClient c, String scriptName, boolean firstUser) {
        if (scripts.containsKey(scriptName)) {
            scripts.get(scriptName).start(new MapScriptMethods(c));
            return;
        }
        String type = "onUserEnter/";
        if (firstUser) {
            type = "onFirstUserEnter/";
        }
        File scriptFile = new File(ServerConfig.WORLD_SCRIPTSPATH + "/map/" + type + scriptName + ".js");
        if (!scriptFile.exists()) {
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(5, "地图触发: 未找到 map/" + type + " 文件中的 " + scriptName + ".js 文件.");
            }
            log.error("地图触发: 未找到 map/" + type + " 文件中的 " + scriptName + ".js 文件. 在地图 " + c.getPlayer().getMapId() + " - " + c.getPlayer().getMap().getMapName());
            return;
        }
        FileReader fr = null;
        ScriptEngine map = sef.getScriptEngine();
        try {
            fr = new FileReader(scriptFile);
            CompiledScript compiled = ((Compilable) map).compile(fr);
            compiled.eval();
        } catch (Exception e) {
            System.err.println("请检查(map/" + type + " 文件中的 " + scriptName + ".js)的文件." + e);
            log.error("请检查(map/" + type + " 文件中的 " + scriptName + ".js)的文件.", e);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (final IOException e) {
                    System.err.println("ERROR CLOSING" + e);
                }
            }
        }
        if (c.getPlayer().isAdmin()) {
            c.getPlayer().dropMessage(5, "开始执行地图触发: map/" + type + " 文件中的 " + scriptName + ".js 文件.");
        }
        MapScript script = ((Invocable) map).getInterface(MapScript.class);
        scripts.put(scriptName, script);
        script.start(new MapScriptMethods(c));
    }

    public void clearScripts() {
        scripts.clear();
    }
}
