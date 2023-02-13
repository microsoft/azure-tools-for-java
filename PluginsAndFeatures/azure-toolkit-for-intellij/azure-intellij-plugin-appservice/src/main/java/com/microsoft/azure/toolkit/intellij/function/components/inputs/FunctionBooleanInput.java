/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function.components.inputs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionSettingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateResources;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Optional;

public class FunctionBooleanInput extends JPanel implements AzureFormInputComponent<String> {
    @Getter
    private JPanel pnlRoot;
    private JCheckBox chkEnable;

    private final FunctionSettingTemplate template;

    public FunctionBooleanInput(@Nonnull final FunctionSettingTemplate template) {
        this.template = template;
        init();
        this.setLayout(new GridLayoutManager(1, 1));
        final GridConstraints labelConstraints = new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
        this.add(pnlRoot, labelConstraints);
    }

    private void init() {
        final String description = Optional.ofNullable(template.getLabel()).map(TemplateResources::getResource)
                .filter(StringUtils::isNoneBlank).orElseGet(template::getName);
        this.chkEnable.setText(description);
        final String defaultValue = Optional.ofNullable(template.getDefaultValue()).orElseGet(Boolean.FALSE::toString);
        this.setValue(defaultValue);
        this.setRequired(template.isRequired());
    }

    @Override
    public String getValue() {
        return String.valueOf(chkEnable.isSelected());
    }

    @Override
    public void setValue(String value) {
        chkEnable.setSelected(Boolean.parseBoolean(template.getDefaultValue()));
    }
}
