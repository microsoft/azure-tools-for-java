package com.microsoft.azuretools.core.mvp.model.springcloud;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.Services;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.rest.LogLevel;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import rx.Observable;

/**
 * AzureSpringCloudMvpModel
 */
public class AzureSpringCloudMvpModel {

    private static AppPlatformManager springManager;
    private static String subscriptionId = "685ba005-af8d-4b04-8f16-a7bf38b2eb5a";
    private AzureSpringCloudMvpModel() {
        if (springManager == null) {
            synchronized (springManager) {
                if (springManager == null) {
                    try {
                        init();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static String getResourceGroup(ServiceResourceInner cluster) {
        final String[] attributes = cluster.id().split("/");
        return attributes[ArrayUtils.indexOf(attributes, "resourceGroups") + 1];
    }

    
    public static AzureSpringCloudMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public List<ServiceResourceInner> listSpringCloudBySubscriptionId(String sid) throws IOException {
        PagedList<ServiceResourceInner> res = springManager.inner().services().list();
        res.loadAll();
        return res;
    }

    public static AppResourceInner getAppByName(String resourceGroupName, String clusterName, String appName) {
        return springManager.apps().inner().get(resourceGroupName, clusterName, appName);
    }
    
    public static DeploymentResourceInner getDeployment(String resourceGroupName, String clusterName, String appName, String deploymentName) {
        return springManager.deployments().inner().get(resourceGroupName, clusterName, appName, deploymentName);
    }

    public static DeploymentResourceInner getActiveDeploymentForApp(String resourceGroupName, String clusterName, String appName) {
        String deploymentName = springManager.apps().inner().get(resourceGroupName, clusterName, appName).properties().activeDeploymentName();
        return springManager.deployments().inner().get(resourceGroupName, clusterName, appName, deploymentName);
    }

    public static Observable<DeploymentResource> listClusterAllDeployments(String resourceGroupName, String clusterName) {
        return springManager.deployments().listClusterAllDeploymentsAsync(resourceGroupName, clusterName);
    }

    public static rx.Completable start(String resourceGroup, String clusterName, String appName, String deploymentName) {
        return springManager.deployments().startAsync(resourceGroup, clusterName, appName, deploymentName);
    }

    public static rx.Completable stop(String resourceGroup, String clusterName, String appName, String deploymentName) {
        return springManager.deployments().stopAsync(resourceGroup, clusterName, appName, deploymentName);
    }

    public static rx.Completable delete(String resourceGroup, String clusterName, String appName) {
        return springManager.apps().deleteAsync(resourceGroup, clusterName, appName);
    }

    public List<AppResourceInner> listAppsByAzureSpringCloudService(ServiceResourceInner service) {
        PagedList<AppResourceInner> res = springManager.inner().apps().list(getResourceGroup(service), service.name());
        res.loadAll();
        return res;
    }

    public List<AppResourceInner> listAppsByServiceName(String resourceGroup, String serviceName) {
        PagedList<AppResourceInner> res = springManager.inner().apps().list(resourceGroup, serviceName);
        res.loadAll();
        return res;
    }

    public static void init() throws IOException {
        AzureTokenCredentials tokens = AzureCliCredentials.create();
        subscriptionId  = StringUtils.isNotEmpty(tokens.defaultSubscriptionId()) ? tokens.defaultSubscriptionId() : subscriptionId;
        springManager = AppPlatformManager.configure().withLogLevel(LogLevel.BODY_AND_HEADERS)
                .withUserAgent(com.microsoft.azuretools.authmanage.CommonSettings.USER_AGENT)
                .authenticate(tokens, subscriptionId);
    }

    private static final class SingletonHolder {
        private static final AzureSpringCloudMvpModel INSTANCE = new AzureSpringCloudMvpModel();
    }
}