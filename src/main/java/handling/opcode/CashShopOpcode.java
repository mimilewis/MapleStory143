/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.opcode;

import handling.ExternalCodeTableGetter;
import handling.WritableIntValueHolder;

import java.io.*;
import java.util.Properties;

/**
 * @author PlayDK
 */
public enum CashShopOpcode implements WritableIntValueHolder {

    加载道具栏(0x06),
    加载礼物(0x08),
    加载购物车(0x0A),
    更新购物车(0x0C),
    购买道具(0x0E),
    商城送礼(0x17),
    错误提示(0x18),
    扩充道具栏(0x19),
    扩充仓库(0x1B),
    购买角色卡(0x2C),
    扩充项链(0x31),
    商城到背包(0x23),
    背包到商城(0x25),
    删除道具(0x27),
    道具到期(0x29),
    换购道具(0x46),
    购买礼包(0x4A),
    商城送礼包(0x4C),
    购买任务道具(0x4E),
    打开箱子(0x6A),
    抵用券兑换道具(0x93),
    领奖卡提示(-2),
    注册商城(-2),
    商城提示(-2),;

    static {
        reloadValues();
    }

    private int code = -2;

    CashShopOpcode(int code) {
        this.code = code;
    }

    public static Properties getDefaultProperties() throws IOException {
        Properties props = new Properties();
        FileInputStream fileInputStream = new FileInputStream("properties/cashops.properties");
        BufferedReader buff = new BufferedReader(new InputStreamReader(fileInputStream, "GBK"));
        props.load(buff);
        fileInputStream.close();
        buff.close();
        return props;
    }

    public static void reloadValues() {
        try {
            File file = new File("properties/cashops.properties");
            if (file.exists()) {
                ExternalCodeTableGetter.populateValues(getDefaultProperties(), values());
            }
        } catch (IOException e) {
            throw new RuntimeException("加载 cashops.properties 文件出现错误", e);
        }
    }

    @Override
    public short getValue() {
        return (short) code;
    }

    @Override
    public void setValue(short code) {
        this.code = code;
    }
}
