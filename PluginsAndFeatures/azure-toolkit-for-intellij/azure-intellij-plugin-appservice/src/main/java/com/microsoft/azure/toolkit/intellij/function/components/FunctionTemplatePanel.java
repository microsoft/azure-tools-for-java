/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components;

import com.intellij.icons.AllIcons;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.function.components.inputs.FunctionBooleanInput;
import com.microsoft.azure.toolkit.intellij.function.components.inputs.FunctionEnumInput;
import com.microsoft.azure.toolkit.intellij.function.components.inputs.FunctionStringInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionSettingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateMetadata;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateResources;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionTemplatePanel extends JPanel implements AzureFormPanel<Map<String, String>> {
    public static final String BOOLEAN = "boolean";
    private final FunctionTemplate template;
    private final BindingTemplate binding;
    private final Map<String, AzureFormInputComponent<String>> inputs = new HashMap<>();

    public FunctionTemplatePanel(@Nonnull FunctionTemplate template) {
        super();
        this.template = template;
        this.binding = this.template.getBinding();
        // generate components based on template
        initComponents();
    }

    private void initComponents() {
        final List<String> userInputs = Optional.ofNullable(template.getMetadata())
                .map(TemplateMetadata::getUserPrompt).orElse(Collections.emptyList());
        final List<FunctionSettingTemplate> templates = userInputs.stream()
                .map(this::getFunctionSettingsTemplate)
                .sorted(this::compareFunctionSettingsTemplate).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(templates)) {
            return;
        }
        this.setLayout(new GridLayoutManager(templates.size(), 2));
        for (int i = 0; i < templates.size(); i++) {
            final FunctionSettingTemplate inputTemplate = templates.get(i);
            final AzureFormInputComponent<String> component = createComponent(inputTemplate);
            if (StringUtils.equalsIgnoreCase(inputTemplate.getValue(), BOOLEAN)) {
                final GridConstraints booleanConstraints = new GridConstraints(i, 0, 1, 2, 0, 3, 3, 3, null, null, null, 0);
                this.add((Component) component, booleanConstraints);
            } else {
                final String labelText = Optional.ofNullable(inputTemplate).map(FunctionSettingTemplate::getLabel)
                        .map(TemplateResources::getResource)
                        .orElseGet(() -> String.format("%s:", inputTemplate.getName()));
                final JLabel label = new JLabel(StringUtils.capitalize(labelText));
                Optional.ofNullable(inputTemplate.getHelp()).map(TemplateResources::getResource)
                        .filter(StringUtils::isNotEmpty)
                        .ifPresent(description -> {
                            label.setIcon(AllIcons.General.ContextHelp);
                            label.setHorizontalTextPosition(JLabel.LEADING);
                            label.setToolTipText(description);
                        });
                final GridConstraints labelConstraints = new GridConstraints(i, 0, 1, 1, 0, 0, 0, 0, null, null, null, 0);
                this.add(label, labelConstraints);
                final GridConstraints componentConstraints = new GridConstraints(i, 1, 1, 1, 0, 3, 3, 3, null, null, null, 0);
                this.add((Component) component, componentConstraints);
            }
            inputs.put(inputTemplate.getName(), component);
        }
    }

    // sort function templates, put boolean parameters to the last
    private int compareFunctionSettingsTemplate(@Nonnull FunctionSettingTemplate first, @Nonnull FunctionSettingTemplate second) {
        if (!StringUtils.equalsAnyIgnoreCase("boolean", first.getValue(), second.getValue())) {
            return 0;
        }
        return StringUtils.equals(first.getValue(), second.getValue()) ? 0 :
                StringUtils.equalsIgnoreCase(first.getValue(), second.getValue()) ? -1 : 1;
    }

    private FunctionSettingTemplate getFunctionSettingsTemplate(final String prompt) {
        return Optional.ofNullable(binding.getSettingTemplateByName(prompt))
                .orElseGet(() -> {
                    final FunctionSettingTemplate functionSettingTemplate = new FunctionSettingTemplate();
                    functionSettingTemplate.setName(prompt);
                    functionSettingTemplate.setValue("string");
                    return functionSettingTemplate;
                });
    }

    @Nonnull
    private AzureFormInputComponent<String> createComponent(@Nullable FunctionSettingTemplate inputTemplate) {
        if (inputTemplate == null) {
            return new AzureTextInput();
        }
        switch (inputTemplate.getValue()) {
            case "boolean":
                return new FunctionBooleanInput(inputTemplate);
            case "enum":
                return new FunctionEnumInput(inputTemplate);
            case "string":
            default:
                return new FunctionStringInput(inputTemplate);
        }
    }

    @Override
    public Map<String, String> getValue() {
        final Map<String, String> result = new HashMap<>();
        inputs.forEach((parameter, input) -> result.put(parameter, input.getValue()));
        return result;
    }

    @Override
    public void setValue(@Nonnull final Map<String, String> data) {
        data.forEach((parameter, value) -> Optional.ofNullable(inputs.get(parameter)).ifPresent(input -> input.setValue(value)));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return new ArrayList<>(inputs.values());
    }
}
