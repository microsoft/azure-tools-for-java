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

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.azurecommons.util.FileUtil;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.webapp.Constants;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

public class WebAppRunState extends AzureRunProfileState<WebAppBase> {

    private static final String CREATE_WEBAPP = "Creating new Web App...";
    private static final String CREATE_DEPLOYMENT_SLOT = "Creating new Deployment Slot...";
    private static final String CREATE_FAILED = "Failed to create Web App. Error: %s ...";
    private static final String CREATE_SLOT_FAILED = "Failed to create Deployment Slot. Error: %s ...";
    private static final String GETTING_DEPLOYMENT_CREDENTIAL = "Getting Deployment Credential...";
    private static final String CONNECTING_FTP = "Connecting to FTP server...";
    private static final String UPLOADING_ARTIFACT = "Uploading artifact to: %s ...";
    private static final String UPLOADING_WEB_CONFIG = "Uploading web.config (check more details at: " +
        "https://aka.ms/spring-boot)...";
    private static final String UPLOADING_SUCCESSFUL = "Uploading successfully...";
    private static final String STOP_WEB_APP = "Stopping Web App...";
    private static final String STOP_DEPLOYMENT_SLOT = "Stopping Deployment Slot...";
    private static final String START_WEB_APP = "Starting Web App...";
    private static final String START_DEPLOYMENT_SLOT = "Starting Deployment Slot...";
    private static final String LOGGING_OUT = "Logging out of FTP server...";
    private static final String DEPLOY_SUCCESSFUL = "Deploy successfully!";
    private static final String STOP_DEPLOY = "Deploy Failed!";
    private static final String NO_WEB_APP = "Cannot get webapp for deploy.";
    private static final String NO_TARGET_FILE = "Cannot find target file: %s.";
    private static final String FAIL_FTP_STORE = "FTP client can't store the artifact, reply code: %s.";

    private static final String WEB_CONFIG_PACKAGE_PATH = "/webapp/web.config";
    private static final String BASE_PATH = "/site/wwwroot/";
    private static final String ROOT_PATH = BASE_PATH + "app.jar";
    private static final String WEB_CONFIG_FTP_PATH = "/site/wwwroot/web.config";
    private static final String WEB_APP_BASE_PATH = BASE_PATH + "webapps/";
    private static final String CONTAINER_ROOT_PATH = WEB_APP_BASE_PATH + "ROOT";
    private static final String TEMP_FILE_PREFIX = "azuretoolkit";

    private static final int SLEEP_TIME = 5000; // milliseconds
    private static final int UPLOADING_MAX_TRY = 3;

    private WebAppConfiguration webAppConfiguration;
    private final IntelliJWebAppSettingModel webAppSettingModel;

    /**
     * Place to execute the Web App deployment task.
     */
    public WebAppRunState(Project project, WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.webAppSettingModel = webAppConfiguration.getModel();
    }

    @Nullable
    @Override
    public WebAppBase executeSteps(@NotNull RunProcessHandler processHandler
        , @NotNull Map<String, String> telemetryMap) throws Exception {
        File file = new File(webAppSettingModel.getTargetPath());
        if (!file.exists()) {
            throw new FileNotFoundException(String.format(NO_TARGET_FILE, webAppSettingModel.getTargetPath()));
        }
        WebAppBase deployTarget = getDeployTargetByConfiguration(processHandler);
        processHandler.setText(isDeployToSlot() ? STOP_DEPLOYMENT_SLOT : STOP_WEB_APP);
        deployTarget.stop();

        int indexOfDot = webAppSettingModel.getTargetName().lastIndexOf(".");
        String fileName = webAppSettingModel.getTargetName().substring(0, indexOfDot);
        String fileType = webAppSettingModel.getTargetName().substring(indexOfDot + 1);

        switch (fileType) {
            case MavenConstants.TYPE_WAR:
                try (FileInputStream input = new FileInputStream(webAppSettingModel.getTargetPath())) {
                    uploadWarArtifact(input, deployTarget, fileName, processHandler, telemetryMap);
                }
                break;
            case MavenConstants.TYPE_JAR:
                try (FileInputStream input = new FileInputStream(webAppSettingModel.getTargetPath())) {
                    uploadJarArtifactViaFTP(input, deployTarget, processHandler, telemetryMap);
                }
                break;
            default:
                break;
        }

        processHandler.setText(isDeployToSlot() ? START_DEPLOYMENT_SLOT : START_WEB_APP);
        deployTarget.start();
        return deployTarget;
    }

    private boolean isDeployToSlot() {
        return !webAppSettingModel.isCreatingNew() && webAppSettingModel.isDeployToSlot();
    }

    private void openWebAppInBrowser(String url, RunProcessHandler processHandler) {
        try {
            Desktop.getDesktop().browse(new URL(url).toURI());
        } catch (Exception e) {
            processHandler.println(e.getMessage(), ProcessOutputTypes.STDERR);
        }
    }

    @Override
    protected void onSuccess(WebAppBase result, @NotNull RunProcessHandler processHandler) {
        if (webAppSettingModel.isCreatingNew() && AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
        }
        updateConfigurationDataModel(result);
        AzureWebAppMvpModel.getInstance().listAllWebApps(true /*force*/);

        int indexOfDot = webAppSettingModel.getTargetName().lastIndexOf(".");
        final String fileName = webAppSettingModel.getTargetName().substring(0, indexOfDot);
        final String fileType = webAppSettingModel.getTargetName().substring(indexOfDot + 1);
        final String url = getUrl(result, fileName, fileType);
        processHandler.setText(DEPLOY_SUCCESSFUL);
        processHandler.setText("URL: " + url);
        if (webAppSettingModel.isOpenBrowserAfterDeployment()) {
            openWebAppInBrowser(url, processHandler);
        }
        processHandler.notifyComplete();
    }

    @Override
    protected void onFail(@NotNull String errMsg, @NotNull RunProcessHandler processHandler) {
        processHandler.println(errMsg, ProcessOutputTypes.STDERR);
        processHandler.notifyComplete();
    }

    @Override
    protected String getDeployTarget() {
        return isDeployToSlot() ? "DeploymentSlot" : "WebApp";
    }

    @Override
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
        telemetryMap.put("SubscriptionId", webAppSettingModel.getSubscriptionId());
        telemetryMap.put("CreateNewApp", String.valueOf(webAppSettingModel.isCreatingNew()));
        telemetryMap.put("CreateNewSP", String.valueOf(webAppSettingModel.isCreatingAppServicePlan()));
        telemetryMap.put("CreateNewRGP", String.valueOf(webAppSettingModel.isCreatingResGrp()));
        telemetryMap.put("FileType", MavenRunTaskUtil.getFileType(webAppSettingModel.getTargetName()));
    }

    @NotNull
    private WebAppBase getDeployTargetByConfiguration(@NotNull RunProcessHandler processHandler) throws Exception {
        if (webAppSettingModel.isCreatingNew()) {
            return createWebApp(processHandler);
        }

        final WebApp webApp = AzureWebAppMvpModel.getInstance()
            .getWebAppById(webAppSettingModel.getSubscriptionId(), webAppSettingModel.getWebAppId());
        if (webApp == null) {
            processHandler.setText(STOP_DEPLOY);
            throw new Exception(NO_WEB_APP);
        }

        if (isDeployToSlot()) {
            if (webAppSettingModel.getSlotName() == Constants.CREATE_NEW_SLOT) {
                return createDeploymentSlot(processHandler);
            } else {
                return webApp.deploymentSlots().getByName(webAppSettingModel.getSlotName());
            }
        } else {
            return webApp;
        }
    }

    private WebApp createWebApp(@NotNull RunProcessHandler processHandler) throws Exception {
        processHandler.setText(CREATE_WEBAPP);
        try {
            return AzureWebAppMvpModel.getInstance().createWebApp(webAppSettingModel);
        } catch (Exception e) {
            processHandler.setText(STOP_DEPLOY);
            throw new Exception(String.format(CREATE_FAILED, e.getMessage()));
        }
    }

    private DeploymentSlot createDeploymentSlot(@NotNull RunProcessHandler processHandler) throws Exception {
        processHandler.setText(CREATE_DEPLOYMENT_SLOT);
        try {
            return AzureWebAppMvpModel.getInstance().createDeploymentSlot(webAppSettingModel);
        } catch (Exception e) {
            processHandler.setText(STOP_DEPLOY);
            throw new Exception(String.format(CREATE_SLOT_FAILED, e.getMessage()));
        }
    }

    private File prepareWebConfig() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(WEB_CONFIG_PACKAGE_PATH)) {
            final File tempFolder = Files.createTempDirectory(TEMP_FILE_PREFIX).toFile();
            final File tempWebConfigFile = new File(tempFolder.getPath() + "/web.config");
            Files.copy(inputStream, tempWebConfigFile.toPath());
            return tempWebConfigFile;
        }
    }

    private File prepareJarArtifact(@NotNull final String fileName, @NotNull final String artifactName) {
        final File originalJarFile = new File(fileName);
        final File artifact = new File(originalJarFile.getPath().replace(originalJarFile.getName(), artifactName));
        originalJarFile.renameTo(artifact);
        return artifact;
    }

    private void uploadWarArtifact(@NotNull final FileInputStream input, @NotNull final WebAppBase webApp,
                                   @NotNull final String fileName, @NotNull final RunProcessHandler processHandler,
                                   @NotNull final Map<String, String> telemetryMap) throws IOException {
        processHandler.setText(GETTING_DEPLOYMENT_CREDENTIAL);
        final PublishingProfile profile = webApp.getPublishingProfile();
        processHandler.setText(CONNECTING_FTP);
        final FTPClient ftp = WebAppUtils.getFtpConnection(profile);
        int uploadCount;

        // Workaround for Linux web apps, because unlike Windows ones, the webapps folder is not created in the
        // beginning and thus cause ftp failure with reply code 550 when deploy directly.
        // Issue https://github.com/Azure/azure-libraries-for-java/issues/584.
        if (webApp.operatingSystem() == OperatingSystem.LINUX) {
            ensureWebAppsFolderExist(ftp);
        }
        if (webAppSettingModel.isDeployToRoot()) {
            WebAppUtils.removeFtpDirectory(ftp, CONTAINER_ROOT_PATH, processHandler);
            processHandler.setText(String.format(UPLOADING_ARTIFACT, CONTAINER_ROOT_PATH + ".war"));
            uploadCount = uploadFileToFtp(ftp, CONTAINER_ROOT_PATH + ".war", input, processHandler);
        } else {
            WebAppUtils.removeFtpDirectory(ftp, WEB_APP_BASE_PATH + fileName, processHandler);
            processHandler.setText(String.format(UPLOADING_ARTIFACT,
                WEB_APP_BASE_PATH + webAppSettingModel.getTargetName()));
            uploadCount = uploadFileToFtp(ftp, WEB_APP_BASE_PATH + webAppSettingModel.getTargetName(),
                input, processHandler);
        }
        telemetryMap.put("artifactUploadCount", String.valueOf(uploadCount));

        processHandler.setText(LOGGING_OUT);
        ftp.logout();
        if (ftp.isConnected()) {
            ftp.disconnect();
        }
    }

    private void ensureWebAppsFolderExist(@NotNull FTPClient ftp) {
        try {
            ftp.getStatus(WEB_APP_BASE_PATH);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.makeDirectory(WEB_APP_BASE_PATH);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadJarArtifact(@NotNull String fileName, @NotNull WebAppBase webApp,
                                   @NotNull RunProcessHandler processHandler,
                                   @NotNull Map<String, String> telemetryMap) throws Exception {
        final File targetZipFile = File.createTempFile(TEMP_FILE_PREFIX, ".zip");
        // Java SE web app needs the artifact named app.jar
        final String artifactName = Constants.LINUX_JAVA_SE_RUNTIME.equalsIgnoreCase(webApp.linuxFxVersion())
            ? "app.jar" : "ROOT.jar";
        final File jarArtifact = prepareJarArtifact(fileName, artifactName);

        final File[] files;
        if (webApp.operatingSystem() == OperatingSystem.WINDOWS) {
            files = new File[]{jarArtifact, prepareWebConfig()};
        } else {
            files = new File[]{jarArtifact};
        }

        FileUtil.zipFiles(files, targetZipFile);
        processHandler.setText(String.format(UPLOADING_ARTIFACT, BASE_PATH + artifactName));
        final int uploadCount = uploadFileViaZipDeploy(webApp, targetZipFile, processHandler);
        telemetryMap.put("artifactUploadCount", String.valueOf(uploadCount));
    }

    private void uploadJarArtifactViaFTP(@NotNull final FileInputStream input, @NotNull WebAppBase webApp,
                                         @NotNull RunProcessHandler processHandler,
                                         @NotNull Map<String, String> telemetryMap) throws Exception {
        processHandler.setText(GETTING_DEPLOYMENT_CREDENTIAL);
        PublishingProfile profile = webApp.getPublishingProfile();

        processHandler.setText(CONNECTING_FTP);
        FTPClient ftp = WebAppUtils.getFtpConnection(profile);

        if (webApp.operatingSystem() == OperatingSystem.WINDOWS) {
            processHandler.setText(UPLOADING_WEB_CONFIG);
            try (InputStream webConfigInput = getClass().getResourceAsStream(WEB_CONFIG_PACKAGE_PATH)) {
                int webConfigUploadCount = uploadFileToFtp(ftp, WEB_CONFIG_FTP_PATH, webConfigInput, processHandler);
                telemetryMap.put("webConfigCount", String.valueOf(webConfigUploadCount));
            }
        }

        processHandler.setText(String.format(UPLOADING_ARTIFACT, ROOT_PATH));
        final int uploadCount = uploadFileToFtp(ftp, ROOT_PATH, input, processHandler);
        telemetryMap.put("artifactUploadCount", String.valueOf(uploadCount));
    }

    // Add retry logic here to avoid Kudu's socket timeout issue.
    // For each try, the method will wait 5 seconds.
    // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
    private int uploadFileViaZipDeploy(@NotNull WebAppBase webapp, @NotNull File zipFile,
                                       @NotNull RunProcessHandler processHandler) throws Exception {
        int uploadCount = 0;
        while (uploadCount < UPLOADING_MAX_TRY) {
            uploadCount += 1;
            try {
                Thread.sleep(SLEEP_TIME);
                webapp.zipDeploy(zipFile);
                processHandler.setText(UPLOADING_SUCCESSFUL);
                return uploadCount;
            } catch (Exception e) {
                processHandler.setText(
                    String.format("Upload file via zip deploy met exception: %s, retry immediately...",
                        e.getMessage()));
            }
        }
        throw new Exception(String.format("Upload failed after %d times of retry.", UPLOADING_MAX_TRY));
    }

    /**
     * when upload file to FTP, the plugin will retry 3 times in case of unexpected errors.
     */
    private int uploadFileToFtp(@NotNull FTPClient ftp, @NotNull String path,
                                @NotNull InputStream stream, RunProcessHandler processHandler) throws IOException {
        int retry = UPLOADING_MAX_TRY;
        while (retry > 0) {
            try {
                retry -= 1;
                if (ftp.storeFile(path, stream)) {
                    processHandler.setText(UPLOADING_SUCCESSFUL);
                    return UPLOADING_MAX_TRY - retry;
                }
            } catch (IOException e) {
                // swallow exception
            }
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                // swallow exception
            }
        }
        throw new IOException(String.format(FAIL_FTP_STORE, ftp.getReplyCode()));
    }

    @NotNull
    private String getUrl(@NotNull WebAppBase webApp, @NotNull String fileName, @NotNull String fileType) {
        String url = "https://" + webApp.defaultHostName();
        if (Comparing.equal(fileType, MavenConstants.TYPE_WAR)
            && !webAppSettingModel.isDeployToRoot()) {
            url += "/" + fileName;
        }
        return url;
    }

    private void updateConfigurationDataModel(@NotNull WebAppBase app) {
        webAppSettingModel.setCreatingNew(false);
        // todo: add flag to indicate create new slot or not
        if (app instanceof DeploymentSlot) {
            webAppSettingModel.setSlotName(app.name());
            webAppSettingModel.setNewSlotConfigurationSource(Constants.DO_NOT_CLONE_SLOT_CONFIGURATION);
            webAppSettingModel.setNewSlotName("");
            webAppSettingModel.setWebAppId(((DeploymentSlot) app).parent().id());
        } else {
            webAppSettingModel.setWebAppId(app.id());
        }
        webAppSettingModel.setWebAppName("");
        webAppSettingModel.setResourceGroup("");
        webAppSettingModel.setAppServicePlanName("");
    }
}
