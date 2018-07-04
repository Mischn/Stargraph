package net.stargraph;

import java.util.UUID;

public class IDGenerator {
    public static String generateUUID() {
        return String.valueOf(UUID.randomUUID()).replaceAll("-", "");
    }
}
