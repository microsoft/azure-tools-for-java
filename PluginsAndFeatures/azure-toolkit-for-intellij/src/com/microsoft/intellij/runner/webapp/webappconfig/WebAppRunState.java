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
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.idea.maven.model.MavenConstants;

import rx.Observable;

public class WebAppRunState implements RunProfileState {

    private static final String CREATE_WEBAPP = "Creating new WebApp...";
    private static final String CREATE_FAILED = "Failed to create WebApp. Error: %s ...";
    private static final String GETTING_DEPLOYMENT_CREDENTIAL = "Getting Deployment Credential...";
    private static final String CONNECTING_FTP = "Connecting to FTP server...";
    private static final String UPLOADING_ARTIFACT = "Uploading artifact to: %s ...";
    private static final String UPLOADING_WEB_CONFIG = "Uploading web.config (check more details at: https://aka.ms/spring-boot)...";
    private static final String UPLOADING_SUCCESSFUL = "Uploading successfully...";
    private static final String LOGGING_OUT = "Logging out of FTP server...";
    private static final String DEPLOY_SUCCESSFUL = "Deploy successfully!";
    private static final String STOP_DEPLOY = "Deploy Failed!";
    private static final String NO_WEB_APP = "Cannot get webapp for deploy.";
    private static final String NO_TARGET_FILE = "Cannot find target file: %s.";
    private static final String FAIL_FTP_STORE = "FTP client can't store the artifact, reply code: %s.";

    private static final String WEB_CONFIG_PACKAGE_PATH = "/webapp/web.config";
    private static final String BASE_PATH = "/site/wwwroot/";
    private static final String WEB_APP_BASE_PATH = BASE_PATH + "webapps/";
    private static final String WEB_CONFIG_FTP_PATH = "/site/wwwroot/web.config";
    private static final String ROOT_PATH = BASE_PATH + "ROOT";
    private static final String CONTAINER_ROOT_PATH = WEB_APP_BASE_PATH + "ROOT";

    private final Project project;
    private final WebAppSettingModel webAppSettingModel;
    private final RunProcessHandler processHandler;

    /**
     * Place to execute the Web App deployment task.
     */
    public WebAppRunState(Project project, WebAppSettingModel webAppSettingModel) {
        this.project = project;
        this.webAppSettingModel = webAppSettingModel;
        this.processHandler = new RunProcessHandler();
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        processHandler.addDefaultListener();
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);
        Observable.fromCallable(() -> {
            File file = new File(webAppSettingModel.getTargetPath());
            if (!file.exists()) {
                throw new FileNotFoundException(String.format(NO_TARGET_FILE, webAppSettingModel.getTargetPath()));
            }
            WebApp webApp = getWebAppAccordingToConfiguration();

            processHandler.setText(GETTING_DEPLOYMENT_CREDENTIAL);
            PublishingProfile profile = webApp.getPublishingProfile();

            processHandler.setText(CONNECTING_FTP);
            FTPClient ftp = WebAppUtils.getFtpConnection(profile);

            int indexOfDot = webAppSettingModel.getTargetName().lastIndexOf(".");
            String fileName = webAppSettingModel.getTargetName().substring(0, indexOfDot);
            String fileType = webAppSettingModel.getTargetName().substring(indexOfDot + 1);

            webApp.stop();
            uploadWebConfigFile(ftp, fileType);

            try (FileInputStream input = new FileInputStream(webAppSettingModel.getTargetPath())) {
                uploadArtifact(input, webApp, ftp, fileName, fileType);
            }
            webApp.start();

            processHandler.setText(LOGGING_OUT);
            ftp.logout();
            if (ftp.isConnected()) {
                ftp.disconnect();
            }

            String url = getUrl(webApp, fileName, fileType);
            processHandler.setText(DEPLOY_SUCCESSFUL);
            processHandler.setText("URL: " + url);
            return webApp;

        }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
                webApp -> {
                    processHandler.notifyComplete();
                    if (webAppSettingModel.isCreatingNew() && AzureUIRefreshCore.listeners != null) {
                        try {
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
                    processHandler.println(err.getMessage(), ProcessOutputTypes.STDERR);
                    processHandler.notifyComplete();
                    sendTelemetry(false, err.getMessage());
                }
        );
        return new DefaultExecutionResult(consoleView, processHandler);
    }

    @NotNull
    private WebApp getWebAppAccordingToConfiguration() throws Exception {
        WebApp webApp;
        if (webAppSettingModel.isCreatingNew()) {
            processHandler.setText(CREATE_WEBAPP);
            try {
                webApp = AzureWebAppMvpModel.getInstance().createWebApp(webAppSettingModel);
            } catch (Exception e) {
                processHandler.setText(STOP_DEPLOY);
                throw new Exception(String.format(CREATE_FAILED, e.getMessage()));
            }
        } else {
            webApp = AzureWebAppMvpModel.getInstance()
                    .getWebAppById(webAppSettingModel.getSubscriptionId(), webAppSettingModel.getWebAppId());
        }
        if (webApp == null) {
            processHandler.setText(STOP_DEPLOY);
            throw new Exception(NO_WEB_APP);
        }
        return webApp;
    }

    private void uploadWebConfigFile(@NotNull FTPClient ftp, @NotNull String fileType) throws IOException {
        if (webAppSettingModel.isCreatingNew() && Comparing.equal(fileType, MavenConstants.TYPE_JAR)) {
            processHandler.setText(UPLOADING_WEB_CONFIG);
            try (InputStream webConfigInput = getClass()
                    .getResourceAsStream(WEB_CONFIG_PACKAGE_PATH)) {
                uploadFileToFtp(ftp, WEB_CONFIG_FTP_PATH, webConfigInput);
            }
        }
    }

    private void uploadArtifact(@NotNull FileInputStream input, @NotNull WebApp webApp, @NotNull FTPClient ftp,
                                @NotNull String fileName, @NotNull String fileType)
            throws IOException {
        switch (fileType) {
            case MavenConstants.TYPE_WAR:
                if (webAppSettingModel.isDeployToRoot()) {
                    WebAppUtils.removeFtpDirectory(ftp, CONTAINER_ROOT_PATH, processHandler);
                    processHandler.setText(String.format(UPLOADING_ARTIFACT, CONTAINER_ROOT_PATH + "." + fileType));
                    uploadFileToFtp(ftp, CONTAINER_ROOT_PATH + "." + fileType, input);
                } else {
                    WebAppUtils.removeFtpDirectory(ftp, WEB_APP_BASE_PATH + fileName, processHandler);
                    processHandler.setText(String.format(UPLOADING_ARTIFACT,
                            WEB_APP_BASE_PATH + webAppSettingModel.getTargetName()));
                    uploadFileToFtp(ftp, WEB_APP_BASE_PATH + webAppSettingModel.getTargetName(), input);
                }
                break;
            case MavenConstants.TYPE_JAR:
                processHandler.setText(String.format(UPLOADING_ARTIFACT, ROOT_PATH + "." + fileType));
                uploadFileToFtp(ftp, ROOT_PATH + "." + fileType, input);
                break;
            default:
                break;
        }
    }

    private void uploadFileToFtp(@NotNull FTPClient ftp, @NotNull String path,
                                 @NotNull InputStream stream) throws IOException {
        if (!ftp.storeFile(path, stream)) {
            int rc = ftp.getReplyCode();
            processHandler.setText(String.format(FAIL_FTP_STORE, rc));
            throw new IOException(String.format(FAIL_FTP_STORE, rc));
        }
        processHandler.setText(UPLOADING_SUCCESSFUL);
    }

    @NotNull
    private String getUrl(@NotNull WebApp webApp, @NotNull String fileName, @NotNull String fileType) {
        String url = "https://" + webApp.defaultHostName();
        if (Comparing.equal(fileType, MavenConstants.TYPE_WAR)
                && !webAppSettingModel.isDeployToRoot()) {
            url += "/" + fileName;
        }
        return url;
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
