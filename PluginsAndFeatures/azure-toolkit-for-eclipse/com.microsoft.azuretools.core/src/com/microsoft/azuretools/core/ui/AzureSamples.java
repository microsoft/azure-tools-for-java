/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui;

import java.net.URL;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.core.ui.commoncontrols.Messages;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class AzureSamples  extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent executionEvent) throws ExecutionException {
        try {
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(Messages.azureSamplesPageLinkMS));
        } catch (Exception ex) {
            PluginUtil.displayErrorDialogAndLog(PluginUtil.getParentShell(),
                    Messages.error,
                    Messages.azureSamplesDlgErMsg, ex);
        }
        return null;
    }
}
