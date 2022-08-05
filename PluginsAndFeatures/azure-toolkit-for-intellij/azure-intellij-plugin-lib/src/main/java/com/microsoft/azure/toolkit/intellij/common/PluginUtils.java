package com.microsoft.azure.toolkit.intellij.common;

public class PluginUtils {
    private static final String IDEA_PREFIX = "idea";
    private static final String IDEA_CE_PREFIX = "Idea";
    private static final String COMMUNITY_PREFIX = IDEA_CE_PREFIX;
    private static final String PLATFORM_PREFIX_KEY = "idea.platform.prefix";

    public static String getPlatformPrefix() {
        return getPlatformPrefix(IDEA_PREFIX);
    }

    public static String getPlatformPrefix(String defaultPrefix) {
        return System.getProperty(PLATFORM_PREFIX_KEY, defaultPrefix);
    }

    public static boolean isIdeaUltimate() {
        return is(IDEA_PREFIX);
    }

    public static boolean isIdeaCommunity() {
        return is(COMMUNITY_PREFIX);
    }

    private static boolean is(String idePrefix) {
        return idePrefix.equals(getPlatformPrefix());
    }
}
