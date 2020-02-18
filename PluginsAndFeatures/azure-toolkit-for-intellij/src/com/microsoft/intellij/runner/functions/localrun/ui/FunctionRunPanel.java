package com.microsoft.intellij.runner.functions.localrun.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.functions.localrun.FunctionRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;

public class FunctionRunPanel extends AzureSettingPanel<FunctionRunConfiguration> {

    private JPanel settings;
    private JPanel pnlMain;
    private JComboBox cbMavenProject;
    private JLabel lblMavenProject;
    private JComboBox cbArtifact;
    private JLabel lblArtifact;
    private TextFieldWithBrowseButton txtStagingFolder;
    private TextFieldWithBrowseButton txtFunc;
    private JPanel pnlAppSettings;
    private JComboBox<Module> cbFunctionModule;

    private FunctionRunConfiguration functionRunConfiguration;

    public FunctionRunPanel(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration) {
        super(project);
        this.functionRunConfiguration = functionRunConfiguration;
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Run Function";
    }

    @Override
    public void disposeEditor() {

    }

    @Override
    protected void resetFromConfig(@NotNull FunctionRunConfiguration configuration) {

    }

    @Override
    protected void apply(@NotNull FunctionRunConfiguration configuration) {

    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlMain;
    }

    @NotNull
    @Override
    protected JComboBox<Artifact> getCbArtifact() {
        return new JComboBox<Artifact>();
    }

    @NotNull
    @Override
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    @Override
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new JComboBox<MavenProject>();
    }

    @NotNull
    @Override
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
