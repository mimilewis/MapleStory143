package client.inventory;

import client.MapleCharacter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import configs.ServerConfig;
import configs.StarForceConfig;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import handling.channel.handler.ItemScrollHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.StructItemOption;
import tools.DateUtil;
import tools.Randomizer;
import tools.packet.InventoryPacket;

import java.io.Serializable;
import java.util.*;

public class Equip extends Item implements Serializable {

    public static final long ARMOR_RATIO = 350000;
    public static final long WEAPON_RATIO = 700000;
    private static final long serialVersionUID = -4385634094556865314L;
    //charm: -1 = has not been initialized yet, 0 = already been worn, >0 = has teh charm exp
    private byte upgradeSlots = 0, level = 0, vicioushammer = 0, state = 0, addState, enhance = 0;
    private short enhanctBuff = 0, reqLevel = 0, yggdrasilWisdom = 0, bossDamage = 0, ignorePDR = 0, totalDamage = 0, allStat = 0, karmaCount = -1; //新增的装备属性
    private boolean finalStrike = false;  //新增的装备属性
    private short str = 0, dex = 0, _int = 0, luk = 0, hp = 0, mp = 0, watk = 0, matk = 0, wdef = 0, mdef = 0, acc = 0, avoid = 0, hands = 0, speed = 0, jump = 0, charmExp = 0, pvpDamage = 0;
    private int durability = -1, incSkill = -1, statemsg = 0;
    private int potential1 = 0, potential2 = 0, potential3 = 0, potential4 = 0, potential5 = 0, potential6 = 0;
    private int socket1 = -1, socket2 = -1, socket3 = -1; //V.102新增 装备插槽
    private int itemSkin = 0; //装备皮肤 也是装备外观改变 以后会用到暂时写在这
    private int limitBreak = 0; //武器装备的攻击突破上限附加数字
    private MapleRing ring = null;
    private MapleAndroid android = null;
    // 潜能锁
    private int lockSlot = 0;
    private short lockId = 0;
    private byte sealedLevel = 0;
    private long sealedExp = 0, itemEXP = 0, fire = -1;
    private short soulname, soulenchanter, soulpotential;
    private int soulSkill = 0;
    private Map<EquipStats, Long> statsTest = new LinkedHashMap<>();
    private int iIncReq;
    private NirvanaFlame nirvanaFlame = new NirvanaFlame();
    private int bonus = 0;
    private short ARC;
    private int ARCExp;
    private short ARCLevel = 1;

    @JsonCreator
    public Equip(@JsonProperty("id") int id, @JsonProperty("position") short position, @JsonProperty("flag") short flag) {
        super(id, position, (short) 1, flag);
    }

    public Equip(int id, short position, int uniqueid, short flag, short espos) {
        super(id, position, (short) 1, flag, uniqueid, espos);
    }

    @Override
    public Item copy() {
        Equip ret = new Equip(getItemId(), getPosition(), getUniqueId(), getFlag(), getESPos());
        ret.str = str; //力量
        ret.dex = dex; //敏捷
        ret._int = _int; //智力
        ret.luk = luk; //运气
        ret.hp = hp; //Hp
        ret.mp = mp; //Mp
        ret.matk = matk; //魔法攻击
        ret.mdef = mdef; //魔法防御
        ret.watk = watk; //物理攻击
        ret.wdef = wdef; //物理防御
        ret.acc = acc; //命中率
        ret.avoid = avoid; //回避率
        ret.hands = hands; //手技
        ret.speed = speed; //移动速度
        ret.jump = jump; //跳跃力
        ret.upgradeSlots = upgradeSlots;  //可升级次数
        ret.level = level; //已升级次数
        ret.itemEXP = itemEXP;
        ret.durability = durability; //耐久度
        ret.vicioushammer = vicioushammer; //金锤子
        ret.state = state; //潜能等级
        ret.addState = addState;
        ret.enhance = enhance; //星级
        ret.potential1 = potential1; //潜能1
        ret.potential2 = potential2; //潜能2
        ret.potential3 = potential3; //潜能3
        ret.potential4 = potential4; //潜能4
        ret.potential5 = potential5; //潜能5
        ret.potential6 = potential6; //潜能6
        ret.charmExp = charmExp; //魅力经验
        ret.pvpDamage = pvpDamage; //大乱斗攻击力
        ret.incSkill = incSkill; //是否拥有技能
        ret.statemsg = statemsg; //星级提示
        ret.socket1 = socket1; //镶嵌宝石1
        ret.socket2 = socket2; //镶嵌宝石1
        ret.socket3 = socket3; //镶嵌宝石1
        ret.itemSkin = itemSkin; //道具合成后的外观
        ret.limitBreak = limitBreak; //武器攻击突破上限
        //---------------------------------------------------------
        //下面的为新增的装备属性
        ret.enhanctBuff = enhanctBuff;
        ret.reqLevel = reqLevel;
        ret.yggdrasilWisdom = yggdrasilWisdom;
        ret.finalStrike = finalStrike;
        ret.bossDamage = bossDamage;
        ret.ignorePDR = ignorePDR;
        ret.totalDamage = totalDamage;
        ret.allStat = allStat;
        ret.karmaCount = karmaCount;
        ret.statsTest = statsTest;
        //---------------------------------------------------------
        ret.setGMLog(getGMLog()); //装备是从什么地方获得的信息
        ret.setGiftFrom(getGiftFrom()); //是谁送的礼物
        ret.setOwner(getOwner()); //拥有者名字
        ret.setQuantity(getQuantity()); //数量
        ret.setExpiration(getExpiration()); //道具经验
        ret.setInventoryId(getInventoryId()); //道具的SQLid分解装备和合成装备需要
        ret.setEquipOnlyId(getEquipOnlyId()); //装备道具的唯一ID
        //--------------------------------------------------------
        ret.lockSlot = lockSlot;
        ret.lockId = lockId;
        ret.sealedLevel = sealedLevel;
        ret.sealedExp = sealedExp;
        //灵魂武器
        ret.soulname = soulname;
        ret.soulenchanter = soulenchanter;
        ret.soulpotential = soulpotential;
        ret.soulSkill = soulSkill;
        ret.fire = fire;
        ret.nirvanaFlame = nirvanaFlame;
        ret.ARC = ARC;
        ret.ARCExp = ARCExp;
        ret.ARCLevel = ARCLevel;
        return ret;
    }

    public Item inherit(Equip srcEquip, Equip decEquip) {
        this.str = (short) (this.str + (short) (srcEquip.str - decEquip.str));
        this.dex = (short) (this.dex + (short) (srcEquip.dex - decEquip.dex));
        this._int = (short) (this._int + (short) (srcEquip._int - decEquip._int));
        this.luk = (short) (this.luk + (short) (srcEquip.luk - decEquip.luk));
        this.hp = (short) (this.hp + (short) (srcEquip.hp - decEquip.hp));
        this.mp = (short) (this.mp + (short) (srcEquip.mp - decEquip.mp));
        this.matk = (short) (this.matk + (short) (srcEquip.matk - decEquip.matk));
        this.mdef = (short) (this.mdef + (short) (srcEquip.mdef - decEquip.mdef));
        this.watk = (short) (this.watk + (short) (srcEquip.watk - decEquip.watk));
        this.wdef = (short) (this.wdef + (short) (srcEquip.wdef - decEquip.wdef));
        this.acc = (short) (this.acc + (short) (srcEquip.acc - decEquip.acc));
        this.avoid = (short) (this.avoid + (short) (srcEquip.avoid - decEquip.avoid));
        this.hands = (short) (this.hands + (short) (srcEquip.hands - decEquip.hands));
        this.speed = (short) (this.speed + (short) (srcEquip.speed - decEquip.speed));
        this.jump = (short) (this.jump + (short) (srcEquip.jump - decEquip.jump));
        this.upgradeSlots = srcEquip.upgradeSlots;
        this.level = srcEquip.level;
        this.itemEXP = srcEquip.itemEXP;
        this.durability = srcEquip.durability;
        this.vicioushammer = srcEquip.vicioushammer;
        this.enhance = srcEquip.enhance;
        this.charmExp = srcEquip.charmExp;
        this.pvpDamage = srcEquip.pvpDamage;
        this.incSkill = srcEquip.incSkill;
        this.limitBreak = srcEquip.limitBreak;
        this.enhanctBuff = srcEquip.enhanctBuff;
        this.reqLevel = srcEquip.reqLevel;
        this.yggdrasilWisdom = srcEquip.yggdrasilWisdom;
        this.finalStrike = srcEquip.finalStrike;
        this.bossDamage = srcEquip.bossDamage;
        this.ignorePDR = srcEquip.ignorePDR;
        this.totalDamage = srcEquip.totalDamage;
        this.allStat = srcEquip.allStat;
        this.karmaCount = srcEquip.karmaCount;
        this.fire = srcEquip.fire;
        this.nirvanaFlame = new NirvanaFlame(srcEquip.nirvanaFlame);
        this.soulname = srcEquip.soulname;
        this.soulenchanter = srcEquip.soulenchanter;
        this.soulpotential = srcEquip.soulpotential;
        this.sealedLevel = srcEquip.sealedLevel;
        this.sealedExp = srcEquip.sealedExp;
        this.setGiftFrom(this.getGiftFrom());
        this.copyPotential(srcEquip);
        return this;
    }

    @Override
    public byte getType() {
        return 1;
    }

    public Equip copyPotential(final Equip equip) {
        this.potential1 = equip.potential1;
        this.potential2 = equip.potential2;
        this.potential3 = equip.potential3;
        this.potential4 = equip.potential4;
        this.potential5 = equip.potential5;
        this.potential6 = equip.potential6;
        this.state = equip.state;
        this.addState = equip.addState;
        return this;
    }

    public byte getUpgradeSlots() {
        return upgradeSlots;
    }

    public void setUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public short getStr() {
        return (short) (str + nirvanaFlame.getNstr());
    }

    public void setStr(short str) {
        if (str < 0) {
            str = 0;
        }
        this.str = str;
    }

    public short getDex() {
        return (short) (dex + nirvanaFlame.getNdex());
    }

    public void setDex(short dex) {
        if (dex < 0) {
            dex = 0;
        }
        this.dex = dex;
    }

    public short getInt() {
        return (short) (_int + nirvanaFlame.getNint());
    }

    public void setInt(short _int) {
        if (_int < 0) {
            _int = 0;
        }
        this._int = _int;
    }

    public short getLuk() {
        return (short) (luk + nirvanaFlame.getNluk());
    }

    public void setLuk(short luk) {
        if (luk < 0) {
            luk = 0;
        }
        this.luk = luk;
    }

    public short getHp() {
        return (short) (hp + nirvanaFlame.getNhp());
    }

    public void setHp(short hp) {
        if (hp < 0) {
            hp = 0;
        }
        this.hp = hp;
    }

    public short getMp() {
        return (short) (mp + nirvanaFlame.getNmp());
    }

    public void setMp(short mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public short getWatk() {
        return (short) (watk + nirvanaFlame.getNwatk());
    }

    public void setWatk(short watk) {
        if (watk < 0) {
            watk = 0;
        }
        this.watk = watk;
    }

    public short getMatk() {
        return (short) (matk + nirvanaFlame.getNmatk());
    }

    public void setMatk(short matk) {
        if (matk < 0) {
            matk = 0;
        }
        this.matk = matk;
    }

    public short getWdef() {
        return (short) (wdef + nirvanaFlame.getNwdef());
    }

    public void setWdef(short wdef) {
        if (wdef < 0) {
            wdef = 0;
        }
        this.wdef = wdef;
    }

    public short getMdef() {
        return (short) (mdef + nirvanaFlame.getNmdef());
    }

    public void setMdef(short mdef) {
        if (mdef < 0) {
            mdef = 0;
        }
        this.mdef = mdef;
    }

    public short getAcc() {
        return (short) (acc + nirvanaFlame.getNacc());
    }

    public void setAcc(short acc) {
        if (acc < 0) {
            acc = 0;
        }
        this.acc = acc;
    }

    public short getAvoid() {
        return (short) (avoid + nirvanaFlame.getNavoid());
    }

    public void setAvoid(short avoid) {
        if (avoid < 0) {
            avoid = 0;
        }
        this.avoid = avoid;
    }

    public short getHands() {
        return (short) (hands + nirvanaFlame.getNhands());
    }

    public void setHands(short hands) {
        if (hands < 0) {
            hands = 0;
        }
        this.hands = hands;
    }

    public short getSpeed() {
        return (short) (speed + nirvanaFlame.getNspeed());
    }

    public void setSpeed(short speed) {
        if (speed < 0) {
            speed = 0;
        }
        this.speed = speed;
    }

    public short getJump() {
        return (short) (jump + nirvanaFlame.getNjump());
    }

    public void setJump(short jump) {
        if (jump < 0) {
            jump = 0;
        }
        this.jump = jump;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public byte getViciousHammer() {
        return vicioushammer;
    }

    public void setViciousHammer(byte ham) {
        vicioushammer = ham;
    }

    public long getItemEXP() {
        return itemEXP;
    }

    public void setItemEXP(long itemEXP) {
        if (itemEXP < 0) {
            itemEXP = 0;
        }
        this.itemEXP = itemEXP;
    }

    public long getEquipExp() {
        if (itemEXP <= 0) {
            return 0;
        }
        //aproximate value
        if (ItemConstants.isWeapon(getItemId())) {
            return itemEXP / WEAPON_RATIO;
        } else {
            return itemEXP / ARMOR_RATIO;
        }
    }

    public long getEquipExpForLevel() {
        if (getEquipExp() <= 0) {
            return 0;
        }
        long expz = getEquipExp();
        for (int i = getBaseLevel(); i <= GameConstants.getMaxLevel(getItemId()); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else {
                break;
            }
        }
        return expz;
    }

    public long getExpPercentage() {
        if (getEquipLevel() < getBaseLevel() || getEquipLevel() > GameConstants.getMaxLevel(getItemId()) || GameConstants.getExpForLevel(getEquipLevel(), getItemId()) <= 0) {
            return 0;
        }
        return getEquipExpForLevel() * 100 / GameConstants.getExpForLevel(getEquipLevel(), getItemId());
    }

    public int getEquipLevel() {
        int fixLevel = 0;
        Map<String, Integer> equipStats = MapleItemInformationProvider.getInstance().getItemBaseInfo(getItemId());
        if (equipStats.containsKey("fixLevel")) {
            fixLevel = equipStats.get("fixLevel");
        }

        if (GameConstants.getMaxLevel(getItemId()) <= 0) {
            return fixLevel;
        }

        int levelz = getBaseLevel() + fixLevel;
        if (getEquipExp() <= 0) {
            return levelz;
        }
        long expz = getEquipExp();
        for (int i = levelz; i < GameConstants.getMaxLevel(getItemId()); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                levelz++;
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else {
                break;
            }
        }
        return levelz;
    }

    public int getBaseLevel() {
        return (GameConstants.getStatFromWeapon(getItemId()) == null ? 1 : 0);
    }

    @Override
    public void setQuantity(short quantity) {
        if (quantity < 0 || quantity > 1) {
            throw new RuntimeException("设置装备的数量错误 欲设置的数量： " + quantity + " (道具ID: " + getItemId() + ")");
        }
        super.setQuantity(quantity);
    }

    /*
     * 耐久度也就是持久
     */
    public int getDurability() {
        return durability;
    }

    public void setDurability(int dur) {
        this.durability = dur;
    }

    /*
     * 星级
     */
    public byte getEnhance() {
        return enhance;
    }

    public void setEnhance(byte en) {
        this.enhance = en;
    }

    public int getPotential(int pos, boolean add) {
        switch (pos) {
            case 1: {
                if (add) {
                    return this.potential4;
                }
                return this.potential1;
            }
            case 2: {
                if (add) {
                    return this.potential5;
                }
                return this.potential2;
            }
            case 3: {
                if (add) {
                    return this.potential6;
                }
                return this.potential3;
            }
        }
        return 0;
    }

    public void setPotential(int potential, int pos, boolean add) {
        switch (pos) {
            case 1: {
                if (add) {
                    this.potential4 = potential;
                    break;
                }
                this.potential1 = potential;
                break;
            }
            case 2: {
                if (add) {
                    this.potential5 = potential;
                    break;
                }
                this.potential2 = potential;
                break;
            }
            case 3: {
                if (add) {
                    this.potential6 = potential;
                    break;
                }
                this.potential3 = potential;
            }
        }
    }

    /*
     * 潜能属性1
     */
    public int getPotential1() {
        return potential1;
    }

    public void setPotential1(int en) {
        this.potential1 = en;
    }

    /*
     * 潜能属性2
     */
    public int getPotential2() {
        return potential2;
    }

    public void setPotential2(int en) {
        this.potential2 = en;
    }

    /*
     * 潜能属性3
     */
    public int getPotential3() {
        return potential3;
    }

    public void setPotential3(int en) {
        this.potential3 = en;
    }

    /*
     * 潜能属性4
     */
    public int getPotential4() {
        return potential4;
    }

    public void setPotential4(int en) {
        this.potential4 = en;
    }

    /*
     * 潜能属性5
     */
    public int getPotential5() {
        return potential5;
    }

    public void setPotential5(int en) {
        this.potential5 = en;
    }

    /*
     * 潜能属性6
     */
    public int getPotential6() {
        return potential6;
    }

    public void setPotential6(int en) {
        this.potential6 = en;
    }

    /*
     * 装备的等级
     * 15 = 未鉴定 16以下 20以上都是未鉴定
     * 16 = C级
     * 17 = B级
     * 18 = A级
     * 19 = S级
     * 20 = SS级
     */
    public byte getState(boolean add) {
        if (add) {
            return addState;
        }
        return state;
    }

    public void setState(byte en, boolean add) {
        if (add) {
            addState = en;
        } else {
            state = en;
        }
    }

    public void initAllState() {
        initState(false);
        initState(true);
    }

    public void initState(boolean useAddPot) {
        int n = 0;
        int n2;
        int n3;
        int n4;
        if (!useAddPot) {
            n2 = this.potential1;
            n3 = this.potential2;
            n4 = this.potential3;
        } else {
            n2 = this.potential4;
            n3 = this.potential5;
            n4 = this.potential6;
        }
        if (n2 >= 40000 || n3 >= 40000 || n4 >= 40000) {
            n = 20;
        } else if (n2 >= 30000 || n3 >= 30000 || n4 >= 30000) {
            n = 19;
        } else if (n2 >= 20000 || n3 >= 20000 || n4 >= 20000) {
            n = 18;
        } else if (n2 >= 1 || n3 >= 1 || n4 >= 1) {
            n = 17;
        } else if (n2 == -20 || n3 == -20 || n2 == -4 || n3 == -4) {
            n = 4;
        } else if (n2 == -19 || n3 == -19 || n2 == -3 || n3 == -3) {
            n = 3;
        } else if (n2 == -18 || n3 == -18 || n2 == -2 || n3 == -2) {
            n = 2;
        } else if (n2 == -17 || n3 == -17 || n2 == -1 || n3 == -1) {
            n = 1;
        } else if (n2 < 0 || n3 < 0 || n4 < 0) {
            return;
        }
        this.setState((byte) n, useAddPot);
    }

    public void resetPotential_Fuse(boolean half, int potentialState) { //maker skill - equip first receive
        //0.16% chance unique, 4% chance epic, else rare
        potentialState = -potentialState;
        if (Randomizer.nextInt(100) < 4) {
            potentialState -= Randomizer.nextInt(100) < 4 ? 2 : 1;
        }
        setPotential1(potentialState);
        setPotential2((Randomizer.nextInt(half ? 5 : 10) == 0 ? potentialState : 0)); //1/10 chance of 3 line
        setPotential3(0); //just set it theoretically
        initState(false);
    }

    public void renewPotential(final boolean add) {
        this.renewPotential(0, add);
    }

    public void renewPotential(final int rank, final boolean add) {
        this.renewPotential(rank, false, add);
    }

    public void renewPotential(final boolean b, final boolean add) {
        this.renewPotential(0, b, add);
    }

    public void renewPotential(final int rank, final boolean b, final boolean add) {
        int state;
        switch (rank) {
            case 1: {
                state = -17;
                break;
            }
            case 2: {
                state = -18;
                break;
            }
            case 3: {
                state = -19;
                break;
            }
            case 4: {
                state = -20;
                break;
            }
            default: {
                state = ((Randomizer.nextInt(100) < 4) ? ((Randomizer.nextInt(100) < 4) ? -19 : -18) : -17);
                break;
            }
        }
        final boolean b3 = (this.getState(add) != 0 && this.getPotential(3, add) != 0) || b;
        this.setPotential(state, 1, add);
        this.setPotential(Randomizer.nextInt(10) <= 1 || b3 ? state : 0, 2, add);
        this.setPotential(0, 3, add);
        this.initState(add);
    }

    public void renewPotential_A() {
        this.setPotential(-18, 1, false);
        this.setPotential((Randomizer.nextInt(1000) <= 100) ? -18 : 0, 2, false);
        this.setPotential(0, 3, false);
        this.initState(false);
    }

    public void renewPotential_S() {
        this.setPotential(-19, 1, false);
        this.setPotential((Randomizer.nextInt(1000) <= 50) ? -19 : 0, 2, false);
        this.setPotential(0, 3, false);
        this.initState(false);
    }

    public boolean resetPotential(int itemid, MapleCharacter player, int lockslot, short lockid) {
        if (player.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= 1) {
            int rate = ServerConfig.CHANNEL_RATE_POTENTIALLEVEL * ItemConstants.PotentialConstants.getCubeRate(itemid) * 10;
            int flag = ItemConstants.PotentialConstants.getDefaultPotentialFlag(itemid);
            boolean bl2 = ItemConstants.PotentialConstants.PotentialFlag.附加潜能.check(flag);
            if (!ItemConstants.PotentialConstants.canUse(this, itemid) || itemid == 2711002 && !JobConstants.is神之子(player.getJob())) {
                player.dropMessage(5, "你无法对这个物品使用这个魔方。");
                return false;
            }
            switch (itemid) {
                case 5062009:
                case 5062010:
                case 5062022:
                case 5062500:
                case 5062501:
                case 5062502:
                case 5062503: {
                    long meso = ItemConstants.getCubeNeedMeso(this);
                    if (player.getMeso() >= meso) {
                        player.gainMeso(-meso, false);
                        break;
                    }
                    player.dropMessage(5, "您没有足够的金币。");
                    player.sendEnableActions();
                    return false;
                }
            }
            switch (itemid) {
                case 5062000:
                case 5062009:
                case 5062010:
                case 5062500: {
                    if (!player.haveItem(4009453, 1)) {
                        player.dropMessage(5, "为了激活魔方需要有一个魔方的精髓！");
                        return false;
                    }
                    player.removeItem(4009453, 1);
                }
            }
            if (this.getState(bl2) >= 17 && this.getState(bl2) <= 20) {
                block3:
                switch (itemid) {
                    case 5062010:
                    case 5062090:
                    case 5062503: {
                        this.savePotentialInfo(player, flag, player.isAdmin() ? 999 : rate, itemid);
                        break;
                    }
                    default: {
                        Item item = player.getInventory(MapleInventoryType.CASH).findById(5067000);
                        if (lockslot > 0) {
                            if (item == null) {
                                return false;
                            }
                            MapleInventoryManipulator.removeById(player.getClient(), MapleInventoryType.CASH, item.getPosition(), 1, false, true);
                        }
                        this.c(player.isAdmin() ? 999 : rate, flag, lockslot);
                        switch (itemid) {
                            case 5062024: {
                                updateSxuanState(player);
                                break block3;
                            }
                            case 2710000:
                            case 2710001:
                            case 2710002:
                            case 2710003:
                            case 2711000:
                            case 2711001:
                            case 2711002:
                            case 2711003:
                            case 2711004:
                            case 2711005:
                            case 2711006:
                            case 2711007:
                            case 2711008:
                            case 2711009:
                            case 5062009:
                            case 5062022:
                            case 5062500:
                            case 5062501:
                            case 5062502: {
                                resetPotential(player, flag, itemid);
                            }
                        }
                    }
                }
                player.forceUpdateItem(this);
                if (bl2) {
                    player.getMap().broadcastMessage(InventoryPacket.潜能变化效果(player.getId(), true, itemid));
                } else {
                    player.getMap().broadcastMessage(InventoryPacket.showPotentialReset(false, player.getId(), true, itemid));
                }
                int debris = ItemConstants.PotentialConstants.getCubeDebris(itemid);
                if (debris > 0) {
                    MapleInventoryManipulator.addById(player.getClient(), debris, (short) 1, "Cube on " + DateUtil.getCurrentDate());
                }
                return true;
            }
            player.dropMessage(5, "请确认您要重置的道具具有潜能属性。");
        }
        return false;
    }

    public void updateSxuanState(MapleCharacter player) {
        int n2 = this.getPotential(2, false) != 0 ? 6 : 4;
        List<StructItemOption> arrayList = this.br(n2);
        player.updateOneInfo(52998, "dst", String.valueOf(this.getPosition()));
        player.getClient().announce(InventoryPacket.showHyunPotentialResult(false, n2 / 2, arrayList));
    }

    public void resetPotential(final MapleCharacter player, final int flag, final int itemid) {
        final boolean check = ItemConstants.PotentialConstants.PotentialFlag.附加潜能.check(flag);
        ItemScrollHandler.ItemPotentialAndMagnify(this, player, false);
        this.initState(check);
        player.send(InventoryPacket.showCubeResult(player.getId(), itemid, this.getPosition(), this.copy()));
    }

    public void savePotentialInfo(MapleCharacter player, int flag, int rate, int itemid) {
        Equip nEquip = (Equip) this.copy();
        nEquip.c(rate, flag, 0);
        boolean bl2 = ItemConstants.PotentialConstants.PotentialFlag.附加潜能.check(flag);
        int n5 = this.getPotential(2, bl2) != 0 ? 3 : 2;
        nEquip.setStateMsg(3);
        ItemScrollHandler.ItemPotentialAndMagnify(nEquip, player, false);
        player.updateInfoQuest(52889, "dst=-1;Pot0=-1;Pot1=-1;Pot2=-1;add=0");
        for (int i2 = 0; i2 < n5; ++i2) {
            player.updateOneInfo(52889, "Pot" + i2, String.valueOf(nEquip.getPotential(i2 + 1, bl2)));
        }
        player.updateOneInfo(52889, "dst", String.valueOf(this.getPosition()));
        player.updateOneInfo(52889, "add", ItemConstants.PotentialConstants.PotentialFlag.附加潜能.check(flag) ? "1" : "0");
        player.send(InventoryPacket.showCubeResetResult((int) this.getPosition(), nEquip, itemid, (int) player.getInventory(MapleInventoryType.CASH).findById(itemid).getPosition()));
    }

    public List<StructItemOption> br(int n2) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int n3 = ii.getReqLevel(this.getItemId()) / 10;
        n3 = n3 >= 20 ? 19 : n3;
        List<List<StructItemOption>> linkedList = new LinkedList<>(ii.getAllPotentialInfo().values());
        int n4 = this.getState(false) + 16;
        if (n4 > 20 || n4 < 17) {
            n4 = 17;
        }
        int n5 = Math.abs(this.getPotential(3, false));
        List<StructItemOption> arrayList = new ArrayList<>(6);
        for (int i2 = 1; i2 <= n2; ++i2) {
            boolean bl2 = false;
            while (!bl2) {
                StructItemOption itemOption = linkedList.get(Randomizer.nextInt(linkedList.size())).get(n3);
                if (itemOption == null
                        || GameConstants.isAboveA(itemOption.opID)
                        || !GameConstants.optionTypeFits(itemOption.optionType, this.getItemId())
                        || !GameConstants.isBlockedPotential(this, itemOption.opID, false, ItemConstants.PotentialConstants.PotentialFlag.点券光环.check(n5))
                        || !GameConstants.potentialIDFits(itemOption.opID, n4, ItemConstants.PotentialConstants.PotentialFlag.对等.check(n5) ? 1 : i2)
                        || ItemConstants.PotentialConstants.PotentialFlag.去掉无用潜能.check(n5) &&
                        (!ItemConstants.PotentialConstants.PotentialFlag.去掉无用潜能.check(n5) || ItemConstants.PotentialConstants.checkProperties(itemOption)))
                    continue;
                arrayList.add(itemOption);
                bl2 = true;
            }
        }
        return arrayList;
    }

    public void c(int defaultRate, int flag, int lockSlot) {
//        boolean bl2;
//        boolean bl3;
//        int n5;
//        block12 : {
//            int n6 = 1;
//            if (b.c.abn) {
//                n6 *= 2;
//            }
//            bl3 = this.getPotential(3, bl2 = f.a.a.acC.check(n3)) > 0;
//            int n7 = n5 = Randomizer.nextInt(1000) < defaultRate * n6 ? 1 : 0;
//            if (f.a.a.acw.check(n3) && n5 == 0) {
//                int n8 = n5 = Randomizer.nextInt(1000) < (defaultRate + 200) * n6 ? -1 : 0;
//            }
//            if (f.a.a.acB.check(n3)) {
//                n3 -= Randomizer.nextInt(10) <= 5 ? f.a.a.acB.getValue() : 0;
//            }
//            if (this.getState(bl2) + n5 >= 17) {
//                if (this.getState(bl2) + n5 <= (!f.a.a.acv.check(n3) ? (!f.a.a.acu.check(n3) ? (!f.a.a.act.check(n3) ? 17 : 18) : 19) : 20)) break block12;
//            }
//            n5 = 0;
//        }
//        this.setState((byte)(this.getState(bl2) + n5 - 16), bl2);
//        if (n4 != 0 && n4 <= 3) {
//            this.setPotential(- n4 * 100000 + this.getPotential(n4, bl2), 1, bl2);
//        } else {
//            this.setPotential(- this.getState(bl2), 1, bl2);
//        }
//        if (f.a.a.acx.check(n3)) {
//            this.setPotential(ai.nextInt(10) <= 2 ? - this.getState(bl2) : 0, 2, bl2);
//        } else if (bl3) {
//            this.setPotential(- this.getState(bl2), 2, bl2);
//        } else {
//            this.setPotential(0, 2, bl2);
//        }
//        this.setPotential(- n3, 3, bl2);
//        if (f.a.a.acy.check(n3)) {
//            this.setFlag((short)(this.getFlag() | f.DL.getValue()));
//        }
//        this.f(bl2);


        int n4 = 1;
        final boolean check = ItemConstants.PotentialConstants.PotentialFlag.附加潜能.check(flag);
        final boolean b = this.getPotential(3, check) > 0;
        int n5 = (Randomizer.nextInt(1000) < defaultRate * n4) ? 1 : 0;
        if (ItemConstants.PotentialConstants.PotentialFlag.等级下降.check(flag) && n5 == 0) {
            n5 = ((Randomizer.nextInt(1000) < (defaultRate + 200) * n4) ? -1 : 0);
        }
        if (ItemConstants.PotentialConstants.PotentialFlag.前两条相同.check(flag)) {
            flag -= ((Randomizer.nextInt(10) <= 5) ? ItemConstants.PotentialConstants.PotentialFlag.前两条相同.getValue() : 0);
        }
        if (this.getState(check) + n5 < 17 || this.getState(check) + n5 > (ItemConstants.PotentialConstants.PotentialFlag.SS级.check(flag) ? 20 : (ItemConstants.PotentialConstants.PotentialFlag.S级.check(flag) ? 19 : (ItemConstants.PotentialConstants.PotentialFlag.A级.check(flag) ? 18 : 17)))) {
            n5 = 0;
        }
        this.setState((byte) (this.getState(check) + n5 - 16), check);
        if (lockSlot != 0 && lockSlot <= 3) {
            this.setPotential(-(lockSlot * 100000 + this.getPotential(lockSlot, check)), 1, check);
        } else {
            this.setPotential(-this.getState(check), 1, check);
        }
        if (ItemConstants.PotentialConstants.PotentialFlag.调整潜能条数.check(flag)) {
            this.setPotential((Randomizer.nextInt(10) <= 2) ? (-this.getState(check)) : 0, 2, check);
        } else if (b) {
            this.setPotential(-this.getState(check), 2, check);
        } else {
            this.setPotential(0, 2, check);
        }
        this.setPotential(-flag, 3, check);
        if (ItemConstants.PotentialConstants.PotentialFlag.洗后无法交易.check(flag)) {
            this.setFlag((short) (this.getFlag() | ItemFlag.不可交易.getValue()));
        }
        this.initState(check);
    }

    /*
     * 装备技能
     */
    public int getIncSkill() {
        return incSkill;
    }

    public void setIncSkill(int inc) {
        this.incSkill = inc;
    }

    /*
     * 装备魅力经验
     */
    public short getCharmEXP() {
        return charmExp;
    }

    public void setCharmEXP(short s) {
        this.charmExp = s;
    }

    /*
     * 装备大乱斗攻击力
     */
    public short getPVPDamage() {
        return pvpDamage;
    }

    public void setPVPDamage(short p) {
        this.pvpDamage = p;
    }

    /*
     * 戒指
     */
    public MapleRing getRing() {
        if (!ItemConstants.isEffectRing(getItemId()) || getUniqueId() <= 0) {
            return null;
        }
        if (ring == null) {
            ring = MapleRing.loadFromDb(getUniqueId(), getPosition() < 0);
        }
        return ring;
    }

    public void setRing(MapleRing ring) {
        this.ring = ring;
    }

    /*
     * 安卓
     */
    public MapleAndroid getAndroid() {
        if (getItemId() / 10000 != 166 || getUniqueId() <= 0) {
            return null;
        }
        if (android == null) {
            android = MapleAndroid.loadFromDb(getItemId(), getUniqueId());
        }
        return android;
    }

    public void setAndroid(MapleAndroid ring) {
        this.android = ring;
    }

    /*
     * 星级提示次数
     */
    public int getStateMsg() {
        return statemsg;
    }

    public void setStateMsg(int en) {
        if (en >= 3) {
            this.statemsg = 3;
        } else if (en < 0) {
            this.statemsg = 0;
        } else {
            this.statemsg = en;
        }
    }

    /*
     * 装备插槽 可以镶嵌宝石
     * V.102新增功能
     * 0x01 = 你可以在这件物品上镶入星岩。
     * 0x03 = 你可以在这件物品上镶入星岩。 有个镶嵌的孔 未镶嵌
     * 0x13 = 有1个插孔 已经镶嵌东西
     */
    public short getSocketState() {
        short flag = 0;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        boolean isSocketItem = !ii.isCash(getItemId()); //ii.isActivatedSocketItem(getItem());
        if (isSocketItem) {
            flag |= SocketFlag.可以镶嵌.getValue();
            if (socket1 == -1 && isSocketItem) {
                setSocket1(0);
            }
            if (socket1 != -1) {
                flag |= SocketFlag.已打孔01.getValue();
            }
            if (socket2 != -1) {
                flag |= SocketFlag.已打孔02.getValue();
            }
            if (socket3 != -1) {
                flag |= SocketFlag.已打孔03.getValue();
            }
            if (socket1 > 0) {
                flag |= SocketFlag.已镶嵌01.getValue();
            }
            if (socket2 > 0) {
                flag |= SocketFlag.已镶嵌02.getValue();
            }
            if (socket3 > 0) {
                flag |= SocketFlag.已镶嵌03.getValue();
            }
        }
        return flag;
    }

    public int getSocket1() {
        return socket1;
    }

    public void setSocket1(int socket) {
        this.socket1 = socket;
    }

    public int getSocket2() {
        return socket2;
    }

    public void setSocket2(int socket) {
        this.socket2 = socket;
    }

    public int getSocket3() {
        return socket3;
    }

    public void setSocket3(int socket) {
        this.socket3 = socket;
    }

    /*
     * 装备合成后的外观
     */
    public int getItemSkin() {
        return itemSkin;
    }

    public void setItemSkin(int id) {
        this.itemSkin = id;
    }

    /*
     * 新增的装备属性
     */
    public short getEnhanctBuff() {
        return enhanctBuff;
    }

    public void setEnhanctBuff(short enhanctBuff) {
        if (enhanctBuff < 0) {
            enhanctBuff = 0;
        }
        this.enhanctBuff = enhanctBuff;
    }

    public short getReqLevel() {
        return reqLevel;
    }

    public void setReqLevel(short reqLevel) {
        if (reqLevel < 0) {
            reqLevel = 0;
        }
        this.reqLevel = reqLevel;
    }

    public short getYggdrasilWisdom() {
        return yggdrasilWisdom;
    }

    public void setYggdrasilWisdom(short yggdrasilWisdom) {
        if (yggdrasilWisdom < 0) {
            yggdrasilWisdom = 0;
        }
        this.yggdrasilWisdom = yggdrasilWisdom;
    }

    public boolean getFinalStrike() {
        return finalStrike;
    }

    public void setFinalStrike(boolean finalStrike) {
        this.finalStrike = finalStrike;
    }

    public short getBossDamage() {
        return (short) (bossDamage + nirvanaFlame.getNbossDamage());
    }

    public void setBossDamage(short bossDamage) {
        if (bossDamage < 0) {
            bossDamage = 0;
        }
        this.bossDamage = bossDamage;
    }

    public short getIgnorePDR() {
        return (short) (ignorePDR + nirvanaFlame.getNignorePDR());
    }

    public void setIgnorePDR(short ignorePDR) {
        if (ignorePDR < 0) {
            ignorePDR = 0;
        }
        this.ignorePDR = ignorePDR;
    }

    /*
     * 新增的装备特殊属性
     */
    public short getTotalDamage() {
        return (short) (totalDamage + nirvanaFlame.getNtotalDamage());
    }

    public void setTotalDamage(short totalDamage) {
        if (totalDamage < 0) {
            totalDamage = 0;
        }
        this.totalDamage = totalDamage;
    }

    public short getAllStat() {
        return (short) (allStat + nirvanaFlame.getNallStat());
    }

    public void setAllStat(short allStat) {
        if (allStat < 0) {
            allStat = 0;
        }
        this.allStat = allStat;
    }

    public short getKarmaCount() {
        return karmaCount;
    }

    public void setKarmaCount(short karmaCount) {
        this.karmaCount = karmaCount;
    }

    public Map<EquipStats, Long> getStatsTest() {
        return statsTest;
    }

    /*
     * 装备的总体状态
     */
    public int getEquipFlag() {
        int flag = 0;
        if (getUpgradeSlots() > 0) {
            flag |= EquipStats.可升级次数.getValue(); //可升级次数
        }
        if (getLevel() > 0) {
            flag |= EquipStats.已升级次数.getValue(); //已升级次数
        }
        if (getStr() > 0) {
            flag |= EquipStats.力量.getValue(); //力量
        }
        if (getDex() > 0) {
            flag |= EquipStats.敏捷.getValue(); //敏捷
        }
        if (getInt() > 0) {
            flag |= EquipStats.智力.getValue(); //智力
        }
        if (getLuk() > 0) {
            flag |= EquipStats.运气.getValue(); //运气
        }
        if (getHp() > 0) {
            flag |= EquipStats.Hp.getValue(); //Hp
        }
        if (getMp() > 0) {
            flag |= EquipStats.Mp.getValue(); //Mp
        }
        if (getWatk() > 0) {
            flag |= EquipStats.物攻.getValue(); //物理攻击
        }
        if (getMatk() > 0) {
            flag |= EquipStats.魔攻.getValue(); //魔法攻击
        }
        if (getWdef() > 0) {
            flag |= EquipStats.物防.getValue(); //物理防御
        }
//        if (getMdef() > 0) {
//            flag |= EquipStats.魔防.getValue(); //魔法防御
//        }
//        if (getAcc() > 0) {
//            flag |= EquipStats.命中.getValue(); //命中率
//        }
//        if (getAvoid() > 0) {
//            flag |= EquipStats.回避.getValue(); //回避率
//        }
        if (getHands() > 0) {
            flag |= EquipStats.手技.getValue(); //手技
        }
        if (getSpeed() > 0) {
            flag |= EquipStats.速度.getValue(); //移动速度
        }
        if (getJump() > 0) {
            flag |= EquipStats.跳跃.getValue(); //跳跃力
        }
        if (getFlag() > 0) {
            flag |= EquipStats.状态.getValue(); //道具状态
        }
        if (getIncSkill() > 0) {
            flag |= EquipStats.技能.getValue(); //是否拥有技能
        }
        if (isSealedEquip()) {
            if (getSealedLevel() > 0) {
                flag |= EquipStats.道具等级.getValue();
            }
            if (getSealedExp() > 0) {
                flag |= EquipStats.道具经验.getValue(); //道具经验
            }
        } else {
            if (getEquipLevel() > 0) {
                flag |= EquipStats.道具等级.getValue(); //道具等级
            }
            if (getExpPercentage() > 0) {
                flag |= EquipStats.道具经验.getValue(); //道具经验
            }
        }
        if (getDurability() > 0) {
            flag |= EquipStats.耐久度.getValue(); //耐久度
        }
        if (getViciousHammer() > 0) {
            flag |= EquipStats.金锤子.getValue(); //金锤子
        }
        if (getPVPDamage() > 0) {
            flag |= EquipStats.大乱斗攻击力.getValue(); //大乱斗攻击力
        }
        if (getEnhanctBuff() > 0) {
            flag |= EquipStats.ENHANCT_BUFF.getValue(); //强化效果
        }
        if (getiIncReq() > 0) {
            flag |= EquipStats.REQUIRED_LEVEL.getValue(); //穿戴装备的等级要求提高
        }
        if (getYggdrasilWisdom() > 0) {
            flag |= EquipStats.YGGDRASIL_WISDOM.getValue();
        }
        if (getFinalStrike()) {
            flag |= EquipStats.FINAL_STRIKE.getValue(); //最终一击卷轴成功
        }
        if (getBossDamage() > 0) {
            flag |= EquipStats.BOSS伤害.getValue(); //BOSS伤害增加百分比
        }
        if (getIgnorePDR() > 0) {
            flag |= EquipStats.无视防御.getValue(); //无视怪物增加百分比
        }
        return flag;
    }

    /*
     * 装备的特殊状态
     */
    public int getEquipSpecialFlag() {
        int flag = 0;
        if (getTotalDamage() > 0) {
            flag |= EquipSpecialStat.总伤害.getValue(); //装备总伤害百分比增加
        }
        if (getAllStat() > 0) {
            flag |= EquipSpecialStat.全属性.getValue(); //装备所有属性百分比增加
        }
        flag |= EquipSpecialStat.剪刀次数.getValue(); //可以使用剪刀多少次 默认必须
//        flag |= 0x08;
        if (getSealedExp() > 0) {
            flag |= EquipSpecialStat.漩涡经验.getValue();
        }
        flag |= EquipSpecialStat.星级.getValue();
        return flag;
    }

    /*
     * 武器装备的攻击突破上限附加数字
     * 5000000 = 500万
     * 上限设置为 21亿
     */
    public int getLimitBreak() {
        return Math.min(limitBreak, 2100000000);
    }

    public void setLimitBreak(int lb) {
        this.limitBreak = lb;
    }

    public void setLockPotential(int slot, short id) {
        lockSlot = slot;
        lockId = id;
    }

    public int getLockSlot() {
        return lockSlot;
    }

    public int getLockId() {
        return lockId;
    }

    public boolean isSealedEquip() {
        return GameConstants.isSealedEquip(getItemId());
    }

    public byte getSealedLevel() {
        return sealedLevel;
    }

    public void setSealedLevel(byte level) {
        sealedLevel = level;
    }

    public void gainSealedExp(long gain) {
        sealedExp += gain;
    }

    public long getSealedExp() {
        return sealedExp;
    }

    public void setSealedExp(long exp) {
        sealedExp = exp;
    }

    public short getSoulName() {
        return soulname;
    }

    public void setSoulName(short soulname) {
        this.soulname = soulname;
    }

    public short getSoulEnchanter() {
        return soulenchanter;
    }

    public void setSoulEnchanter(short soulenchanter) {
        this.soulenchanter = soulenchanter;
    }

    public short getSoulPotential() {
        return soulpotential;
    }

    public void setSoulPotential(short soulpotential) {
        this.soulpotential = soulpotential;
    }

    public int getSoulSkill() {
        return soulSkill;
    }

    public void setSoulSkill(int skillid) {
        this.soulSkill = skillid;
    }

    public int getiIncReq() {
        return iIncReq;
    }

    public NirvanaFlame getNirvanaFlame() {
        return nirvanaFlame;
    }

    public void setNirvanaFlame(NirvanaFlame nirvanaFlame) {
        this.nirvanaFlame = nirvanaFlame;
    }

    public long getFire() {
        return fire;
    }

    public void setFire(long fire) {
        this.fire = fire;
    }

    public int getNeedStar() {
        return Math.max(this.getEnhance() <= 18 ? 100 - this.getEnhance() * 5 : 10 - (this.getEnhance() - 18) * 2, 1);
    }

    public boolean getStartCurse() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        boolean bl2 = ii.isSuperiorEquip(getItemId());
        int n2 = StarForceConfig.START_CURSE;
        return this.getEnhance() > n2 || bl2 && getEnhance() > n2 / 3;
    }

    public boolean getStartDown() {
        int n2 = StarForceConfig.START_DOWN;
        return n2 > 0 && this.getEnhance() >= n2 && this.getEnhance() % 5 != 0;
    }

    public boolean getBonusTime() {
        int bonus_time = StarForceConfig.BONUS_TIME;
        return bonus_time > 0 && getBonus() >= bonus_time;
    }

    public int getBonus() {
        return bonus;
    }

    public void setBonus(int bonus) {
        this.bonus = bonus;
    }

    public short getARC() {
        return ARC;
    }

    public void setARC(short ARC) {
        this.ARC = ARC;
    }

    public int getARCExp() {
        return ARCExp;
    }

    public void setARCExp(int ARCExp) {
        this.ARCExp = ARCExp;
    }

    public short getARCLevel() {
        return ARCLevel;
    }

    public void setARCLevel(short ARCLevel) {
        this.ARCLevel = ARCLevel;
    }

    public enum ScrollResult {

        失败, 成功, 消失
    }
}
