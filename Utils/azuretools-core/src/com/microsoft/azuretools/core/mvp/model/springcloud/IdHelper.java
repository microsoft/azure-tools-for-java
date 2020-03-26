package com.microsoft.azuretools.core.mvp.model.springcloud;

import org.apache.commons.lang3.ArrayUtils;

public class IdHelper {
    public static String getSubscriptionId(String serviceId) {
        final String[] attributes = serviceId.split("/");
        return attributes[ArrayUtils.indexOf(attributes, "subscriptions") + 1];
    }

    public static String getResourceGroup(String serviceId) {
        final String[] attributes = serviceId.split("/");
        return attributes[ArrayUtils.indexOf(attributes, "resourceGroups") + 1];
    }

    public static String getClusterName(String serviceId) {
        final String[] attributes = serviceId.split("/");
        return attributes[ArrayUtils.indexOf(attributes, "Spring") + 1];
    }

    public static String getAppName(String serviceId) {
        final String[] attributes = serviceId.split("/");
        return attributes[ArrayUtils.indexOf(attributes, "apps") + 1];
    }
}
