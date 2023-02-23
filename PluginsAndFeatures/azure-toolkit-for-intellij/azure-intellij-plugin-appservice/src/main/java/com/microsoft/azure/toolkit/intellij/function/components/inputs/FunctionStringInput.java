/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components.inputs;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionSettingTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.template.TemplateResources;
import com.microsoft.azure.toolkit.lib.legacy.function.template.ValidatorTemplate;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

public class FunctionStringInput extends AzureTextInput {

    private final FunctionSettingTemplate template;

    public FunctionStringInput(@Nonnull final FunctionSettingTemplate template) {
        super();
        this.template = template;
        init();
    }

    private void init() {
        if (template == null) {
            return;
        }
        this.setRequired(template.isRequired());
        Optional.ofNullable(template.getDefaultValue()).ifPresent(this::setValue);
        Optional.ofNullable(template.getValidators()).map(FunctionStringInputValidator::new).ifPresent(this::addValidator);
    }

    @AllArgsConstructor
    class FunctionStringInputValidator implements Validator {
        private ValidatorTemplate[] validators;

        @Override
        public AzureValidationInfo doValidate() {
            final String value = FunctionStringInput.super.getValue();
            return Arrays.stream(validators).filter(rule -> !value.matches(rule.getExpression()))
                    .map(rule -> AzureValidationInfo.error(TemplateResources.getResource(rule.getErrorText()), FunctionStringInput.this))
                    .findFirst()
                    .orElseGet(() -> AzureValidationInfo.success(FunctionStringInput.this));
        }
    }
}
