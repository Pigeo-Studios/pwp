package org.acivllas;

import java.security.SecureRandom;

public class HWIDUtils {
    private static final String CHARS = "0123456789abcdef";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String getHWID() {
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(16)));
        }
        return sb.toString();
    }
}
