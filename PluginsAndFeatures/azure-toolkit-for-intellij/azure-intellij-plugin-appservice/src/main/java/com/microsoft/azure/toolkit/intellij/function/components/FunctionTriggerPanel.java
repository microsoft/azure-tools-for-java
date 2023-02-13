/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function.components;

import com.intellij.icons.AllIcons;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.legacy.function.template.BindingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionSettingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateMetadata;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateResources;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

public class FunctionTriggerPanel implements AzureFormPanel<Map<String, String>> {
    public static final int DEFAULT_LABEL_WIDTH = 100;
    @Getter
    private JPanel pnlRoot;
    private JPanel pnlTemplate;
    private JPanel pnlCustomized;
    private JCheckBox chkMode;
    private final FunctionTemplate functionTemplate;

    private FunctionTemplatePanel templatePanel;
    private AzureFormPanel<Map<String, String>> customizedPanel;

    public FunctionTriggerPanel(@Nonnull FunctionTemplate template) {
        super();
        this.functionTemplate = template;
        // generate components based on template
        $$$setupUI$$$();
        init();
    }

    private void init() {
        buildTemplatePanel(functionTemplate);
        buildCustomizedPanel(functionTemplate);
        setLabelWidth(getLabelWidth());
        chkMode.addItemListener(ignore -> onModeChanged());
        chkMode.setVisible(Objects.nonNull(customizedPanel));
        chkMode.setSelected(Objects.nonNull(customizedPanel));
    }

    private void onModeChanged() {
        pnlCustomized.setVisible(chkMode.isSelected());
        pnlTemplate.setVisible(!chkMode.isSelected());
    }

    private void buildCustomizedPanel(@Nonnull final FunctionTemplate template) {
        this.customizedPanel = BindingPanelProvider.createPanel(template.getBinding());
        if (customizedPanel instanceof Component) {
            final GridConstraints constraints = new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
            this.pnlCustomized.add((Component) customizedPanel, constraints);
        }
    }

    private void buildTemplatePanel(@Nonnull final FunctionTemplate template) {
        this.templatePanel = new FunctionTemplatePanel(template);
        final GridConstraints constraints = new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
        this.pnlTemplate.add(templatePanel, constraints);
    }

    public void setLabelWidth(int width) {
        getLabels().forEach(component -> {
            component.setMinimumSize(new Dimension(width, (int) component.getPreferredSize().getHeight()));
            component.setMaximumSize(new Dimension(width, (int) component.getPreferredSize().getHeight()));
            component.setPreferredSize(new Dimension(width, (int) component.getPreferredSize().getHeight()));
        });
    }

    private List<JLabel> getLabels() {
        final List<JLabel> result = new ArrayList<>();
        final Queue<JComponent> components = new ArrayDeque<>();
        components.add(templatePanel);
        Optional.ofNullable(customizedPanel).ifPresent(panel -> components.add((JComponent) customizedPanel));
        while (!components.isEmpty()) {
            final JComponent component = components.poll();
            if (component instanceof JLabel) {
                result.add((JLabel) component);
            } else {
                Arrays.stream(component.getComponents()).filter(c -> c instanceof JComponent)
                        .map(c -> (JComponent) c).forEach(components::add);
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getValue() {
        return getActivePanel().getValue();
    }

    @Override
    public void setValue(@Nonnull final Map<String, String> data) {
        getActivePanel().setValue(data);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return getActivePanel().getInputs();
    }

    private AzureFormPanel<Map<String, String>> getActivePanel() {
        return chkMode.isSelected() ? customizedPanel : templatePanel;
    }

    @Cacheable(value = "functionClassCreationLabelWidth")
    public static int getLabelWidth() {
        final Icon help = AllIcons.General.ContextHelp;
        final int maxLabelWidth = FunctionUtils.loadAllFunctionTemplates().stream()
                .map(FunctionTriggerPanel::getTemplateLabels)
                .flatMap(List::stream)
                .map(label -> new JLabel(label).getPreferredSize().getWidth())
                .max(Double::compare).orElse(0.0).intValue();
        return Math.max(DEFAULT_LABEL_WIDTH, maxLabelWidth + help.getIconWidth());
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

