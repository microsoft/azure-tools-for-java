/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.base;

import com.azure.core.implementation.http.HttpClientProviders;
import com.intellij.ide.AppLifecycleListener;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.intellij.common.task.IntellijAzureTaskManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.model.AuthType;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureRxTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Hooks;

import java.util.List;

public class PluginLifeCycleListener implements AppLifecycleListener {
    public static final String PLUGIN_ID = AzurePlugin.PLUGIN_ID;

    static {
        // fix the class load problem for intellij plugin
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(PluginLifeCycleListener.class.getClassLoader());
            HttpClientProviders.createInstance();
            Azure.az(AzureAccount.class);

            Hooks.onErrorDropped(ex -> AzureMessager.getMessager().error(ex));
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        AzureTaskManager.register(new IntellijAzureTaskManager());
        AzureRxTaskManager.register();
        AzureMessager.setDefaultMessager(new IntellijAzureMessager());
        IntellijAzureActionManager.register();
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            Azure.az(AzureAccount.class).login(AuthType.AZURE_CLI);
        });
    }
}
