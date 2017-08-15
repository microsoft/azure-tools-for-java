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

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzulZuluModel;
import com.microsoft.azuretools.utils.WebAppUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AzureWebAppMvpModel {
    private static final String NOT_SIGNED_ERROR = "Plugin not signed in error.";
    private static final String CANNOT_GET_AZURE_MANAGER = "Cannot get Azure Manager.";
    private static final String CANNOT_GET_AZURE_BY_SID = "Cannot get Azure by subscription ID.";
    private final Map<String, List<ResourceEx<WebApp>>> subscriptionIdToWebAppsMap;
    private final Map<String, List<ResourceEx<SiteInner>>> subscriptionIdToWebAppsOnLinuxMap;

    private AzureWebAppMvpModel() {
        subscriptionIdToWebAppsOnLinuxMap = new ConcurrentHashMap<>();
        subscriptionIdToWebAppsMap = new ConcurrentHashMap<>();
    }

    public static AzureWebAppMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static WebApp.DefinitionStages.WithCreate withCreateNewSPlan(
            @NotNull Azure azure,
            @NotNull WebAppSettingModel model) throws Exception {
        String[] tierSize = model.getPricing().split("_");
        if (tierSize.length != 2) {
            throw new Exception("Cannot get valid price tier");
        }
        PricingTier pricing = new PricingTier(tierSize[0], tierSize[1]);
        AppServicePlan.DefinitionStages.WithCreate withCreatePlan;

        WebApp.DefinitionStages.WithCreate withCreateWebApp;
        if (model.isCreatingResGrp()) {
            withCreatePlan = azure.appServices().appServicePlans()
                    .define(model.getAppServicePlanName())
                    .withRegion(model.getRegion())
                    .withNewResourceGroup(model.getResourceGroup())
                    .withPricingTier(pricing)
                    .withOperatingSystem(OperatingSystem.WINDOWS);
            withCreateWebApp = azure.webApps().define(model.getWebAppName())
                    .withRegion(model.getRegion())
                    .withNewResourceGroup(model.getResourceGroup())
                    .withNewWindowsPlan(withCreatePlan);
        } else {
            withCreatePlan = azure.appServices().appServicePlans()
                    .define(model.getAppServicePlanName())
                    .withRegion(model.getRegion())
                    .withExistingResourceGroup(model.getResourceGroup())
                    .withPricingTier(pricing)
                    .withOperatingSystem(OperatingSystem.WINDOWS);
            withCreateWebApp = azure.webApps().define(model.getWebAppName())
                    .withRegion(model.getRegion())
                    .withExistingResourceGroup(model.getResourceGroup())
                    .withNewWindowsPlan(withCreatePlan);
        }
        return withCreateWebApp;
    }

    private static WebApp.DefinitionStages.WithCreate withCreateExistingSPlan(
            @NotNull Azure azure,
            @NotNull WebAppSettingModel model) {
        AppServicePlan servicePlan = azure.appServices().appServicePlans().getById(model.getAppServicePlanId());
        WebApp.DefinitionStages.WithCreate withCreate;
        if (model.isCreatingResGrp()) {
            withCreate = azure.webApps().define(model.getWebAppName())
                    .withExistingWindowsPlan(servicePlan)
                    .withNewResourceGroup(model.getResourceGroup());
        } else {
            withCreate = azure.webApps().define(model.getWebAppName())
                    .withExistingWindowsPlan(servicePlan)
                    .withExistingResourceGroup(model.getResourceGroup());
        }

        return withCreate;
    }

    public WebApp getWebAppById(String sid, String id) throws IOException {
        Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(sid);
        return azure.webApps().getById(id);
    }

    // TODO
    public WebApp createWebApp(@NotNull WebAppSettingModel model) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            throw new Exception("There is no azure manager");
        }
        Azure azure = azureManager.getAzure(model.getSubscriptionId());
        if (azure == null) {
            throw new Exception(
                    String.format("Cannot get azure instance for subID  %s", model.getSubscriptionId())
            );
        }

        WebApp.DefinitionStages.WithCreate withCreate;
        if (model.isCreatingAppServicePlan()) {
            withCreate = withCreateNewSPlan(azure, model);
        } else {
            withCreate = withCreateExistingSPlan(azure, model);
        }

        WebApp webApp;
        if (WebAppSettingModel.JdkChoice.DEFAULT.toString().equals(model.getJdkChoice())) {
            webApp = withCreate
                    .withJavaVersion(JavaVersion.JAVA_8_NEWEST)
                    .withWebContainer(WebContainer.fromString(model.getWebContainer()))
                    .create();
        } else {
            webApp = withCreate.create();
        }
        return webApp;
    }

    public void deployWebApp() {
        // TODO
    }

    public void deleteWebApp(String sid, String appid) throws IOException {
        getAzureBySid(sid).webApps().deleteById(appid);
        // TODO: update cache
    }

    /**
     * API to create Web App on Linux.
     *
     * @param sid          subscription id
     * @param profile      parameters
     * @param imageSetting container related settings
     * @return instance of created WebApp
     * @throws IOException IOExceptions
     */
    public WebApp createWebAppOnLinux(String sid, WebAppOnLinuxDeployModel profile, ImageSetting imageSetting)
            throws IOException {
        if (!(imageSetting instanceof PrivateRegistryImageSetting)) {
            // TODO: other types of ImageSetting, e.g. Docker Hub
            return null;
        }
        PrivateRegistryImageSetting pr = (PrivateRegistryImageSetting) imageSetting;
        WebApp app;
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null ) {
            throw new IOException("AzureManager not found.");
        }
        Azure azure = azureManager.getAzure(sid);
        PricingTier pricingTier = new PricingTier(profile.getPricingSkuTier(), profile.getPricingSkuSize());


        WebApp.DefinitionStages.Blank webAppDefinition = azure.webApps().define(profile.getWebAppName());
        if (profile.isCreatingNewAppServicePlan()) {
            // new asp
            AppServicePlan.DefinitionStages.WithCreate asp;
            if (profile.isCreatingNewResourceGroup()) {
                // new rg
                asp = azure.appServices().appServicePlans()
                        .define(profile.getAppServicePlanName())
                        .withRegion(Region.findByLabelOrName(profile.getLocationName()))
                        .withNewResourceGroup(profile.getResourceGroupName())
                        .withPricingTier(pricingTier)
                        .withOperatingSystem(OperatingSystem.LINUX);
                app = webAppDefinition
                        .withRegion(Region.findByLabelOrName(profile.getLocationName()))
                        .withNewResourceGroup(profile.getResourceGroupName())
                        .withNewLinuxPlan(asp)
                        .withPrivateRegistryImage(pr.getImageNameWithTag(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            } else {
                // old rg
                asp = azure.appServices().appServicePlans()
                        .define(profile.getAppServicePlanName())
                        .withRegion(Region.findByLabelOrName(profile.getLocationName()))
                        .withExistingResourceGroup(profile.getResourceGroupName())
                        .withPricingTier(pricingTier)
                        .withOperatingSystem(OperatingSystem.LINUX);
                app = webAppDefinition
                        .withRegion(Region.findByLabelOrName(profile.getLocationName()))
                        .withExistingResourceGroup(profile.getResourceGroupName())
                        .withNewLinuxPlan(asp)
                        .withPrivateRegistryImage(pr.getImageNameWithTag(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            }
        } else {
            // old asp
            AppServicePlan asp = azure.appServices().appServicePlans().getById(profile.getAppServicePlanId());
            if (profile.isCreatingNewResourceGroup()) {
                // new rg
                app = webAppDefinition
                        .withExistingLinuxPlan(asp)
                        .withNewResourceGroup(profile.getResourceGroupName())
                        .withPrivateRegistryImage(pr.getImageNameWithTag(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            } else {
                // old rg
                app = webAppDefinition
                        .withExistingLinuxPlan(asp)
                        .withExistingResourceGroup(profile.getResourceGroupName())
                        .withPrivateRegistryImage(pr.getImageNameWithTag(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            }
        }
        return app;
        // TODO: update cache
    }

    /**
     * Update container settings for existing Web App on Linux.
     *
     * @param sid          Subscription id
     * @param webAppId     id of Web App on Linux instance
     * @param imageSetting new container settings
     * @return instance of the updated Web App on Linux
     */
    public WebApp updateWebAppOnLinux(String sid, String webAppId, ImageSetting imageSetting) throws IOException {
        WebApp app = AzureWebAppMvpModel.getInstance().getWebAppById(sid, webAppId);
        if (imageSetting instanceof PrivateRegistryImageSetting) {
            PrivateRegistryImageSetting pr = (PrivateRegistryImageSetting) imageSetting;
            app.update().withPrivateRegistryImage(pr.getImageNameWithTag(), pr.getServerUrl())
                    .withCredentials(pr.getUsername(), pr.getPassword())
                    .withStartUpCommand(pr.getStartupFile()).apply();
        } else {
            // TODO: other types of ImageSetting, e.g. Docker Hub
        }
        return app;
    }

    public void deleteWebAppOnLinux(String sid, String appid) throws IOException {
        deleteWebApp(sid, appid);
    }

    public void restartWebApp(String sid, String appid) throws IOException {
        getAzureBySid(sid).webApps().getById(appid).restart();
    }

    public void startWebApp(String sid, String appid) throws IOException {
        getAzureBySid(sid).webApps().getById(appid).start();
    }

    public void stopWebApp(String sid, String appid) throws IOException {
        getAzureBySid(sid).webApps().getById(appid).stop();
    }

    /**
     * List app service plan by subscription id and resource group name.
     */
    public List<AppServicePlan> listAppServicePlanBySubscriptionIdAndResourceGroupName(String sid, String group)
            throws Exception {
        List<AppServicePlan> list;
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            if (azureManager == null) {
                throw new Exception(NOT_SIGNED_ERROR);
            }
            Azure azure = azureManager.getAzure(sid);
            if (azure == null) {
                throw new Exception(CANNOT_GET_AZURE_BY_SID);
            }
            list = azure.appServices().appServicePlans().listByResourceGroup(group);
        } catch (Exception e) {
            throw new Exception(CANNOT_GET_AZURE_BY_SID);
        }
        return list;
    }


    /**
     * List app service plan by subscription id.
     */
    public List<AppServicePlan> listAppServicePlanBySubscriptionId(String sid) throws Exception {
        List<AppServicePlan> list;
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            if (azureManager == null) {
                throw new Exception(NOT_SIGNED_ERROR);
            }
            Azure azure = azureManager.getAzure(sid);
            if (azure == null) {
                throw new Exception(CANNOT_GET_AZURE_BY_SID);
            }
            list = azure.appServices().appServicePlans().list();
        } catch (Exception e) {
            throw new Exception(CANNOT_GET_AZURE_BY_SID);
        }
        return list;
    }

    /**
     * List Web Apps by Subscription ID.
     */
    public List<ResourceEx<WebApp>> listWebAppsBySubscriptionId(String sid, boolean force) {
        if (!force && subscriptionIdToWebAppsMap.containsKey(sid)) {
            return subscriptionIdToWebAppsMap.get(sid);
        }
        List<ResourceEx<WebApp>> webAppList = new ArrayList<>();
        try {
            Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(sid);
            for (WebApp webApp : azure.webApps().list()) {
                if (webApp.operatingSystem().equals(OperatingSystem.WINDOWS)) {
                    webAppList.add(new ResourceEx<>(webApp, sid));
                }
            }
            subscriptionIdToWebAppsMap.put(sid, webAppList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return webAppList;
    }

    /**
     * List all the Web Apps in selected subscriptions.
     */
    public List<ResourceEx<WebApp>> listWebApps(boolean force) {
        List<ResourceEx<WebApp>> webAppList = new ArrayList<>();
        List<Subscription> subscriptions = AzureMvpModel.getInstance().getSelectedSubscriptions();
        for (Subscription sub : subscriptions) {
            webAppList.addAll(this.listWebAppsBySubscriptionId(sub.subscriptionId(), force));
        }
        return webAppList;
    }

    /**
     * List Web App on Linux by Subscription ID.
     *
     * @param sid   subscription Id
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App on Linux (SiteInner instances)
     */
    public List<ResourceEx<SiteInner>> listWebAppsOnLinuxBySubscriptionId(String sid, boolean force) {
        List<ResourceEx<SiteInner>> wal = new ArrayList<>();
        if (!force && subscriptionIdToWebAppsOnLinuxMap.containsKey(sid)) {
            return subscriptionIdToWebAppsOnLinuxMap.get(sid);
        }
        try {
            Azure azure = AuthMethodManager.getInstance().getAzureManager().getAzure(sid);
            wal.addAll(azure.webApps().inner().list()
                    .stream()
                    .filter(app -> app.kind().equals("app,linux"))
                    .map(app -> new ResourceEx<>(app, sid))
                    .collect(Collectors.toList())
            );
            subscriptionIdToWebAppsOnLinuxMap.put(sid, wal);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wal;
    }

    /**
     * List available Web Containers.
     */
    public List<WebAppUtils.WebContainerMod> listWebContainers() {
        List<WebAppUtils.WebContainerMod> webContainers = new ArrayList<>();
        Collections.addAll(webContainers, WebAppUtils.WebContainerMod.values());
        return webContainers;
    }

    /**
     * List available Third Party JDKs.
     */
    public List<AzulZuluModel> listThirdPartyJdk() {
        List<AzulZuluModel> jdks = new ArrayList<>();
        for (AzulZuluModel jdk : AzulZuluModel.values()) {
            if (jdk.isDeprecated()) {
                continue;
            }
            jdks.add(jdk);
        }
        return jdks;
    }

    /**
     * List Web App on Linux in all selected subscriptions.
     *
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App on Linux (SiteInner instances)
     */
    public List<ResourceEx<SiteInner>> listAllWebAppsOnLinux(boolean force) {
        List<ResourceEx<SiteInner>> ret = new ArrayList<>();
        for (Subscription sb : AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            List<ResourceEx<SiteInner>> wal = listWebAppsOnLinuxBySubscriptionId(sb.subscriptionId(), force);
            ret.addAll(wal);
        }
        return ret;
    }

    public void cleanWebApps() {
        subscriptionIdToWebAppsMap.clear();
    }

    public void cleanWebAppsOnLinux() {
        subscriptionIdToWebAppsOnLinuxMap.clear();
    }

    private Azure getAzureBySid(String sid) throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null ) {
            throw new IOException(CANNOT_GET_AZURE_MANAGER);
        }
        Azure azure = azureManager.getAzure(sid);
        if (azure == null ) {
            throw new IOException(CANNOT_GET_AZURE_BY_SID);
        }
        return azure;
    }
    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }
}
