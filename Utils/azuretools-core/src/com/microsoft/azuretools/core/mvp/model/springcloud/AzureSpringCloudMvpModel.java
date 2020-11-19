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
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.AppResourceProperties;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentInstance;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResourceProperties;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.ServiceResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.TestKeys;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.ServiceResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import rx.Completable;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper.getAppName;
import static com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper.getClusterName;
import static com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper.getResourceGroup;
import static com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper.getSubscriptionId;


public class AzureSpringCloudMvpModel {
    private static final int SPRING_LOG_STREAMING_CONNECT_TIMEOUT = 3 * 1000; // 3s
    private static final int SPRING_LOG_STREAMING_READ_TIMEOUT = 10 * 60 * 1000; // 10min
    private static final String LOG_STREAMING_ENDPOINT = "%s/api/logstream/apps/%s/instances/%s?follow=%b";

    public static List<ServiceResourceInner> listAllSpringCloudClusters() throws IOException {
        final List<ServiceResourceInner> clusters = new ArrayList<>();
        List<Subscription> subs = AzureMvpModel.getInstance().getSelectedSubscriptions();
        if (subs.size() == 0) {
            return clusters;
        }
        Observable.from(subs).flatMap((sd) -> Observable.create((subscriber) -> {
            try {
                List<ServiceResourceInner> clustersInSubs = listAllSpringCloudClustersBySubscription(
                        sd.subscriptionId());
                synchronized (clusters) {
                    clusters.addAll(clustersInSubs);
                }
            } catch (IOException e) {
                // swallow exception and skip error subscription
            }
            subscriber.onCompleted();
        }).subscribeOn(Schedulers.io()), subs.size()).subscribeOn(Schedulers.io()).toBlocking().subscribe();
        return clusters;
    }

    public static List<ServiceResourceInner> listAllSpringCloudClustersBySubscription(String sid) throws IOException {
        PagedList<ServiceResourceInner> res = getSpringManager(sid).inner().services().list();
        res.loadAll();
        return res;
    }

    public static List<AppResourceInner> listAppsByClusterId(String id) throws IOException {
        PagedList<AppResourceInner> res = getSpringManager(getSubscriptionId(id)).inner().apps()
                .list(getResourceGroup(id), getClusterName(id));
        res.loadAll();
        return res;
    }

    public static Observable<DeploymentResource> listAllDeploymentsByClusterId(String id) throws IOException {
        return getSpringManager(getSubscriptionId(id)).deployments()
                .listClusterAllDeploymentsAsync(getResourceGroup(id), getClusterName(id));
    }

    public static AppResourceInner getAppById(String appId) throws IOException {
        return getSpringManager(getSubscriptionId(appId)).apps().inner().get(getResourceGroup(appId),
                getClusterName(appId), getAppName(appId));
    }

    public static DeploymentResourceInner getAppDeployment(String appId, String deploymentName) throws IOException {
        String sid = getSubscriptionId(appId);
        String rid = getResourceGroup(appId);
        String cid = getClusterName(appId);
        String appName = getAppName(appId);
        return getSpringManager(sid).deployments().inner().get(rid, cid, appName, deploymentName);
    }

    public static DeploymentResourceInner getActiveDeploymentForApp(String appId) throws IOException {
        AppResourceInner app = getAppById(appId);
        if (app == null || StringUtils.isEmpty(app.properties().activeDeploymentName())) {
            return null;
        }
        String activeDeployment = app.properties().activeDeploymentName();
        return getAppDeployment(appId, activeDeployment);
    }

    public static Completable startApp(String appId, String deploymentName) throws IOException {
        return getSpringManager(getSubscriptionId(appId)).deployments().startAsync(getResourceGroup(appId),
                getClusterName(appId), getAppName(appId), deploymentName);
    }

    public static Completable stopApp(String appId, String deploymentName) throws IOException {
        return getSpringManager(getSubscriptionId(appId)).deployments().stopAsync(getResourceGroup(appId),
                getClusterName(appId), getAppName(appId), deploymentName);
    }

    public static Completable restartApp(String appId, String deploymentName) throws IOException {
        return getSpringManager(getSubscriptionId(appId)).deployments().restartAsync(getResourceGroup(appId),
                getClusterName(appId), getAppName(appId), deploymentName);
    }

    public static Completable deleteApp(String appId) throws IOException {
        return getSpringManager(getSubscriptionId(appId)).apps().deleteAsync(getResourceGroup(appId),
                getClusterName(appId), getAppName(appId));
    }

    public static AppResourceInner setPublic(String appId, boolean isPublic) throws IOException {
        return getSpringManager(getSubscriptionId(appId)).apps().inner().update(getResourceGroup(appId),
                getClusterName(appId), getAppName(appId), new AppResourceProperties().withPublicProperty(isPublic));
    }

    public static AppResourceInner updateAppProperties(String appId, AppResourceProperties update) throws IOException {
        return getSpringManager(getSubscriptionId(appId)).apps().inner().update(getResourceGroup(appId),
                getClusterName(appId), getAppName(appId), update);
    }

    public static DeploymentResourceInner updateProperties(String appId, String activeDeploymentName,
            DeploymentResourceProperties pr) throws IOException {
        String sid = getSubscriptionId(appId);
        String rid = getResourceGroup(appId);
        String cid = getClusterName(appId);
        String appName = getAppName(appId);
        return getSpringManager(sid).deployments().inner().update(rid, cid, appName, activeDeploymentName, pr);
    }

    public static String getTestEndpoint(String appId) throws IOException {
        String sid = getSubscriptionId(appId);
        String rid = getResourceGroup(appId);
        String cid = getClusterName(appId);
        String appName = getAppName(appId);
        final TestKeys testKeys = getSpringManager(sid).services().listTestKeysAsync(rid, cid).toBlocking().first();
        if (testKeys == null) {
            return null;
        } else {
            return String.format("%s/%s/default/", testKeys.primaryTestEndpoint(), appName);
        }
    }

    public static String getTestEndpointForApp(String primaryTestEndpoint, String appName) {
        return StringUtils.isNotEmpty(primaryTestEndpoint) && StringUtils.isNotEmpty(appName) ?
               String.format("%s/%s/default/", primaryTestEndpoint, appName) : null;
    }

    public static String getPrimaryTestEndpoint(String clusterId) throws IOException {
        String sid = getSubscriptionId(clusterId);
        String rid = getResourceGroup(clusterId);
        String cid = getClusterName(clusterId);
        final TestKeys testKeys = getSpringManager(sid).services().listTestKeysAsync(rid, cid).toBlocking().first();
        if (testKeys == null) {
            return null;
        } else {
            return testKeys.primaryTestEndpoint();
        }
    }

    public static Map<String, InputStream> getLogStreamReaderList(String appId) throws IOException, HttpException {
        final Map<String, InputStream> result = new HashMap<>();
        final DeploymentResourceInner activeDeployment = getActiveDeploymentForApp(appId);
        for (final DeploymentInstance instance : activeDeployment.properties().instances()) {
            result.put(instance.name(), getLogStream(appId, instance.name(), 0, 10, 0, true));
        }
        return result;
    }

    public static void uploadFileToStorage(File file, String sasUrl)
            throws IOException, StorageException, URISyntaxException {
        final CloudFile cloudFile = new CloudFile(new URI(sasUrl));
        cloudFile.uploadFromFile(file.getPath());
    }

    public static InputStream getLogStream(String appId,
            String instanceName, int sinceSeconds, int tailLines, int limitBytes, boolean follow)
            throws IOException, HttpException {
        String subscriptionId = getSubscriptionId(appId);
        String resourceGroup = getResourceGroup(appId);
        String cluster = getClusterName(appId);
        String appName = getAppName(appId);
        final AppPlatformManager manager = getSpringManager(subscriptionId);
        final TestKeys testKeys = manager.services().listTestKeysAsync(resourceGroup, cluster).toBlocking().first();
        String endpoint = String.format(LOG_STREAMING_ENDPOINT, testKeys.primaryTestEndpoint().replace(".test", ""),
                appName, instanceName, follow);
        if (sinceSeconds > 0) {
            endpoint += "&sinceSeconds=" + sinceSeconds;
        }
        if (tailLines > 0) {
            endpoint += "&tailLines=" + tailLines;
        }
        if (limitBytes > 0) {
            endpoint += "&limitBytes=" + limitBytes;
        }
        final String userName = "primary";
        final String password = testKeys.primaryKey();
        HttpURLConnection connection;
        final URL url = new URL(endpoint);
        connection = (HttpURLConnection) url.openConnection();
        final String userpass = userName + ":" + password;
        final String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        connection.setRequestProperty("Authorization", basicAuth);

        connection.setReadTimeout(SPRING_LOG_STREAMING_READ_TIMEOUT);
        connection.setConnectTimeout(SPRING_LOG_STREAMING_CONNECT_TIMEOUT);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        }
        throw new HttpException(
                String.format("Failed to get log stream due to http error, unexpectedly status code: " + connection.getResponseCode()));
    }

    public static ServiceResource getClusterById(String subscriptionId, String clusterId) throws IOException {
        final String clusterName = SpringCloudIdHelper.getClusterName(clusterId);
        final String resourceGroup = SpringCloudIdHelper.getResourceGroup(clusterId);
        final AppPlatformManager manager = getSpringManager(subscriptionId);
        return manager.services().getByResourceGroupAsync(resourceGroup, clusterName).toBlocking().firstOrDefault(null);
    }

    private static AppPlatformManager getSpringManager(String sid) throws IOException {
        return AuthMethodManager.getInstance().getAzureSpringCloudClient(sid);
    }
}
