/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.storage.component;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.table.TableModule;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class StorageNameTextField extends AzureTextInput {
    private static final Pattern STORAGE_NAME_PATTERN = Pattern.compile("^[a-z0-9-]{3,63}$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-z0-9]{3,36}$");
    @Setter
    private AbstractAzResourceModule<?, ? extends IStorageAccount, ?> module;

    public StorageNameTextField() {
        super();
        this.addValidator(this::doValidateValue);
        this.setRequired(true);
    }

    public AzureValidationInfo doValidateValue() {
        if (this.module instanceof TableModule) {
            return this.doValidateTableValue();
        }
        final String value = StringUtils.trim(this.getValue());
        final String type = this.module.getResourceTypeName();
        if (!STORAGE_NAME_PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.error(type + " name can only contain lowercase letters, digits and hyphens and must be between 3 and 63 characters", this);
        } else if (value.contains("--")) {
            return AzureValidationInfo.error(type + " name cannot contain two hyphens in a row", this);
        } else if (value.endsWith("-") || value.startsWith("-")) {
            return AzureValidationInfo.error(type + " name cannot begin or end with a hyphen", this);
        } else if (this.module.exists(value, this.module.getParent().getResourceGroupName())) {
            return AzureValidationInfo.error(type + " with the same name already exists", this);
        }
        return AzureValidationInfo.success(this);
    }

    public AzureValidationInfo doValidateTableValue() {
        final String value = StringUtils.trim(this.getValue());
        if (StringUtils.equalsIgnoreCase(value, "tables")) {
            return AzureValidationInfo.error("Table name can not be \"tables\"", this);
        } else if (!TABLE_NAME_PATTERN.matcher(value).matches()) {
            return AzureValidationInfo.error("Table name can only contain lowercase letters and digits and must be between 3 and 36 characters", this);
        } else if (Character.isDigit(value.charAt(0))) {
            return AzureValidationInfo.error("Table name cannot begin with a digit", this);
        } else if (this.module.exists(value, this.module.getParent().getResourceGroupName())) {
            return AzureValidationInfo.error("Table with the same name already exists", this);
        }
        return AzureValidationInfo.success(this);
    }
}
