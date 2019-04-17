package client.inventory;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import server.MapleItemInformationProvider;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class Item implements Comparable<Item>, Serializable {


    private static final long serialVersionUID = -1063003648432448717L;
    private int id; //道具的ID
    private short position; //道具的位置
    private short quantity; //道具的数量
    private short flag; //道具的状态
    private long expiration = -1; //道具的时间
    private long inventoryitemid = -1; //道具在SQL中的ID
    private FamiliarCard familiarCard = null;
    private MaplePet pet = null; //道具是否是宠物
    private int uniqueid, sn; //商城道具的唯一ID
    private int equipOnlyId = -1; //装备道具的唯一ID
    private String owner = ""; //道具的所有者
    private String GameMaster_log = ""; //道具的日志信息
    private String giftFrom = ""; //道具礼物获得信息
    private int familiarid = 0;
    private short espos;

    public Item() {

    }

    public Item(int id, short position, short quantity, short flag, int uniqueid, short espos) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.flag = flag;
        this.uniqueid = uniqueid;
        this.equipOnlyId = -1;
    }

    public Item(int id, short position, short quantity, short flag) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.flag = flag;
        this.uniqueid = -1;
        this.equipOnlyId = -1;
    }

    public Item(int id, byte position, short quantity) {
        super();
        this.id = id;
        this.position = position;
        this.quantity = quantity;
        this.uniqueid = -1;
        this.equipOnlyId = -1;
    }

    /*
     * 回购道具需要此功能
     */
    public Item copyWithQuantity(short quantitys) {
        Item ret = new Item(id, position, quantitys, flag, uniqueid, espos);
        ret.pet = pet;
        ret.owner = owner;
        ret.sn = sn;
        ret.GameMaster_log = GameMaster_log;
        ret.expiration = expiration;
        ret.giftFrom = giftFrom;
        ret.equipOnlyId = equipOnlyId;
        return ret;
    }

    public Item copy() {
        Item ret = new Item(id, position, quantity, flag, uniqueid, espos);
        ret.pet = pet;
        ret.owner = owner;
        ret.sn = sn;
        ret.GameMaster_log = GameMaster_log;
        ret.expiration = expiration;
        ret.giftFrom = giftFrom;
        ret.equipOnlyId = equipOnlyId;
        ret.familiarCard = familiarCard;
        ret.familiarid = familiarid;
        return ret;
    }

    public int getItemId() {
        return id;
    }

    public void setItemId(int id) {
        this.id = id;
    }

    public short getPosition() {
        return position;
    }

    public void setPosition(short position) {
        this.position = position;
        if (pet != null) {
            pet.setInventoryPosition(position);
        }
    }

    public short getFlag() {
        return flag;
    }

    public void setFlag(short flag) {
        this.flag = flag;
    }

    public short getQuantity() {
        return quantity;
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    public byte getType() {
        return 2; // An Item
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void removeFlag(short flag) {
        this.flag &= ~flag;
    }

    public void addFlag(short flag) {
        this.flag |= flag;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expire) {
        this.expiration = expire;
    }

    public String getGMLog() {
        return GameMaster_log;
    }

    public void setGMLog(String GameMaster_log) {
        this.GameMaster_log = GameMaster_log;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public void setUniqueId(int ui) {
        this.uniqueid = ui;
    }

    public int getSN() {
        return sn;
    }

    public void setSN(int sn) {
        this.sn = sn;
    }

    public int getFamiliarid() {
        return familiarid;
    }

    public void setFamiliarid(int familiarid) {
        this.familiarid = familiarid;
    }

    public FamiliarCard getFamiliarCard() {
        return familiarCard;
    }

    public void setFamiliarCard(FamiliarCard familiarCard) {
        this.familiarCard = familiarCard;
    }

    /*
             * 有这个ID的道具必须是装备道具 且不是点装 且这个ID小于等于0 且这个道具为装备道具类型
             */
    public boolean hasSetOnlyId() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        return !(uniqueid > 0 || ii.isCash(id) || id / 1000000 != 1) && equipOnlyId <= 0;
    }

    public int getEquipOnlyId() {
        return equipOnlyId;
    }

    public void setEquipOnlyId(int OnlyId) {
        this.equipOnlyId = OnlyId;
    }

    public long getInventoryId() {
        return inventoryitemid;
    }

    public void setInventoryId(long ui) {
        this.inventoryitemid = ui;
    }

    public MaplePet getPet() {
        return pet;
    }

    public void setPet(MaplePet pet) {
        this.pet = pet;
        if (pet != null) {
            this.uniqueid = pet.getUniqueId();
        }
    }

    public String getGiftFrom() {
        return giftFrom;
    }

    public void setGiftFrom(String gf) {
        this.giftFrom = gf;
    }

    public short getESPos() {
        return espos;
    }

    public void setESPos(short espos) {
        this.espos = espos;
    }

    /*
         * 道具是否为技能皮肤
         */
    public boolean isSkillSkin() {
        return id / 1000 == 1603;
    }

    @Override
    public int compareTo(Item other) {
        if (Math.abs(position) < Math.abs(other.getPosition())) {
            return -1;
        } else if (Math.abs(position) == Math.abs(other.getPosition())) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Item)) {
            return false;
        }
        Item ite = (Item) obj;
        return uniqueid == ite.getUniqueId() && id == ite.getItemId() && quantity == ite.getQuantity() && Math.abs(position) == Math.abs(ite.getPosition());
    }

    @Override
    public String toString() {
        return "物品: " + id + " 数量: " + quantity;
    }
}
