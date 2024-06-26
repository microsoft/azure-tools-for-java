/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.ui.GroupedComboBoxRenderer;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfileType;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class WorkloadProfileTypeComboBox extends AzureComboBox<WorkloadProfileType> {
    private final String subscriptionId;
    private final Region region;

    public WorkloadProfileTypeComboBox(String subscriptionId, Region region) {
        super();
        this.subscriptionId = subscriptionId;
        this.region = region;
        this.setRenderer(new GroupedComboBoxRenderer<>(this){

            @Nullable
            @Override
            public String getSecondaryText(WorkloadProfileType type) {
                return String.format("vCPU: %s, RAM(GiB): %s", type.getCores(), type.getMemory());
            }

            @Nonnull
            @Override
            public String getText(WorkloadProfileType item) {
                return item.getDisplayName();
            }

            @Nullable
            @Override
            public ListSeparator separatorFor(WorkloadProfileType workloadProfileType) {
                final WorkloadProfileType firstProfile = getItems().stream()
                        .filter(profile -> StringUtils.equalsIgnoreCase(workloadProfileType.getCategory(), profile.getCategory()))
                        .findFirst().orElse(null);
                return Objects.equals(firstProfile, workloadProfileType) ? new ListSeparator(workloadProfileType.getCategory()) : null;
            }

            @Nullable
            @Override
            protected String getCaption(@Nullable JList<? extends WorkloadProfileType> list, WorkloadProfileType value) {
                return Optional.ofNullable(value).map(WorkloadProfileType::getCategory).orElse(null);
            }

            @Override
            protected boolean isSeparatorVisible(@Nullable JList<? extends WorkloadProfileType> list, WorkloadProfileType value) {
                final ListSeparator listSeparator = Optional.ofNullable(value).map(this::separatorFor).orElse(null);
                return Objects.nonNull(listSeparator);
            }
        });
    }

    @Nonnull
    @Override
    protected List<? extends WorkloadProfileType> loadItems() throws Exception {
        if (StringUtils.isBlank(subscriptionId) || Objects.isNull(region)) {
            return Collections.emptyList();
        }
        return Azure.az(AzureContainerApps.class).forSubscription(subscriptionId)
                .listAvailableWorkloadProfiles(region.getName()).stream()
                .filter(p -> !StringUtils.equalsIgnoreCase(p.getCategory(), WorkloadProfile.CONSUMPTION))
                .toList();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof WorkloadProfileType type ? String.format("%s (vCPU: %s, RAM: %s)", type.getDisplayName(), type.getCores(), type.getMemory()) : super.getItemText(item);
    }
}
