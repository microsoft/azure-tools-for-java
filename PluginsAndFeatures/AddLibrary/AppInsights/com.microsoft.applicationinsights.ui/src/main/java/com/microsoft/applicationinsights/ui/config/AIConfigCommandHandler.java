/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.ui.config;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azuretools.core.utils.AzureAbstractHandler;

/**
 * Command handler for Application Insights menu option.
 */
public class AIConfigCommandHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent arg0) throws ExecutionException {
        AIProjConfigWizardDialog dialog = new AIProjConfigWizardDialog(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
        dialog.create();
        dialog.open();
        return null;
    }
}
