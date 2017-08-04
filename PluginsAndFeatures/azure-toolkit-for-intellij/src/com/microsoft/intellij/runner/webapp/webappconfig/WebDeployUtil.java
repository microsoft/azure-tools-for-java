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

import com.intellij.execution.process.ProcessOutputTypes;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.IProgressIndicator;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.RunProcessHandler;

import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;

import java.io.FileInputStream;
import java.io.IOException;

import rx.Observable;

public class WebDeployUtil {

    private static final String GETTING_DEPLOYMENT_CREDENTIAL = "Getting Deployment Credential...";
    private static final String CONNECTING_FTP = "Connecting to FTP server...";
    private static final String UPLOADING_WAR = "Uploading war file...";
    private static final String UPLOADING_SUCCESSFUL = "Uploading successfully...";
    private static final String LOGGING_OUT = "Logging out of FTP server...";
    private static final String DEPLOY_SUCCESSFUL = "Deploy successfully...";
    private static final String CREATE_WEBAPP = "Creating new WebApp...";
    private static final String CREATE_SUCCESSFUL = "WebApp created...";
    private static final String DEPLOY_JDK = "Deploying custom JDK...";
    private static final String JDK_SUCCESSFUL = "Custom JDK deployed successfully...";

    private static final String BASE_PATH = "/site/wwwroot/webapps/";
    private static final String ROOT_PATH = BASE_PATH + "ROOT";
    private static final String ROOT_FILE_PATH = ROOT_PATH + "." + MavenConstants.TYPE_WAR;

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

        WebApp.DefinitionStages.WithCreate withCreate =  null;
        if (model.isCreatingAppServicePlan()) {
            withCreate = withCreateNewSPlan(azure, model);
        } else {
            withCreate = withCreateExistingSPlan(azure, model);
        }

        WebApp webApp = null;
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
        WebApp.DefinitionStages.WithCreate withCreate = null;
        String[] tierSize = model.getPricing().split("_");
        if (tierSize == null || tierSize.length != 2) {
            throw new Exception("Cannot get valid price tier");
        }
        PricingTier pricing = new PricingTier(tierSize[0], tierSize[1]);
        if (model.isCreatingResGrp()) {
            withCreate = azure.webApps().define(model.getWebAppName())
                    .withRegion(model.getRegion())
                    .withNewResourceGroup(model.getResourceGroup())
                    .withNewWindowsPlan(pricing);
        } else {
            withCreate = azure.webApps().define(model.getWebAppName())
                    .withRegion(model.getRegion())
                    .withExistingResourceGroup(model.getResourceGroup())
                    .withNewWindowsPlan(pricing);
        }
        return withCreate;
    }

    private static WebApp.DefinitionStages.WithCreate withCreateExistingSPlan(
            @NotNull Azure azure,
            @NotNull WebAppSettingModel model
    ) throws Exception {
        AppServicePlan servicePlan =  azure.appServices().appServicePlans().getById(model.getAppServicePlan());
        WebApp.DefinitionStages.WithCreate withCreate = null;
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

    private static WebApp createWebAppWithMsg(
            WebAppSettingModel webAppSettingModel, IProgressIndicator handler) throws Exception {
        handler.setText(CREATE_WEBAPP);
        WebApp webApp = createWebApp(webAppSettingModel);
        if (null == webApp) {
            return null;
        }
        if (!WebAppSettingModel.JdkChoice.DEFAULT.toString().equals(webAppSettingModel.getJdkChoice())) {
            handler.setText(DEPLOY_JDK);
            //TODO: wrap a new message to replace deployCustomJdk
            WebAppUtils.deployCustomJdk(webApp, webAppSettingModel.getJdkUrl(),
                    WebContainer.fromString(webAppSettingModel.getWebContainer()),
                    handler);
            handler.setText(JDK_SUCCESSFUL);
        }
        handler.setText(CREATE_SUCCESSFUL);

        return webApp;
    }

    public static void deployWebApp(WebAppSettingModel webAppSettingModel, IProgressIndicator handler) {
        Observable.fromCallable(() -> {
            WebApp webApp = null;
            if (webAppSettingModel.isCreatingNew()) {
                webApp = createWebAppWithMsg(webAppSettingModel, handler);
            } else {
                webApp = AzureWebAppMvpModel.getInstance()
                        .getWebAppById(webAppSettingModel.getSubscriptionId(), webAppSettingModel.getWebAppId());
            }

            if (webApp == null) {
                throw new Exception("Cannot get webapp for deploy");
            }

            handler.setText(GETTING_DEPLOYMENT_CREDENTIAL);
            FTPClient ftp;
            handler.setText(CONNECTING_FTP);
            ftp = WebAppUtils.getFtpConnection(webApp.getPublishingProfile());
            handler.setText(UPLOADING_WAR);
            FileInputStream input = new FileInputStream(webAppSettingModel.getTargetPath());
            boolean isSuccess;
            if (webAppSettingModel.isDeployToRoot()) {
                // Deploy to Root
                WebAppUtils.removeFtpDirectory(ftp, ROOT_PATH, null);
                isSuccess = ftp.storeFile(ROOT_FILE_PATH, input);
            } else {
                //Deploy according to war file name
                WebAppUtils.removeFtpDirectory(ftp, BASE_PATH + webAppSettingModel.getTargetName(), handler);
                isSuccess = ftp.storeFile(BASE_PATH + webAppSettingModel.getTargetName(), input);
            }
            if (!isSuccess) {
                int rc = ftp.getReplyCode();
                throw new IOException("FTP client can't store the artifact, reply code: " + rc);
            }
            handler.setText(UPLOADING_SUCCESSFUL);
            handler.setText(LOGGING_OUT);
            ftp.logout();
            input.close();
            if (ftp.isConnected()) {
                ftp.disconnect();
            }
            return true;
        })
        .subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io())
        .subscribe(isSucceeded -> {
            handler.setText(DEPLOY_SUCCESSFUL);
            handler.setText("URL: ");
            String url = webAppSettingModel.getWebAppUrl();
            if (!webAppSettingModel.isDeployToRoot()) {
                url += "/" + webAppSettingModel.getTargetName()
                        .substring(0, webAppSettingModel.getTargetName().indexOf("." + MavenConstants.TYPE_WAR));
            }
            handler.setText(url);
            handler.notifyComplete();
        }, err -> {
            handler.setText(err.getMessage());
            handler.notifyComplete();
        });
    }
}
