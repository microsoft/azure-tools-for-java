package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

public enum DeploymentSlotState {
    RUNNING,
    STOPPED;

    private static DeploymentSlotState[] copyOfValues = values();

    public static DeploymentSlotState fromString(final String name) {
        for(final DeploymentSlotState value: copyOfValues) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
