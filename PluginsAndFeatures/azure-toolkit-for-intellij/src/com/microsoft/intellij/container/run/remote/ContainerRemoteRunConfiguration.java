package com.microsoft.intellij.container.run.remote;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContainerRemoteRunConfiguration extends RunConfigurationBase {

    private static final String CONFIGURATION_ELEMENT_NODE_NAME = "Deploy";
    private final ContainerRemoteRunModel containerRemoteRunModel;

    public ContainerRemoteRunModel getContainerRemoteRunModel() {
        return containerRemoteRunModel;
    }

    protected ContainerRemoteRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory,
                                              String name) {
        super(project, factory, name);
        containerRemoteRunModel = new ContainerRemoteRunModel();
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new ContainerRemoteSettingsEditor(this.getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        containerRemoteRunModel.readExternal(element.getChild(CONFIGURATION_ELEMENT_NODE_NAME));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        Element thisElement = new Element(CONFIGURATION_ELEMENT_NODE_NAME);
        containerRemoteRunModel.writeExternal(thisElement);
        element.addContent(thisElement);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new ContainerRemoteRunState(getProject(), containerRemoteRunModel);
    }
}
