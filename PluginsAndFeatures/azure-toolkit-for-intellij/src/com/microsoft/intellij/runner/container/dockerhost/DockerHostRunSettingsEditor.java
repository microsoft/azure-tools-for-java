package com.microsoft.intellij.runner.container.dockerhost;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.microsoft.intellij.runner.container.dockerhost.ui.SettingPanel;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.JComponent;

public class DockerHostRunSettingsEditor extends SettingsEditor<DockerHostRunConfiguration> {
    private final Project project;
    private SettingPanel settingPanel;

    public DockerHostRunSettingsEditor(Project project) {
        this.project = project;
        this.settingPanel = new SettingPanel(project);
    }

    @Override
    protected void resetEditorFrom(@NotNull DockerHostRunConfiguration containerLocalRunConfiguration) {
        if (containerLocalRunConfiguration.isFirstTimeCreated()) {
            if (MavenRunTaskUtil.isMavenProject(project)) {
                MavenRunTaskUtil.addMavenPackageBeforeRunTask(containerLocalRunConfiguration);
            } else {
                List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
                if (null != artifacts && artifacts.size() > 0) {
                    BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(project,
                            containerLocalRunConfiguration, artifacts.get(0));
                }
            }
        }
        containerLocalRunConfiguration.setFirstTimeCreated(false);
        settingPanel.reset(containerLocalRunConfiguration);
    }


    @Override
    protected void applyEditorTo(@NotNull DockerHostRunConfiguration containerLocalRunConfiguration) throws
            ConfigurationException {
        settingPanel.apply(containerLocalRunConfiguration);
        containerLocalRunConfiguration.validate();
    }


    @NotNull
    @Override
    protected JComponent createEditor() {
        return settingPanel.getRootPanel();
    }


}
