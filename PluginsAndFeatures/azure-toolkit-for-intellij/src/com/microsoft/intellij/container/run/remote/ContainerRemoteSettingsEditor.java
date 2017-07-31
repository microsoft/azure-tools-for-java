package com.microsoft.intellij.container.run.remote;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.container.run.remote.ui.RemoteRunPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ContainerRemoteSettingsEditor extends SettingsEditor<ContainerRemoteRunConfiguration> {
    private Project project;
    private RemoteRunPanel remoteRunPanel;

    public ContainerRemoteSettingsEditor(Project project) {
        this.project = project;
        remoteRunPanel = new RemoteRunPanel();
    }

    @Override
    protected void resetEditorFrom(@NotNull ContainerRemoteRunConfiguration containerRemoteRunConfiguration) {
        remoteRunPanel.reset(containerRemoteRunConfiguration);
    }

    @Override
    protected void applyEditorTo(@NotNull ContainerRemoteRunConfiguration containerRemoteRunConfiguration) throws ConfigurationException {
        remoteRunPanel.apply(containerRemoteRunConfiguration);
    }


    @NotNull
    @Override
    protected JComponent createEditor() {
        return remoteRunPanel.getRootPanel();
    }
}
