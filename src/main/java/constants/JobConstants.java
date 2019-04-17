/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import client.PlayerStats;
import tools.Pair;

/**
 * @author ODINMR
 */
public class JobConstants {

    public static boolean is战士(int job) {
        return getTrueJobGrade(job) == 1;
    }

    public static boolean is英雄(int job) {
        return job >= 110 && job <= 112;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is神之子(int job) {
        return job == 10000 || job == 10100 || job == 10110 || job == 10111 || job == 10112;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is圣骑士(int job) {
        return job / 10 == 12;
    }

    public static boolean is黑骑士(int job) {
        return job / 10 == 13;
    }

    public static boolean is神射手(int job) {
        return job / 10 == 31;
    }

    public static boolean is箭神(int job) {
        return job / 10 == 32;
    }

    public static boolean is魔法师(int job) {
        return job / 100 == 2;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is隐士(int job) {
        return (job >= 410 && job <= 412);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is侠盗(int job) {
        return job / 10 == 42;
    }

    public static boolean is双刀(int job) {
        return job / 10 == 43;
    }

    public static boolean is冲锋队长(int job) {
        return job / 10 == 51;
    }

    public static boolean is船长(int job) {
        return job / 10 == 52;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is火炮手(int job) {
        return job == 501 || (job >= 530 && job <= 532);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is夜行者(int job) {
        return job >= 1400 && job <= 1412;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is冒险家(int job) {
        return job >= 0 && job < 1000;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is风灵使者(int job) {
        return job >= 1300 && job <= 1312;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean noBulletJob(int job) {
        return is机械师(job) || is火炮手(job) || is双弩精灵(job) || is龙的传人(job) || is米哈尔(job) || is狂龙战士(job) || is爆莉萌天使(job) || is尖兵(job);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean isNotMpJob(int job) {
        return is恶魔猎手(job) || is恶魔复仇者(job) || is爆莉萌天使(job) || is神之子(job) || is超能力者(job);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is夜光(int job) {
        return job == 2004 || (job >= 2700 && job <= 2712);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is反抗者(int job) {
        return job == 3000 || (job >= 3200 && job <= 3712);
    }

    public static boolean is唤灵斗师(int job) {
        return job == 3200 || (job >= 3210 && job <= 3212);
    }

    public static boolean is豹弩游侠(int job) {
        return job == 3300 || (job >= 3310 && job <= 3312);
    }

    public static boolean is机械师(int job) {
        return job == 3500 || (job >= 3510 && job <= 3512);
    }

    public static boolean is爆破手(int job) {
        return job / 100 == 37;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is恶魔猎手(int job) {
        return job == 3001 || job == 3100 || job == 3110 || job == 3111 || job == 3112;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is狂龙战士(int job) {
        return job == 6000 || (job >= 6100 && job <= 6112);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is品克缤(int job) {
        return job == 13000 || job == 13100;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is恶魔(int job) {
        return is恶魔猎手(job) || is恶魔复仇者(job);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is火枪手(int job) {
        return job == 500 || (job >= 520 && job <= 522);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is米哈尔(int job) {
        return job == 5000 || (job >= 5100 && job <= 5112);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is火枪手新(int job) {
        return job == 509 || (job >= 590 && job <= 592);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is爆莉萌天使(int job) {
        return job == 6001 || (job >= 6500 && job <= 6512);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is恶魔复仇者(int job) {
        return job == 3101 || job == 3120 || job == 3121 || job == 3122;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is龙神(int job) {
        return job == 2001 || (job >= 2200 && job <= 2218);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is新骑士团(int job) {
        return job >= 1000 && job < 2000;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is超能力者(int job) {
        return job == 14000 || job >= 14200 && job <= 14212;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is阴阳师(int job) {
        return job == 4002 || job >= 4200 && job <= 4212;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is战神(int job) {
        return job == 2000 || (job >= 2100 && job <= 2112);
    }

    /**
     * @param baseJob    the value of baseJob
     * @param currentJob the value of currentJob
     * @return the boolean
     */
    public static boolean isJobFamily(int baseJob, int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is炎术士(int job) {
        return job >= 1200 && job <= 1212;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is拳手新(int job) {
        return job == 509 || (job >= 580 && job <= 582);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is双弩精灵(int job) {
        return job == 2002 || (job >= 2300 && job <= 2312);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is暗影双刀(int job) {
        return job >= 430 && job <= 434;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is奇袭者(int job) {
        return job >= 1500 && job <= 1512;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is骑士团(int job) {
        return job >= 1000 && job < 2000;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is尖兵(int job) {
        return job == 3002 || (job >= 3600 && job <= 3612);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is魂骑士(int job) {
        return job >= 1100 && job <= 1112;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is幻影(int job) {
        return job == 2003 || (job >= 2400 && job <= 2412);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is林之灵(int job) {
        return job == 11000 || job == 11200 || job == 11210 || job == 11211 || job == 11212;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is拳手(int job) {
        return job == 500 || (job >= 510 && job <= 512);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is龙的传人(int job) {
        return job == 508 || (job >= 570 && job <= 572);
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is剑豪(int job) {
        return job == 4001 || job >= 4100 && job <= 4112;
    }

    /**
     * @param job the value of job
     * @return the boolean
     */
    public static boolean is隐月(int job) {
        return job == 2005 || (job >= 2500 && job <= 2512);
    }

    public static boolean is火毒(int job) {
        return job >= 210 && job <= 212;
    }

    public static boolean is主教(int job) {
        return job >= 230 && job <= 232;
    }

    /**
     * @param Job the value of Job
     * @return the int
     */
    public static int getTecahSkillID(int Job) {
        int skillId = -1;
        if (JobConstants.is火炮手(Job)) {
            skillId = 110;
        } else if (JobConstants.is龙的传人(Job)) {
            skillId = 1214;
        } else if (JobConstants.is双弩精灵(Job)) {
            skillId = 20021110;
        } else if (JobConstants.is幻影(Job)) {
            skillId = 20030204;
        } else if (JobConstants.is夜光(Job)) {
            skillId = 20040218;
        } else if (JobConstants.is恶魔猎手(Job)) {
            skillId = 30010112;
        } else if (JobConstants.is恶魔复仇者(Job)) {
            skillId = 30010241;
        } else if (JobConstants.is尖兵(Job)) {
            skillId = 30020233;
        } else if (JobConstants.is米哈尔(Job)) {
            skillId = 50001214;
        } else if (JobConstants.is狂龙战士(Job)) {
            skillId = 60000222;
        } else if (JobConstants.is爆莉萌天使(Job)) {
            skillId = 60011219;
        } else if (JobConstants.is林之灵(Job)) {
            skillId = 110000800;
        } else if (JobConstants.is神之子(Job)) {
            skillId = 100000271;
        } else if (JobConstants.is骑士团(Job)) {
            skillId = 10000255 + (int) Math.floor(Job % 1000 / 100);
        }
        return skillId;
    }

    /*
     * 获取角色的默认发型和脸型
     */
    public static Pair<Integer, Integer> getDefaultFaceAndHair(int job, int gender) {
        int face = gender == 0 ? 20100 : 21700;
        int hair = gender == 0 ? 30030 : 31002;
        if (JobConstants.is暗影双刀(job)) {
            face = gender == 0 ? 20265 : 21261;
            hair = gender == 0 ? 33830 : 34820;
        } else if (JobConstants.is龙的传人(job)) {
            face = gender == 0 ? 20100 : 21700;
            hair = gender == 0 ? 36120 : 34990;
        } else if (JobConstants.is双弩精灵(job)) {
            face = gender == 0 ? 20549 : 21547;
            hair = gender == 0 ? 33453 : 34423;
        } else if (JobConstants.is幻影(job)) {
            face = gender == 0 ? 20659 : 21656;
            hair = gender == 0 ? 33703 : 34703;
        } else if (JobConstants.is夜光(job)) {
            face = gender == 0 ? 20174 : 21169;
            hair = gender == 0 ? 36190 : 37070;
        } else if (JobConstants.is恶魔猎手(job)) {
            face = gender == 0 ? 20248 : 21246;
            hair = gender == 0 ? 33531 : 34411;
        } else if (JobConstants.is恶魔复仇者(job)) {
            face = gender == 0 ? 20248 : 21280;
            hair = gender == 0 ? 36460 : 37450;
        } else if (JobConstants.is尖兵(job)) {
            face = gender == 0 ? 20185 : 21182;
            hair = gender == 0 ? 36470 : 37490;
        } else if (JobConstants.is米哈尔(job)) {
            face = gender == 0 ? 20169 : 21700;
            hair = gender == 0 ? 36033 : 31002;
        } else if (JobConstants.is狂龙战士(job)) {
            face = gender == 0 ? 20576 : 21571;
            hair = gender == 0 ? 36245 : 37125;
        } else if (JobConstants.is爆莉萌天使(job)) {
            face = gender == 0 ? 20576 : 21374;
            hair = gender == 0 ? 36245 : 37242;
        }
        return new Pair<>(face, hair);
    }

    public static int getSkillBookBySkill(int skillId) {
        return getSkillBookByJob(skillId / 10000, skillId);
    }

    public static int getTrueJobGrade(int job) {
        int result;
        result = job % 1000 / 100;

        switch (job / 100) {
            case 27:
            case 140:
            case 142:
                result = 2;
                break;
            case 36:
                result = 4;
                break;
            case 37:
                result = 1;
                break;
        }

        return result;
    }

    public static int getJobNumber(int jobz) {
        int job = jobz % 1000;
        if (job / 100 == 0 || is新手职业(jobz)) {
            return 0; //新手 beginner
        } else if ((job / 10) % 10 == 0 || job == 501) {
            return 1;
        } else {
            return 2 + (job % 10);
        }
    }

    public static boolean isSeparatedSpJobEx(int job) {
        if (JobConstants.is林之灵(job)) {
            return false;
        } else if (job == 0 || job == 100 || job == 200 || job == 300 || job == 400 || job == 500 || job == 501) {
            //冒险家新手
            return true;
        } else if (job >= 110 && job <= 132) {
            //冒险家战士
            return true;
        } else if (job >= 210 && job <= 232) {
            //冒险家法师
            return true;
        } else if (job >= 310 && job <= 322) {
            //冒险家弓手
            return true;
        } else if (job >= 410 && job <= 434) {
            //冒险家飞侠
            return true;
        } else if (job >= 510 && job <= 532) {
            //冒险家海盗
            return true;
        } else if (job == 1000) {
            //初心者
            return true;
        } else if (job >= 1100 && job <= 1112) {
            //魂骑士
            return true;
        } else if (job >= 1200 && job <= 1212) {
            //炎术士
            return true;
        } else if (job >= 1300 && job <= 1312) {
            //风灵使者
            return true;
        } else if (job >= 1400 && job <= 1412) {
            //夜行者
            return true;
        } else if (job >= 1500 && job <= 1512) {
            //奇袭者
            return true;
        } else if (job == 2001 || (job >= 2200 && job <= 2218)) {
            //龙神
            return true;
        } else if (job == 2002 || (job >= 2300 && job <= 2312)) {
            //双弩精灵
            return true;
        } else if (job == 2003 || (job >= 2400 && job <= 2412)) {
            //幻影
            return true;
        } else if (job == 2005 || (job >= 2500 && job <= 2512)) {
            //隐月
            return true;
        } else if (job == 2004 || (job >= 2700 && job <= 2712)) {
            //夜光
            return true;
        } else if (job >= 3000) {
            //好像后面的职业都是特殊SP职业
            return true;
        }
        return false;
    }

    public static int getSkillBookByLevel(int job, int level) {
        if (job >= 430 && job <= 434) {
            return level <= 20 ? 0 : (level > 20 && level <= 30) ? 1 : (level > 30 && level <= 55) ? 2 : (level > 55 && level <= 70) ? 3 : (level > 70 && level <= 100) ? 4 : level > 100 ? 5 : 0;
        }
        if (isSeparatedSpJob(job)) {
            return level <= 30 ? 0 : (level > 30 && level <= 60) ? 1 : (level > 60 && level <= 100) ? 2 : level > 100 ? 3 : 0;
        }
        //普通职业
        return 0;
    }

    public static boolean isSeparatedSpJob(int job) {
        return !(JobConstants.is林之灵(job) || is管理员(job) || JobConstants.is暗影双刀(job) || JobConstants.is品克缤(job));
    }

    public static boolean is管理员(int job) {
        return job == 800 || job == 900 || job == 910;
    }

    public static boolean is新手职业(int job) {
        switch (job) {
            case 0://新手
            case 1000: //初心者
            case 2000: //战童
            case 2001: //小不点
            case 2002: //双弩精灵
            case 2003: //幻影
            case 2004: //夜光法师
            case 2005: //隐月
            case 3000: //预备兵
            case 3001: //恶魔猎手
            case 3002: //尖兵
            case 4001: //剑豪
            case 4002: //阴阳师
            case 5000: //无名少年
            case 6000: //狂龙战士
            case 6001: //爆莉萌天使
            case 10000: //神之子
            case 11000: //林之灵
            case 13000: //品克缤
            case 14000: //超能力者
                return true;
        }
        return false;
    }

    public static int getSkillBookByJob(int job) {
        return getSkillBookByJob(job, 0);
    }

    public static int getSkillBookByJob(int job, int skillId) {
        if (job >= 430 && job <= 434) {
            return job - 429;
        }
        if (JobConstants.is神之子(job)) {
            if (skillId > 0) {
                int type = (skillId % 1000) / 100;
                return type == 1 ? 1 : 0;
            } else {
                return 0;
            }
        }
        switch (job) {
            case 110: //英雄2转
            case 120: //圣骑士2转
            case 130: //黑骑士2转
            case 210: //火毒2转
            case 220: //冰雷2转
            case 230: //牧师2转
            case 310: //弓手2转
            case 320: //弩手2转
            case 410: //标飞2转
            case 420: //刀飞2转
            case 510: //拳手2转
            case 520: //枪手2转
            case 530: //炮手2转
            case 570: //龙的传人2转
            case 1110: //魂骑士
            case 1210: //炎术士
            case 1310: //风灵使者
            case 1410: //夜行者
            case 1510: //奇袭者
            case 2110: //战神2转
            case 2211: //龙神2转
            case 2310: //双弩精灵2转
            case 2410: //幻影2转
            case 2710: //夜光2转
            case 3110: //恶魔猎手2转
            case 3120: //恶魔复仇者2转
            case 3210: //幻灵斗师2转
            case 3310: //弩豹游侠2转
            case 3510: //机械师2转
            case 3610: //尖兵2转
            case 5110: //米哈尔2转
            case 6110: //狂龙战士2转
            case 6510: //爆莉萌天使2转
            case 2510:
            case 4110: //剑豪2转
            case 4210: //阴阳师2转
            case 14210: //超能力者2转
            case 3710:
            case 13100:
                return 1;
            case 111: //英雄3转
            case 121: //圣骑士3转
            case 131: //黑骑士3转
            case 211: //火毒3转
            case 221: //冰雷3转
            case 231: //牧师3转
            case 311: //弓手3转
            case 321: //弩手3转
            case 411: //标飞3转
            case 421: //刀飞3转
            case 511: //拳手3转
            case 521: //枪手3转
            case 531: //炮手3转
            case 571: //龙的传人3转
            case 1111: //魂骑士
            case 1211: //炎术士
            case 1311: //风灵使者
            case 1411: //夜行者
            case 1511: //奇袭者
            case 2111: //战神3转
            case 2214: //龙神3转
            case 2311: //双弩精灵3转
            case 2411: //幻影3转
            case 2711: //夜光3转
            case 3111: //恶魔猎手3转
            case 3121: //恶魔复仇者3转
            case 3211: //幻灵斗师3转
            case 3311: //弩豹游侠3转
            case 3511: //机械师3转
            case 3611: //尖兵3转
            case 5111: //米哈尔3转
            case 6111: //狂龙战士3转
            case 6511: //爆莉萌天使3转
            case 2511:
            case 4111: //剑豪3转
            case 4211: //阴阳师3转
            case 14211: //超能力者3转
            case 3711:
                return 2;
            case 112: //英雄4转
            case 122: //圣骑士4转
            case 132: //黑骑士4转
            case 212: //火毒4转
            case 222: //冰雷4转
            case 232: //牧师4转
            case 312: //弓手4转
            case 322: //弩手4转
            case 412: //标飞4转
            case 422: //刀飞4转
            case 512: //拳手4转
            case 522: //枪手4转
            case 532: //炮手4转
            case 572: //龙的传人4转
            case 1112: //魂骑士
            case 1212: //炎术士
            case 1312: //风灵使者
            case 1412: //夜行者
            case 1512: //奇袭者
            case 2112: //战神4转
            case 2217: //龙神4转
            case 2312: //双弩精灵4转
            case 2412: //幻影4转
            case 2712: //夜光4转
            case 3112: //恶魔猎手4转
            case 3122: //恶魔复仇者4转
            case 3212: //幻灵斗师4转
            case 3312: //弩豹游侠4转
            case 3512: //机械师4转
            case 3612: //尖兵4转
            case 5112: //米哈尔4转
            case 6112: //狂龙战士4转
            case 6512: //爆莉萌天使4转
            case 2512:
            case 4112: //剑豪4转
            case 4212: //阴阳师4转
            case 14212: //超能力者2转
            case 3712:
                return 3;
        }
        return 0;
    }

    public static int getBOF_ForJob(int job) {
        return PlayerStats.getSkillByJob(12, job);
    }

    public static int getEmpress_ForJob(int job) {
        return PlayerStats.getSkillByJob(73, job);
    }


    public static String getJobNameById(int job) {
        switch (job) {
            case 0:
            case 1:
                return "新手";
            case 1000:
                return "初心者";
            case 2000:
                return "战童";
            case 2001:
                return "小不点";
            case 3000:
                return "预备兵";
            case 2500:
            case 2510:
            case 2511:
            case 2512:
            case 2005:
                return "隐月";

            case 100:
                return "战士";
            case 110:
                return "剑客";
            case 111:
                return "勇士";
            case 112:
                return "英雄";
            case 120:
                return "准骑士";
            case 121:
                return "骑士";
            case 122:
                return "圣骑士";
            case 130:
                return "枪战士";
            case 131:
                return "龙骑士";
            case 132:
                return "黑骑士";

            case 200:
                return "魔法师";
            case 210:
                return "火毒法师";
            case 211:
                return "火毒巫师";
            case 212:
                return "火毒魔导师";
            case 220:
                return "冰雷法师";
            case 221:
                return "冰雷巫师";
            case 222:
                return "冰雷魔导师";
            case 230:
                return "牧师";
            case 231:
                return "祭祀";
            case 232:
                return "主教";

            case 300:
                return "弓箭手";
            case 310:
                return "猎人";
            case 311:
                return "射手";
            case 312:
                return "神射手";
            case 320:
                return "弩弓手";
            case 321:
                return "游侠";
            case 322:
                return "箭神";

            case 400:
                return "飞侠";
            case 410:
                return "刺客";
            case 411:
                return "无影人";
            case 412:
                return "隐士";
            case 420:
                return "侠客";
            case 421:
                return "独行客";
            case 422:
                return "侠盗";
            case 430:
                return "见习刀客";
            case 431:
                return "双刀客";
            case 432:
                return "双刀侠";
            case 433:
                return "血刀";
            case 434:
                return "暗影双刀";
            case 500:
                return "海盗";
            case 510:
                return "拳手";
            case 511:
                return "斗士";
            case 512:
                return "冲锋队长";
            case 520:
                return "火枪手";
            case 521:
                return "大副";
            case 522:
                return "船长";
            case 501:
                return "海盗炮手";
            case 530:
                return "火炮手";
            case 531:
                return "毁灭炮手";
            case 532:
                return "神炮王";
            case 508:
            case 570:
            case 571:
            case 572:
                return "龙的传人";
            case 1100:
            case 1110:
            case 1111:
            case 1112:
                return "魂骑士";
            case 1200:
            case 1210:
            case 1211:
            case 1212:
                return "炎术士";
            case 1300:
            case 1310:
            case 1311:
            case 1312:
                return "风灵使者";
            case 1400:
            case 1410:
            case 1411:
            case 1412:
                return "夜行者";
            case 1500:
            case 1510:
            case 1511:
            case 1512:
                return "奇袭者";
            case 2100:
            case 2110:
            case 2111:
            case 2112:
                return "战神";
            case 2200:
            case 2211:
            case 2214:
            case 2217:
                return "龙神";
            case 2002:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
                return "双弩精灵";
            case 2003:
            case 2400:
            case 2410:
            case 2411:
            case 2412:
                return "幻影";
            case 2004:
            case 2700:
            case 2710:
            case 2711:
            case 2712:
                return "夜光法师";
            case 3001:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
                return "恶魔猎手";
            case 3101:
            case 3120:
            case 3121:
            case 3122:
                return "恶魔复仇者";
            case 3200:
            case 3210:
            case 3211:
            case 3212:
                return "幻灵斗师";
            case 3300:
            case 3310:
            case 3311:
            case 3312:
                return "弩豹游侠";
            case 3500:
            case 3510:
            case 3511:
            case 3512:
                return "机械师";
            case 3700:
            case 3710:
            case 3711:
            case 3712:
                return "爆破手";
            case 5000:
                return "无名少年";
            case 5100:
            case 5110:
            case 5111:
            case 5112:
                return "米哈尔";
            case 6000:
            case 6100:
            case 6110:
            case 6111:
            case 6112:
                return "狂龙战士";
            case 6001:
            case 6500:
            case 6510:
            case 6511:
            case 6512:
                return "爆莉萌天使";
            case 3002:
            case 3600:
            case 3610:
            case 3611:
            case 3612:
                return "尖兵";
            case 10000:
            case 10100:
            case 10110:
            case 10111:
            case 10112:
                return "神之子";
            case 11000:
            case 11200:
            case 11210:
            case 11211:
            case 11212:
                return "林之灵";
            case 4001:
            case 4100:
            case 4110:
            case 4111:
            case 4112:
                return "剑豪";
            case 4002:
            case 4200:
            case 4210:
            case 4211:
            case 4212:
                return "阴阳师";
            case 900:
                return "管理员";
            case 910:
                return "超级管理员";
            case 800:
                return "管理者";
            default:
                return "";
        }
    }

    public static String getJobBasicNameById(int job) {
        switch (job) {
            case 0:
            case 1:
            case 1000:
            case 2000:
            case 2001:
            case 2002:
            case 2003:
            case 2004:
            case 3000:
            case 3001:
            case 3002:
            case 5000:
            case 6000:
            case 6001:
            case 10000:
            case 11000:
                return "新手";
            case 10100:
            case 10110:
            case 10111:
            case 10112:
            case 6100:
            case 6110:
            case 6111:
            case 6112:
            case 5100:
            case 5110:
            case 5111:
            case 5112:
            case 3600:
            case 3610:
            case 3611:
            case 3612:
            case 3700:
            case 3710:
            case 3711:
            case 3712:
            case 3101:
            case 3120:
            case 3121:
            case 3122:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
            case 2100:
            case 2110:
            case 2111:
            case 2112:
            case 1100:
            case 1110:
            case 1111:
            case 1112:
            case 100:
            case 110:
            case 111:
            case 112:
            case 120:
            case 121:
            case 122:
            case 130:
            case 131:
            case 132:
                return "战士";
            case 11200:
            case 11210:
            case 11211:
            case 11212:
            case 2200:
            case 2211:
            case 2214:
            case 2217:
            case 3200:
            case 3210:
            case 3211:
            case 3212:
            case 1200:
            case 1210:
            case 1211:
            case 1212:
            case 200:
            case 210:
            case 211:
            case 212:
            case 220:
            case 221:
            case 222:
            case 230:
            case 231:
            case 232:
            case 2700:
            case 2710:
            case 2711:
            case 2712:
                return "魔法师";
            case 3300:
            case 3310:
            case 3311:
            case 3312:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 1300:
            case 1310:
            case 1311:
            case 1312:
            case 300:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322:
                return "射手";
            case 1400:
            case 1410:
            case 1411:
            case 1412:
            case 400:
            case 410:
            case 411:
            case 412:
            case 420:
            case 421:
            case 422:
            case 430:
            case 431:
            case 432:
            case 433:
            case 434:
                return "飞侠";
            case 6500:
            case 6510:
            case 6511:
            case 6512:
            case 3500:
            case 3510:
            case 3511:
            case 3512:
            case 1500:
            case 1510:
            case 1511:
            case 1512:
            case 500:
            case 501:
            case 510:
            case 511:
            case 512:
            case 520:
            case 521:
            case 522:
            case 530:
            case 531:
            case 532:
            case 508:
            case 570:
            case 571:
            case 572:
            case 509:
            case 580:
            case 581:
            case 582:
            case 590:
            case 591:
            case 592:
                return "海盗";
            default:
                return "";
        }
    }

    public static String getJobNameByIdNull(int job) {
        switch (job) {
            case 0:
            case 1:
                return "新手";
            case 1000:
                return "初心者";
            case 2000:
                return "战童";
            case 2001:
                return "小不点";
            case 3000:
                return "预备兵";
            case 4001:
                return "剑豪";
            case 4002:
                return "阴阳师";

            case 100:
                return "战士";
            case 110:
                return "剑客";
            case 111:
                return "勇士";
            case 112:
                return "英雄";
            case 120:
                return "准骑士";
            case 121:
                return "骑士";
            case 122:
                return "圣骑士";
            case 130:
                return "枪战士";
            case 131:
                return "龙骑士";
            case 132:
                return "黑骑士";

            case 200:
                return "魔法师";
            case 210:
                return "火毒法师";
            case 211:
                return "火毒巫师";
            case 212:
                return "火毒魔导师";
            case 220:
                return "冰雷法师";
            case 221:
                return "冰雷巫师";
            case 222:
                return "冰雷魔导师";
            case 230:
                return "牧师";
            case 231:
                return "祭祀";
            case 232:
                return "主教";

            case 300:
                return "弓箭手";
            case 310:
                return "猎人";
            case 311:
                return "射手";
            case 312:
                return "神射手";
            case 320:
                return "弩弓手";
            case 321:
                return "游侠";
            case 322:
                return "箭神";

            case 400:
                return "飞侠";
            case 410:
                return "刺客";
            case 411:
                return "无影人";
            case 412:
                return "隐士";
            case 420:
                return "侠客";
            case 421:
                return "独行客";
            case 422:
                return "侠盗";
            case 430:
                return "见习刀客";
            case 431:
                return "双刀客";
            case 432:
                return "双刀侠";
            case 433:
                return "血刀";
            case 434:
                return "暗影双刀";
            case 500:
                return "海盗";
            case 510:
                return "拳手";
            case 511:
                return "斗士";
            case 512:
                return "冲锋队长";
            case 520:
                return "火枪手";
            case 521:
                return "大副";
            case 522:
                return "船长";
            case 501:
                return "海盗炮手";
            case 530:
                return "火炮手";
            case 531:
                return "毁灭炮手";
            case 532:
                return "神炮王";
            case 508:
                return "龙的传人(1转)";
            case 570:
                return "龙的传人(2转)";
            case 571:
                return "龙的传人(3转)";
            case 572:
                return "龙的传人(4转)";

            case 1100:
                return "魂骑士(1转)";
            case 1110:
                return "魂骑士(2转)";
            case 1111:
                return "魂骑士(3转)";
            case 1112:
                return "魂骑士(4转)";
            case 1200:
                return "炎术士(1转)";
            case 1210:
                return "炎术士(2转)";
            case 1211:
                return "炎术士(3转)";
            case 1212:
                return "炎术士(4转)";

            case 1300:
                return "风灵使者(1转)";
            case 1310:
                return "风灵使者(2转)";
            case 1311:
                return "风灵使者(3转)";
            case 1312:
                return "风灵使者(4转)";

            case 1400:
                return "夜行者(1转)";
            case 1410:
                return "夜行者(2转)";
            case 1411:
                return "夜行者(3转)";
            case 1412:
                return "夜行者(4转)";

            case 1500:
                return "奇袭者(1转)";
            case 1510:
                return "奇袭者(2转)";
            case 1511:
                return "奇袭者(3转)";
            case 1512:
                return "奇袭者(4转)";

            case 2100:
                return "战神(1转)";
            case 2110:
                return "战神(2转)";
            case 2111:
                return "战神(3转)";
            case 2112:
                return "战神(4转)";

            case 2200:
                return "龙神(1转)";
            case 2211:
                return "龙神(2转)";
            case 2214:
                return "龙神(3转)";
            case 2217:
                return "龙神(4转)";

            case 2002:
                return "双弩精灵";
            case 2300:
                return "双弩精灵(1转)";
            case 2310:
                return "双弩精灵(2转)";
            case 2311:
                return "双弩精灵(3转)";
            case 2312:
                return "双弩精灵(4转)";

            case 2003:
                return "幻影侠盗";
            case 2400:
                return "幻影侠盗(1转)";
            case 2410:
                return "幻影侠盗(2转)";
            case 2411:
                return "幻影侠盗(3转)";
            case 2412:
                return "幻影侠盗(4转)";

            case 2004:
                return "夜光法师";
            case 2700:
                return "夜光法师(1转)";
            case 2710:
                return "夜光法师(2转)";
            case 2711:
                return "夜光法师(3转)";
            case 2712:
                return "夜光法师(4转)";

            case 3001:
                return "恶魔猎手";
            case 3100:
                return "恶魔猎手(1转)";
            case 3110:
                return "恶魔猎手(2转)";
            case 3111:
                return "恶魔猎手(3转)";
            case 3112:
                return "恶魔猎手(4转)";

            case 3101:
                return "恶魔复仇者(1转)";
            case 3120:
                return "恶魔复仇者(2转)";
            case 3121:
                return "恶魔复仇者(3转)";
            case 3122:
                return "恶魔复仇者(4转)";

            case 3200:
                return "幻灵斗师(1转)";
            case 3210:
                return "幻灵斗师(2转)";
            case 3211:
                return "幻灵斗师(3转)";
            case 3212:
                return "幻灵斗师(4转)";

            case 3300:
                return "弩豹游侠(1转)";
            case 3310:
                return "弩豹游侠(2转)";
            case 3311:
                return "弩豹游侠(3转)";
            case 3312:
                return "弩豹游侠(4转)";

            case 3500:
                return "机械师(1转)";
            case 3510:
                return "机械师(2转)";
            case 3511:
                return "机械师(3转)";
            case 3512:
                return "机械师(4转)";

            case 3700:
                return "爆破手(1转)";
            case 3710:
                return "爆破手(2转)";
            case 3711:
                return "爆破手(3转)";
            case 3712:
                return "爆破手(4转)";

            case 3002:
                return "尖兵(新手)";
            case 3600:
                return "尖兵(1转)";
            case 3610:
                return "尖兵(2转)";
            case 3611:
                return "尖兵(3转)";
            case 3612:
                return "尖兵(4转)";

            case 5000:
                return "无名少年";
            case 5100:
                return "米哈尔(1转)";
            case 5110:
                return "米哈尔(2转)";
            case 5111:
                return "米哈尔(3转)";
            case 5112:
                return "米哈尔(4转)";

            case 6000:
                return "狂龙战士";
            case 6100:
                return "狂龙战士(1转)";
            case 6110:
                return "狂龙战士(2转)";
            case 6111:
                return "狂龙战士(3转)";
            case 6112:
                return "狂龙战士(4转)";

            case 6001:
                return "爆莉萌天使";
            case 6500:
                return "爆莉萌天使(1转)";
            case 6510:
                return "爆莉萌天使(2转)";
            case 6511:
                return "爆莉萌天使(3转)";
            case 6512:
                return "爆莉萌天使(4转)";

            case 10000:
                return "神之子";
            case 10100:
                return "神之子(1转)";
            case 10110:
                return "神之子(2转)";
            case 10111:
                return "神之子(3转)";
            case 10112:
                return "神之子(4转)";

            case 11000:
                return "林之灵";
            case 11200:
                return "林之灵(1转)";
            case 11210:
                return "林之灵(2转)";
            case 11211:
                return "林之灵(3转)";
            case 11212:
                return "林之灵(4转)";
            case 4100:
                return "剑豪(1转)";
            case 4110:
                return "剑豪(2转)";
            case 4111:
                return "剑豪(3转)";
            case 4112:
                return "剑豪(4转)";
            case 4200:
                return "阴阳师(1转)";
            case 4210:
                return "阴阳师(2转)";
            case 4211:
                return "阴阳师(3转)";
            case 4212:
                return "阴阳师(4转)";

            case 900:
                return "管理员";
            case 910:
                return "超级管理员";
            case 800:
                return "管理者";
            default:
                return null;
        }
    }

    public static String getJobGroupName(int job) {
        if (is反抗者(job)) {
            return "反抗者";
        } else if (is冒险家(job)) {
            return "冒险家";
        } else if (is骑士团(job)) {
            return "骑士团";
        } else if (is战神(job)) {
            return "战神";
        } else if (is龙神(job)) {
            return "龙神";
        } else if (is双弩精灵(job)) {
            return "双弩精灵";
        } else if (is恶魔(job)) {
            return "恶魔";
        } else if (is幻影(job)) {
            return "幻影";
        } else if (is暗影双刀(job)) {
            return "暗影双刀";
        } else if (is米哈尔(job)) {
            return "米哈尔";
        } else if (is夜光(job)) {
            return "夜光法师";
        } else if (is狂龙战士(job)) {
            return "狂龙战士";
        } else if (is爆莉萌天使(job)) {
            return "爆莉萌天使";
        } else if (is火炮手(job)) {
            return "火炮手";
        } else if (is尖兵(job)) {
            return "尖兵";
        } else if (is神之子(job)) {
            return "神之子";
        } else if (is隐月(job)) {
            return "隐月";
        } else if (is品克缤(job)) {
            return "品克缤";
        } else if (is超能力者(job)) {
            return "超能力者";
        } else if (is林之灵(job)) {
            return "林之灵";
        } else if (is龙的传人(job)) {
            return "龙的传人";
        } else if (is剑豪(job)) {
            return "剑豪";
        } else if (is阴阳师(job)) {
            return "阴阳师";
        }
        return "";
    }

    public static boolean canUseFamiliar(int job) {
        if (is米哈尔(job) || is骑士团(job) || is尖兵(job) || is夜光(job) || is隐月(job) || is战神(job) || is龙的传人(job)) {
            return true;
        } else if (job / 1000 == 3) {
            if (is唤灵斗师(job)) {
                return true;
            } else if (getTrueJobGrade(job) == 2 || getTrueJobGrade(job) == 4) {
                return false;
            }
        }
        return false;
    }

    public static short getBeginner(short jobid) {
        if (jobid % 1000 < 10) {
            return jobid;
        }
        switch (jobid / 100) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5: {
                return (short) 0;
            }
            case 8: {
                return (short) 800;
            }
            case 9: {
                return (short) 900;
            }
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15: {
                return (short) 1000;
            }
            case 21:
            case 20: {
                return (short) 2000;
            }
            case 22: {
                return (short) 2001;
            }
            case 23: {
                return (short) 2002;
            }
            case 24: {
                return (short) 2003;
            }
            case 25: {
                return (short) 2005;
            }
            case 27: {
                return (short) 2004;
            }
            case 31: {
                return (short) 3001;
            }
            case 36: {
                return (short) 3002;
            }
            case 30:
            case 32:
            case 33:
            case 35:
            case 37: {
                return (short) 3000;
            }
            case 40:
            case 41: {
                return (short) 4001;
            }
            case 42: {
                return (short) 4002;
            }
            case 50:
            case 51: {
                return (short) 5000;
            }
            case 60:
            case 61: {
                return (short) 6000;
            }
            case 65: {
                return (short) 6001;
            }
            case 100:
            case 101: {
                return (short) 10000;
            }
            case 110:
            case 112: {
                return (short) 11000;
            }
            case 130:
            case 131: {
                return (short) 13000;
            }
            case 140:
            case 142: {
                return (short) 14000;
            }
        }
        return (short) 1000;
    }
}
