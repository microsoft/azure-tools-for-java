package com.microsoft.azuretools.core.mvp.model;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
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

    Map<String, Subscription> subscriptionIdToSubscriptionMap;
    Map<String, SubscriptionDetail> subscriptionIdToSubscriptionDetailMap;
    Map<String, List<ResourceGroup>> subscriptionIdToResourceGroupMap;

    private AzureMvpModel(){
        subscriptionIdToSubscriptionMap = new ConcurrentHashMap<>();
        subscriptionIdToSubscriptionDetailMap = new ConcurrentHashMap<>();
        subscriptionIdToResourceGroupMap = new ConcurrentHashMap<>();
        try {
            updateSubscriptionMaps();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Subscription getSubscriptionById(String sid){
        return subscriptionIdToSubscriptionMap.get(sid);
    }
    
    public List<Subscription> getSelectedSubscriptions(){
        List<Subscription> ret = new ArrayList<>();
        for(SubscriptionDetail subDetail : subscriptionIdToSubscriptionDetailMap.values()){
            if(subDetail.isSelected()){
               ret.add(subscriptionIdToSubscriptionMap.get(subDetail.getSubscriptionId()));
            }
        }
        return ret;
    }

    public List<ResourceGroup> getResouceGroupsBySubscriptionId(String sid, boolean force) {
        if(force){
            try{
                AzureManager azureManager  = AuthMethodManager.getInstance().getAzureManager();
                Azure azure = azureManager.getAzure(sid);
                return azure.resourceGroups().list();
            }
            catch (IOException e){
                e.printStackTrace();
                return new ArrayList<ResourceGroup>();
            }
        }
        else{
            return subscriptionIdToResourceGroupMap.get(sid);
        }
    }


    public synchronized void updateSubscriptionMaps() throws IOException {
        clearCache();
        AzureManager azureManager  = AuthMethodManager.getInstance().getAzureManager();

        for(Subscription sub : azureManager.getSubscriptions()){
            String sid = sub.subscriptionId();
            subscriptionIdToSubscriptionMap.put(sid, sub);
            Azure azure = azureManager.getAzure(sid);
            subscriptionIdToResourceGroupMap.put(sid, azure.resourceGroups().list());
        }

        SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
        for(SubscriptionDetail sd : subscriptionManager.getSubscriptionDetails()){
            subscriptionIdToSubscriptionDetailMap.put(sd.getSubscriptionId(), sd);
        }

    }

    private void clearCache() {
        subscriptionIdToResourceGroupMap.clear();
        subscriptionIdToSubscriptionDetailMap.clear();
        subscriptionIdToSubscriptionMap.clear();
    }
}
