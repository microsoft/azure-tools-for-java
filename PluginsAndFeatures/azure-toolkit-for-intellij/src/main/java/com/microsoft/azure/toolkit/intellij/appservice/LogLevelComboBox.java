/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.Arrays;
import java.util.List;

public class LogLevelComboBox extends AzureComboBox<LogLevel> {

    @NotNull
    @Override
    protected List<? extends LogLevel> loadItems() throws Exception {
        return Arrays.asList(LogLevel.ERROR, LogLevel.WARNING, LogLevel.INFORMATION, LogLevel.VERBOSE);
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof LogLevel ? ((LogLevel) item).getValue() : super.getItemText(item);
    }
}
