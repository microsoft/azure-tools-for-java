/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.*;

public class WorkloadProfileComboBox extends AzureComboBox<WorkloadProfile> {

    private ContainerAppsEnvironment environment;

    public void setEnvironment(@Nullable final ContainerAppsEnvironment environment) {
        if (Objects.equals(environment, this.environment)) {
            return;
        }
        this.environment = environment;
        if (environment == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nonnull
    @Override
    protected List<? extends WorkloadProfile> loadItems() throws Exception {
        final List<WorkloadProfile> workloadProfiles = Optional.ofNullable(environment).map(ContainerAppsEnvironment::getWorkloadProfiles).orElse(Collections.emptyList());
        if (workloadProfiles.contains(WorkloadProfile.CONSUMPTION_PROFILE)) {
            return workloadProfiles;
        } else {
            final List<WorkloadProfile> result = new ArrayList<>(workloadProfiles);
            result.add(WorkloadProfile.CONSUMPTION_PROFILE);
            return result;
        }
    }

    @Override
    public String getItemText(Object item) {
        if (item instanceof WorkloadProfile profile) {
            return String.format(String.format("%s (%s)", profile.getName(), profile.getWorkloadProfileType()));
        }
        return super.getItemText(item);
    }
}
