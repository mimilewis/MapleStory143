package tools.packet;

import handling.opcode.SendPacketOpcode;
import server.MapleSignin;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Map;

import static tools.packet.SigninPacket.SIGNIN_TYPE.签到窗口;
import static tools.packet.SigninPacket.SIGNIN_TYPE.领取奖励;

public class SigninPacket {

    public static byte[] showSigninUI() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SIGIN_INFO.getValue());

        mplew.write(0);
        mplew.writeInt(签到窗口.ordinal());
        mplew.writeLong(PacketHelper.getTime(-2));
        mplew.writeLong(PacketHelper.getTime(-1));
        mplew.writeInt(MapleSignin.getInstance().getSiginRewards().size());
        mplew.writeInt(2);
        mplew.writeInt(MapleSignin.getInstance().getSiginRewards().size());

        for (MapleSignin.SiginRewardInfo rewardInfo : MapleSignin.getInstance().getSiginRewards()) {
            mplew.writeInt(rewardInfo.getRank());
            mplew.writeInt(rewardInfo.getItemId());
            mplew.writeInt(rewardInfo.getQuantity());
            if (rewardInfo.getExpiredate() > 0) {
                mplew.writeInt(1);
                mplew.writeInt(rewardInfo.getExpiredate());
            } else {
                mplew.writeLong(0);
            }
            mplew.writeInt(rewardInfo.getIsCash());
            mplew.write(0);
        }

        mplew.writeInt(MapleSignin.MINLEVEL);
        mplew.writeInt(MapleSignin.getInstance().getUnknownMap().size());
        for (Map.Entry<Integer, Integer> integerEntry : MapleSignin.getInstance().getUnknownMap().entrySet()) {
            mplew.writeInt(integerEntry.getKey());
            mplew.writeInt(integerEntry.getValue());
        }

        return mplew.getPacket();
    }

    public static byte[] getSigninReward(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SIGIN_INFO.getValue());

        mplew.write(1);
        mplew.writeInt(领取奖励.ordinal());
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    enum SIGNIN_TYPE {
        UNKNOWN,
        签到窗口,
        领取奖励,;
    }
}
