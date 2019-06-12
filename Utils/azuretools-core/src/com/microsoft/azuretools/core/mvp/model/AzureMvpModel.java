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
 *
 */

package com.microsoft.azuretools.core.mvp.model;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.CanceledByUserException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class AzureMvpModel {

    private static final String CANNOT_GET_RESOURCE_GROUP = "Cannot get Resource Group.";

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
        return deployments;
    }

    public List<Deployment> listDeploymentsBySid(String sid) throws IOException {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        return azure.deployments().list();
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
        return res;
    }

    /**
     * Get Log Streaming by Subscription and AppServiceId
     *
     * @param sid subscription
     * @param appServiceId appServiceId
     * @return Log Streaming
     * @throws IOException
     */
    public Observable<String> getAppServiceStreamingLogs(String sid, String appServiceId) throws IOException{
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        return azure.webApps().getById(appServiceId).streamAllLogsAsync();
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
        return correctPricingTiers(ret);
    }

    // workaround for SDK not updated the PREMIUM pricing tiers to latest ones
    // https://github.com/Azure/azure-libraries-for-java/issues/660
    private List<PricingTier> correctPricingTiers(final List<PricingTier> pricingTiers) {
        pricingTiers.remove(PricingTier.PREMIUM_P1);
        pricingTiers.remove(PricingTier.PREMIUM_P2);
        pricingTiers.remove(PricingTier.PREMIUM_P3);
        pricingTiers.add(new PricingTier("Premium", "P1V2"));
        pricingTiers.add(new PricingTier("Premium", "P2V2"));
        pricingTiers.add(new PricingTier("Premium", "P3V2"));
        return pricingTiers;
    }

    private static final class SingletonHolder {
        private static final AzureMvpModel INSTANCE = new AzureMvpModel();
    }
}
