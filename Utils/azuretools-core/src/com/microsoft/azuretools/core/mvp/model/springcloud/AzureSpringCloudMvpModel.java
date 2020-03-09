/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.core.mvp.model.springcloud;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.AzureCliCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.rest.LogLevel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import rx.Completable;
import rx.Observable;

import java.io.IOException;
import java.util.List;

/**
 * AzureSpringCloudMvpModel
 */
public class AzureSpringCloudMvpModel {
    private static AppPlatformManager springManager;
    //TODO: remove hard coded sid
    private static String subscriptionId = "685ba005-af8d-4b04-8f16-a7bf38b2eb5a";

    private AzureSpringCloudMvpModel() {
    }

    public static AzureSpringCloudMvpModel getInstance() {
        if (springManager == null) {
            synchronized (AzureSpringCloudMvpModel.class) {
                if (springManager == null) {
                    try {
                        init();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return SingletonHolder.INSTANCE;
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

    public List<ServiceResourceInner> listAllSpringCloudClusters() throws IOException {
        // TODO: load all clusters according to sid
        return listAllSpringCloudClustersBySubscription(null);
    }

    public List<ServiceResourceInner> listAllSpringCloudClustersBySubscription(String sid) throws IOException {
        PagedList<ServiceResourceInner> res = springManager.inner().services().list();
        res.loadAll();
        return res;
    }

    public static DeploymentResourceInner getAppDeployment(String sid, String resourceGroupName, String clusterName, String appName, String deploymentName) {
        return springManager.deployments().inner().get(resourceGroupName, clusterName, appName, deploymentName);
    }

    public static AppResourceInner getAppByName(String sid, String resourceGroupName, String clusterName, String appName) {
        return springManager.apps().inner().get(resourceGroupName, clusterName, appName);
    }

    public static Completable startApp(String appId, String deploymentName) {
        return springManager.deployments().startAsync(getResourceGroup(appId), getClusterName(appId), getAppName(appId), deploymentName);
    }

    public static Completable stopApp(String appId, String deploymentName) {
        return springManager.deployments().stopAsync(getResourceGroup(appId), getClusterName(appId), getAppName(appId), deploymentName);
    }

    public static Completable restartApp(String appId, String deploymentName) {
        return springManager.deployments().restartAsync(getResourceGroup(appId), getClusterName(appId), getAppName(appId), deploymentName);
    }

    public static Completable deleteApp(String appId) {
        return springManager.apps().deleteAsync(getResourceGroup(appId), getClusterName(appId), getAppName(appId));
    }

    public List<AppResourceInner> listAppsByClusterId(String id) {
        PagedList<AppResourceInner> res = springManager.inner().apps().list(getResourceGroup(id), getClusterName(id));
        res.loadAll();
        return res;
    }

    public static Observable<DeploymentResource> listAllDeploymentsByClusterId(String id) {
        return springManager.deployments().listClusterAllDeploymentsAsync(getResourceGroup(id), getClusterName(id));
    }

    public static void init() throws IOException {
        AzureTokenCredentials tokens = AzureCliCredentials.create();
        subscriptionId = StringUtils.isNotEmpty(tokens.defaultSubscriptionId()) ? tokens.defaultSubscriptionId() : subscriptionId;
        springManager = AppPlatformManager.configure().withLogLevel(LogLevel.BODY_AND_HEADERS)
                .withUserAgent(com.microsoft.azuretools.authmanage.CommonSettings.USER_AGENT)
                .authenticate(tokens, subscriptionId);
    }

    private static final class SingletonHolder {
        private static final AzureSpringCloudMvpModel INSTANCE = new AzureSpringCloudMvpModel();
    }
}
