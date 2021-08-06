/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.wizard.module;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.microsoft.intellij.util.ValidationUtils;
import com.microsoft.azure.toolkit.intellij.function.wizard.AzureFunctionsConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Predicate;

public class FunctionsModuleInfoStep extends ModuleWizardStep implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionsModuleInfoStep.class);
    private static final String MAVEN_TOOL = "Maven";
    private static final String GRADLE_TOOL = "Gradle";

    private JPanel panel;

    private ComboBox<String> toolComboBox;

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
            final CollectionComboBoxModel<String> toolModel = new CollectionComboBoxModel<>(
                    Arrays.asList(MAVEN_TOOL, GRADLE_TOOL));
            toolComboBox = new ComboBox<>(toolModel);
            formBuilder.addLabeledComponent("Tool:", toolComboBox);
            groupIdField = new JBTextField(getCurrentOrDefaultValue(groupId, "com.example"));
            formBuilder.addLabeledComponent("Group:", groupIdField);
            artifactIdField = new JBTextField(getCurrentOrDefaultValue(artifactId, "azure-function-examples"));
            formBuilder.addLabeledComponent("Artifact:", artifactIdField);
            versionField = new JBTextField(getCurrentOrDefaultValue(version, "1.0.0-SNAPSHOT"));
            formBuilder.addLabeledComponent("Version:", versionField);

            packageNameField = new JBTextField(getCurrentOrDefaultValue(packageName, "org.example.functions"));
            formBuilder.addLabeledComponent("Package name:", packageNameField);

            panel.add(ScrollPaneFactory.createScrollPane(formBuilder.getPanel(), true), "North");
        } catch (final RuntimeException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            throw e;
        }
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
        return true;
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
