/*
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

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.RunProcessHandler;

import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;

public class WebAppRunState implements RunProfileState {

    private static final String GETTING_DEPLOYMENT_CREDENTIAL = "Getting Deployment Credential...";
    private static final String CONNECTING_FTP = "Connecting to FTP server...";
    private static final String UPLOADING_WAR = "Uploading war file...";
    private static final String UPLOADING_SUCCESSFUL = "Uploading successfully...";
    private static final String LOGGING_OUT = "Logging out of FTP server...";
    private static final String DEPLOY_SUCCESSFUL = "Deploy successfully...";
    private static final String STOP_DEPLOY = "Deploy Failed.";
    private static final String NO_WEBAPP = "Cannot get webapp for deploy";
    private static final String NO_TARGETFILE = "Cannot find target file: %s";
    private static final String FAIL_FTPSTORE = "FTP client can't store the artifact, reply code: %s";
    private static final String BASE_PATH = "/site/wwwroot/webapps/";
    private static final String ROOT_PATH = BASE_PATH + "ROOT";
    private static final String ROOT_FILE_PATH = ROOT_PATH + "." + MavenConstants.TYPE_WAR;
    private final Project project;
    private final WebAppSettingModel webAppSettingModel;

    public WebAppRunState(Project project, WebAppSettingModel webAppSettingModel) {
        this.project = project;
        this.webAppSettingModel = webAppSettingModel;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        final RunProcessHandler processHandler = new RunProcessHandler();
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);
        Observable.fromCallable(() -> {
            File file = new File(webAppSettingModel.getTargetPath());
            FileInputStream input;
            if (file.exists()) {
                input = new FileInputStream(webAppSettingModel.getTargetPath());
            } else {
                throw new FileNotFoundException(String.format(NO_TARGETFILE, webAppSettingModel.getTargetPath()));
            }
            WebApp webApp;
            if (webAppSettingModel.isCreatingNew()) {
                try {
                    webApp = WebDeployUtil.createWebAppWithMsg(webAppSettingModel, processHandler);
                } catch (Exception e) {
                    processHandler.setText(STOP_DEPLOY);
                    throw new Exception("Cannot create webapp for deploy");
                }
            } else {
                webApp = AzureWebAppMvpModel.getInstance()
                        .getWebAppById(webAppSettingModel.getSubscriptionId(), webAppSettingModel.getWebAppId());
            }

            if (webApp == null) {
                processHandler.setText(NO_WEBAPP);
                processHandler.setText(STOP_DEPLOY);
                throw new Exception(NO_WEBAPP);
            }
            processHandler.setText(GETTING_DEPLOYMENT_CREDENTIAL);
            FTPClient ftp;
            processHandler.setText(CONNECTING_FTP);
            ftp = WebAppUtils.getFtpConnection(webApp.getPublishingProfile());
            processHandler.setText(UPLOADING_WAR);
            String url = "https://" + webApp.defaultHostName();
            boolean isSuccess;
            if (webAppSettingModel.isDeployToRoot()) {
                // Deploy to Root
                WebAppUtils.removeFtpDirectory(ftp, ROOT_PATH, processHandler);
                isSuccess = ftp.storeFile(ROOT_FILE_PATH, input);
            } else {
                //Deploy according to war file name
                String subDirectoryName = webAppSettingModel.getTargetName().substring(0,
                        webAppSettingModel.getTargetName().lastIndexOf("."));
                url += "/" + subDirectoryName;
                WebAppUtils.removeFtpDirectory(ftp, BASE_PATH + subDirectoryName, processHandler);
                isSuccess = ftp.storeFile(BASE_PATH + webAppSettingModel.getTargetName(), input);
            }
            if (!isSuccess) {
                int rc = ftp.getReplyCode();
                processHandler.setText(String.format(FAIL_FTPSTORE, rc));
                throw new IOException(String.format(FAIL_FTPSTORE, rc));
            }
            processHandler.setText(UPLOADING_SUCCESSFUL);
            processHandler.setText(LOGGING_OUT);
            ftp.logout();
            input.close();
            if (ftp.isConnected()) {
                ftp.disconnect();
            }
            processHandler.setText(DEPLOY_SUCCESSFUL);
            processHandler.setText("URL: " + url);
            return webApp;
        }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
                webApp -> {
                    processHandler.notifyComplete();
                    if (webAppSettingModel.isCreatingNew() && AzureUIRefreshCore.listeners != null) {
                        try {
                            ResourceGroup resourceGroup = AzureMvpModel.getInstance()
                                    .getResourceGroupBySubscriptionIdAndName(webAppSettingModel.getSubscriptionId(),
                                            webAppSettingModel.getResourceGroup());
                            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH,
                                    null));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    updateConfigurationDataModel(webApp);
                    AzureWebAppMvpModel.getInstance().listWebApps(true /*force*/);
                    sendTelemetry(true, null);
                }, err -> {
                    processHandler.setText(err.getMessage());
                    processHandler.notifyComplete();
                    sendTelemetry(false, err.getMessage());
                }
        );
        return new DefaultExecutionResult(consoleView, processHandler);
    }

    private void updateConfigurationDataModel(@NotNull WebApp app) {
        webAppSettingModel.setCreatingNew(false);
        webAppSettingModel.setWebAppId(app.id());
        webAppSettingModel.setWebAppName("");
        webAppSettingModel.setResourceGroup("");
        webAppSettingModel.setAppServicePlanName("");
    }

    // TODO: refactor later
    private void sendTelemetry(boolean success, @Nullable String errorMsg) {
        Map<String, String> map = new HashMap<>();
        map.put("SubscriptionId", webAppSettingModel.getSubscriptionId());
        map.put("CreateNewApp", String.valueOf(webAppSettingModel.isCreatingNew()));
        map.put("CreateNewSP", String.valueOf(webAppSettingModel.isCreatingAppServicePlan()));
        map.put("CreateNewRGP", String.valueOf(webAppSettingModel.isCreatingResGrp()));
        map.put("Success", String.valueOf(success));
        if (!success) {
            map.put("ErrorMsg", errorMsg);
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "Webapp", "Deploy", map);
    }
}
