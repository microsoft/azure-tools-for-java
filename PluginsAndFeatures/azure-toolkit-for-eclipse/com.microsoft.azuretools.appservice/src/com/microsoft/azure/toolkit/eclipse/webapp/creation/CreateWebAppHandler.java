/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp.creation;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;

public class CreateWebAppHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell();
        WebAppConfig defaultConfig = WebAppConfig.getWebAppDefaultConfig();
        defaultConfig.setRuntime(Runtime.LINUX_JAVA17);
        CreateWebAppDialog createDialog = new CreateWebAppDialog(shell, defaultConfig);
        createDialog.setOkActionListener(config -> {
            createDialog.close();
            AzureTaskManager.getInstance().runInBackground(AzureString.format("Creating web app %s", config.getName()),
                    () -> new CreateOrUpdateWebAppTask(WebAppConfig.convertToTaskConfig(config)).execute());
        });
        createDialog.open();
        return null;
    }
}
