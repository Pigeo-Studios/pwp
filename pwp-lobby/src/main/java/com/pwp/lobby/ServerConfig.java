package com.pwp.lobby;

public class ServerConfig {

    private static boolean autoStartEnabled = true;
    private static int maxMatches = 2;

    public static boolean isAutoStartEnabled() { return autoStartEnabled; }
    public static void setAutoStartEnabled(boolean v) { autoStartEnabled = v; }

    public static int getMaxMatches() { return maxMatches; }
    public static void setMaxMatches(int n) { maxMatches = Math.max(1, n); }
}
