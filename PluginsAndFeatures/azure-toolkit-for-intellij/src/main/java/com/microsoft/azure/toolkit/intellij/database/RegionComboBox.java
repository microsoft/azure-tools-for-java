/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class RegionComboBox extends AzureComboBox<Region> {

    @Getter
    private Subscription subscription;
    @Setter
    private Function<RegionComboBox, AzureValidationInfo> validateFunction;

    private boolean validateRequired;
    private boolean itemLoaded;
    private AzureValidationInfo validatedInfo;

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "mysql|region.list.supported",
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends Region> loadItems() {
        if (Objects.isNull(subscription)) {
            return new ArrayList<>();
        }
        List<? extends Region> regions = Azure.az(AzureAccount.class).listRegions(subscription.getId());
        itemLoaded = true;
        return regions;
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Region) {
            return ((Region) item).getLabel();
        }
        return super.getItemText(item);
    }

    @Override
    public void setValue(Region value) {
        if (!Objects.equals(value, getValue())) {
            super.setValue(value);
            validateRequired = true;
        } else {
            super.setValue(value);
        }
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @NotNull
    @Override
    public AzureValidationInfo doValidate() {
        if (Objects.isNull(subscription) || !itemLoaded) {
            return AzureValidationInfo.UNINITIALIZED;
        }
        if (!validateRequired) {
            return Objects.nonNull(validatedInfo) ? validatedInfo : AzureValidationInfo.UNINITIALIZED;
        }
        validatedInfo = super.doValidate();
        if (AzureValidationInfo.OK.equals(validatedInfo)) {
            validatedInfo = validateFunction.apply(this);
        }
        validateRequired = false;
        return validatedInfo;
    }

}
