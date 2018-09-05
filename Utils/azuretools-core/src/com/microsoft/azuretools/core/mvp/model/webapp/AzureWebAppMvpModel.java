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
import com.microsoft.azure.management.appservice.PublishingProfileFormat;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.WebAppUtils;

public class AzureWebAppMvpModel {

    public static final String CANNOT_GET_WEB_APP_WITH_ID = "Cannot get Web App with ID: ";
    private final Map<String, List<ResourceEx<WebApp>>> subscriptionIdToAllWebApps;
    private final Map<String, List<ResourceEx<WebApp>>> subscriptionIdToWebAppsOnWindowsMap;
    private final Map<String, List<ResourceEx<WebApp>>> subscriptionIdToWebAppsOnLinuxMap;

    private AzureWebAppMvpModel() {
        subscriptionIdToAllWebApps = new ConcurrentHashMap<>();
        subscriptionIdToWebAppsOnLinuxMap = new ConcurrentHashMap<>();
        subscriptionIdToWebAppsOnWindowsMap = new ConcurrentHashMap<>();
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
            throw new Exception(CANNOT_GET_WEB_APP_WITH_ID + id); // TODO: specify the type of exception.
        }
        return app;
    }

     /**
     * API to create Web App on Windows .
     *
     * @param model parameters
     * @return instance of created WebApp
     * @throws Exception exception
     */
    public WebApp createWebAppOnWindows(@NotNull WebAppSettingModel model) throws Exception {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(model.getSubscriptionId());

        WebApp.DefinitionStages.WithCreate withCreate;
        if (model.isCreatingAppServicePlan()) {
            withCreate = withCreateNewWindowsServicePlan(azure, model);
        } else {
            withCreate = withExistingWindowsServicePlan(azure, model);
        }

        return withCreate
                .withJavaVersion(model.getJdkVersion())
                .withWebContainer(WebContainer.fromString(model.getWebContainer()))
                .create();
    }

    private WebApp.DefinitionStages.WithCreate withCreateNewWindowsServicePlan(
            @NotNull Azure azure, @NotNull WebAppSettingModel model) throws Exception {

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

    private WebApp.DefinitionStages.WithCreate withExistingWindowsServicePlan(
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
     * API to create Web App on Docker.
     *
     * @param model parameters
     * @return instance of created WebApp
     * @throws IOException IOExceptions
     */
    public WebApp createWebAppWithPrivateRegistryImage(@NotNull WebAppOnLinuxDeployModel model)
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
    public WebApp updateWebAppOnDocker(String sid, String webAppId, ImageSetting imageSetting) throws Exception {
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
        // status-free restart.
        stopWebApp(sid, webAppId);
        startWebApp(sid, webAppId);
        return app;
    }

    /**
     * Update app settings of webapp.
     *
     * @param sid      subscription id
     * @param webAppId webapp id
     * @param toUpdate entries to add/modify
     * @param toRemove entries to remove
     * @throws Exception exception
     */
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
     * List all the Web Apps on Windows in selected subscriptions.
     */
    public List<ResourceEx<WebApp>> listAllWebAppsOnWindows(boolean force) {
        final List<ResourceEx<WebApp>> webAppList = new ArrayList<>();
        for (final Subscription sub: AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            webAppList.addAll(this.listWebAppsBySidAndOS(sub.subscriptionId(), subscriptionIdToWebAppsOnWindowsMap,
                force, OperatingSystem.WINDOWS));
        }
        return webAppList;
    }

    /**
     * List all the Web Apps on Linux in selected subscriptions.
     *
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App on Linux
     */
    public List<ResourceEx<WebApp>> listAllWebAppsOnLinux(boolean force) {
        final List<ResourceEx<WebApp>> webApps = new ArrayList<>();
        for (final Subscription sb: AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            webApps.addAll(this.listWebAppsBySidAndOS(sb.subscriptionId(), subscriptionIdToWebAppsOnLinuxMap,
                force, OperatingSystem.LINUX));
        }
        return webApps;
    }

    /**
     * List all the Web Apps in selected subscriptions.
     *
     * @param force flag indicating whether force to fetch most updated data from server
     * @return list of Web App
     */
    public List<ResourceEx<WebApp>> listAllWebApps(boolean force) {
        final List<ResourceEx<WebApp>> webApps = new ArrayList<>();
        for (final Subscription sb: AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            webApps.addAll(this.listWebAppsBySidAndOS(sb.subscriptionId(), subscriptionIdToAllWebApps, force, null));
        }
        return webApps;
    }

    /**
     * List the Web Apps by subscription id and operating system.
     * If os is null, get all the web apps no matter what kind of os is.
     * @param sid        subscription id
     * @param force      force flag indicating whether force to fetch most updated data from server
     * @param os         operating system
     * @return           list of Web App
     */
    public List<ResourceEx<WebApp>> listWebAppsBySidAndOS(final String sid,
                                                          @NotNull final Map<String, List<ResourceEx<WebApp>>> cacheMap,
                                                          final boolean force, final OperatingSystem os) {
        if (!force && cacheMap.containsKey(sid)) {
            return cacheMap.get(sid);
        }

        final List<ResourceEx<WebApp>> webAppList = new ArrayList<>();

        try {
            final Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
            webAppList.addAll(azure.webApps().list().stream()
                .filter(app -> os != null ? os.equals(app.operatingSystem()) : true)
                .map(app -> new ResourceEx<>(app, sid))
                .collect(Collectors.toList()));
            cacheMap.put(sid, webAppList);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return webAppList;
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
     * Download publish profile.
     *
     * @param sid      subscription id
     * @param webAppId webapp id
     * @param filePath file path to save publish profile
     * @return status indicating whether it is successful or not
     * @throws Exception exception
     */
    public boolean getPublishingProfileXmlWithSecrets(String sid, String webAppId, String filePath) throws Exception {
        WebApp app = getWebAppById(sid, webAppId);
        File file = new File(Paths.get(filePath, app.name() + "_" + System.currentTimeMillis() + ".PublishSettings")
                .toString());
        file.createNewFile();
        try (InputStream inputStream = app.manager().inner().webApps()
                .listPublishingProfileXmlWithSecrets(app.resourceGroupName(), app.name(),
                        PublishingProfileFormat.FTP);
             OutputStream outputStream = new FileOutputStream(file);
        ) {
            IOUtils.copy(inputStream, outputStream);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void cleanWebAppsOnWindows() {
        subscriptionIdToWebAppsOnWindowsMap.clear();
    }

    public void cleanWebAppsOnLinux() {
        subscriptionIdToWebAppsOnLinuxMap.clear();
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

    private static final class SingletonHolder {
        private static final AzureWebAppMvpModel INSTANCE = new AzureWebAppMvpModel();
    }
}
