/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.microsoft.azure.toolkit.intellij.legacy.function.wizard.AzureFunctionsConstants;
import com.microsoft.intellij.util.GradleUtils;
import com.microsoft.intellij.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class FunctionsModuleInfoStep extends ModuleWizardStep implements Disposable {
    public static final String MAVEN_TOOL = "Maven";
    public static final String GRADLE_TOOL = "Gradle";

    private JPanel panel;

    private ComboBox<String> toolComboBox;

    private ComboBox parentComboBox;

    private JBTextField groupIdField;

    private JBTextField artifactIdField;

    private JBTextField versionField;

    private JBTextField packageNameField;

    private final WizardContext context;

    public FunctionsModuleInfoStep(final WizardContext context) {
        this.context = context;
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    @Override
    public void updateDataModel() {
        context.putUserData(AzureFunctionsConstants.WIZARD_TOOL_KEY,
                (String) toolComboBox.getModel().getSelectedItem());
        context.putUserData(AzureFunctionsConstants.WIZARD_GROUPID_KEY, groupIdField.getText());
        context.putUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY, artifactIdField.getText());
        context.putUserData(AzureFunctionsConstants.WIZARD_VERSION_KEY, versionField.getText());
        context.putUserData(AzureFunctionsConstants.WIZARD_PACKAGE_NAME_KEY, packageNameField.getText());
        if (!context.isCreatingNewProject()) {
            Object parent = parentComboBox.getItem();
            if (parent instanceof MavenProject) {
                context.putUserData(AzureFunctionsConstants.PARENT_PATH, ((MavenProject) parent).getDirectory());
            } else if (parent instanceof ExternalProjectPojo) {
                context.putUserData(AzureFunctionsConstants.PARENT_PATH, ((ExternalProjectPojo) parent).getPath());
            }
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public void _init() {
        panel = new JPanel(new BorderLayout());
        final String groupId = context.getUserData(AzureFunctionsConstants.WIZARD_GROUPID_KEY);
        final String artifactId = context.getUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY);
        final String version = context.getUserData(AzureFunctionsConstants.WIZARD_VERSION_KEY);
        final String packageName = context.getUserData(AzureFunctionsConstants.WIZARD_PACKAGE_NAME_KEY);
        try {
            final FormBuilder formBuilder = new FormBuilder();
            if (!context.isCreatingNewProject()) {
                parentComboBox = new ComboBox();
                formBuilder.addLabeledComponent("Parent:", parentComboBox);
            }

            final CollectionComboBoxModel<String> toolModel = new CollectionComboBoxModel<>(
                    Arrays.asList(MAVEN_TOOL, GRADLE_TOOL));
            toolComboBox = new ComboBox<>(toolModel);
            toolComboBox.addItemListener(ignore -> refreshProjectComboBox());
            formBuilder.addLabeledComponent("Tool:", toolComboBox);

            groupIdField = new JBTextField(getCurrentOrDefaultValue(groupId, "com.example"));
            formBuilder.addLabeledComponent("Group:", groupIdField);
            artifactIdField = new JBTextField(getCurrentOrDefaultValue(artifactId, "azure-function-examples"));
            formBuilder.addLabeledComponent("Artifact:", artifactIdField);
            versionField = new JBTextField(getCurrentOrDefaultValue(version, "1.0.0-SNAPSHOT"));
            formBuilder.addLabeledComponent("Version:", versionField);

            packageNameField = new JBTextField(getCurrentOrDefaultValue(packageName, "org.example.functions"));
            formBuilder.addLabeledComponent("Package name:", packageNameField);

            refreshProjectComboBox();

            final JPanel content = formBuilder.getPanel();
            content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(ScrollPaneFactory.createScrollPane(content, true), "North");
        } catch (final RuntimeException e) {
            log.error(e.getLocalizedMessage(), e);
            throw e;
        }
    }

    private void refreshProjectComboBox() {
        if (context.isCreatingNewProject()) {
            return;
        }
        final String item = toolComboBox.getItem();
        if (StringUtils.equals(MAVEN_TOOL, item)) {
            listMavenProjects(context.getProject());
        } else if (StringUtils.equals(GRADLE_TOOL, item)) {
            listGradleProjects(context.getProject());
        }
    }

    private void listMavenProjects(final Project project) {
        final List<MavenProject> projects = MavenProjectsManager.getInstance(project).getProjects();
        Collections.sort(projects, (project1, project2) -> StringUtils.compare(project1.getMavenId().getArtifactId(), project2.getMavenId().getArtifactId()));
        final CollectionComboBoxModel<MavenProject> mavenModel = new CollectionComboBoxModel<>(projects);
        parentComboBox.setModel(mavenModel);
        parentComboBox.setRenderer(new ListCellRendererWithRightAlignedComponent() {
            @Override
            protected void customize(Object value) {
                if (value instanceof MavenProject) {
                    setLeftText(((MavenProject) value).getMavenId().getArtifactId());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });
    }

    private void listGradleProjects(final Project project) {
        final List<ExternalProjectPojo> externalProjects = GradleUtils.listGradleRootProjectPojo(project);
        Collections.sort(externalProjects, (project1, project2) -> StringUtils.compare(project1.getName(), project2.getName()));
        final CollectionComboBoxModel<ExternalProjectPojo> gradleModel = new CollectionComboBoxModel<>(externalProjects);
        parentComboBox.setModel(gradleModel);
        parentComboBox.setRenderer(new ListCellRendererWithRightAlignedComponent() {
            @Override
            protected void customize(Object value) {
                if (value instanceof ExternalProjectPojo) {
                    setLeftText(((ExternalProjectPojo) value).getName());
                    setIcon(AllIcons.Nodes.Module);
                }
            }
        });
    }

    private static String getCurrentOrDefaultValue(final String currentValue, final String defaultValue) {
        if (StringUtils.isNoneEmpty(currentValue)) {
            return currentValue;
        }
        return defaultValue;
    }

    @Override
    public boolean validate() throws ConfigurationException {
        validateProperties("Group id", groupIdField.getText(), ValidationUtils::isValidGroupIdArtifactId);
        validateProperties("Artifact id", artifactIdField.getText(), ValidationUtils::isValidGroupIdArtifactId);
        validateProperties("Version", versionField.getText(), ValidationUtils::isValidVersion);
        validateProperties("Package name", packageNameField.getText(), ValidationUtils::isValidJavaPackageName);
        if (!context.isCreatingNewProject() && Objects.isNull(parentComboBox.getItem())) {
            throw new ConfigurationException("Parent module must be specified");
        }
        validateDuplicatedArtifacts();
        return true;
    }

    private boolean validateDuplicatedArtifacts() throws ConfigurationException {
        final String artifactId = artifactIdField.getText();
        if (getExistingArtifacts().contains(artifactId)) {
            throw new ConfigurationException(String.format("Module '%s' already exists", artifactId));
        }
        return true;
    }

    private List<String> getExistingArtifacts() {
        if (context.isCreatingNewProject()) {
            return Collections.emptyList();
        }
        final String item = toolComboBox.getItem();
        if (StringUtils.equals(item, MAVEN_TOOL)) {
            return MavenProjectsManager.getInstance(context.getProject()).getProjects().stream()
                    .map(project -> project.getMavenId().getArtifactId())
                    .collect(Collectors.toList());
        } else {
            return ProjectDataManager.getInstance().getExternalProjectsData(context.getProject(), GradleConstants.SYSTEM_ID).stream()
                    .map(project -> project.getExternalProjectStructure())
                    .filter(Objects::nonNull)
                    .map(value -> value.getData().getExternalName())
                    .collect(Collectors.toList());
        }
    }

    private static void validateProperties(String propertyName, String text, Predicate<String> validator)
            throws ConfigurationException {
        if (text.isEmpty()) {
            throw new ConfigurationException(propertyName + " must be specified");
        }

        if (!validator.test(text)) {
            throw new ConfigurationException(String.format("Invalid %s: %s", propertyName, text));
        }
    }
}
