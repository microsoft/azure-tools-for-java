package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

public enum WebAppState {
    RUNNING,
    STOPPED;

    private static WebAppState[] copyOfValues = values();

    public static WebAppState fromString(final String name) {
        for (final WebAppState value : copyOfValues) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
