package com.microsoft.intellij.runner.functions.deploy.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;


public class FunctionDeploymentPanel extends AzureSettingPanel<FunctionDeployConfiguration> {

    private ResourceEx<FunctionApp> selectedFunctionApp = null;
    private AppServicePlan selectedFunctionAppServicePlan = null;
    private Map<String, AppServicePlan> appServicePlanMap = new HashMap<>();

    private JLabel lblArtifact;
    private JComboBox cbArtifact;
    private JLabel lblMavenProject;
    private JComboBox cbMavenProject;
    private JPanel pnlRoot;
    private JComboBox cbxFunctionApp;
    private HyperlinkLabel lblCreateFunctionApp;
    private JPanel pnlAppSettings;
    private TextFieldWithBrowseButton txtStagingFolder;
    private JComboBox<Module> cbFunctionModule;

    // presenter
    private FunctionDeployConfiguration functionDeployConfiguration;

    public FunctionDeploymentPanel(@NotNull Project project, @NotNull FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
    }

    @NotNull
    @Override
    public String getPanelName() {
        return "Deploy Azure Functions";
    }

    @Override
    public void disposeEditor() {
    }

    @NotNull
    @Override
    public JPanel getMainPanel() {
        return pnlRoot;
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

    @Override
    protected void resetFromConfig(@NotNull FunctionDeployConfiguration configuration) {

    }

    @Override
    protected void apply(@NotNull FunctionDeployConfiguration configuration) {

    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
