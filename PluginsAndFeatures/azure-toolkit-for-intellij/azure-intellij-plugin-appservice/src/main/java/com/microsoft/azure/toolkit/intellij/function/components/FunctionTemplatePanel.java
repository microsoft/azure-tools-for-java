/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.function.components.connection.FunctionConnectionComboBox;
import com.microsoft.azure.toolkit.intellij.function.components.inputs.FunctionBooleanInput;
import com.microsoft.azure.toolkit.intellij.function.components.inputs.FunctionEnumInput;
import com.microsoft.azure.toolkit.intellij.function.components.inputs.FunctionStringInput;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionSettingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateMetadata;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateResources;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionTemplatePanel extends JPanel implements AzureFormPanel<Map<String, String>> {
    public static final String BOOLEAN = "boolean";
    public static final FunctionSettingTemplate LOCAL_SETTINGS_JSON_TEMPLATE = FunctionSettingTemplate.builder().name("Local Settings").build();
    public static final int DEFAULT_LABEL_WIDTH = 100;
    private final FunctionTemplate template;
    private final BindingTemplate binding;
    private final Module module;
    private final Map<String, AzureFormInputComponent<String>> inputs = new HashMap<>();
    private FunctionConnectionComboBox connectionComboBox;

    public FunctionTemplatePanel(@Nonnull FunctionTemplate template, @Nonnull Module module) {
        super();
        this.template = template;
        this.module = module;
        this.binding = this.template.getBinding();
        // generate components based on template
        initComponents();
    }

    private void initComponents() {
        final List<FunctionSettingTemplate> templates = getSettingTemplates();
        if (CollectionUtils.isEmpty(templates)) {
            return;
        }
        addComponentsByTemplates(templates);
    }

    private void addComponentsByTemplates(final List<FunctionSettingTemplate> templates) {
        final Dimension labelSize = new Dimension(getLabelWidth(), -1);
        final Dimension componentSize = new Dimension(getLabelWidth() * 3, -1);
        this.setLayout(new GridLayoutManager(templates.size(), 2));
        for (int i = 0; i < templates.size(); i++) {
            final FunctionSettingTemplate inputTemplate = templates.get(i);
            final AzureFormInputComponent<?> component = createComponent(inputTemplate);
            component.setRequired(inputTemplate.isRequired());
            if (StringUtils.equalsIgnoreCase(inputTemplate.getValue(), BOOLEAN)) {
                final GridConstraints booleanConstraints = new GridConstraints(i, 0, 1, 2, 0, 3, 3, 3, null, null, null, 0);
                this.add((Component) component, booleanConstraints);
            } else {
                final String labelText = Optional.ofNullable(inputTemplate.getLabel()).map(TemplateResources::getResource)
                        .map(value -> String.format("%s:", WordUtils.capitalize(value)))
                        .orElseGet(() -> String.format("%s:", inputTemplate.getName()));
                final JLabel label = new JLabel(StringUtils.capitalize(labelText));
                Optional.ofNullable(inputTemplate.getHelp()).map(TemplateResources::getResource)
                        .filter(StringUtils::isNotEmpty)
                        .ifPresent(description -> {
                            label.setIcon(AllIcons.General.ContextHelp);
                            label.setHorizontalTextPosition(JLabel.LEADING);
                            label.setToolTipText(description);
                        });
                label.setLabelFor((Component) component);
                final GridConstraints labelConstraints = new GridConstraints(i, 0, 1, 1, 0, 0, 0, 0, labelSize, labelSize, labelSize, 0);
                this.add(label, labelConstraints);
                final GridConstraints componentConstraints = new GridConstraints(i, 1, 1, 1, 0, 3, 3, 3, null, componentSize, null, 0);
                this.add((Component) component, componentConstraints);
            }
        }
    }

    private List<FunctionSettingTemplate> getSettingTemplates() {
        final List<String> userInputs = Optional.ofNullable(template.getMetadata())
                .map(TemplateMetadata::getUserPrompt).orElse(Collections.emptyList());
        return userInputs.stream()
                .map(this::getFunctionSettingsTemplate)
                .sorted(this::compareFunctionSettingsTemplate).collect(Collectors.toList());
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
                .orElseGet(() -> FunctionSettingTemplate.builder().name(prompt).value("string").build());
    }

    @Nonnull
    private AzureFormInputComponent<?> createComponent(@Nullable FunctionSettingTemplate inputTemplate) {
        if (inputTemplate == null) {
            return new AzureTextInput();
        } else if (StringUtils.isNotEmpty(inputTemplate.getResource())) {
            this.connectionComboBox = new FunctionConnectionComboBox(module, inputTemplate.getResource(), inputTemplate.getName());
            this.connectionComboBox.setUsePreferredSizeAsMinimum(false);
            return this.connectionComboBox;
        }
        final AzureFormInputComponent<String> result;
        switch (inputTemplate.getValue()) {
            case "boolean":
                result = new FunctionBooleanInput(inputTemplate);
                break;
            case "enum":
                result = new FunctionEnumInput(inputTemplate);
                break;
            case "string":
            default:
                result = new FunctionStringInput(inputTemplate);
                break;
        }
        inputs.put(inputTemplate.getName(), result);
        return result;
    }

    private void onSelectLocalSettings(final VirtualFile localSettingsFile) {
        Optional.ofNullable(connectionComboBox).ifPresent(combo -> combo.setLocalSettings(localSettingsFile));
    }

    @Override
    public Map<String, String> getValue() {
        final Map<String, String> result = new HashMap<>();
        inputs.forEach((parameter, input) -> result.put(parameter, input.getValue()));
        Optional.ofNullable(connectionComboBox).map(FunctionConnectionComboBox::getValue)
                .ifPresent(config -> result.put(connectionComboBox.getPropertyName(), config.getName()));
        return result;
    }

    @Override
    public void setValue(@Nonnull final Map<String, String> data) {
        data.forEach((parameter, value) -> Optional.ofNullable(inputs.get(parameter)).ifPresent(input -> input.setValue(value)));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final ArrayList<AzureFormInput<?>> result = new ArrayList<>(inputs.values());
        Optional.ofNullable(connectionComboBox).ifPresent(result::add);
        return result;
    }

    @Cacheable(value = "functionClassCreationLabelWidth")
    public static int getLabelWidth() {
        final Icon help = AllIcons.General.ContextHelp;
        final int maxLabelWidth = FunctionUtils.loadAllFunctionTemplates().stream()
                .map(FunctionTemplatePanel::getTemplateLabels)
                .flatMap(List::stream)
                .map(label -> new JLabel(label).getPreferredSize().getWidth())
                .max(Double::compare).orElse(0.0).intValue();
        return Math.max(DEFAULT_LABEL_WIDTH, maxLabelWidth + help.getIconWidth() + 5);
    }

    private static List<String> getTemplateLabels(final FunctionTemplate functionTemplate) {
        final BindingTemplate binding = functionTemplate.getBinding();
        final List<String> prompts = Optional.ofNullable(functionTemplate.getMetadata())
                .map(TemplateMetadata::getUserPrompt)
                .orElse(Collections.emptyList());
        return Objects.isNull(binding) ? prompts : prompts.stream()
                .map(binding::getSettingTemplateByName).filter(Objects::nonNull)
                .filter(settingTemplate -> !StringUtils.equalsIgnoreCase(settingTemplate.getValue(), "boolean"))
                .map(FunctionSettingTemplate::getLabel)
                .map(TemplateResources::getResource).collect(Collectors.toList());
    }
}
