/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.icons.AllIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.legacy.appservice.jfr.FlightRecorderManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProcessComboBox extends AzureComboBox<ProcessInfo> {
    @Setter
    @Getter
    private AppServiceAppBase<?, ?, ?> appService;

    @Nonnull
    @Override
    @AzureOperation(
        name = "appservice.list_flight_recorders.app",
        params = {"this.appService.name()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<ProcessInfo> loadItems() throws Exception {
        if (Objects.isNull(appService)) {
            return Collections.emptyList();
        }
        return FlightRecorderManager.getFlightRecorderStarter(appService).listProcess();
    }

    protected String getItemText(Object item) {
        if (item == null) {
            return StringUtils.EMPTY;
        }
        if (item instanceof ProcessInfo) {
            final ProcessInfo processInfo = (ProcessInfo) item;
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
