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
package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * Created by vlashch on 6/10/16.
 */
public class AzureCodeSamples extends AzureAnAction {
    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        JXHyperlink portalLing = new JXHyperlink();
        portalLing.setURI(URI.create("https://azure.microsoft.com/en-us/documentation/samples/?platform=java"));
        portalLing.doClick();
        return true;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.SYSTEM;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.AZURECODE_SAMPLES;
    }
}
