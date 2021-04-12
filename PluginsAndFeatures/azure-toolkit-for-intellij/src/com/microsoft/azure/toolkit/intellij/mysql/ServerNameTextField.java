/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

public class ServerNameTextField extends ValidationDebouncedTextInput {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]+[a-z0-9]$");
    private Subscription subscription;
    private ResourceGroup resourceGroup;

    public void setSubscription(Subscription subscription) {
        if (!Objects.equals(subscription, this.subscription)) {
            this.subscription = subscription;
        }
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        if (!Objects.equals(resourceGroup, this.resourceGroup)) {
            this.resourceGroup = resourceGroup;
            this.revalidateValue();
        }
    }

    /**
     * Server name must be at least 3 characters and at most 63 characters.
     * Server name must only contain lowercase letters, numbers, and hyphens. The server name must not start or end in a hyphen.
     * Server name must be available.
     */
    @NotNull
    public AzureValidationInfo doValidateValue() {
        final AzureValidationInfo info = super.doValidateValue();
        if (!AzureValidationInfo.OK.equals(info)) {
            return info;
        }
        final String value = this.getValue();
        // validate length
        if (StringUtils.length(value) < 3 || StringUtils.length(value) > 63) {
            return AzureValidationInfo.builder().input(this).message("Server name must be at least 3 characters and at most 63 characters.")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate special character
        if (!PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.builder().input(this)
                    .message("Server name must only contain lowercase letters, numbers, and hyphens. The server name must not start or end in a hyphen.")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate availability
        if (!MySQLMvpModel.checkNameAvailabilitys(subscription.subscriptionId(), value)) {
            return AzureValidationInfo.builder().input(this).message(this.getValue() + " already existed.").type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public boolean isRequired() {
        return true;
    }

}
