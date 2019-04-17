package handling.channel.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.maps.AnimatedMapleMapObject;
import server.movement.*;
import tools.HexTool;
import tools.data.input.LittleEndianAccessor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MovementParse {

    /**
     * Logger for this class.
     */
    public static final Logger log = LogManager.getLogger(MovementParse.class.getName());

    /*
     * 1 = 玩家移动
     * 2 = 怪物移动
     * 3 = 宠物移动
     * 4 = 召唤兽移动
     * 5 = 龙神龙龙移动
     * 6 = 玩家攻击怪物移动
     * 7 = 小白移动
     */
    public static List<LifeMovementFragment> parseMovement(LittleEndianAccessor lea, int kind) {
        List<LifeMovementFragment> res = new ArrayList<>();
        byte numCommands = lea.readByte(); //循环次数
        String packet = lea.toString(true);
        for (byte i = 0; i < numCommands; i++) {
            byte command = lea.readByte(); //移动类型
            switch (command) {
                case -1: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short unk = lea.readShort();
                    short fh = lea.readShort(); // 是什么高度
                    byte newstate = lea.readByte(); // 状态姿势
                    short duration = lea.readShort();
                    BounceMovement bm = new BounceMovement(command, new Point(xpos, ypos), duration, newstate);
                    bm.setFH(fh);
                    bm.setUnk(unk);
                    res.add(bm);
                    break;
                }
                case 0x00:
                case 0x08:
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                case 0x38:
                case 0x39:
                case 0x3E:
                case 0x41:
                case 0x42: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    short fh = lea.readShort();
                    short xoffset = lea.readShort();
                    short yoffset = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    alm.setNewFH(fh);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    alm.setOffset(new Point(xoffset, yoffset));
                    res.add(alm);
                    break;
                }
                case 0x01:
                case 0x02:
                case 0x18: {
                    short xmod = lea.readShort();
                    short ymod = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xmod, ymod), duration, newstate);
                    res.add(rlm);
                    break;
                }
                case 0x04:
                case 0x05: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), duration, newstate);
                    tm.setFh(fh);
                    res.add(tm);
                    break;
                }
                case 0x0C: {
                    res.add(new ChangeEquipSpecialAwesome(command, lea.readByte()));
                    break;
                }
                case 0x03:
                case 0x06:
                case 0x09:
                case 0x0A:
                case 0x0B:
                case 0x0D:
                case 0x0E:
                case 0x16:
                case 0x19:
                case 0x1A:
                case 0x1B:
                case 0x32:
                case 0x33:
                case 0x34:
                case 0x35:
                case 0x4B:
                case 0x4E:
                case 0x54: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
                    cm.setNewFH(fh);
                    res.add(cm);
                    break;
                }
                case 0x1C:
                case 0x1D:
                case 0x1E:
                case 0x1F:
                case 0x20:
                case 0x21:
                case 0x22:
                case 0x23:
                case 0x24:
                case 0x25:
                case 0x26:
                case 0x27:
                case 0x28:
                case 0x29:
                case 0x2A:
                case 0x2B:
                case 0x2C:
                case 0x2D:
                case 0x2E:
                case 0x2F:
                case 0x30:
                case 0x31:
                case 0x36:
                case 0x3A:
                case 0x3B:
                case 0x43:
                case 0x45:
                case 0x46:
                case 0x49:
                case 0x4A:
                case 0x4C:
                case 0x4D:
                case 0x4F: {
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    AranMovement am = new AranMovement(command, new Point(0, 0), duration, newstate);
                    res.add(am);
                    break;
                }
                case 0x0F: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    short unk = lea.readShort();
                    short fh = lea.readShort();
                    short xoffset = lea.readShort();
                    short yoffset = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
                    jdm.setUnk(unk);
                    jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    jdm.setOffset(new Point(xoffset, yoffset));
                    jdm.setFH(fh);
                    res.add(jdm);
                    break;
                }
                case 0x37:
                case 0x3D:
                case 0x40:
                case 0x74: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    short fh = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    StaticLifeMovement slm = new StaticLifeMovement(command, new Point(xpos, ypos), duration, newstate);
                    slm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    slm.setNewFH(fh);
                    res.add(slm);
                    break;
                }
                case 0x17: {
                    short xpos = lea.readShort();
                    short ypos = lea.readShort();
                    short xwobble = lea.readShort();
                    short ywobble = lea.readShort();
                    byte newstate = lea.readByte();
                    short duration = lea.readShort();
                    lea.skip(1);
                    UnknownMovement um = new UnknownMovement(command, new Point(xpos, ypos), duration, newstate);
                    um.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(um);
                    break;
                }
                default: {
                    log.error(getKindName(kind) + "未知移动封包 剩余次数: " + (numCommands - res.size()) + " 移动类型: 0x" + HexTool.toString(command) + ", 封包: " + packet);
                    return null;
                }
            }
        }
        double skip = lea.readByteAsInt();
        skip = Math.ceil(skip / 2);
        lea.skip((int) skip);
        if (numCommands != res.size()) {
            log.error(getKindName(kind) + " 循环次数[" + numCommands + "]和实际上获取的循环次数[" + res.size() + "]不符" + packet);
            return null;
        }
        return res;
    }

    public static void updatePosition(List<LifeMovementFragment> movement, AnimatedMapleMapObject target, int yoffset) {
        if (movement == null) {
            return;
        }
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    Point position = move.getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }

    public static String getKindName(int kind) {
        String moveMsg;
        switch (kind) {
            case 1:
                moveMsg = "玩家";
                break;
            case 2:
                moveMsg = "怪物";
                break;
            case 3:
                moveMsg = "宠物";
                break;
            case 4:
                moveMsg = "召唤兽";
                break;
            case 5:
                moveMsg = "龙龙";
                break;
            case 6:
                moveMsg = "怪怪";
                break;
            case 7:
                moveMsg = "小白";
                break;
            case 8:
                moveMsg = "小白人型";
                break;
            case 9:
                moveMsg = "安卓";
                break;
            default:
                moveMsg = "未知kind";
                break;
        }
        return moveMsg;
    }
}