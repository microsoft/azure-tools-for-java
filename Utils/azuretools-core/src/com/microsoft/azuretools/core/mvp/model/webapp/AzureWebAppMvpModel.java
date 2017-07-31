package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AzureWebAppMvpModel {
    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }

    public static AzureWebAppMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private AzureWebAppMvpModel() {
        subscriptionIdToWebAppsOnLinuxMap = new ConcurrentHashMap<>();
        subscriptionIdToWebAppsMap = new ConcurrentHashMap<>();
    }

    private Map<String, List<ResourceEx<WebApp>>> subscriptionIdToWebAppsMap;
    private Map<String, List<ResourceEx<SiteInner>>> subscriptionIdToWebAppsOnLinuxMap;

    public WebApp getWebAppById(String sid, String id) throws IOException {
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

    public List<WebApp> listWebAppsBySubscriptionId(String sid, boolean force){
        return null;
    }

    public List<ResourceEx<SiteInner>> listWebAppsOnLinuxBySubscriptionId(String sid, boolean force) {
        List<ResourceEx<SiteInner>> wal = new ArrayList<ResourceEx<SiteInner>>();
        if(!force && subscriptionIdToWebAppsOnLinuxMap.containsKey(sid)) {
            return subscriptionIdToWebAppsOnLinuxMap.get(sid);
        }
        try {
            Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(sid);
            List<ResourceGroup> rgl = AzureMvpModel.getInstance().getResouceGroupsBySubscriptionId(sid, false);

            for (ResourceGroup rg : rgl) {
                for (SiteInner si : azure.webApps().inner().listByResourceGroup(rg.name())) {
                    if (si.kind().equals("app,linux")) {
                        wal.add(new ResourceEx<SiteInner>(si, sid));
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
