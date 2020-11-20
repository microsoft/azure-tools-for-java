/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.jfr.FlightRecorderManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProcessComboBox extends AzureComboBox<ProcessInfo> {
    @Setter
    @Getter
    private WebAppBase appService;

    @NotNull
    @Override
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
                AllIcons.Actions.Refresh, "Refresh", this::refreshItems);
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
