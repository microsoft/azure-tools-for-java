/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vlashch on 1/6/17.
 */
public class AzureModel {
    private Map<SubscriptionDetail, List<ResourceGroup>> subscriptionToResourceGroupMap = null;
    private Map<SubscriptionDetail, List<Location>> subscriptionToLocationMap = null;
    private Map<ResourceGroup, List<WebApp>> resourceGroupToWebAppMap = null;
    private Map<ResourceGroup, List<AppServicePlan>> resourceGroupToAppServicePlanMap = null;
    private Map<String, Subscription> sidToSubscriptionMap = null;


    private static AzureModel instance = null;
    private AzureModel() {}

    public static AzureModel getInstance() {
        if (instance == null) {
            instance = new AzureModel();
        }
        return instance;
    }

    // == sidToSubscriptionMap
    public Map<String, Subscription> getSidToSubscriptionMap() {
        return sidToSubscriptionMap;
    }

    public Map<String, Subscription> createSidToSubscriptionMap() {
        return new HashMap<String, Subscription>();
    }

    public void setSidToSubscriptionMap(Map<String, Subscription> sidToSubscriptionMap) {
        this.sidToSubscriptionMap = sidToSubscriptionMap;
    }

    // == subscriptionToLocationMap

    public Map<SubscriptionDetail, List<Location>> getSubscriptionToLocationMap() {
        return subscriptionToLocationMap;
    }

    public Map<SubscriptionDetail, List<Location>> createSubscriptionToRegionMap() {
        return new ConcurrentHashMap<>();
    }

    public void setSubscriptionToLocationMap(Map<SubscriptionDetail, List<Location>> subscriptionToLocationMap) {
        this.subscriptionToLocationMap = subscriptionToLocationMap;
    }

    // == subscriptionToResourceGroupMap

    public synchronized Map<SubscriptionDetail, List<ResourceGroup>> getSubscriptionToResourceGroupMap() {
        return subscriptionToResourceGroupMap;
    }

    public synchronized void setSubscriptionToResourceGroupMap(Map<SubscriptionDetail, List<ResourceGroup>> subsriptoionToResourceGroupMap) {
        this.subscriptionToResourceGroupMap = subsriptoionToResourceGroupMap;
    }

    public synchronized Map<SubscriptionDetail, List<ResourceGroup>> createSubscriptionToResourceGroupMap() {
        return new ConcurrentHashMap<>();
    }

    // == resourceGroupToWebAppMap

    public synchronized Map<ResourceGroup, List<WebApp>> getResourceGroupToWebAppMap() {
        return resourceGroupToWebAppMap;
    }

    public synchronized void setResourceGroupToWebAppMap(Map<ResourceGroup, List<WebApp>> resourceGroupToWebAppMap) {
        this.resourceGroupToWebAppMap = resourceGroupToWebAppMap;
    }

    public synchronized Map<ResourceGroup, List<WebApp>> createResourceGroupToWebAppMap() {
        return new ConcurrentHashMap<ResourceGroup, List<WebApp>>();
    }

    // == resourceGroupToAppServicePlanMap

    public synchronized Map<ResourceGroup, List<AppServicePlan>> getResourceGroupToAppServicePlanMap() {
        return resourceGroupToAppServicePlanMap;
    }

    public synchronized void setResourceGroupToAppServicePlanMap(Map<ResourceGroup, List<AppServicePlan>> resourceGroupToAppServicePlanMap) {
        this.resourceGroupToAppServicePlanMap = resourceGroupToAppServicePlanMap;
    }

    public synchronized Map<ResourceGroup, List<AppServicePlan>> createResourceGroupToAppServicePlanMap() {
        return new ConcurrentHashMap<ResourceGroup, List<AppServicePlan>>();
    }

}
