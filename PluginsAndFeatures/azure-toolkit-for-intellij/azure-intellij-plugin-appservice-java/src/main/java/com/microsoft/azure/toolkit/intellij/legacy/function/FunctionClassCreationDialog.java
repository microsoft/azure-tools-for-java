/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.module.Module;
import com.intellij.ui.TitledSeparator;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.function.components.FunctionTemplatePanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.intellij.function.components.FunctionTemplatePanel.getLabelWidth;

public class FunctionClassCreationDialog extends AzureDialog<FunctionClassCreationDialog.FunctionCreationResult> implements AzureForm<FunctionClassCreationDialog.FunctionCreationResult> {
    public static final String CLASS_NAME = "className";
    public static final String PACKAGE_NAME = "packageName";
    public static final String FUNCTION_NAME = "functionName";
    private JPanel pnlRoot;
    private JLabel lblPackageName;
    private AzureTextInput txtPackageName;
    private JLabel lblFunctionName;
    private AzureTextInput txtFunctionName;
    private JLabel lblTriggerType;
    private JPanel pnlAdditionalParameters;
    private AzureComboBox<FunctionTemplate> cbTriggerType;
    private TitledSeparator titleTriggerParameters;
    private TitledSeparator titleBasicConfigurataion;

    private FunctionTemplatePanel templatePanel;
    private final Module module;
    private final int labelWidth;

    public FunctionClassCreationDialog(final Module module) {
        super(module.getProject());
        this.module = module;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.labelWidth = getLabelWidth();
        this.init();
    }

    protected void init() {
        super.init();
        this.setLabelWidth(labelWidth);
        this.cbTriggerType.addValueChangedListener(this::onTriggerChanged);
        this.txtPackageName.setRequired(true);
        this.txtFunctionName.setRequired(true);
        this.cbTriggerType.setRequired(true);
        this.lblPackageName.setLabelFor(txtPackageName);
        this.lblFunctionName.setLabelFor(txtFunctionName);
        this.lblTriggerType.setLabelFor(cbTriggerType);
    }

    private void onTriggerChanged(@Nullable FunctionTemplate template) {
        this.pnlAdditionalParameters.removeAll();
        if (Objects.isNull(template)) {
            this.templatePanel = null;
            return;
        }
        this.templatePanel = new FunctionTemplatePanel(template, module);
        this.templatePanel.setRequired(true);
        final GridConstraints labelConstraints = new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
        this.pnlAdditionalParameters.add(this.templatePanel, labelConstraints);
        this.titleTriggerParameters.setVisible(CollectionUtils.isNotEmpty(templatePanel.getInputs()));
        this.pnlAdditionalParameters.setVisible(CollectionUtils.isNotEmpty(templatePanel.getInputs()));
        this.pack();
        this.repaint();
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
        this.cbTriggerType = new AzureComboBox<>() {
            @Override
            protected String getItemText(Object item) {
                return item instanceof FunctionTemplate ? ((FunctionTemplate) item).getMetadata().getName() : super.getItemText(item);
            }

            @Nonnull
            @Override
            protected List<? extends FunctionTemplate> loadItems() {
                final FunctionExtensionVersion bundleVersion = getBundleVersion();
                return FunctionUtils.loadAllFunctionTemplates().stream()
                        .filter(t -> bundleVersion == null || t.isBundleSupported(bundleVersion))
                        .collect(Collectors.toList());
            }
        };
    }

    @Nullable
    protected FunctionExtensionVersion getBundleVersion() {
        // todo: add configuration for host.json location
        final String defaultHostJsonPath = com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils.getDefaultHostJsonPath(module);
        try (final FileInputStream fis = new FileInputStream(defaultHostJsonPath)) {
            final String content = IOUtils.toString(fis, Charset.defaultCharset());
            final JsonNode jsonNode = JsonUtils.fromJson(content, JsonNode.class);
            return Optional.ofNullable(jsonNode.at("/extensionBundle/version"))
                    .filter(node -> !node.isMissingNode())
                    .map(node -> FunctionUtils.parseFunctionExtensionVersionFromHostJson(node.asText()))
                    .orElse(null);
        } catch (final RuntimeException | IOException e) {
            // swallow exception when read bundle version
            return null;
        }
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
        final FunctionTemplate template = cbTriggerType.getValue();
        final Map<String, String> parameters = new HashMap<>();
        parameters.put(CLASS_NAME, txtFunctionName.getText());
        parameters.put(FUNCTION_NAME, txtFunctionName.getText());
        parameters.put(PACKAGE_NAME, txtPackageName.getText());
        Optional.ofNullable(templatePanel).map(AzureFormPanel::getValue).ifPresent(parameters::putAll);
        return new FunctionCreationResult(template, parameters);
    }

    @Override
    public void setValue(@Nonnull final FunctionCreationResult data) {
        final Map<String, String> parameters = ObjectUtils.firstNonNull(data.getParameters(), Collections.emptyMap());
        Optional.ofNullable(parameters.get(CLASS_NAME)).ifPresent(txtFunctionName::setValue);
        Optional.ofNullable(parameters.get(PACKAGE_NAME)).ifPresent(txtPackageName::setValue);
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
        return Stream.of(txtFunctionName, txtPackageName, cbTriggerType, templatePanel)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    @Data
    @AllArgsConstructor
    public static class FunctionCreationResult {
        private FunctionTemplate template;
        private Map<String, String> parameters;
    }
}
