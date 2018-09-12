package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azuretools.adauth.StringUtils;

public enum WebAppState {
    RUNNING,
    STOPPED;

    public static WebAppState fromString(final String state) {
        if (StringUtils.isNullOrEmpty(state)) {
            return null;
        }
        switch (state.toUpperCase()) {
            case "RUNNING":
                return RUNNING;
            case "STOPPED":
                return STOPPED;
            default:
                return null;
        }
    }
}
