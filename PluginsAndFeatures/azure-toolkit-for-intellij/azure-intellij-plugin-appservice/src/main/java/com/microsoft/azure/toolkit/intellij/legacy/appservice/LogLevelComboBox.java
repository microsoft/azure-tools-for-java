/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LogLevelComboBox extends AzureComboBox<LogLevel> {

    @Nonnull
    @Override
    protected List<? extends LogLevel> loadItems() throws Exception {
        return Arrays.asList(LogLevel.ERROR, LogLevel.WARNING, LogLevel.INFORMATION, LogLevel.VERBOSE);
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof LogLevel ? ((LogLevel) item).getValue() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
