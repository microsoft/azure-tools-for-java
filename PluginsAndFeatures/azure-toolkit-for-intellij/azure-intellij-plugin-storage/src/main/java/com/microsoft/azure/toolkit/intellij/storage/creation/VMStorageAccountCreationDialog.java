/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.storage.model.Kind;
import com.microsoft.azure.toolkit.lib.storage.model.Performance;
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy;

import java.util.Arrays;

public class VMStorageAccountCreationDialog extends BaseStorageAccountCreationDialog {

    public VMStorageAccountCreationDialog(Project project) {
        super(project);
    }

    @Override
    protected void init() {
        super.init();
        performanceComboBox.setValue(Performance.STANDARD, true);
        kindComboBox.setItemsLoader(() -> Arrays.asList(Kind.STORAGE_V2, Kind.STORAGE));
        redundancyComboBox.setItemsLoader(() -> Arrays.asList(Redundancy.PREMIUM_LRS, Redundancy.STANDARD_GRS, Redundancy.STANDARD_RAGRS));
    }
}
