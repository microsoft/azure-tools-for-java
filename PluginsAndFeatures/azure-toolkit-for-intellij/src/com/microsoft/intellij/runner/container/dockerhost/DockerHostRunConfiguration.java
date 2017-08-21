package com.microsoft.intellij.runner.container.dockerhost;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azuretools.azurecommons.util.Utils;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DockerHostRunConfiguration extends RunConfigurationBase {
    // TODO: move to util
    private static final String MISSING_ARTIFACT = "A web archive (.war) artifact has not been configured.";
    private static final String INVALID_WAR_FILE = "The artifact name %s is invalid. "
            + "An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive), "
            + "and the digits '0' through '9', '.', '-' and '_'.";
    private static final String WAR_NAME_REGEX = "^[.A-Za-z0-9_-]+\\.war$";
    private DockerHostRunModel dockerHostRunModel;
    private boolean firstTimeCreated = true;

    protected DockerHostRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory, String.format("%s:%s", factory.getName(), project.getName()));
        dockerHostRunModel = new DockerHostRunModel();
    }

    public DockerHostRunModel getDockerHostRunModel() {
        return dockerHostRunModel;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        firstTimeCreated = Comparing.equal(element.getAttributeValue("default"), "true");
        XmlSerializer.deserializeInto(dockerHostRunModel, element);
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        XmlSerializer.serializeInto(dockerHostRunModel, element);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DockerHostRunSettingsEditor(this.getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

    }

    public void validate() throws ConfigurationException {
        // try {
        //     if (!AuthMethodManager.getInstance().isSignedIn()) {
        //         throw new ConfigurationException(NEED_SIGN_IN);
        //     }
        // } catch (IOException e) {
        //     throw new ConfigurationException(NEED_SIGN_IN);
        // }
        // // acr
        // PrivateRegistryImageSetting setting = deployModel.getPrivateRegistryImageSetting();
        // if (Utils.isEmptyString(setting.getServerUrl()) || !setting.getServerUrl().matches(DOMAIN_NAME_REGEX)) {
        //     throw new ConfigurationException(MISSING_SERVER_URL);
        // }
        // if (Utils.isEmptyString(setting.getUsername())) {
        //     throw new ConfigurationException(MISSING_USERNAME);
        // }
        // if (Utils.isEmptyString(setting.getPassword())) {
        //     throw new ConfigurationException(MISSING_PASSWORD);
        // }
        // if (Utils.isEmptyString(setting.getImageNameWithTag())) {
        //     throw new ConfigurationException(MISSING_IMAGE_WITH_TAG);
        // }
        // if (!setting.getImageNameWithTag().startsWith(setting.getServerUrl() + "/")) {
        //     throw new ConfigurationException(String.format(INVALID_IMAGE_WITH_TAG, setting.getServerUrl()));
        // }
        // // web app
        // if (deployModel.isCreatingNewWebAppOnLinux()) {
        //     if (Utils.isEmptyString(deployModel.getWebAppName())) {
        //         throw new ConfigurationException(MISSING_WEB_APP);
        //     }
        //     if (Utils.isEmptyString(deployModel.getSubscriptionId())) {
        //         throw new ConfigurationException(MISSING_SUBSCRIPTION);
        //     }
        //     if (Utils.isEmptyString(deployModel.getResourceGroupName())) {
        //         throw new ConfigurationException(MISSING_RESOURCE_GROUP);
        //     }
        //
        //     if (deployModel.isCreatingNewAppServicePlan()) {
        //         if (Utils.isEmptyString(deployModel.getAppServicePlanName())) {
        //             throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
        //         }
        //     } else {
        //         if (Utils.isEmptyString(deployModel.getAppServicePlanId())) {
        //             throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
        //         }
        //     }
        //
        // } else {
        //     if (Utils.isEmptyString(deployModel.getWebAppId())) {
        //         throw new ConfigurationException(MISSING_WEB_APP);
        //     }
        // }

        // target package
        if (dockerHostRunModel == null || Utils.isEmptyString(dockerHostRunModel.getTargetName())) {
            throw new ConfigurationException(MISSING_ARTIFACT);
        }
        if (!dockerHostRunModel.getTargetName().matches(WAR_NAME_REGEX)) {
            throw new ConfigurationException(String.format(INVALID_WAR_FILE, dockerHostRunModel.getTargetName()));
        }
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new DockerHostRunState(getProject(), dockerHostRunModel);
    }

    public boolean isFirstTimeCreated() {
        return firstTimeCreated;
    }

    public void setFirstTimeCreated(boolean firstTimeCreated) {
        this.firstTimeCreated = firstTimeCreated;
    }
}
