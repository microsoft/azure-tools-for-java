package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WebAppOnLinuxDeployConfiguration extends RunConfigurationBase {

    private static final String CONFIGURATION_ELEMENT_NODE_NAME = "WebAppOnLinuxDeployConfiguration";
    private final WebAppOnLinuxDeployModel webAppOnLinuxDeployModel;

    public WebAppOnLinuxDeployModel getWebAppOnLinuxDeployModel() {
        return webAppOnLinuxDeployModel;
    }

    protected WebAppOnLinuxDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory,
                                               String name) {
        super(project, factory, name);
        webAppOnLinuxDeployModel = new WebAppOnLinuxDeployModel();
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WebAppOnLinuxDeploySettingsEditor(this.getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        webAppOnLinuxDeployModel.readExternal(element.getChild(CONFIGURATION_ELEMENT_NODE_NAME));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        Element thisElement = new Element(CONFIGURATION_ELEMENT_NODE_NAME);
        webAppOnLinuxDeployModel.writeExternal(thisElement);
        element.addContent(thisElement);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new WebAppOnLinuxDeployState(getProject(), webAppOnLinuxDeployModel);
    }
}
