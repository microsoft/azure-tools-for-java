/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachine;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NetworkAvailabilityOptionsComboBox extends AzureComboBox<String> {

    public static final String DISABLE = "No infrastructure redundancy required";

    private Subscription subscription;

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    @Override
    public String getValue() {
        final String value = super.getValue();
        return StringUtils.equalsIgnoreCase(value, DISABLE) ? null : value;
    }

    @Override
    public void setValue(String value) {
        if (value == null) {
            super.setValue(DISABLE);
        } else {
            super.setValue(value);
        }
    }

    @Nonnull
    @Override
    protected List<? extends String> loadItems() throws Exception {
        final List<String> availabilitySets = Optional.ofNullable(subscription).map(subscription -> Azure.az(AzureVirtualMachine.class).availabilitySets(subscription.getId())).orElse(Collections.emptyList());
        return ListUtils.union(Arrays.asList(DISABLE), availabilitySets);
    }
}
