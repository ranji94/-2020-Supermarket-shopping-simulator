package utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

    public static int randInt(int min, int max) {
        return min + ThreadLocalRandom.current().nextInt(max + 1 - min);
    }

    public static boolean getRandomBooleanWithProbability(float probability) {
        Random random = new Random();
        return random.nextFloat() < probability;
    }

    public static double randDouble(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble(max + 1 - min);
    }
}
