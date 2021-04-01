/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.intellij.webapp.WebAppBasePropertyView;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.intellij.helpers.base.AppBasePropertyView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class FunctionAppPropertyView extends AppBasePropertyView {
    private static final String ID = "com.microsoft.azure.toolkit.intellij.function.FunctionAppPropertyView";

    public static AppBasePropertyView create(@NotNull final Project project, @NotNull final String sid,
                                             @NotNull final String webAppId, @NotNull final VirtualFile virtualFile) {
        final FunctionAppPropertyView view = new FunctionAppPropertyView(project, sid, webAppId, virtualFile);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }


    protected FunctionAppPropertyView(@NotNull Project project, @NotNull String sid, @NotNull String resId, @NotNull final VirtualFile virtualFile) {
        super(project, sid, resId, null, virtualFile);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new WebAppBasePropertyViewPresenter() {
            @Override
            protected WebAppBase getWebAppBase(String subscriptionId, String functionAppId, String name) throws IOException {
                return AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, functionAppId);
            }

            @Override
            protected void updateAppSettings(String subscriptionId, String functionAppId, String name, Map toUpdate, Set toRemove)
                    throws IOException {
                AzureFunctionMvpModel.getInstance().updateWebAppSettings(subscriptionId, functionAppId, toUpdate, toRemove);
            }

            @Override
            protected boolean getPublishingProfile(String subscriptionId, String functionAppId, String name, String filePath)
                    throws IOException {
                return AzureFunctionMvpModel.getInstance().getPublishingProfileXmlWithSecrets(subscriptionId, functionAppId, filePath);
            }
        };
    }
}
