package server.life;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import redis.clients.jedis.Jedis;
import tools.JsonUtil;
import tools.RedisUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MobSkillFactory {

    private static final Logger log = LogManager.getLogger(MobSkillFactory.class.getName());

    private MobSkillFactory() {
        initialize();
    }

    /*
     * 加载怪物技能信息
     */
    public static void initialize() {
        Jedis jedis = RedisUtil.getJedis();
        if (!jedis.exists(RedisUtil.KEYNAMES.MOBSKILL_DATA.getKeyName())) {
            MapleDataProvider dataSource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Skill.wz"));
            MapleData skillRoot = dataSource.getData("MobSkill.img");
            for (MapleData skillData : skillRoot.getChildren()) {
                for (MapleData levelData : skillData.getChildByPath("level").getChildren()) {
                    int skillId = Integer.parseInt(skillData.getName());
                    int level = Integer.parseInt(levelData.getName());
                    List<Integer> toSummon = new ArrayList<>();
                    for (int i = 0; i <= 200; i++) { //暂时只循环200次 哪有召唤那么多的怪物
                        if (levelData.getChildByPath(String.valueOf(i)) == null) {
                            break;
                        }
                        toSummon.add(MapleDataTool.getInt(levelData.getChildByPath(String.valueOf(i)), 0));
                    }
                    MapleData ltdata = levelData.getChildByPath("lt");
                    Point lt = null;
                    if (ltdata != null) {
                        lt = (Point) ltdata.getData();
                    }
                    MapleData rbdata = levelData.getChildByPath("rb");
                    Point rb = null;
                    if (rbdata != null) {
                        rb = (Point) rbdata.getData();
                    }
                    MobSkill ret = new MobSkill(skillId, level);
                    ret.addSummons(toSummon);
                    ret.setCoolTime(MapleDataTool.getInt("interval", levelData, 0) * 1000);
                    ret.setDuration(MapleDataTool.getInt("time", levelData, 0) * 1000);
                    ret.setHp(MapleDataTool.getInt("hp", levelData, 100));
                    ret.setMpCon(MapleDataTool.getInt("mpCon", levelData, 0));
                    ret.setSpawnEffect(MapleDataTool.getInt("summonEffect", levelData, 0));
                    ret.setX(MapleDataTool.getInt("x", levelData, 1));
                    ret.setY(MapleDataTool.getInt("y", levelData, 1));
                    ret.setProp(MapleDataTool.getInt("prop", levelData, 100) / 100f);
                    ret.setLimit((short) MapleDataTool.getInt("limit", levelData, 0));
                    ret.setOnce(MapleDataTool.getInt("summonOnce", levelData, 0) > 0);
                    ret.setLtRb(lt, rb);
                    ret.setAreaSequenceDelay(MapleDataTool.getInt("areaSequenceDelay", levelData, 0));
                    ret.setSkillAfter(MapleDataTool.getInt("skillAfter", levelData, 0));
                    ret.setForce(MapleDataTool.getInt("force", levelData, 0));
                    ret.setForcex(MapleDataTool.getInt("forcex", levelData, 0));
                    try {
                        jedis.hset(RedisUtil.KEYNAMES.MOBSKILL_DATA.getKeyName(), skillId + ":" + level, JsonUtil.getMapperInstance().writeValueAsString(ret));
                    } catch (JsonProcessingException e) {
                        log.fatal("加载怪物技能失败", e);
                    }
                }
            }
            log.info("共加载 " + jedis.hlen(RedisUtil.KEYNAMES.MOBSKILL_DATA.getKeyName()) + " 个怪物技能信息...");
        }
        RedisUtil.returnResource(jedis);
    }

    /*
     * 通过技能ID 和 等级 获取怪物的技能信息
     */
    public static MobSkill getMobSkill(int skillId, int level) {
        String data = RedisUtil.hget(RedisUtil.KEYNAMES.MOBSKILL_DATA.getKeyName(), skillId + ":" + level);
        if (data == null) {
            return null;
        }
        try {
            return JsonUtil.getMapperInstance().readValue(data, MobSkill.class);
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }
}
