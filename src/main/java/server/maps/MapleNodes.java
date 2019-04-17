package server.maps;

import tools.Pair;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MapleNodes {

    private final Map<Integer, MapleNodeInfo> nodes; //used for HOB pq.
    private final List<Rectangle> areas;
    private final List<MaplePlatform> platforms;
    private final List<MonsterPoint> monsterPoints;
    private final List<Integer> skillIds;
    private final List<Pair<Integer, Integer>> mobsToSpawn;
    private final List<Pair<Point, Integer>> guardiansToSpawn;
    private final List<Pair<String, Integer>> flags;
    private final List<DirectionInfo> directionInfo;
    private final int mapid;
    private int nodeStart = -1;
    private boolean firstHighest = true;

    public MapleNodes(int mapid) {
        nodes = new LinkedHashMap<>();
        areas = new ArrayList<>();
        platforms = new ArrayList<>();
        skillIds = new ArrayList<>();
        directionInfo = new ArrayList<>();
        monsterPoints = new ArrayList<>();
        mobsToSpawn = new ArrayList<>();
        guardiansToSpawn = new ArrayList<>();
        flags = new ArrayList<>();
        this.mapid = mapid;
    }

    public void setNodeStart(int ns) {
        this.nodeStart = ns;
    }

    public void addDirection(int key, DirectionInfo d) {
        this.directionInfo.add(key, d);
    }

    public DirectionInfo getDirection(int key) {
        if (key >= directionInfo.size()) {
            return null;
        }
        return directionInfo.get(key);
    }

    public List<Pair<String, Integer>> getFlags() {
        return flags;
    }

    public void addFlag(Pair<String, Integer> f) {
        flags.add(f);
    }

    public void addNode(MapleNodeInfo mni) {
        this.nodes.put(mni.key, mni);
    }

    public Collection<MapleNodeInfo> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    public MapleNodeInfo getNode(int index) {
        int i = 1;
        for (MapleNodeInfo x : getNodes()) {
            if (i == index) {
                return x;
            }
            i++;
        }
        return null;
    }

    public boolean isLastNode(int index) {
        return index == nodes.size();
    }

    private int getNextNode(MapleNodeInfo mni) {
        if (mni == null) {
            return -1;
        }
        addNode(mni);
        // output part
    /*
         * StringBuilder b = new StringBuilder(mapid + " added key " + mni.key +
         * ". edges: "); for (int i : mni.edge) { b.append(i + ", "); }
         * System.out.println(b.toString());
         * FileoutputUtil.log(FileoutputUtil.PacketEx_Log, b.toString());
         */
        // output part end

        int ret = -1;
        for (int i : mni.edge) {
            if (!nodes.containsKey(i)) {
                if (ret != -1 && (mapid / 100 == 9211204 || mapid / 100 == 9320001)) {
                    if (!firstHighest) {
                        ret = Math.min(ret, i);
                    } else {
                        firstHighest = false;
                        ret = Math.max(ret, i);
                        //two ways for stage 5 to get to end, thats highest ->lowest, and lowest -> highest(doesn't work)
                        break;
                    }
                } else {
                    ret = i;
                }
            }
        }
        mni.nextNode = ret;
        return ret;
    }

    public void sortNodes() {
        if (nodes.size() <= 0 || nodeStart < 0) {
            return;
        }
        Map<Integer, MapleNodeInfo> unsortedNodes = new HashMap<>(nodes);
        int nodeSize = unsortedNodes.size();
        nodes.clear();
        int nextNode = getNextNode(unsortedNodes.get(nodeStart));
        while (nodes.size() != nodeSize && nextNode >= 0) {
            nextNode = getNextNode(unsortedNodes.get(nextNode));
        }
    }

    public void addMapleArea(Rectangle rec) {
        areas.add(rec);
    }

    public List<Rectangle> getAreas() {
        return new ArrayList<>(areas);
    }

    public Rectangle getArea(int index) {
        return getAreas().get(index);
    }

    public void addPlatform(MaplePlatform mp) {
        this.platforms.add(mp);
    }

    public List<MaplePlatform> getPlatforms() {
        return new ArrayList<>(platforms);
    }

    public List<MonsterPoint> getMonsterPoints() {
        return monsterPoints;
    }

    public void addMonsterPoint(int x, int y, int fh, int cy, int team) {
        this.monsterPoints.add(new MonsterPoint(x, y, fh, cy, team));
    }

    public void addMobSpawn(int mobId, int spendCP) {
        this.mobsToSpawn.add(new Pair<>(mobId, spendCP));
    }

    public List<Pair<Integer, Integer>> getMobsToSpawn() {
        return mobsToSpawn;
    }

    public void addGuardianSpawn(Point guardian, int team) {
        this.guardiansToSpawn.add(new Pair<>(guardian, team));
    }

    public List<Pair<Point, Integer>> getGuardians() {
        return guardiansToSpawn;
    }

    public List<Integer> getSkillIds() {
        return skillIds;
    }

    public void addSkillId(int z) {
        this.skillIds.add(z);
    }

    public static class MapleNodeInfo {

        public final int node;
        public final int key;
        public final int x;
        public final int y;
        public final int attr;
        public final List<Integer> edge;
        public int nextNode = -1;

        public MapleNodeInfo(int node, int key, int x, int y, int attr, List<Integer> edge) {
            this.node = node;
            this.key = key;
            this.x = x;
            this.y = y;
            this.attr = attr;
            this.edge = edge;
        }
    }

    public static class DirectionInfo {

        public final int x;
        public final int y;
        public final int key;
        public final boolean forcedInput;
        public final List<String> eventQ = new ArrayList<>();

        public DirectionInfo(int key, int x, int y, boolean forcedInput) {
            this.key = key;
            this.x = x;
            this.y = y;
            this.forcedInput = forcedInput;
        }
    }

    public static class MaplePlatform {

        public final String name;
        public final int start;
        public final int speed;
        public final int x1;
        public final int y1;
        public final int x2;
        public final int y2;
        public final int r;
        public final List<Integer> SN;

        public MaplePlatform(String name, int start, int speed, int x1, int y1, int x2, int y2, int r, List<Integer> SN) {
            this.name = name;
            this.start = start;
            this.speed = speed;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.r = r;
            this.SN = SN;
        }
    }

    public static class MonsterPoint {

        public final int x;
        public final int y;
        public final int fh;
        public final int cy;
        public final int team;

        public MonsterPoint(int x, int y, int fh, int cy, int team) {
            this.x = x;
            this.y = y;
            this.fh = fh;
            this.cy = cy;
            this.team = team;
        }
    }
}
