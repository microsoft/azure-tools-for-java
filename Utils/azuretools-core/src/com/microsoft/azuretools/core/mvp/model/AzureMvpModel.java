/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
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
 *
 */

package com.microsoft.azuretools.core.mvp.model;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppDiagnosticLogs;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.CanceledByUserException;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AzureMvpModel {

    public static final String CANNOT_GET_RESOURCE_GROUP = "Cannot get Resource Group.";
    public static final String APPLICATION_LOG_NOT_ENABLED = "Application log is not enabled.";

    private AzureMvpModel() {
    }

    public static AzureMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
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
            if (azureManager == null) {
                return ret;
            }
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
        Collections.sort(ret, getComparator(Subscription::displayName));
        return ret;
    }

    /**
     * List all the resource groups in selected subscriptions.
     * @return
     */
    public List<ResourceEx<ResourceGroup>> getResourceGroups(boolean forceUpdate) throws IOException, CanceledByUserException {
        List<ResourceEx<ResourceGroup>> resourceGroups = new ArrayList<>();
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance()
            .getSubscriptionToResourceGroupMap();
        if (srgMap == null || srgMap.size() < 1 || forceUpdate) {
            AzureModelController.updateSubscriptionMaps(null);
        }
        srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
        if (srgMap == null) {
            return resourceGroups;
        }
        for (SubscriptionDetail sd : srgMap.keySet()) {
            resourceGroups.addAll(srgMap.get(sd).stream().map(
                resourceGroup -> new ResourceEx<>(resourceGroup, sd.getSubscriptionId())).collect(Collectors.toList()));
        }
        Collections.sort(resourceGroups, getComparator((ResourceEx<ResourceGroup> resourceGroupResourceEx) ->
                resourceGroupResourceEx.getResource().name()));
        return resourceGroups;
    }

    /**
     *
     * @param rgName resource group name
     * @param sid subscription id
     * @return
     */
    public void deleteResourceGroup(String rgName, String sid) throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(sid);
        azure.resourceGroups().deleteByName(rgName);
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
            Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
            ret.addAll(azure.resourceGroups().list());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.sort(ret, getComparator(ResourceGroup::name));
        return ret;
    }

    /**
     * Get Resource Group by Subscription ID and Resource Group name.
     */
    public ResourceGroup getResourceGroupBySubscriptionIdAndName(String sid, String name) throws Exception {
        ResourceGroup resourceGroup;
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        try {
            resourceGroup = azure.resourceGroups().getByName(name);
            if (resourceGroup == null) {
                throw new Exception(CANNOT_GET_RESOURCE_GROUP);
            }
        } catch (Exception e) {
            throw new Exception(CANNOT_GET_RESOURCE_GROUP);
        }
        return resourceGroup;
    }

    public List<Deployment> listAllDeployments() {
        List<Deployment> deployments = new ArrayList<>();
        List<Subscription> subs = getSelectedSubscriptions();
        Observable.from(subs).flatMap((sub) ->
            Observable.create((subscriber) -> {
                try {
                    List<Deployment> sidDeployments = listDeploymentsBySid(sub.subscriptionId());
                    synchronized (deployments) {
                        deployments.addAll(sidDeployments);
                    }
                } catch (IOException e) {
                }
                subscriber.onCompleted();
            }).subscribeOn(Schedulers.io()), subs.size()).subscribeOn(Schedulers.io()).toBlocking().subscribe();
        Collections.sort(deployments, getComparator(Deployment::name));
        return deployments;
    }

    public List<Deployment> listDeploymentsBySid(String sid) throws IOException {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        List<Deployment> deployments = azure.deployments().list();
        Collections.sort(deployments, getComparator(Deployment::name));
        return deployments;
    }

    /**
     * Get deployment by resource group name
     * @param rgName
     * @return
     */
    public List<ResourceEx<Deployment>> getDeploymentByRgName(String sid, String rgName) throws IOException {
        List<ResourceEx<Deployment>> res = new ArrayList<>();
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        res.addAll(azure.deployments().listByResourceGroup(rgName).stream().
            map(deployment -> new ResourceEx<>(deployment, sid)).collect(Collectors.toList()));
        Collections.sort(res,
                getComparator((ResourceEx<Deployment> deploymentResourceEx) -> deploymentResourceEx.getResource().name()));
        return res;
    }

    /**
     * Get Log Streaming by Subscription and AppServiceId
     *
     * @param sid subscription
     * @param appServiceId appServiceId
     * @return
     * @throws IOException
     */
    public Observable<String> getAppServiceStreamingLogs(String sid, String appServiceId) throws IOException{
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        WebApp webApp = azure.webApps().getById(appServiceId);
        if (isServerLogEnabled(webApp.diagnosticLogsConfig())) {
            return webApp.streamAllLogsAsync();
        } else {
            throw new IOException(APPLICATION_LOG_NOT_ENABLED);
        }
    }

    public void enableAppServiceContainerLogging(String sid, String appServiceId) throws IOException {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        WebApp webApp = azure.webApps().getById(appServiceId);
        webApp.update().withContainerLoggingEnabled().apply();
    }

    /**
     * Get Log Streaming of AppService Deployment Slots
     *
     * @param sid subscription
     * @param appServiceId appServiceId
     * @return
     * @throws IOException
     */
    public Observable<String> getAppServiceSlotStreamingLogs(String sid, String appServiceId, String slotName) throws IOException {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        WebApp webApp = azure.webApps().getById(appServiceId);

        DeploymentSlot deploymentSlot = webApp.deploymentSlots().getByName(slotName);
        if (isServerLogEnabled(deploymentSlot.diagnosticLogsConfig())) {
            return deploymentSlot.streamHttpLogsAsync();
        } else {
            throw new IOException(APPLICATION_LOG_NOT_ENABLED);
        }
    }

    public void enableDeploymentSlotLogging(String sid, String appServiceId, String slotName) throws IOException {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        WebApp webApp = azure.webApps().getById(appServiceId);
        DeploymentSlot deploymentSlot = webApp.deploymentSlots().getByName(slotName);
        deploymentSlot.update().withContainerLoggingEnabled().apply();
    }

    private boolean isServerLogEnabled(WebAppDiagnosticLogs webAppDiagnosticLogs){
        return webAppDiagnosticLogs.inner().httpLogs().fileSystem().enabled();
    }

    /**
     * List Location by Subscription ID.
     *
     * @param sid subscription Id
     * @return List of Location instances
     */
    public List<Location> listLocationsBySubscriptionId(String sid) {
        List<Location> locations = new ArrayList<>();
        Subscription subscription = getSubscriptionById(sid);
        try {
            locations.addAll(subscription.listLocations());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(locations, getComparator(Location::name));
        return locations;
    }

    /**
     * List all Pricing Tier supported by SDK.
     *
     * @return List of PricingTier instances.
     */
    public List<PricingTier> listPricingTier() throws IllegalAccessException {
        List<PricingTier> ret = new ArrayList<>();
        for (Field field : PricingTier.class.getDeclaredFields()) {
            int modifier = field.getModifiers();
            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier) && Modifier.isFinal(modifier)) {
                PricingTier pt = (PricingTier) field.get(null);
                ret.add(pt);
            }
        }
        Collections.sort(ret, getComparator(PricingTier::toString));
        return correctPricingTiers(ret);
    }

    private static <T> Comparator<T> getComparator(Function<T, String> toStringMethod) {
        return (first, second) ->
                StringUtils.compareIgnoreCase(toStringMethod.apply(first), toStringMethod.apply(second));
    }

    // Remove Premium pricing tier which has performance issues with java app services
    private List<PricingTier> correctPricingTiers(final List<PricingTier> pricingTiers) {
        pricingTiers.remove(PricingTier.PREMIUM_P1);
        pricingTiers.remove(PricingTier.PREMIUM_P2);
        pricingTiers.remove(PricingTier.PREMIUM_P3);
        return pricingTiers;
    }

    public URI getResourceUri(String subscriptionId, String id) throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        String portalUrl = azureManager.getPortalUrl();

        // Note: [SubscriptionManager.getSubscriptionTenant()] method does not update Subscription to TenantId map while
        //       [SubscriptionManager.getSubscriptionDetails()] force to update and get the correct value
        List<SubscriptionDetail> subscriptionDetails = azureManager.getSubscriptionManager().getSubscriptionDetails();
        String tenantId = null;
        for (SubscriptionDetail subscriptionDetail : subscriptionDetails) {
            if (subscriptionDetail.getSubscriptionId().equals(subscriptionId)) {
                tenantId = subscriptionDetail.getTenantId();
                break;
            }
        }

        if (tenantId == null) {
            return null;
        }

        String path = "/#@" + tenantId + "/resource/" + id + "/overview".replaceAll("/+", "/");

        return URI.create(portalUrl + "/" + path).normalize();
    }

    private static final class SingletonHolder {
        private static final AzureMvpModel INSTANCE = new AzureMvpModel();
    }
}
