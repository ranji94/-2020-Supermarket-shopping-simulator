package utils;

import java.util.UUID;

public class UUIDUtils {
    public static String shortId() {
        return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}
