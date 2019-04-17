/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.opcode;

import handling.WritableIntValueHolder;

/**
 * @author PlayDK
 */
public enum InteractionOpcode implements WritableIntValueHolder {
    设置物品(0x00),
    设置物品_001(0x01),
    设置物品_002(0x02),
    设置物品_003(0x03),
    设置金币(0x04),
    设置金币_005(0x05),
    设置金币_006(0x06),
    设置金币_007(0x07),
    确认交易(0x08),
    确认交易_009(0x09),
    确认交易_00A(0x0A),
    确认交易_00B(0x0B),
    创建(0x10),
    FISH_NOTICE(0x12),
    访问(0x13),
    房间(0x14),
    交易邀请(0x15),
    拒绝邀请(0x16),
    聊天(0x18),
    聊天事件(0x19),
    打开(0x1A),
    退出(0x1C),
    雇佣商店_维护(0x1E),
    添加物品(0x1F),
    添加物品_0020(0x20),
    添加物品_0021(0x21),
    添加物品_0022(0x22),
    BUY_ITEM_STORE(-2),
    雇佣商店_购买道具(0x23),
    雇佣商店_购买道具0024(0x24),
    雇佣商店_购买道具0025(0x25),
    雇佣商店_购买道具0026(0x26),
    雇佣商店_求购道具(0x27),
    MERCH_ITEM_STOR_REQUEST(0x29),
    MERCH_ITEM_STOR(0x2A),
    MERCH_ITEM_STOR_2(0x2B),
    SHOW_MERCH_ITEM_STOR(0x2C),
    移除物品(0x2F),
    雇佣商店_开启(0x30),
    雇佣商店_整理(0x31),
    雇佣商店_关闭(0x32),
    雇佣商店_关闭完成(0x33),
    管理员修改雇佣商店名称(0x36),
    雇佣商店_查看访问名单(0x37),
    雇佣商店_查看黑名单(0x38),
    雇佣商店_添加黑名单(0x39),
    雇佣商店_移除黑名单(0x3A),
    雇佣商店_修改商店名称(0x3B),
    玩家商店_添加道具(0x3D),
    玩家商店_添加道具003E(0x3E),
    玩家商店_添加道具003F(0x3F),
    玩家商店_添加道具0040(0x40),
    玩家商店_购买道具(0x41),
    玩家商店_购买道具0042(0x42),
    玩家商店_购买道具0043(0x43),
    玩家商店_购买道具0044(0x44),
    雇佣商店_错误提示(0x46),
    雇佣商店_更新信息(0x4C),
    玩家商店_移除玩家(0x4F),
    雇佣商店_维护开启(0x51),
    请求平局(0x56),
    应答平局(0x57),
    GIVE_UP(-2),
    请求弃权(0x58),
    应答弃权(0x59),

    退出游戏(0x5C),
    取消退出(0x5D),
    准备开始(0x5E),
    准备就绪(0x5F),

    踢出玩家(0x60),
    开始游戏(0x61),
    GAME_RESULT(0x62),
    SKIP(0x63),
    移动棋子(0x64),
    选择卡片(0x68),;

//    static {
//        reloadValues();
//    }

    private int code = -2;

    InteractionOpcode(int code) {
        this.code = code;
    }

    public static InteractionOpcode getByAction(int packetId) {
        for (InteractionOpcode interaction : InteractionOpcode.values()) {
            if (interaction.getValue() == packetId) {
                return interaction;
            }
        }
        return null;
    }

//    public static Properties getDefaultProperties() throws IOException {
//        Properties props = new Properties();
//        FileInputStream fileInputStream = new FileInputStream("properties/Interaction.properties");
//        BufferedReader buff = new BufferedReader(new InputStreamReader(fileInputStream, "GBK"));
//        props.load(buff);
//        fileInputStream.close();
//        buff.close();
//        return props;
//    }
//
//    public static void reloadValues() {
//        try {
//            File file = new File("properties/Interaction.properties");
//            if (file.exists()) {
//                ExternalCodeTableGetter.populateValues(getDefaultProperties(), values());
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("加载 Interaction.properties 文件出现错误", e);
//        }
//    }

    @Override
    public short getValue() {
        return (short) code;
    }

    @Override
    public void setValue(short code) {
        this.code = code;
    }
}
