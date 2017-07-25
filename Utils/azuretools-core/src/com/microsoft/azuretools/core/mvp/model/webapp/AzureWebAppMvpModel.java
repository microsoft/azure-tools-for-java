package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzureWebAppMvpModel {
    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }

    public static AzureWebAppMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private AzureWebAppMvpModel() {
        subscriptionIdToWebAppsOnLinuxMap = new HashMap<>();
        subscriptionIdToWebAppsMap = new HashMap<>();
    }

    private Map<String, List<WebApp>> subscriptionIdToWebAppsMap;
    private Map<String, List<SiteInner>> subscriptionIdToWebAppsOnLinuxMap;

    public synchronized WebApp getWebAppById(String sid, String id) throws IOException {
        // TODO
        Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(sid);
        return azure.webApps().getById(id);
    }

    public void createWebApp(){
        // TODO
    }
    public void deployWebApp(){
        // TODO
    }
    public void createWebAppOnLinux(){
        // TODO
    }
    public void updateWebAppOnLinux(){
        // TODO
    }

    public synchronized List<WebApp> listWebAppsBySubscriptionId(String sid, boolean force){
        return null;
    }

    public synchronized List<SiteInner> listWebAppsOnLinuxBySubscriptionId(String sid, boolean force) {
        List<SiteInner> wal = new ArrayList<SiteInner>();
        if(!force && subscriptionIdToWebAppsOnLinuxMap.containsKey(sid)) {
            return subscriptionIdToWebAppsOnLinuxMap.get(sid);
        }
        try {
            Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(sid);
            List<ResourceGroup> rgl = AzureMvpModel.getInstance().getResouceGroupsBySubscriptionId(sid, false);

            for (ResourceGroup rg : rgl) {
                for (SiteInner si : azure.webApps().inner().listByResourceGroup(rg.name())) {
                    if (si.kind().equals("app,linux")) {
                        wal.add(si);
                    }
                }
            }
            if (subscriptionIdToWebAppsOnLinuxMap.containsKey(sid)){
                subscriptionIdToWebAppsOnLinuxMap.remove(sid);
            }
            subscriptionIdToWebAppsOnLinuxMap.put(sid, wal);
        } catch (IOException e){
            e.printStackTrace();
        }
        return wal;
    }
}
