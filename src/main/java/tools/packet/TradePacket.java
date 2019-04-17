/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import handling.opcode.InteractionOpcode;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleTrade;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author PlayDK
 */
public class TradePacket {

    private static final Logger log = LogManager.getLogger(TradePacket.class);

    /*
     * 玩家交易邀请
     */
    public static byte[] getTradeInvite(MapleCharacter chr) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.交易邀请.getValue()); //V.110.1修改 以前0x0B
        mplew.write(0x04); //普通交易是0x04 现金交易是0x07
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeInt(0); // Trade ID

        return mplew.getPacket();
    }

    /*
     * 玩家交易设置金币
     */
    public static byte[] getTradeMesoSet(byte number, int meso) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.设置金币.getValue());
        mplew.write(number);
        mplew.writeLong(meso);

        return mplew.getPacket();
    }

    /*
     * 玩家交易放入道具
     */
    public static byte[] getTradeItemAdd(byte number, Item item) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.设置物品.getValue());
        mplew.write(number);
        mplew.write(item.getPosition());
        PacketHelper.addItemInfo(mplew, item);

        return mplew.getPacket();
    }

    /*
     * 交易开始
     * 双方角色都进入交易界面
     */
    public static byte[] getTradeStart(MapleClient c, MapleTrade trade, byte number) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.房间.getValue());
        mplew.write(0x04); //普通交易是0x04 现金交易是0x07
        mplew.write(0x02); //应该是交易的人数
        mplew.write(number);
        if (number == 1) {
            mplew.write(0);
            PacketHelper.addCharLook(mplew, trade.getPartner().getChr(), true, trade.getPartner().getChr().isZeroSecondLook());
            mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
            mplew.writeShort(trade.getPartner().getChr().getJob());
        }
        mplew.write(number);
        PacketHelper.addCharLook(mplew, c.getPlayer(), true, c.getPlayer().isZeroSecondLook());
        mplew.writeMapleAsciiString(c.getPlayer().getName());
        mplew.writeShort(c.getPlayer().getJob());
        mplew.write(0xFF);

        return mplew.getPacket();
    }

    public static byte[] getTradeConfirmation() {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.确认交易.getValue());

        return mplew.getPacket();
    }

    public static byte[] TradeMessage(byte number, byte message) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.退出.getValue());
        mplew.write(number);
        /*
         * 0x01 已经关闭了。
         * 0x02 对方中止交易。
         * 0x07 交易成功了。请再确认交易的结果。
         * 0x08 交易失败了。
         * 0x09 因部分道具有数量限制只能拥有一个交易失败了。
         * 0x0C 双方在不同的地图不能交易。
         * 0x0D 游戏文件损坏，无法交易物品。请重新安装游戏后，再重新尝试。
         */
        mplew.write(message);

        return mplew.getPacket();
    }

    /*
     * 玩家交易取消
     * 好像现在没有提示对方背包已满的什么提示 直接是交易失败
     */
    public static byte[] getTradeCancel(byte number, int message) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.退出.getValue());
        mplew.write(number);
        mplew.write(message == 0 ? 0x02 : 0x09);

        return mplew.getPacket();
    }
}
