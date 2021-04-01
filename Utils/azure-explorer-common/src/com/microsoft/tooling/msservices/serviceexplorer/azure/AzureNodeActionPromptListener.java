/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure;

import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.awt.Component;
import java.util.concurrent.Callable;

public abstract class AzureNodeActionPromptListener extends AzureNodeActionListener {
    private static final String PROMPT_TITLE = "Azure Explorer";
    private static final String[] PROMPT_OPTIONS = new String[]{"Yes", "No"};

    private final String promptMessage;
    private boolean optionDialog;

    public AzureNodeActionPromptListener(@NotNull Node azureNode,
                                         @NotNull String promptMessage,
                                         @NotNull String progressMessage) {
        super(azureNode, progressMessage);
        this.promptMessage = promptMessage;
    }

    @NotNull
    @Override
    protected Callable<Boolean> beforeAsyncActionPerformed() {
        return () -> {
            AzureTaskManager.getInstance().runAndWait(() -> {
                final Component component = (azureNode == null || azureNode.getTree() == null) ? null : this.azureNode.getTree().getParent();
                optionDialog = component == null ?
                        DefaultLoader.getUIHelper().showConfirmation(promptMessage, PROMPT_TITLE, PROMPT_OPTIONS, null) :
                        DefaultLoader.getUIHelper().showConfirmation(component, promptMessage, PROMPT_TITLE, PROMPT_OPTIONS, null);
            }, AzureTask.Modality.ANY);
            return optionDialog;
        };
    }
}
