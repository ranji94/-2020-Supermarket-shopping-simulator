package utils;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

    public static int randInt(int min, int max) {
        return min + ThreadLocalRandom.current().nextInt(max + 1 - min);
    }

    public static double randDouble(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble(max + 1 - min);
    }
}
