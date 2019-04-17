package client.skills;

import client.status.MonsterStatus;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import configs.Config;
import configs.ServerConfig;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.*;
import redis.clients.jedis.Jedis;
import tools.*;
import tools.RedisUtil.KEYNAMES;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class SkillFactory {

    private static final ObjectMapper mapper = JsonUtil.getMapperInstance();
    private static final Logger log = LogManager.getLogger(SkillFactory.class);
    private static final MapleData delayData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Character.wz")).getData("00002000.img");
    private static final MapleData stringData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")).getData("Skill.img");
    private static final MapleDataProvider datasource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Skill.wz"));
    private static final Map<Integer, Skill> skills = new HashMap<>();
    private static final Map<Integer, CraftingEntry> craftings = new HashMap<>();
    private static final List<Integer> finalAttackSkills = new LinkedList<>();
    private static final Jedis jedis = RedisUtil.getJedis();

    public static void loadDelays() {
        if (!jedis.exists(KEYNAMES.DELAYS.getKeyName())) {
            int del = 0; //buster is 67 but its the 57th one!
            for (MapleData delay : delayData) {
                if (!delay.getName().equals("info")) {
                    jedis.hset(KEYNAMES.DELAYS.getKeyName(), delay.getName(), String.valueOf(del));
                    del++;
                }
            }
        }
    }

    public static void loadSkillData() {
        if (!jedis.exists(KEYNAMES.SKILL_DATA.getKeyName())) {
            MapleDataDirectoryEntry root = datasource.getRoot();
            int skillid;
            MapleData summon_data;
            Map<Integer, List<Integer>> skillsByJob = new HashMap<>();
            for (MapleDataFileEntry topDir : root.getFiles()) { // Loop thru jobs
                if (topDir.getName().length() <= 10) {
                    for (MapleData data : datasource.getData(topDir.getName())) { // Loop thru each jobs
                        if (data.getName().equals("skill")) {
                            for (MapleData data2 : data) { // Loop thru each jobs
                                if (data2 != null) {
                                    skillid = Integer.parseInt(data2.getName());
                                    Skill skil = Skill.loadFromData(skillid, data2, delayData);
                                    skills.put(skillid, skil);
                                    List<Integer> job = skillsByJob.computeIfAbsent(skillid / 10000, k -> new ArrayList<>());
                                    job.add(skillid);
                                    try {
                                        jedis.hset(KEYNAMES.SKILL_DATA.getKeyName(), String.valueOf(skillid), mapper.writeValueAsString(skil));
                                        jedis.hset(KEYNAMES.SKILL_NAME.getKeyName(), String.valueOf(skillid), getName(skillid, stringData));
                                    } catch (JsonProcessingException e) {
                                        log.error("", e);
                                    }

                                    summon_data = data2.getChildByPath("summon/attack1/info");
                                    if (summon_data != null) {
                                        SummonSkillEntry sse = new SummonSkillEntry();
                                        sse.type = (byte) MapleDataTool.getInt("type", summon_data, 0);
                                        sse.mobCount = (byte) MapleDataTool.getInt("mobCount", summon_data, 1);
                                        sse.attackCount = (byte) MapleDataTool.getInt("attackCount", summon_data, 1);
                                        sse.targetPlus = (byte) MapleDataTool.getInt("targetPlus", summon_data, 1);
                                        if (summon_data.getChildByPath("range/lt") != null) {
                                            MapleData ltd = summon_data.getChildByPath("range/lt");
                                            sse.lt = (Point) ltd.getData();
                                            sse.rb = (Point) summon_data.getChildByPath("range/rb").getData();
                                        } else {
                                            sse.lt = new Point(-100, -100);
                                            sse.rb = new Point(100, 100);
                                        }
//                                        sse.range = (short) MapleDataTool.getInt("range/r", summon_data, 0);
//                                        sse.delay = MapleDataTool.getInt("effectAfter", summon_data, 0) + MapleDataTool.getInt("attackAfter", summon_data, 0);
//                                        sse.delay = MapleDataTool.getInt("bulletSpeed", summon_data, 0);
                                        for (MapleData effect : summon_data) {
                                            if (effect.getChildren().size() > 0) {
                                                for (MapleData effectEntry : effect) {
                                                    sse.delay += MapleDataTool.getIntConvert("delay", effectEntry, 0);
                                                }
                                            }
                                        }
                                        for (MapleData effect : data2.getChildByPath("summon/attack1")) {
                                            sse.delay += MapleDataTool.getIntConvert("delay", effect, 0);
                                        }
                                        try {
                                            jedis.hset(KEYNAMES.SUMMON_SKILL_DATA.getKeyName(), data2.getName(), mapper.writeValueAsString(sse));
                                        } catch (JsonProcessingException e) {
                                            log.error("", e);
                                        }
//                                        SummonSkillInformation.put(skillid, sse);
                                    }

                                    // 查找所有骑宠关联ID
                                    for (MapleData data3 : data2) {
                                        if (data3.getName().equals("vehicleID")) {
                                            jedis.hset(KEYNAMES.MOUNT_ID.getKeyName(), data2.getName(), MapleDataTool.getString("vehicleID", data2, "0"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (topDir.getName().startsWith("Familiar")) {
                    for (MapleData data : datasource.getData(topDir.getName())) {
                        FamiliarEntry skil = new FamiliarEntry();
                        skil.prop = (byte) MapleDataTool.getInt("prop", data, 0);
                        skil.time = (byte) MapleDataTool.getInt("time", data, 0);
                        skil.attackCount = (byte) MapleDataTool.getInt("attackCount", data, 1);
                        skil.targetCount = (byte) MapleDataTool.getInt("targetCount", data, 1);
                        skil.speed = (byte) MapleDataTool.getInt("speed", data, 1);
                        skil.knockback = MapleDataTool.getInt("knockback", data, 0) > 0 || MapleDataTool.getInt("attract", data, 0) > 0;
                        if (data.getChildByPath("lt") != null) {
                            skil.lt = (Point) data.getChildByPath("lt").getData();
                            skil.rb = (Point) data.getChildByPath("rb").getData();
                        }
                        if (MapleDataTool.getInt("stun", data, 0) > 0) {
                            skil.status.add(MonsterStatus.MOB_STAT_Stun);
                        }
                        //if (MapleDataTool.getInt("poison", data, 0) > 0) {
                        //	status.add(MonsterStatus.中毒);
                        //}
                        if (MapleDataTool.getInt("slow", data, 0) > 0) {
                            skil.status.add(MonsterStatus.MOB_STAT_Speed);
                        }
                        try {
                            jedis.hset(KEYNAMES.FAMILIAR_DATA.getKeyName(), data.getName(), mapper.writeValueAsString(skil));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (topDir.getName().startsWith("Recipe")) {
                    for (MapleData data : datasource.getData(topDir.getName())) {
                        skillid = Integer.parseInt(data.getName());
                        CraftingEntry skil = new CraftingEntry(skillid, (byte) MapleDataTool.getInt("incFatigability", data, 0), (byte) MapleDataTool.getInt("reqSkillLevel", data, 0), (byte) MapleDataTool.getInt("incSkillProficiency", data, 0), MapleDataTool.getInt("needOpenItem", data, 0) > 0, MapleDataTool.getInt("period", data, 0));
                        for (MapleData d : data.getChildByPath("target")) {
                            skil.targetItems.add(new Triple<>(MapleDataTool.getInt("item", d, 0), MapleDataTool.getInt("count", d, 0), MapleDataTool.getInt("probWeight", d, 0)));
                        }
                        for (MapleData d : data.getChildByPath("recipe")) {
                            skil.reqItems.put(MapleDataTool.getInt("item", d, 0), MapleDataTool.getInt("count", d, 0));
                        }

                        craftings.put(skillid, skil);

                        try {
                            jedis.hset(KEYNAMES.CRAFT_DATA.getKeyName(), data.getName(), mapper.writeValueAsString(skil));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // SkillByJob 缓存到Redis
            for (Entry<Integer, List<Integer>> entry : skillsByJob.entrySet()) {
                try {
                    jedis.hset(KEYNAMES.SKILL_BY_JOB.getKeyName(), String.valueOf(entry.getKey()), mapper.writeValueAsString(entry.getValue()));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            // finalAttackSkills 缓存到Redis
            finalAttackSkills.forEach(integer -> jedis.sadd(KEYNAMES.FINLA_ATTACK_SKILL.getKeyName(), integer.toString()));
        } else {
            Map<String, String> datas = jedis.hgetAll(KEYNAMES.SKILL_DATA.getKeyName());
//            ForkJoinPool forkJoinPool = new ForkJoinPool(20);
//            forkJoinPool.submit(() -> );
//            forkJoinPool.shutdown();

            datas.entrySet().parallelStream().forEach(k -> {
                try {
                    skills.put(Integer.valueOf(k.getKey()), JsonUtil.getMapperInstance().readValue(k.getValue(), Skill.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Map<String, String> datas_c = jedis.hgetAll(KEYNAMES.CRAFT_DATA.getKeyName());
            datas_c.entrySet().parallelStream().forEach(k -> {
                try {
                    craftings.put(Integer.valueOf(k.getKey()), JsonUtil.getMapperInstance().readValue(k.getValue(), CraftingEntry.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            Set<String> smembers = jedis.smembers(KEYNAMES.FINLA_ATTACK_SKILL.getKeyName());
            smembers.forEach(s -> finalAttackSkills.add(Integer.parseInt(s)));
        }
        finalAttackSkills.sort((o1, o2) -> o1.equals(o2) ? 0 : (o1 > o2 ? 1 : -1));
    }

    public static void reloadSkills(int skillid) {
        jedis.hdel(KEYNAMES.SKILL_DATA.getKeyName(), String.valueOf(skillid));
        Skill skil = Skill.loadFromData(skillid, datasource.getData(String.valueOf(skillid / 10000) + ".img").getChildByPath("skill").getChildByPath(String.valueOf(skillid)), delayData);
        skills.put(skillid, skil);
        try {
            jedis.hset(KEYNAMES.SKILL_DATA.getKeyName(), String.valueOf(skillid), mapper.writeValueAsString(skil));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static void loadMemorySkills() {
        if (!jedis.exists(KEYNAMES.MEMORYSKILL_DATA.getKeyName())) {
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM memoryskills")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int skillId = rs.getInt("skillid");
                            Skill skill = SkillFactory.getSkill(skillId);
                            /*
                             * 如果复制技能中已经有这个技能 或者 这个技能不存在 或者 这个技能不是冒险家职业技能 就跳过不加载
                             */
                            if (jedis.hexists(KEYNAMES.MEMORYSKILL_DATA.getKeyName(), String.valueOf(skillId)) || skill == null || skill.getSkillByJobBook(skillId) == -1) {
                                continue;
                            }
                            jedis.hset(KEYNAMES.MEMORYSKILL_DATA.getKeyName(), String.valueOf(skillId), String.valueOf(skill.getSkillByJobBook(skillId)));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getSkillDefaultData(int skillid, String name) {
        MapleData skill = datasource.getData(String.valueOf(skillid / 10000) + ".img").getChildByPath("skill").getChildByPath(String.valueOf(skillid)).getChildByPath("common");
        if (skill != null) {
            for (MapleData data : skill) {
                if (data.getName().equals(name)) {
                    return String.valueOf(data.getData());
                }
            }
        }
        return null;
    }

    public static int getIdFromSkillId(int skillId) {
        String ret = jedis.hget(KEYNAMES.MEMORYSKILL_DATA.getKeyName(), String.valueOf(skillId));
        return ret != null ? Integer.valueOf(ret) : 0;
    }

    public static boolean isMemorySkill(int skillId) {
        return jedis.hexists(KEYNAMES.MEMORYSKILL_DATA.getKeyName(), String.valueOf(skillId));
    }

    public static List<Integer> getSkillsByJob(int jobId) {
        List<Integer> ret = null;
        String data = jedis.hget(KEYNAMES.SKILL_BY_JOB.getKeyName(), String.valueOf(jobId));
        if (data != null) {
            try {
                ret = mapper.readValue(data, new TypeReference<List<Integer>>() {
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static String getSkillName(int id) {
        return jedis.hget(KEYNAMES.SKILL_NAME.getKeyName(), String.valueOf(id));
    }

    public static Integer getDelay(String id) {
        Delay delay = Delay.fromString(id);
        if (delay != null) {
            return delay.i;
        }
        String data = jedis.hget(KEYNAMES.DELAYS.getKeyName(), id);
        if (data != null) {
            return Integer.valueOf(data);
        }
        return null;
    }

    private static String getName(int id, MapleData stringData) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        MapleData skillroot = stringData.getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("name"), "");
        }
        return "";
    }

    public static String getDesc(int id) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        MapleData skillroot = stringData.getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("desc"), "");
        }
        return "";
    }

    public static String getH(int id) {
        String strId = Integer.toString(id);
        strId = StringUtil.getLeftPaddedStr(strId, '0', 7);
        MapleData skillroot = stringData.getChildByPath(strId);
        if (skillroot != null) {
            return MapleDataTool.getString(skillroot.getChildByPath("h"), "");
        }
        return "";
    }

    public static SummonSkillEntry getSummonData(int skillid) {
        SummonSkillEntry ret = null;
        try {
            String data = jedis.hget(KEYNAMES.SUMMON_SKILL_DATA.getKeyName(), String.valueOf(skillid));
            if (data == null) {
                return null;
            }
            ret = mapper.readValue(data, SummonSkillEntry.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 获得所有技能数据
     *
     * @return
     */
    public static Map<Integer, String> getAllSkills() {
        Map<Integer, String> ret = new HashMap<>();
        Map<String, String> result = jedis.hgetAll(KEYNAMES.SKILL_NAME.getKeyName());
        for (Entry<String, String> entry : result.entrySet()) {
            ret.put(Integer.valueOf(entry.getKey()), entry.getValue());
        }
        return ret;
    }

    public static Skill getSkill(int skillid) {
        // redis
//        String craftsdata = RedisUtil.hget(KEYNAMES.CRAFT_DATA.getKeyName(), String.valueOf(skillid));
//        if (skillid >= 92000000 && skillid < 100000000 && craftsdata != null) {
//            try {
//                return mapper.readValue(craftsdata, CraftingEntry.class);
//            } catch (Exception e) {
//                log.error("读取技能失败:" + skillid, e);
//            }
//        }
//        String skilldata = RedisUtil.hget(KEYNAMES.SKILL_DATA.getKeyName(), String.valueOf(skillid));
//        try {
//            return mapper.readValue(skilldata, Skill.class);
//        } catch (Exception e) {
//            log.error("读取技能失败:" + skillid, e);
//        }
//        return null;

        if (!skills.isEmpty()) {
            if (skillid >= 92000000 && skillid < 100000000 && craftings.containsKey(skillid)) {
                return craftings.get(skillid);
            }
            return skills.get(skillid);
        }
        return null;
    }

    /**
     * 获取技能的默认时间
     * 也就是技能是否是有时间限制的
     */
    public static long getDefaultSExpiry(Skill skill) {
        if (skill == null) {
            return -1;
        }
        return (skill.isTimeLimited() ? (System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L) : -1);
    }

    public static CraftingEntry getCraft(int id) {
        CraftingEntry ret = null;
        String data = jedis.hget(KEYNAMES.CRAFT_DATA.getKeyName(), String.valueOf(id));
        if (data != null) {
            try {
                ret = mapper.readValue(data, CraftingEntry.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static FamiliarEntry getFamiliar(int id) {
        FamiliarEntry ret = null;
        String data = jedis.hget(KEYNAMES.FAMILIAR_DATA.getKeyName(), String.valueOf(id));
        if (data != null) {
            try {
                ret = mapper.readValue(data, FamiliarEntry.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * 检测这个技能是否禁止显示
     */
    public static boolean isBlockedSkill(int skillId) {
        return skillId > 0 && ServerConfig.WORLD_BLOCKSKILLS.contains(String.valueOf(skillId));
    }

    public static boolean addBlockedSkill(int skillId) {
        if (isBlockedSkill(skillId)) {
            return false;
        }
        ServerConfig.WORLD_BLOCKSKILLS += "," + skillId;
        Config.setProperty("world.blockskills", ServerConfig.WORLD_BLOCKSKILLS);
        return true;
    }

    /**
     * 找出相同的传授技能名字
     */
    public static int getTeachSkill(String name) {
        for (Entry<Integer, String> entry : getAllSkills().entrySet()) {
            try {
                if (entry.getValue() != null && entry.getValue().endsWith(name)) {
                    if (entry.getKey() >= 80000000 && entry.getKey() < 90000000) {
                        return entry.getKey();
                    }
                }
            } catch (Exception e) {
                System.out.println(entry.getKey());
            }
        }
        return -1;
    }

    public static int getMountLinkId(int mountid) {
        int ret = 0;
        String data = jedis.hget(KEYNAMES.MOUNT_ID.getKeyName(), String.valueOf(mountid));
        if (data != null) {
            ret = Integer.parseInt(data);
        }
        return ret;
    }

    public static List<Integer> getFinalAttackSkills() {
        return finalAttackSkills;
    }

    public static boolean isFinalAttackSkills(Integer skillid) {
        return finalAttackSkills.contains(skillid);
    }

    public enum Delay {

        walk1(0x00),
        walk2(0x01),
        stand1(0x02),
        stand2(0x03),
        alert(0x04),
        swingO1(0x05),
        swingO2(0x06),
        swingO3(0x07),
        swingOF(0x08),
        swingT1(0x09),
        swingT2(0x0A),
        swingT3(0x0B),
        swingTF(0x0C),
        swingP1(0x0D),
        swingP2(0x0E),
        swingPF(0x0F),
        stabO1(0x10),
        stabO2(0x11),
        stabOF(0x12),
        stabT1(0x13),
        stabT2(0x14),
        stabTF(0x15),
        swingD1(0x16),
        swingD2(0x17),
        stabD1(0x18),
        swingDb1(0x19),
        swingDb2(0x1A),
        swingC1(0x1B),
        swingC2(0x1C),
        rushBoom(0x1C),
        tripleBlow(0x19),
        quadBlow(0x1A),
        deathBlow(0x1B),
        finishBlow(0x1C),
        finishAttack(0x1D),
        finishAttack_link(0x1E),
        finishAttack_link2(0x1E),
        shoot1(0x1F),
        shoot2(0x20),
        shootF(0x21),
        shootDb2(0x28),
        shotC1(0x29),
        dash(0x25),
        dash2(0x26), //hack. doesn't really exist
        proneStab(0x29),
        prone(0x2A),
        heal(0x2B),
        fly(0x2C),
        jump(0x2D),
        sit(0x2E),
        rope(0x2F),
        dead(0x30),
        ladder(0x31),
        rain(0x32),
        alert2(0x34),
        alert3(0x35),
        alert4(0x36),
        alert5(0x37),
        alert6(0x38),
        alert7(0x39),
        ladder2(0x3A),
        rope2(0x3B),
        shoot6(0x3C),
        magic1(0x3D),
        magic2(0x3E),
        magic3(0x3F),
        magic5(0x40),
        magic6(0x41), //----------------------------------
        explosion(0x41),
        burster1(0x42),
        burster2(0x43),
        savage(0x44),
        avenger(0x45),
        assaulter(0x46),
        prone2(0x47),
        assassination(0x48),
        assassinationS(0x49),
        tornadoDash(0x4C),
        tornadoDashStop(0x4C),
        tornadoRush(0x4C),
        rush(0x4D),
        rush2(0x4E),
        brandish1(0x4F),
        brandish2(0x50),
        braveSlash(0x51),
        braveslash1(0x51),
        braveslash2(0x51),
        braveslash3(0x51),
        braveslash4(0x51),
        darkImpale(0x61),
        sanctuary(0x52),
        meteor(0x53),
        paralyze(0x54),
        blizzard(0x55),
        genesis(0x56),
        blast(0x58),
        smokeshell(0x59),
        showdown(0x5A),
        ninjastorm(0x5B),
        chainlightning(0x5C),
        holyshield(0x5D),
        resurrection(0x5E),
        somersault(0x5F),
        straight(0x60),
        eburster(0x61),
        backspin(0x62),
        eorb(0x63),
        screw(0x64),
        doubleupper(0x65),
        dragonstrike(0x66),
        doublefire(0x67),
        triplefire(0x68),
        fake(0x69),
        airstrike(0x6A),
        edrain(0x6B),
        octopus(0x6C),
        backstep(0x6D),
        shot(0x6E), //----------------------------------
        rapidfire(0x6E),
        fireburner(0x70),
        coolingeffect(0x71),
        fist(0x72), //----------------------------------
        timeleap(0x73),
        homing(0x75),
        ghostwalk(0x76),
        ghoststand(0x77),
        ghostjump(0x78),
        ghostproneStab(0x79),
        ghostladder(0x7A),
        ghostrope(0x7B),
        ghostfly(0x7C),
        ghostsit(0x7D),
        cannon(0x7E),
        torpedo(0x7F),
        darksight(0x80),
        bamboo(0x81),
        pyramid(0x82),
        wave(0x83),
        blade(0x84),
        souldriver(0x85),
        firestrike(0x86),
        flamegear(0x87),
        stormbreak(0x88),
        vampire(0x89),
        swingT2PoleArm(0x8B),
        swingP1PoleArm(0x8C),
        swingP2PoleArm(0x8D),
        doubleSwing(0x8E),
        tripleSwing(0x8F),
        fullSwingDouble(0x90),
        fullSwingTriple(0x91),
        overSwingDouble(0x92),
        overSwingTriple(0x93),
        rollingSpin(0x94),
        comboSmash(0x95),
        comboFenrir(0x96),
        comboTempest(0x97),
        finalCharge(0x98),
        finalBlow(0x9A),
        finalToss(0x9B),
        magicmissile(0x9C),
        lightningBolt(0x9D),
        dragonBreathe(0x9E),
        breathe_prepare(0x9F),
        dragonIceBreathe(0xA0),
        icebreathe_prepare(0xA1),
        blaze(0xA2),
        fireCircle(0xA3),
        illusion(0xA4),
        magicFlare(0xA5),
        elementalReset(0xA6),
        magicRegistance(0xA7),
        magicBooster(0xA8),
        magicShield(0xA9),
        recoveryAura(0xAA),
        flameWheel(0xAB),
        killingWing(0xAC),
        OnixBlessing(0xAD),
        Earthquake(0xAE),
        soulStone(0xAF),
        dragonThrust(0xB0),
        ghostLettering(0xB1),
        darkFog(0xB2),
        slow(0xB3),
        mapleHero(0xB4),
        Awakening(0xB5),
        flyingAssaulter(0xB6),
        tripleStab(0xB7),
        fatalBlow(0xB8),
        slashStorm1(0xB9),
        slashStorm2(0xBA),
        bloodyStorm(0xBB),
        flashBang(0xBC),
        upperStab(0xBD),
        bladeFury(0xBE),
        chainPull(0xC0),
        chainAttack(0xC0),
        owlDead(0xC1),
        monsterBombPrepare(0xC3),
        monsterBombThrow(0xC3),
        finalCut(0xC4),
        finalCutPrepare(0xC4),
        suddenRaid(0xC6), //idk, not in data anymore
        fly2(0xC7),
        fly2Move(0xC8),
        fly2Skill(0xC9),
        knockback(0xCA),
        rbooster_pre(0xCE),
        rbooster(0xCE),
        rbooster_after(0xCE),
        crossRoad(0xD1),
        nemesis(0xD2),
        tank(0xD9),
        tank_laser(0xDD),
        siege_pre(0xDF),
        tank_siegepre(0xDF), //just to make it work with the skill, these two
        sonicBoom(0xE2),
        darkLightning(0xE4),
        darkChain(0xE5),
        cyclone_pre(0),
        cyclone(0), //energy attack
        glacialchain(0xF7),
        flamethrower(0xE9),
        flamethrower_pre(0xE9),
        flamethrower2(0xEA),
        flamethrower_pre2(0xEA),
        gatlingshot(0xEF),
        gatlingshot2(0xF0),
        drillrush(0xF1),
        earthslug(0xF2),
        rpunch(0xF3),
        clawCut(0xF4),
        swallow(0xF7),
        swallow_attack(0xF7),
        swallow_loop(0xF7),
        flashRain(0xF9),
        OnixProtection(0x108),
        OnixWill(0x109),
        phantomBlow(0x10A),
        comboJudgement(0x10B),
        arrowRain(0x10C),
        arrowEruption(0x10D),
        iceStrike(0x10E),
        swingT2Giant(0x111),
        cannonJump(0x127),
        swiftShot(0x128),
        giganticBackstep(0x12A),
        mistEruption(0x12B),
        cannonSmash(0x12C),
        cannonSlam(0x12D),
        flamesplash(0x12E),
        noiseWave(0x132),
        superCannon(0x136),
        jShot(0x138),
        demonSlasher(0x139),
        bombExplosion(0x13A),
        cannonSpike(0x13B),
        speedDualShot(0x13C),
        strikeDual(0x13D),
        bluntSmash(0x13F),
        crossPiercing(0x140),
        piercing(0x141),
        elfTornado(0x143),
        immolation(0x144),
        multiSniping(0x147),
        windEffect(0x148),
        elfrush(0x149),
        elfrush2(0x149),
        dealingRush(0x14E),
        maxForce0(0x150),
        maxForce1(0x151),
        maxForce2(0x152),
        maxForce3(0x153),
        //special: pirate morph attacks
        iceAttack1(0x112),
        iceAttack2(0x113),
        iceSmash(0x114),
        iceTempest(0x115),
        iceChop(0x116),
        icePanic(0x117),
        iceDoubleJump(0x118),
        shockwave(0x124),
        demolition(0x125),
        snatch(0x126),
        windspear(0x127),
        windshot(0x128);
        public final int i;

        Delay(int i) {
            this.i = i;
        }

        public static Delay fromString(String s) {
            for (Delay b : Delay.values()) {
                if (b.name().equalsIgnoreCase(s)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static class CraftingEntry extends Skill {
        //reqSkillProficiency -> always seems to be 0

        public final List<Triple<Integer, Integer, Integer>> targetItems = new ArrayList<>(); // itemId / amount / probability
        public final Map<Integer, Integer> reqItems = new HashMap<>(); // itemId / amount
        public boolean needOpenItem;
        public int period;
        public byte incFatigability;
        public byte reqSkillLevel;
        public byte incSkillProficiency;

        public CraftingEntry() {
        }

        public CraftingEntry(int id, byte incFatigability, byte reqSkillLevel, byte incSkillProficiency, boolean needOpenItem, int period) {
            super(id);
            this.incFatigability = incFatigability;
            this.reqSkillLevel = reqSkillLevel;
            this.incSkillProficiency = incSkillProficiency;
            this.needOpenItem = needOpenItem;
            this.period = period;
        }
    }

    public static class FamiliarEntry {

        public final EnumSet<MonsterStatus> status = EnumSet.noneOf(MonsterStatus.class);
        public byte prop, time, attackCount, targetCount, speed;
        public Point lt, rb;
        public boolean knockback;

        public boolean makeChanceResult() {
            return prop >= 100 || Randomizer.nextInt(100) < prop;
        }
    }
}
