package client;

import tools.HexTool;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class LoginCrypto {

    protected final static int extralength = 6;
    private final static String[] Alphabet = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private final static String[] Number = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private final static Random rand = new Random();

    public static String Generate_13DigitAsiasoftPassport() {
        StringBuilder sb = new StringBuilder();
        sb.append(Alphabet[rand.nextInt(Alphabet.length)]); // First Letter
        for (int i = 0; i < 11; i++) {
            sb.append(Number[rand.nextInt(Number.length)]); // 11 Numbers
        }
        sb.append(Alphabet[rand.nextInt(Alphabet.length)]); // Last Letter
        return sb.toString();
    }

    private static String toSimpleHexString(byte[] bytes) {
        return HexTool.toString(bytes).replace(" ", "").toLowerCase();
    }

    private static String hashWithDigest(String in, String digest) {
        try {
            MessageDigest Digester = MessageDigest.getInstance(digest);
            Digester.update(in.getBytes("UTF-8"), 0, in.length());
            byte[] sha1Hash = Digester.digest();
            return toSimpleHexString(sha1Hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Hashing the password failed", ex);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }

    public static String hexSha1(String in) {
        return hashWithDigest(in, "SHA-1");
    }

    public static String hexSha512(String in) {
        return hashWithDigest(in, "SHA-512");
    }

    public static boolean checkSha1Hash(String hash, String password) {
        return hash.equals(hexSha1(password));
    }

    public static boolean checkSaltedSha512Hash(String hash, String password, String salt) {
        return hash.equals(makeSaltedSha512Hash(password, salt));
    }

    public static String makeSaltedSha512Hash(String password, String salt) {
        return hexSha512(password + salt);
    }

    public static String makeSalt() {
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        return toSimpleHexString(salt);
    }

    public static String rand_s(String in) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extralength; i++) {
            sb.append(rand.nextBoolean() ? Alphabet[rand.nextInt(Alphabet.length)] : Number[rand.nextInt(Number.length)]);
        }
        return sb.toString() + in;
    }

    public static String rand_r(String in) {
        return in.substring(extralength, extralength + 128);
    }
}
