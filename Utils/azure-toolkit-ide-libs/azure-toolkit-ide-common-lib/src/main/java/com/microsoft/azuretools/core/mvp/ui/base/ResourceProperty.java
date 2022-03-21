/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.base;

public class ResourceProperty {
    private String name;

    private String type;

    private String groupName;

    private String regionName;

    private String subscriptionId;

    /**
     * Basic class to store azure resource information.
     * @param name Resource name
     * @param type Resource type
     * @param groupName Group name
     * @param regionName Region name
     * @param subscriptionId Subscription Id
     */
    public ResourceProperty(String name, String type, String groupName, String regionName, String subscriptionId) {
        this.name = name;
        this.type = type;
        this.groupName = groupName;
        this.regionName = regionName;
        this.subscriptionId = subscriptionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

}
