package com.microsoft.intellij.runner.container.dockerhost;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DockerHostRunConfiguration extends RunConfigurationBase {
    private static String CONFIGURATION_ELEMENT_NODE_NAME = "DockerHostRunConfiguration";
    private DockerHostRunModel containerLocalRunModel;

    public DockerHostRunModel getContainerLocalRunModel() {
        return containerLocalRunModel;
    }

    protected DockerHostRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        containerLocalRunModel = new DockerHostRunModel();
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        containerLocalRunModel.readExternal(element.getChild(CONFIGURATION_ELEMENT_NODE_NAME));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        Element thisElement = new Element(CONFIGURATION_ELEMENT_NODE_NAME);
        containerLocalRunModel.writeExternal(thisElement);
        element.addContent(thisElement);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DockerHostRunSettingsEditor();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {

    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        return new RunProfileState() {
            @Nullable
            @Override
            public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
                return null;
            }
        };
    }
}
