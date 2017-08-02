package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.container.webapponlinux.ui.RemoteRunPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class WebAppOnLinuxDeploySettingsEditor extends SettingsEditor<WebAppOnLinuxDeployConfiguration> {
    private Project project;
    private RemoteRunPanel remoteRunPanel;

    public WebAppOnLinuxDeploySettingsEditor(Project project) {
        this.project = project;
        remoteRunPanel = new RemoteRunPanel();
    }

    @Override
    protected void resetEditorFrom(@NotNull WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) {
        remoteRunPanel.reset(webAppOnLinuxDeployConfiguration);
    }

    @Override
    protected void applyEditorTo(@NotNull WebAppOnLinuxDeployConfiguration webAppOnLinuxDeployConfiguration) throws ConfigurationException {
        remoteRunPanel.apply(webAppOnLinuxDeployConfiguration);
    }


    @NotNull
    @Override
    protected JComponent createEditor() {
        return remoteRunPanel.getRootPanel();
    }
}
