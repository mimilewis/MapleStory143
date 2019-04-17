/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package configs;


import tools.config.Property;

/**
 * @author admin
 */
public class MovementConfig {

    /**
     * 伤害反弹移动效果
     * 默认: "-1" 使用,分割
     * 封包解析:
     * short xpos = lea.readShort();
     * short ypos = lea.readShort();
     * short unk = lea.readShort();
     * short fh = lea.readShort();
     * byte newstate = lea.readByte();
     * short duration = lea.readShort();
     */
    @Property(key = "Bounce.Movement", defaultValue = "-1")
    public static String bounceMovement;
    /**
     * 正常移动移动效果
     * 默认: "0x00,0x08" 使用,分割
     * 封包解析:
     * short xpos = lea.readShort();
     * short ypos = lea.readShort();
     * short xwobble = lea.readShort();
     * short ywobble = lea.readShort();
     * short fh = lea.readShort();
     * short xoffset = lea.readShort();
     * short yoffset = lea.readShort();
     * byte newstate = lea.readByte();
     * short duration = lea.readShort();
     */
    @Property(key = "Absolute.Life.Movement", defaultValue = "0x00,0x08")
    public static String absoluteLifeMovement;
    /**
     * 类似疾驰移动效果
     * 默认: "0x01,0x02,0x17" 使用,分割
     * 封包解析:
     * short xmod = lea.readShort();
     * short ymod = lea.readShort();
     * byte newstate = lea.readByte();
     * short duration = lea.readShort();
     */
    @Property(key = "Relative.Life.Movement", defaultValue = "0x01,0x02,0x17")
    public static String relativeLifeMovement;
    /**
     * 传送移动的效果
     * 默认: "0x04,0x05" 使用,分割
     * 封包解析:
     * short xpos = lea.readShort();
     * short ypos = lea.readShort();
     * short xwobble = lea.readShort();
     * short ywobble = lea.readShort();
     * byte newstate = lea.readByte();
     */
    @Property(key = "Teleport.Movement", defaultValue = "0x04,0x05")
    public static String teleportMovement;
    /**
     * 类似更换装备或者获得技能出现的移动效果
     * 默认: "0x0C" 使用,分割
     * 封包解析:
     * lea.readByte()
     */
    @Property(key = "Change.Equip.Movement", defaultValue = "0x0C")
    public static String changeEquipMovement;
    /**
     * 类似角色坐在椅子出现的移动效果
     * 默认: "0x03" 使用,分割
     * 封包解析:
     * short xpos = lea.readShort();
     * short ypos = lea.readShort();
     * short fh = lea.readShort();
     * byte newstate = lea.readByte();
     * short duration = lea.readShort();
     */
    @Property(key = "Chair.Movement", defaultValue = "0x03")
    public static String chairMovement;
    /**
     * 类似战神攻击出现的移动效果
     * 默认: "0x13" 使用,分割
     * 封包解析:
     * byte newstate = lea.readByte();
     * short duration = lea.readShort();
     */
    @Property(key = "Aran.Movement", defaultValue = "0x13")
    public static String aranMovement;
    /**
     * 角色向下跳出现的移动效果
     * 默认: "0x0F" 使用,分割
     * 封包解析:
     * short xpos = lea.readShort();
     * short ypos = lea.readShort();
     * short xwobble = lea.readShort();
     * short ywobble = lea.readShort();
     * short unk = lea.readShort();
     * short fh = lea.readShort();
     * short xoffset = lea.readShort();
     * short yoffset = lea.readShort();
     * byte newstate = lea.readByte();
     * short duration = lea.readShort();
     */
    @Property(key = "Jump.Down.Movement", defaultValue = "0x0F")
    public static String jumpDownMovement;
    /**
     * 将怪物从另外1个地方拉到另外1个地方出现的移动效果
     * 默认: "0x37,0x42" 使用,分割
     * 封包解析:
     * short xpos = lea.readShort();
     * short ypos = lea.readShort();
     * short xwobble = lea.readShort();
     * short ywobble = lea.readShort();
     * short fh = lea.readShort();
     * byte newstate = lea.readByte();
     * short duration = lea.readShort();
     */
    @Property(key = "Static.Life.Movement", defaultValue = "0x37,0x42")
    public static String staticLifeMovement;
    /**
     * 未知
     */
    @Property(key = "Unknown.Life.Movement", defaultValue = "0x16")
    public static String unknownLifeMovement;
}
