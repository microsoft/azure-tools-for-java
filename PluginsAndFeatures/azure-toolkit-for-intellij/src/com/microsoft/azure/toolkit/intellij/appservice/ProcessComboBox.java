/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.jfr.FlightRecorderManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ProcessComboBox extends AzureComboBox<ProcessInfo> {
    @Setter
    @Getter
    private WebAppBase appService;

    @NotNull
    @Override
    @AzureOperation(
        name = "appservice|flight_recorder.list.app",
        params = {"@appService.name()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<ProcessInfo> loadItems() throws Exception {
        if (Objects.isNull(appService)) {
            return Collections.emptyList();
        }
        return FlightRecorderManager.getFlightRecorderStarter(appService).listProcess();
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
            AllIcons.Actions.Refresh, message("common.refresh"), this::refreshItems);
    }

    protected String getItemText(Object item) {
        if (item == null) {
            return StringUtils.EMPTY;
        }
        if (item instanceof ProcessInfo) {
            ProcessInfo processInfo = (ProcessInfo) item;
            return String.format("[%d] %s", processInfo.getId(), StringUtils.abbreviate(processInfo.getName(), 50));
        }
        return item.toString();
    }

    protected Icon getItemIcon(Object item) {
        if (item instanceof ProcessInfo) {
            return AllIcons.Ide.LocalScope;
        }
        return null;
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
