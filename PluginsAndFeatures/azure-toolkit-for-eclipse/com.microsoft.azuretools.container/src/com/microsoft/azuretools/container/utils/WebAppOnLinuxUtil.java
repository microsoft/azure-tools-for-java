/**
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

package com.microsoft.azuretools.container.utils;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.CanceledByUserException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class WebAppOnLinuxUtil {
    private static Map<String, String> defaultAppSettings = new HashMap<>();
    private static Region defaultRegion = Region.US_WEST;
    private static PricingTier defaultPricingTier = PricingTier.STANDARD_S1;

    /**
     * Create a new Web App on Linux and deploy on it.
     * 
     * @param subscriptionId
     * @param resourceGroup
     * @param appName
     * @param createRgFlag
     * @return
     * @throws IOException
     */
    public static WebApp deployToNew(String subscriptionId, String resourceGroup, String appName, boolean createRgFlag)
            throws IOException {
        PrivateRegistry pr = new PrivateRegistry(DockerRuntime.getInstance().getRegistryUrl(),
                DockerRuntime.getInstance().getRegistryUsername(), DockerRuntime.getInstance().getRegistryPassword());
        String imageName = DockerRuntime.getInstance().getLatestImageName();
        return deploy(subscriptionId, resourceGroup, appName, pr, imageName, createRgFlag);
    }

    /**
     * Deploy on an existing Web App on Linux.
     * 
     * @param app
     * @return
     * @throws IOException
     */
    public static WebApp deployToExisting(SiteInner app) throws IOException {
        /**
         * TODO: workaround to get WebApp instance for it. should persist subsId
         * or wait for API ready
         */
        PrivateRegistry pr = new PrivateRegistry(DockerRuntime.getInstance().getRegistryUrl(),
                DockerRuntime.getInstance().getRegistryUsername(), DockerRuntime.getInstance().getRegistryPassword());
        String imageName = DockerRuntime.getInstance().getLatestImageName();
        AzureManager azureManager;
        azureManager = AuthMethodManager.getInstance().getAzureManager();
        WebApp webapp = null;
        for (Subscription sb : AzureMvpModel.getInstance().getSelectedSubscriptions()) {
            Azure azure = azureManager.getAzure(sb.subscriptionId());
            try {
                webapp = azure.webApps().getByResourceGroup(app.resourceGroup(), app.name());
            } catch (Exception e) {
            }
            if (webapp != null) {
                return updateApp(webapp, pr, imageName);
            }
        }
        throw new IOException("Cannot find such Web App on Linux in subscriptions.");
    }

    /**
     * List all Web App on Linux under all subscriptions.
     * 
     * @param update
     * @return list of Web App on Linux
     * @throws IOException
     * @throws CanceledByUserException
     */
    public static List<SiteInner> listAllWebAppOnLinux(boolean update) throws IOException, CanceledByUserException {
        List<SiteInner> wal = new ArrayList<SiteInner>();
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            return wal;
        }
        if (update) {
            AzureModelController.updateSubscriptionMaps(null);
        }
        AzureModel azureModel = AzureModel.getInstance();
        Map<SubscriptionDetail, List<ResourceGroup>> subscriptionToResourceGroupMap = azureModel
                .getSubscriptionToResourceGroupMap();
        for (SubscriptionDetail sb : subscriptionToResourceGroupMap.keySet()) {
            Azure azure = azureManager.getAzure(sb.getSubscriptionId());
            for (ResourceGroup rg : subscriptionToResourceGroupMap.get(sb)) {
                for (SiteInner si : azure.webApps().inner().listByResourceGroup(rg.name())) {
                    if (si.kind().equals("app,linux")) {
                        wal.add(si);
                    }
                }
            }
        }
        return wal;
    }

    // private helpers
    private static WebApp deploy(String subscriptionId, String resourceGroup, String appName,
            PrivateRegistry privateRegistry, String imageName, boolean createRgFlag) throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            return null;
        }
        Azure azure = azureManager.getAzure(subscriptionId);
        WebApp app = null;
        /**
         * java.util.NoSuchElementException: Sequence contains no elements
         * occurs when no available WebApp
         */
        try {
            app = azure.webApps().getByResourceGroup(resourceGroup, appName);
        } catch (NoSuchElementException e) {
            // DO NOTHING
        }
        if (app == null) {
            // create new app
            if (createRgFlag) {
                app = createAppWithNewResourceGroup(azure, appName, resourceGroup, privateRegistry, imageName);
            } else {
                app = createAppWithExisitingResourceGroup(azure, appName, resourceGroup, privateRegistry, imageName);
            }
        }
        if (app == null) {
            throw new IOException("Fail to create Web App on Linux.");
        } else {
            return app;
        }
    }

    private static WebApp createAppWithExisitingResourceGroup(Azure azure, String appName, String resourceGroup,
            PrivateRegistry pr, String imageName) {
        return azure.webApps().define(appName).withRegion(defaultRegion).withExistingResourceGroup(resourceGroup)
                .withNewLinuxPlan(defaultPricingTier)
                .withPrivateRegistryImage(String.format("%s/%s", pr.getUrl(), imageName), pr.getUrl())
                .withCredentials(pr.getUsername(), pr.getPassword()).withStartUpCommand(pr.getStartupFile())
                .withAppSettings(defaultAppSettings).create();

    }

    private static WebApp createAppWithNewResourceGroup(Azure azure, String appName, String resourceGroup,
            PrivateRegistry pr, String imageName) {
        return azure.webApps().define(appName).withRegion(defaultRegion).withNewResourceGroup(resourceGroup)
                .withNewLinuxPlan(defaultPricingTier)
                .withPrivateRegistryImage(String.format("%s/%s", pr.getUrl(), imageName), pr.getUrl())
                .withCredentials(pr.getUsername(), pr.getPassword()).withStartUpCommand(pr.getStartupFile())
                .withAppSettings(defaultAppSettings).create();
    }

    private static WebApp updateApp(WebApp app, PrivateRegistry pr, String imageName) {
        app.update().withPrivateRegistryImage(String.format("%s/%s", pr.getUrl(), imageName), pr.getUrl())
                .withCredentials(pr.getUsername(), pr.getPassword()).withStartUpCommand(pr.getStartupFile())
                .withAppSettings(defaultAppSettings).apply();
        return app;
    }

}
