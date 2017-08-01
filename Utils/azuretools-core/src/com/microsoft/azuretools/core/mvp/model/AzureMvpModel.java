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

public class AzureMvpModel {
    private static final class SingletonHolder {
        private static final AzureMvpModel INSTANCE = new AzureMvpModel();
    }

    public static AzureMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private AzureMvpModel() {
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
            if (subscriptionIdToSubscriptionMap != null) {
                ret = subscriptionIdToSubscriptionMap.get(sid);
            }
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
                    .getSubscriptionIdToSubscriptionDetailsMap();
            Map<String, Subscription> sidToSubscriptionMap = azureManager.getSubscriptionManager()
                    .getSubscriptionIdToSubscriptionMap();
            if (sidToSubDetailMap != null && sidToSubscriptionMap != null) {
                for (SubscriptionDetail subDetail : sidToSubDetailMap.values()) {
                    if (subDetail.isSelected()) {
                        ret.add(sidToSubscriptionMap.get(subDetail.getSubscriptionId()));
                    }
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
     * @param sid subscription Id
     * @return List of ResourceGroup instances
     */
    public List<ResourceGroup> getResourceGroupsBySubscriptionId(String sid) {
        List<ResourceGroup> ret = new ArrayList<>();
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            Azure azure = azureManager.getAzure(sid);
            ret.addAll(azure.resourceGroups().list());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
