/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.IJavaAgentSupported;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.Constants;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppModule;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.RunProcessHandler;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WebAppRunState extends AzureRunProfileState<WebAppBase<?, ?, ?>> {
    private static final String LIBS_ROOT = "/home/site/wwwroot/libs/";
    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String CATALINA_OPTS = "CATALINA_OPTS";
    private File artifact;
    private final WebAppConfiguration webAppConfiguration;
    private final IntelliJWebAppSettingModel webAppSettingModel;

    private final Map<String, String> appSettingsForResourceConnection = new HashMap<>();

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
    @AzureOperation(name = "webapp.deploy_artifact.app", params = {"this.webAppConfiguration.getWebAppName()"}, type = AzureOperation.Type.ACTION)
    public WebAppBase<?, ?, ?> executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception {
        final RunProcessHandlerMessenger messenger = new RunProcessHandlerMessenger(processHandler);
        OperationContext.current().setMessager(messenger);
        artifact = new File(getTargetPath());
        if (!artifact.exists()) {
            throw new FileNotFoundException(message("webapp.deploy.error.noTargetFile", artifact.getAbsolutePath()));
        }
        final WebAppBase<?, ?, ?> deployTarget = getOrCreateDeployTargetFromAppSettingModel(processHandler);
        applyResourceConnection(deployTarget, processHandler);
        updateApplicationSettings(deployTarget, processHandler);
        AzureWebAppMvpModel.getInstance().deployArtifactsToWebApp(deployTarget, artifact, webAppSettingModel.isDeployToRoot(), processHandler);
        return deployTarget;
    }

    private void applyResourceConnection(@Nonnull WebAppBase<?, ?, ?> deployTarget, RunProcessHandler processHandler) {
        final Connection<?, ?> connection = webAppConfiguration.getConnection();
        if (Objects.nonNull(connection)) {
            Optional.ofNullable(connection.getEnvironmentVariables(this.project)).ifPresent(appSettingsForResourceConnection::putAll);
        }
        if (Objects.nonNull(connection) && connection.getResource().getDefinition() instanceof IJavaAgentSupported) {
            uploadJavaAgent(deployTarget, ((IJavaAgentSupported) connection.getResource().getDefinition()).getJavaAgent());
        }
    }

    private void uploadJavaAgent(@Nonnull WebAppBase<?, ?, ?> deployTarget, @Nullable File javaAgent) {
        if (javaAgent == null || !javaAgent.exists()) {
            return;
        }
        final String targetPath = PathUtil.toSystemIndependentName(Paths.get(LIBS_ROOT, javaAgent.getName()).toString());
        deployJavaAgentToAppService(deployTarget, javaAgent, targetPath);
        updateAppServiceVMOptions(deployTarget, targetPath);
    }

    private void deployJavaAgentToAppService(WebAppBase<?, ?, ?> deployTarget, File javaAgent, String targetPath) {
        AppServiceFile file;
        try {
            file = deployTarget.getFileByPath(targetPath);
        } catch (final RuntimeException e) {
            file = null;
        }
        if (file == null) {
            AzureMessager.getMessager().info(AzureString.format("Uploading java agent to web app %s", deployTarget.getName()));
            deployTarget.deploy(DeployType.STATIC, javaAgent, targetPath);
        } else {
            AzureMessager.getMessager().info(AzureString.format("Skip upload java agent as file with same name already exists"));
        }
    }

    private void updateAppServiceVMOptions(WebAppBase<?, ?, ?> deployTarget, String targetPath) {
        final Map<String, String> applicationSettings = webAppConfiguration.getApplicationSettings();
        final WebContainer webContainer = Objects.requireNonNull(deployTarget.getRuntime()).getWebContainer();
        final String javaOptsParameter = (webContainer == WebContainer.JAVA_SE || webContainer == WebContainer.JBOSS_7) ? JAVA_OPTS : CATALINA_OPTS;
        final String javaOpts = Optional.ofNullable(webAppConfiguration.getApplicationSettings())
                .map(settings -> settings.get(javaOptsParameter)).orElse(StringUtils.EMPTY);
        final String javaAgentValue = String.format("-javaagent:%s", targetPath);
        if (StringUtils.contains(javaOpts, javaAgentValue)) {
            return;
        }
        final String value = StringUtils.isEmpty(javaOpts) ? javaAgentValue : javaOpts + " " + javaAgentValue;
        appSettingsForResourceConnection.put(javaOptsParameter, value);
    }

    private void updateApplicationSettings(WebAppBase<?, ?, ?> deployTarget, RunProcessHandler processHandler) {
        final Map<String, String> applicationSettings = new HashMap<>(ObjectUtils.firstNonNull(webAppConfiguration.getApplicationSettings(), Collections.emptyMap()));
        final Set<String> appSettingsToRemove = webAppConfiguration.isCreatingNew() ? Collections.emptySet() : getAppSettingsToRemove(deployTarget, applicationSettings);
        applicationSettings.putAll(appSettingsForResourceConnection);
        if (MapUtils.isEmpty(applicationSettings)) {
            return;
        }
        if (deployTarget instanceof WebApp) {
            processHandler.setText("Updating application settings...");
            final WebAppDraft draft = (WebAppDraft) deployTarget.update();
            Optional.ofNullable(appSettingsToRemove).ifPresent(keys -> keys.forEach(draft::removeAppSetting));
            draft.setAppSettings(applicationSettings);
            draft.updateIfExist();
            processHandler.setText("Update application settings successfully.");
        } else if (deployTarget instanceof WebAppDeploymentSlot) {
            processHandler.setText("Updating deployment slot application settings...");
            final WebAppDeploymentSlotDraft update = (WebAppDeploymentSlotDraft) deployTarget.update();
            update.setAppSettings(applicationSettings);
            Optional.ofNullable(appSettingsToRemove).ifPresent(keys -> keys.forEach(update::removeAppSetting));
            update.updateIfExist();
            processHandler.setText("Update deployment slot application settings successfully.");
        }
    }

    private Set<String> getAppSettingsToRemove(final WebAppBase<?, ?, ?> target, final Map<String, String> applicationSettings) {
        return target.getAppSettings().keySet().stream()
                .filter(key -> !applicationSettings.containsKey(key))
                .collect(Collectors.toSet());
    }

    private boolean isDeployToSlot() {
        return !webAppSettingModel.isCreatingNew() && webAppSettingModel.isDeployToSlot();
    }

    private void openWebAppInBrowser(String url, RunProcessHandler processHandler) {
        try {
            Desktop.getDesktop().browse(new URL(url).toURI());
        } catch (final IOException | URISyntaxException e) {
            processHandler.println(e.getMessage(), ProcessOutputTypes.STDERR);
        }
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP);
    }

    @Override
    @AzureOperation(name = "webapp.open_public_url.app", type = AzureOperation.Type.ACTION)
    protected void onSuccess(WebAppBase<?, ?, ?> result, @NotNull RunProcessHandler processHandler) {
        updateConfigurationDataModel(result);
        final String fileName = FileNameUtils.getBaseName(artifact.getName());
        final String fileType = FileNameUtils.getExtension(artifact.getName());
        final String url = getUrl(result, fileName, fileType);
        processHandler.setText(message("appService.deploy.hint.succeed"));
        processHandler.setText("URL: " + url);
        if (webAppSettingModel.isOpenBrowserAfterDeployment()) {
            openWebAppInBrowser(url, processHandler);
        }
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("artifactType", webAppConfiguration.getAzureArtifactType() == null ? null : webAppConfiguration.getAzureArtifactType().name());
        properties.putAll(webAppSettingModel.getTelemetryProperties(Collections.EMPTY_MAP));
        return properties;
    }

    @NotNull
    private WebAppBase<?, ?, ?> getOrCreateDeployTargetFromAppSettingModel(@NotNull RunProcessHandler processHandler) throws Exception {
        final AzureAppService azureAppService = Azure.az(AzureWebApp.class);
        final WebApp webApp = getOrCreateWebappFromAppSettingModel(azureAppService, processHandler);
        if (!isDeployToSlot()) {
            return webApp;
        }
        // todo: add new boolean indicator instead of comparing string values
        if (StringUtils.equals(webAppSettingModel.getSlotName(), Constants.CREATE_NEW_SLOT)) {
            return createDeploymentSlot(webApp, processHandler);
        } else {
            return Objects.requireNonNull(webApp.slots().get(webAppSettingModel.getSlotName(), webAppSettingModel.getResourceGroup()));
        }
    }

    private WebApp getOrCreateWebappFromAppSettingModel(AzureAppService azureAppService, RunProcessHandler processHandler) throws Exception {
        final String name = webAppSettingModel.getWebAppName();
        final String rg = webAppSettingModel.getResourceGroup();
        final String id = webAppSettingModel.getWebAppId();
        final WebAppModule webapps = Azure.az(AzureWebApp.class).webApps(webAppSettingModel.getSubscriptionId());
        final WebApp webApp = StringUtils.isNotBlank(id) ? webapps.get(id) : webapps.get(name, rg);
        if (Objects.nonNull(webApp)) {
            return webApp;
        }
        if (webAppSettingModel.isCreatingNew()) {
            processHandler.setText(message("webapp.deploy.hint.creatingWebApp"));
            return AzureWebAppMvpModel.getInstance().createWebAppFromSettingModel(webAppSettingModel);
        } else {
            processHandler.setText(message("appService.deploy.hint.failed"));
            throw new Exception(message("webapp.deploy.error.noWebApp"));
        }
    }

    @AzureOperation(
        name = "webapp.get_artifact.app",
        params = {"this.webAppConfiguration.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private String getTargetPath() {
        final AzureArtifact azureArtifact =
                AzureArtifactManager.getInstance(project).getAzureArtifactById(webAppConfiguration.getAzureArtifactType(),
                                                                               webAppConfiguration.getArtifactIdentifier());
        if (Objects.isNull(azureArtifact)) {
            final String error = String.format("selected artifact[%s] not found", webAppConfiguration.getArtifactIdentifier());
            throw new AzureToolkitRuntimeException(error);
        }
        return AzureArtifactManager.getInstance(project).getFileForDeployment(azureArtifact);
    }

    @AzureOperation(
        name = "webapp.create_deployment.app",
        params = {"this.webAppConfiguration.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private WebAppDeploymentSlot createDeploymentSlot(final WebApp webApp,
                                                       @NotNull RunProcessHandler processHandler) {
        processHandler.setText(message("webapp.deploy.hint.creatingDeploymentSlot"));
        try {
            return AzureWebAppMvpModel.getInstance().createDeploymentSlotFromSettingModel(webApp, webAppSettingModel);
        } catch (final RuntimeException e) {
            processHandler.setText(message("webapp.deploy.error.noWebApp"));
            throw e;
        }
    }

    @NotNull
    private String getUrl(@NotNull WebAppBase<?, ?, ?> webApp, @NotNull String fileName, @NotNull String fileType) {
        String url = "https://" + webApp.getHostName();
        if (Objects.equals(fileType, MavenConstants.TYPE_WAR) && !webAppSettingModel.isDeployToRoot()) {
            url += "/" + WebAppUtils.encodeURL(fileName.replaceAll("#", StringUtils.EMPTY)).replaceAll("\\+", "%20");
        }
        return url;
    }

    private void updateConfigurationDataModel(@NotNull WebAppBase<?, ?, ?> app) {
        webAppConfiguration.setCreatingNew(false);
        // todo: add flag to indicate create new slot or not
        if (app instanceof WebAppDeploymentSlot) {
            webAppConfiguration.setSlotName(app.getName());
            webAppConfiguration.setNewSlotConfigurationSource(AzureWebAppMvpModel.DO_NOT_CLONE_SLOT_CONFIGURATION);
            webAppConfiguration.setNewSlotName("");
            webAppConfiguration.setWebAppId(((WebAppDeploymentSlot) app).getParent().getId());
        } else {
            webAppConfiguration.setWebAppId(app.getId());
        }
        webAppConfiguration.setApplicationSettings(app.getAppSettings());
        webAppConfiguration.setWebAppName(app.name());
        webAppConfiguration.setWebAppName("");
        webAppConfiguration.setResourceGroup("");
        webAppConfiguration.setAppServicePlanName("");
        webAppConfiguration.setAppServicePlanResourceGroupName("");
    }
}
