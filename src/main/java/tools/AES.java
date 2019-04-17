package tools;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * AES 加解密服務的類別。
 *
 * @author andychao
 */
public class AES {
    private Cipher oCipher = null;
    private SecretKeySpec specKey = null;
    private IvParameterSpec specIV = null;

    public AES(String key, String iv) throws Exception {
        // Create Instance.
        this.oCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        // Decode Key and IV.
        byte[] byKeys = key.getBytes("UTF-8");
        byte[] byIVs = iv.getBytes("UTF-8");
        // Restore Key and IV.
        this.specKey = new SecretKeySpec(byKeys, "AES");
        this.specIV = new IvParameterSpec(byIVs);
    }

    public String Encrypt(String originalValue) throws Exception {
        // Declare variables.
        int nLength = 0;
        String szResult = null;
        // Initialize Cipher of AES.
        this.oCipher.init(Cipher.ENCRYPT_MODE, this.specKey, this.specIV);
        // Convert original value to bytes.
        byte[] bySource = originalValue.getBytes("UTF-8");
        // Get output size for ciphers.
        byte[] byCiphers = new byte[this.oCipher.getOutputSize(bySource.length)];
        // Encrypt data.
        nLength = this.oCipher.update(bySource, 0, bySource.length, byCiphers, 0);
        nLength += this.oCipher.doFinal(byCiphers, nLength);
        // Convert to Base64 string.
        szResult = DatatypeConverter.printBase64Binary(byCiphers);
        // Return encrypted data.
        return szResult;
    }

    public String Decrypt(String encryptValue) throws Exception {
        // Declare variables.
        int nLength;
        String szResult = null;
        // Decrypt string as bytes.
        byte[] byCiphers = DatatypeConverter.parseBase64Binary(encryptValue);
        // Initialize Cipher of AES.
        this.oCipher.init(Cipher.DECRYPT_MODE, this.specKey, this.specIV);
        // Get output size for ciphers.
        byte[] bySource = new byte[this.oCipher.getOutputSize(byCiphers.length)];
        // Decrypt data.
        nLength = this.oCipher.update(byCiphers, 0, byCiphers.length, bySource, 0);
        nLength += this.oCipher.doFinal(bySource, nLength);
        // Convert bytes to string.
        szResult = new String(bySource, 0, nLength, "UTF-8");
        // Return Decrypted data.
        return szResult;
    }
}
