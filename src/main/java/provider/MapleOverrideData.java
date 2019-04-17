package provider;

import com.fasterxml.jackson.core.type.TypeReference;
import tools.JsonUtil;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * WZ重载数据的操作类
 *
 * @author 免责声明：本模拟器源代码下载自ragezone.com，仅用于技术研究学习，无任何商业行为。
 */
public class MapleOverrideData {

    private static MapleOverrideData instance = new MapleOverrideData();
    public String key = "4652352523425";
    private KeyGenerator kgen;
    private Cipher cipher;
    private SecretKeySpec secretKeySpec;
    private Map<Integer, Map<String, String>> overridedata;

    MapleOverrideData() {
        try {
            kgen = KeyGenerator.getInstance("AES");
            kgen.init(128, new SecureRandom(key.getBytes()));
            cipher = Cipher.getInstance("AES");
            secretKeySpec = new SecretKeySpec(kgen.generateKey().getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public static MapleOverrideData getInstance() {
        return instance;
    }

    /**
     * 将二进制转换成16进制
     *
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     *
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 加密文件内容
     *
     * @param json
     * @return
     */
    public String encrypt(byte[] json) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return parseByte2HexStr(cipher.doFinal(json));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密文件内容
     *
     * @param data
     * @return
     */
    public String decrypt(byte[] data) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            return new String(cipher.doFinal(data));
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读取重载的数据
     */
    public void init() {
        File file = new File("config\\overridedata.dat");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    System.out.println("找不到 overridedata.json 文件，读取失败。");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (file.length() != 0) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[inputStream.available()];
                    inputStream.read(buffer);
                    overridedata = JsonUtil.getMapperInstance().readValue(decrypt(parseHexStr2Byte(new String(buffer, "UTF-8"))), new TypeReference<Map<Integer, Map<String, String>>>() {
                    });
                }
            } else {
                overridedata = new HashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重载的数据保存到文件
     */
    public void save() {
        File file = new File("config\\overridedata.dat");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    System.out.println("找不到 overridedata.json 文件，保存失败。");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                    bufferedOutputStream.write(encrypt(JsonUtil.getMapperInstance().writeValueAsBytes(overridedata)).getBytes());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Map<String, String>> getOverridedata() {
        return overridedata;
    }

    public String getOverrideValue(int skillid, String name) {
        if (overridedata.containsKey(skillid) && overridedata.get(skillid).containsKey(name)) {
            return overridedata.get(skillid).get(name);
        }
        return "";
    }
}
