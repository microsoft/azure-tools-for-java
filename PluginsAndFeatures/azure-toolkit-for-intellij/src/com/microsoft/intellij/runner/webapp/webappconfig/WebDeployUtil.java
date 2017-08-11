/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.webapp.webappconfig;

import com.microsoft.azure.management.Azure;

import com.microsoft.azure.management.appservice.*;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.IProgressIndicator;
import com.microsoft.azuretools.utils.WebAppUtils;

import org.jetbrains.annotations.NotNull;

public class WebDeployUtil {

    private static final String CREATE_WEBAPP = "Creating new WebApp...";
    private static final String CREATE_SUCCESSFUL = "WebApp created...";
    private static final String CREATE_FALIED = "Failed to create WebApp. Error: %s ...";
    private static final String DEPLOY_JDK = "Deploying custom JDK...";
    private static final String JDK_SUCCESSFUL = "Custom JDK deployed successfully...";
    private static final String JDK_FAILED = "Failed to deploy custom JDK";

    private static WebApp createWebApp(@NotNull WebAppSettingModel model) throws Exception {
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
        AppServicePlan servicePlan =  azure.appServices().appServicePlans().getById(model.getAppServicePlanId());
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

    public static WebApp createWebAppWithMsg(
            WebAppSettingModel webAppSettingModel, IProgressIndicator handler) throws Exception {
        handler.setText(CREATE_WEBAPP);
        WebApp webApp = null;
        try {
            webApp = createWebApp(webAppSettingModel);
        } catch (Exception e) {
            handler.setText(String.format(CREATE_FALIED, e.getMessage()));
            throw new Exception(e);
        }

        if (!WebAppSettingModel.JdkChoice.DEFAULT.toString().equals(webAppSettingModel.getJdkChoice())) {
            handler.setText(DEPLOY_JDK);
            try {
                WebAppUtils.deployCustomJdk(webApp, webAppSettingModel.getJdkUrl(),
                        WebContainer.fromString(webAppSettingModel.getWebContainer()),
                        handler);
                handler.setText(JDK_SUCCESSFUL);
            } catch (Exception e) {
                handler.setText(JDK_FAILED);
                throw new Exception(e);
            }
        }
        handler.setText(CREATE_SUCCESSFUL);

        return webApp;
    }
}
