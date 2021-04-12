/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.management.appservice.LogLevel;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.Arrays;
import java.util.List;

public class LogLevelComboBox extends AzureComboBox<LogLevel> {

    @NotNull
    @Override
    protected List<? extends LogLevel> loadItems() throws Exception {
        return Arrays.asList(LogLevel.ERROR, LogLevel.WARNING, LogLevel.INFORMATION, LogLevel.VERBOSE);
    }
}
