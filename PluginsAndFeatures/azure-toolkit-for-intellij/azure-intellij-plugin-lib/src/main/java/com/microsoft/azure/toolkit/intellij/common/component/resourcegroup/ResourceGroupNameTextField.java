/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common.component.resourcegroup;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class ResourceGroupNameTextField extends AzureTextInput {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9._()-]+[a-z0-9_()-]$");
    @Setter
    private Subscription subscription;

    public ResourceGroupNameTextField() {
        this.setValidator(this::doValidateValue);
        this.setRequired(true);
    }

    public AzureValidationInfo doValidateValue() {
        final String value = this.getValue();
        // validate length
        int minLength = 1;
        int maxLength = 90;
        if (StringUtils.length(value) < minLength) {
            return AzureValidationInfo.builder().input(this)
                .message("The value must not be empty.")
                .type(AzureValidationInfo.Type.ERROR).build();
        } else if (StringUtils.length(value) > maxLength) {
            return AzureValidationInfo.builder().input(this)
                .message(String.format("Resource group names only allow up to %s characters.", maxLength))
                .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special character
        if (!PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.builder().input(this)
                .message("Resource group names only allow alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.")
                .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate availability
        try {
            if (!Azure.az(AzureGroup.class).checkNameAvailability(subscription.getId(), value)) {
                return AzureValidationInfo.builder().input(this).message(value + " already existed.").type(AzureValidationInfo.Type.ERROR).build();
            }
        } catch (CloudException e) {
            return AzureValidationInfo.builder().input(this).message(e.getMessage()).type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.success(this);
    }
}
