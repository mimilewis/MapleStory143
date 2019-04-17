package scripting.event;

import handling.channel.ChannelServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.AbstractScriptManager;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Matze
 */
public class EventScriptManager extends AbstractScriptManager {

    private static final Logger log = LogManager.getLogger(EventScriptManager.class.getName());
    private static final AtomicInteger runningInstanceMapId = new AtomicInteger(0);
    private final Map<String, EventEntry> events = new LinkedHashMap<>(), defaultEvents = new LinkedHashMap<>();

    public EventScriptManager(ChannelServer cserv, String[] scripts) {
        super();
        List<String> strings = Arrays.asList(scripts);
        strings.stream().filter(s -> !s.isEmpty()).forEach(s -> {
            Invocable iv = getInvocable("event/" + s + ".js", null);
            if (iv != null) {
                events.put(s, new EventEntry(s, iv, new EventManager(cserv, iv, s)));
            }
        });

//        BossEventManager.getInstance().getAllBossEventName().stream().filter(s -> !s.isEmpty()).forEach(s -> {
//            Invocable iv = getDefaultInvocable(ServerConfig.WORLD_SCRIPTSPATH + "/event/" + s + ".js");
//            if (iv != null) {
//                defaultEvents.put(s, new EventEntry(s, iv, new EventManager(cserv, iv, s)));
//            }
//        });
    }

    public static int getNewInstanceMapId() {
        return runningInstanceMapId.addAndGet(1);
    }

    public EventManager getEventManager(String event) {
        EventEntry entry;
        if (BossEventManager.ISOPEN && BossEventManager.getInstance().canUseDefaultScript(event)) {
            entry = defaultEvents.get(event);
        } else {
            entry = events.get(event);
        }
        if (entry == null) {
            return null;
        }
        return entry.em;
    }

    public void init() {
        List<EventEntry> events_temp = new ArrayList<>(events.values());
        events_temp.addAll(defaultEvents.values());
        events_temp.forEach(eventEntry -> {
            try {
                ((ScriptEngine) eventEntry.iv).put("em", eventEntry.em);
                eventEntry.iv.invokeFunction("init", (Object) null);
            } catch (final Exception ex) {
                log.error("Error initiating event: " + eventEntry.script + ":" + ex);
            }
        });
    }

    public void cancel() {
        for (EventEntry entry : events.values()) {
            entry.em.cancel();
        }
    }
}
