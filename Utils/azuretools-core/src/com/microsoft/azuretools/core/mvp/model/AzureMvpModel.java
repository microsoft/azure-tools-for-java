package com.microsoft.azuretools.core.mvp.model;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AzureMvpModel {
    private static final class SingletonHolder {
        private static final AzureMvpModel INSTANCE = new AzureMvpModel();
    }

    public static AzureMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // TODO: retire later, DO NOT cache for sake of consistency
    Map<String, List<ResourceGroup>> subscriptionIdToResourceGroupMap;

    private AzureMvpModel() {
        subscriptionIdToResourceGroupMap = new ConcurrentHashMap<>();
        try {
            updateSubscriptionToResourceGroupMaps();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get subscription by subscriptionId.
     *
     * @param sid Subscription Id
     * @return Instance of Subscription
     */
    public Subscription getSubscriptionById(String sid) {
        Subscription ret = null;
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            Map<String, Subscription> subscriptionIdToSubscriptionMap = azureManager.getSubscriptionManager()
                    .getSubscriptionIdToSubscriptionMap();
            ret = subscriptionIdToSubscriptionMap.get(sid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Get list of selected Subscriptions.
     *
     * @return List of Subscription instances
     */
    public List<Subscription> getSelectedSubscriptions() {
        List<Subscription> ret = new ArrayList<>();
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            Map<String, SubscriptionDetail> sidToSubDetailMap = azureManager.getSubscriptionManager()
                    .getSubscriptionToSubscriptionDetailsMap();
            Map<String, Subscription> subscriptionIdToSubscriptionMap = azureManager.getSubscriptionManager()
                    .getSubscriptionIdToSubscriptionMap();
            for (SubscriptionDetail subDetail : sidToSubDetailMap.values()) {
                if (subDetail.isSelected()) {
                    ret.add(subscriptionIdToSubscriptionMap.get(subDetail.getSubscriptionId()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * List Resource Group by Subscription ID.
     *
     * @param sid   subscription Id
     * @param force flag indicating whether force to fetch most updated data from server
     * @return List of ResourceGroup instances
     */
    // TODO: Force to fetch most updated later, DO NOT cache for sake of consistency
    public List<ResourceGroup> getResouceGroupsBySubscriptionId(String sid, boolean force) {
        List<ResourceGroup> ret = new ArrayList<>();
        if (!force && subscriptionIdToResourceGroupMap.containsKey(sid)) {
            return subscriptionIdToResourceGroupMap.get(sid);
        }
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            Azure azure = azureManager.getAzure(sid);
            ret.addAll(azure.resourceGroups().list());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    // it caches sid->RG map
    // TODO: Remove later, DO NOT cache for sake of consistency
    private synchronized void updateSubscriptionToResourceGroupMaps() throws IOException {
        clearCache();
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Map<String, Subscription> subscriptionIdToSubscriptionMap = azureManager.getSubscriptionManager()
                .getSubscriptionIdToSubscriptionMap();
        if (subscriptionIdToSubscriptionMap != null && subscriptionIdToSubscriptionMap.size() > 0) {
            for (Subscription sub : subscriptionIdToSubscriptionMap.values()) {
                String sid = sub.subscriptionId();
                Azure azure = azureManager.getAzure(sid);
                subscriptionIdToResourceGroupMap.put(sid, azure.resourceGroups().list());
            }
        }
    }

    // TODO: Remove later, DO NOT cache for sake of consistency
    private void clearCache() {
        subscriptionIdToResourceGroupMap.clear();
    }
}
