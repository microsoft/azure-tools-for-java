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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.CsmPublishingProfileOptionsInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.WebAppUtils;

public class AzureWebAppMvpModel {

    public static final String CANNOT_GET_WEB_APP_WITH_ID = "Cannot get Web App with ID: ";
    private final Map<String, List<ResourceEx<WebApp>>> subscriptionIdToWebAppsMap;
    private final Map<String, List<ResourceEx<WebApp>>> subscriptionIdToWebAppsOnLinuxMap;

    private AzureWebAppMvpModel() {
        subscriptionIdToWebAppsOnLinuxMap = new ConcurrentHashMap<>();
        subscriptionIdToWebAppsMap = new ConcurrentHashMap<>();
    }

    public static AzureWebAppMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * get the web app by ID.
     */
    public WebApp getWebAppById(String sid, String id) throws Exception {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        WebApp app = azure.webApps().getById(id);
        if (app == null) {
            throw new Exception(CANNOT_GET_WEB_APP_WITH_ID + id);
        }
        return app;
    }

    /**
     * Create an Azure web app service.
     */
    public WebApp createWebApp(@NotNull WebAppSettingModel model) throws Exception {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(model.getSubscriptionId());

        WebApp.DefinitionStages.WithCreate withCreate;
        if (model.isCreatingAppServicePlan()) {
            withCreate = withCreateNewSPlan(azure, model);
        } else {
            withCreate = withCreateExistingSPlan(azure, model);
        }

        return withCreate
                .withJavaVersion(model.getJdkVersion())
                .withWebContainer(WebContainer.fromString(model.getWebContainer()))
                .create();
    }

    private WebApp.DefinitionStages.WithCreate withCreateNewSPlan(
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

    private WebApp.DefinitionStages.WithCreate withCreateExistingSPlan(
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

    public void deployWebApp() {
        // TODO
    }

    public void deleteWebApp(String sid, String appid) throws IOException {
        AuthMethodManager.getInstance().getAzureClient(sid).webApps().deleteById(appid);
        // TODO: update cache
    }

    /**
     * API to create Web App on Linux.
     *
     * @param model      parameters
     * @return instance of created WebApp
     * @throws IOException IOExceptions
     */
    public WebApp createWebAppOnLinux(WebAppOnLinuxDeployModel model)
            throws IOException {
        PrivateRegistryImageSetting pr = model.getPrivateRegistryImageSetting();
        WebApp app;
        Azure azure = AuthMethodManager.getInstance().getAzureClient(model.getSubscriptionId());
        PricingTier pricingTier = new PricingTier(model.getPricingSkuTier(), model.getPricingSkuSize());

        WebApp.DefinitionStages.Blank webAppDefinition = azure.webApps().define(model.getWebAppName());
        if (model.isCreatingNewAppServicePlan()) {
            // new asp
            AppServicePlan.DefinitionStages.WithCreate asp;
            if (model.isCreatingNewResourceGroup()) {
                // new rg
                asp = azure.appServices().appServicePlans()
                        .define(model.getAppServicePlanName())
                        .withRegion(Region.findByLabelOrName(model.getLocationName()))
                        .withNewResourceGroup(model.getResourceGroupName())
                        .withPricingTier(pricingTier)
                        .withOperatingSystem(OperatingSystem.LINUX);
                app = webAppDefinition
                        .withRegion(Region.findByLabelOrName(model.getLocationName()))
                        .withNewResourceGroup(model.getResourceGroupName())
                        .withNewLinuxPlan(asp)
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            } else {
                // old rg
                asp = azure.appServices().appServicePlans()
                        .define(model.getAppServicePlanName())
                        .withRegion(Region.findByLabelOrName(model.getLocationName()))
                        .withExistingResourceGroup(model.getResourceGroupName())
                        .withPricingTier(pricingTier)
                        .withOperatingSystem(OperatingSystem.LINUX);
                app = webAppDefinition
                        .withRegion(Region.findByLabelOrName(model.getLocationName()))
                        .withExistingResourceGroup(model.getResourceGroupName())
                        .withNewLinuxPlan(asp)
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            }
        } else {
            // old asp
            AppServicePlan asp = azure.appServices().appServicePlans().getById(model.getAppServicePlanId());
            if (model.isCreatingNewResourceGroup()) {
                // new rg
                app = webAppDefinition
                        .withExistingLinuxPlan(asp)
                        .withNewResourceGroup(model.getResourceGroupName())
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                        .withCredentials(pr.getUsername(), pr.getPassword())
                        .withStartUpCommand(pr.getStartupFile()).create();
            } else {
                // old rg
                app = webAppDefinition
                        .withExistingLinuxPlan(asp)
                        .withExistingResourceGroup(model.getResourceGroupName())
                        .withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
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
    public WebApp updateWebAppOnLinux(String sid, String webAppId, ImageSetting imageSetting) throws Exception {
        WebApp app = getWebAppById(sid, webAppId);
        clearTags(app);
        if (imageSetting instanceof PrivateRegistryImageSetting) {
            PrivateRegistryImageSetting pr = (PrivateRegistryImageSetting) imageSetting;
            app.update().withPrivateRegistryImage(pr.getImageTagWithServerUrl(), pr.getServerUrl())
                    .withCredentials(pr.getUsername(), pr.getPassword())
                    .withStartUpCommand(pr.getStartupFile()).apply();
        } else {
            // TODO: other types of ImageSetting, e.g. Docker Hub
        }
        startWebApp(sid, webAppId);
        return app;
    }

    public void updateWebAppSettings(String sid, String webAppId, Map<String, String> toUpdate, Set<String> toRemove)
            throws Exception {
        WebApp app = getWebAppById(sid, webAppId);
        clearTags(app);
        com.microsoft.azure.management.appservice.WebAppBase.Update<WebApp> update = app.update()
                .withAppSettings(toUpdate);
        for (String key : toRemove) {
            update = update.withoutAppSetting(key);
        }
        update.apply();
    }

    public void deleteWebAppOnLinux(String sid, String appid) throws IOException {
        deleteWebApp(sid, appid);
    }

    public void restartWebApp(String sid, String appid) throws IOException {
        AuthMethodManager.getInstance().getAzureClient(sid).webApps().getById(appid).restart();
    }

    public void startWebApp(String sid, String appid) throws IOException {
        AuthMethodManager.getInstance().getAzureClient(sid).webApps().getById(appid).start();
    }

    public void stopWebApp(String sid, String appid) throws IOException {
        AuthMethodManager.getInstance().getAzureClient(sid).webApps().getById(appid).stop();
    }

    /**
     * List app service plan by subscription id and resource group name.
     */
    public List<AppServicePlan> listAppServicePlanBySubscriptionIdAndResourceGroupName(String sid, String group) {
        List<AppServicePlan> appServicePlans = new ArrayList<>();
        try {
            Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
            appServicePlans.addAll(azure.appServices().appServicePlans().listByResourceGroup(group));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appServicePlans;
    }


    /**
     * List app service plan by subscription id.
     */
    public List<AppServicePlan> listAppServicePlanBySubscriptionId(String sid) throws IOException {
        return AuthMethodManager.getInstance().getAzureClient(sid).appServices().appServicePlans().list();
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
            Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
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
     * @return list of Web App on Linux
     */
    public List<ResourceEx<WebApp>> listWebAppsOnLinuxBySubscriptionId(String sid, boolean force) {
        List<ResourceEx<WebApp>> wal = new ArrayList<>();
        if (!force && subscriptionIdToWebAppsOnLinuxMap.containsKey(sid)) {
            return subscriptionIdToWebAppsOnLinuxMap.get(sid);
        }
        try {
            Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
            wal.addAll(azure.webApps().list()
                    .stream()
                    .filter(app -> OperatingSystem.LINUX.equals(app.operatingSystem()))
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
    public List<JdkModel> listJdks() {
        List<JdkModel> jdkModels = new ArrayList<>();
        Collections.addAll(jdkModels, JdkModel.values());
        return jdkModels;
    }

    /**
     * List Web App on Linux in all selected subscriptions.
     *
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App on Linux
     */
    public List<ResourceEx<WebApp>> listAllWebAppsOnLinux(boolean force) {
        List<ResourceEx<WebApp>> ret = new ArrayList<>();
        for (Subscription sb : AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            List<ResourceEx<WebApp>> wal = listWebAppsOnLinuxBySubscriptionId(sb.subscriptionId(), force);
            ret.addAll(wal);
        }
        return ret;
    }

    public boolean getPublishingProfileXmlWithSecrets(String sid, String webAppId, String filePath) throws Exception {
        WebApp app = getWebAppById(sid ,webAppId);
        File file = new File(Paths.get(filePath, app.name() + "_" + System.currentTimeMillis() + ".PublishSettings").toString());
        file.createNewFile();
        try (InputStream inputStream = app.manager().inner().webApps()
                .listPublishingProfileXmlWithSecrets(app.resourceGroupName(), app.name(), new CsmPublishingProfileOptionsInner());
                OutputStream outputStream = new FileOutputStream(file);
        ) {
            IOUtils.copy(inputStream, outputStream);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void cleanWebApps() {
        subscriptionIdToWebAppsMap.clear();
    }

    public void cleanWebAppsOnLinux() {
        subscriptionIdToWebAppsOnLinuxMap.clear();
    }

    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }

    /**
     * Work Around:
     * When a web app is created from Azure Portal, there are hidden tags associated with the app.
     * It will be messed up when calling "update" API.
     * An issue is logged at https://github.com/Azure/azure-sdk-for-java/issues/1755 .
     * Remove all tags here to make it work.
     */
    private void clearTags(@NotNull final WebApp app) {
        app.inner().withTags(null);
    }
}
