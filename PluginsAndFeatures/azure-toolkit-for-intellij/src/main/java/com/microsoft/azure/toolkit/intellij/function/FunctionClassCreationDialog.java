/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.function.components.FunctionModuleComboBox;
import com.microsoft.azure.toolkit.intellij.function.components.FunctionTemplatePanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.function.components.FunctionTemplatePanel.getLabelWidth;

public class FunctionClassCreationDialog extends AzureDialog<FunctionClassCreationDialog.FunctionCreationResult> implements AzureForm<FunctionClassCreationDialog.FunctionCreationResult> {
    public static final String CLASS_NAME = "className";
    public static final String PACKAGE_NAME = "packageName";
    public static final String FUNCTION_NAME = "functionName";
    private JPanel pnlRoot;
    private JLabel lblPackageName;
    private AzureTextInput txtPackageName;
    private FunctionModuleComboBox cbFunctionModule;
    private JLabel lblFunctionName;
    private AzureTextInput txtFunctionName;
    private JLabel lblTriggerType;
    private JPanel pnlAdditionalParameters;
    private AzureComboBox<FunctionTemplate> cbTriggerType;

    private FunctionTemplatePanel templatePanel;
    private final Project project;
    private final int labelWidth;

    public FunctionClassCreationDialog(final Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.labelWidth = getLabelWidth();
        this.init();
    }

    protected void init() {
        super.init();
        this.setLabelWidth(labelWidth);
        this.cbTriggerType.addValueChangedListener(this::onTriggerChanged);
        this.cbFunctionModule.addValueChangedListener(this::onModuleChanged);
    }

    private void onModuleChanged(@Nullable Module module) {
        Optional.ofNullable(this.templatePanel).ifPresent(panel -> panel.setModule(module));
    }

    private void onTriggerChanged(@Nullable FunctionTemplate template) {
        this.pnlAdditionalParameters.removeAll();
        if (Objects.isNull(template)) {
            this.templatePanel = null;
            return;
        }
        this.templatePanel = new FunctionTemplatePanel(template, project);
        Optional.ofNullable(cbFunctionModule.getValue()).ifPresent(templatePanel::setModule);
        final GridConstraints labelConstraints = new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
        this.pnlAdditionalParameters.add(this.templatePanel, labelConstraints);
    }

    private void setLabelWidth(final int width) {
        final Component[] components = this.pnlRoot.getComponents();
        Arrays.stream(components)
                .filter(component -> component instanceof JLabel)
                .forEach(component -> {
                    component.setMinimumSize(new Dimension(width, (int) component.getPreferredSize().getHeight()));
                    component.setMaximumSize(new Dimension(width, (int) component.getPreferredSize().getHeight()));
                    component.setPreferredSize(new Dimension(width, (int) component.getPreferredSize().getHeight()));
                });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbTriggerType = new AzureComboBox<>(FunctionUtils::loadAllFunctionTemplates) {
            @Override
            protected String getItemText(Object item) {
                return item instanceof FunctionTemplate ? ((FunctionTemplate) item).getMetadata().getName() : super.getItemText(item);
            }
        };
        this.cbFunctionModule = new FunctionModuleComboBox(project);
    }

    @Override
    public AzureForm<FunctionCreationResult> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Function";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public FunctionCreationResult getValue() {
        final Module module = cbFunctionModule.getValue();
        final FunctionTemplate template = cbTriggerType.getValue();
        final Map<String, String> parameters = new HashMap<>();
        parameters.put(CLASS_NAME, txtFunctionName.getText());
        parameters.put(FUNCTION_NAME, txtFunctionName.getText());
        parameters.put(PACKAGE_NAME, txtPackageName.getText());
        Optional.ofNullable(templatePanel).map(AzureFormPanel::getValue).ifPresent(parameters::putAll);
        return new FunctionCreationResult(module, template, parameters);
    }

    @Override
    public void setValue(@Nonnull final FunctionCreationResult data) {
        final Map<String, String> parameters = ObjectUtils.firstNonNull(data.getParameters(), Collections.emptyMap());
        Optional.ofNullable(parameters.get(CLASS_NAME)).ifPresent(txtFunctionName::setValue);
        Optional.ofNullable(parameters.get(PACKAGE_NAME)).ifPresent(txtPackageName::setValue);
        Optional.ofNullable(data.getModule()).ifPresent(cbFunctionModule::setValue);
        Optional.ofNullable(data.getTemplate()).ifPresent(cbTriggerType::setValue);
        if (ObjectUtils.anyNotNull(templatePanel, data.getParameters())) {
            templatePanel.setValue(data.getParameters());
        }
    }

    public void setPackage(@Nonnull final String packageName) {
        this.txtPackageName.setValue(packageName);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final List<AzureFormInput<?>> components = Arrays.asList(cbFunctionModule, txtFunctionName, txtPackageName, cbTriggerType);
        final List<AzureFormInput<?>> templateInputs = Optional.ofNullable(templatePanel).map(AzureForm::getInputs).orElse(Collections.emptyList());
        return ListUtils.union(components, templateInputs);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    @Data
    @AllArgsConstructor
    public static class FunctionCreationResult {
        private Module module;
        private FunctionTemplate template;
        private Map<String, String> parameters;
    }
}
