/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppBasePropertyView;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

public class FunctionAppPropertyView extends WebAppBasePropertyView {
    private static final String ID = "com.microsoft.azure.toolkit.intellij.function.FunctionAppPropertyView";

    public static WebAppBasePropertyView create(@Nonnull final Project project, @Nonnull final String sid,
                                                @Nonnull final String webAppId, @Nonnull final VirtualFile virtualFile) {
        final FunctionAppPropertyView view = new FunctionAppPropertyView(project, sid, webAppId, virtualFile);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }


    protected FunctionAppPropertyView(@Nonnull Project project, @Nonnull String sid, @Nonnull String resId, @Nonnull final VirtualFile virtualFile) {
        super(project, sid, resId, null, virtualFile);
        AzureEventBus.after("function.start", this::onAppServiceStatusChanged);
        AzureEventBus.after("function.stop", this::onAppServiceStatusChanged);
        AzureEventBus.after("function.restart", this::onAppServiceStatusChanged);
        AzureEventBus.after("function.delete", this::onAppServiceStatusChanged);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new WebAppBasePropertyViewPresenter() {
            @Override
            protected IFunctionApp getWebAppBase(String subscriptionId, String functionAppId, String name) {
                return Azure.az(AzureAppService.class).subscription(subscriptionId).functionApp(functionAppId);
            }

            @Override
            protected void updateAppSettings(String subscriptionId, String functionAppId, String name, Map toUpdate, Set toRemove) {
                final IFunctionApp functionApp = getWebAppBase(subscriptionId, functionAppId, name);
                final IAppServiceUpdater appServiceUpdater = functionApp.update();
                appServiceUpdater.withAppSettings(toUpdate);
                toRemove.forEach(key -> appServiceUpdater.withoutAppSettings((String) key));
                appServiceUpdater.commit();
            }
        };
    }
}
