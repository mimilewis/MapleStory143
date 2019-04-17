package tools;

import java.util.Random;

public class Randomizer {

    private static final Random rand = new Random();

    public static int nextInt() {
        return rand.nextInt();
    }

    public static int nextInt(int arg0) {
        return rand.nextInt(arg0);
    }

    public static void nextBytes(byte[] bytes) {
        rand.nextBytes(bytes);
    }

    public static boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public static double nextDouble() {
        return rand.nextDouble();
    }

    public static float nextFloat() {
        return rand.nextFloat();
    }

    public static long nextLong() {
        return rand.nextLong();
    }

    public static int rand(int lbound, int ubound) {
        return nextInt(ubound - lbound + 1) + lbound;
    }

    public static boolean isSuccess(int rate) {
        return rate > nextInt(100);
    }
}
