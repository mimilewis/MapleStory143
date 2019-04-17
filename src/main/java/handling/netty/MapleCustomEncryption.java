package handling.netty;

import tools.BitTools;

/**
 * Provides MapleStory's custom encryption routines.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 211
 */
public class MapleCustomEncryption {

    /**
     * Encrypts
     * <code>data</code> with Maple's encryption routines.
     *
     * @param data The data to encrypt.
     * @return The encrypted data.
     */
    public static byte[] encryptData(byte data[]) {

        for (int j = 0; j < 6; j++) {
            byte remember = 0;
            byte dataLength = (byte) (data.length & 0xFF);
            // printByteArray(data);
            if (j % 2 == 0) {
                for (int i = 0; i < data.length; i++) {
                    byte cur = data[i];
                    cur = BitTools.rollLeft(cur, 3);
                    cur += dataLength;
                    cur ^= remember;
                    remember = cur;
                    cur = BitTools.rollRight(cur, dataLength & 0xFF);
                    cur = ((byte) ((~cur) & 0xFF));
                    cur += 0x48;
                    dataLength--;
                    data[i] = cur;
                }
            } else {
                for (int i = data.length - 1; i >= 0; i--) {
                    byte cur = data[i];
                    cur = BitTools.rollLeft(cur, 4);
                    cur += dataLength;
                    cur ^= remember;
                    remember = cur;
                    cur ^= 0x13;
                    cur = BitTools.rollRight(cur, 3);
                    dataLength--;
                    data[i] = cur;
                }
            }
            //System.out.println("enc after iteration " + j + ": " + HexTool.toString(data) + " al: " + al);
        }
        return data;
    }

    /**
     * Decrypts
     * <code>data</code> with Maple's encryption routines.
     *
     * @param data The data to decrypt.
     * @return The decrypted data.
     */
    public static byte[] decryptData(byte data[]) {
        for (int j = 1; j <= 6; j++) {
            byte remember = 0;
            byte dataLength = (byte) (data.length & 0xFF);
            byte nextRemember = 0;

            if (j % 2 == 0) {
                for (int i = 0; i < data.length; i++) {
                    byte cur = data[i];
                    cur -= 0x48;
                    cur = ((byte) ((~cur) & 0xFF));
                    cur = BitTools.rollLeft(cur, dataLength & 0xFF);
                    nextRemember = cur;
                    cur ^= remember;
                    remember = nextRemember;
                    cur -= dataLength;
                    cur = BitTools.rollRight(cur, 3);
                    data[i] = cur;
                    dataLength--;
                }
            } else {
                for (int i = data.length - 1; i >= 0; i--) {
                    byte cur = data[i];
                    cur = BitTools.rollLeft(cur, 3);
                    cur ^= 0x13;
                    nextRemember = cur;
                    cur ^= remember;
                    remember = nextRemember;
                    cur -= dataLength;
                    cur = BitTools.rollRight(cur, 4);
                    data[i] = cur;
                    dataLength--;
                }
            }
            //System.out.println("dec after iteration " + j + ": " + HexTool.toString(data));
        }
        return data;
    }
}
