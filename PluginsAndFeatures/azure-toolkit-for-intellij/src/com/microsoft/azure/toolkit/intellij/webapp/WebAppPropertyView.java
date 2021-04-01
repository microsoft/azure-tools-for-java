/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.helpers.base.AppBasePropertyView;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

public class WebAppPropertyView extends AppBasePropertyView {
    private static final String ID = "com.microsoft.intellij.helpers.webapp.WebAppBasePropertyView";

    /**
     * Initialize the Web App Property View and return it.
     */
    public static AppBasePropertyView create(@NotNull final Project project, @NotNull final String sid,
                                             @NotNull final String webAppId, @NotNull final VirtualFile virtualFile) {
        final WebAppPropertyView view = new WebAppPropertyView(project, sid, webAppId, virtualFile);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }

    private WebAppPropertyView(@NotNull final Project project, @NotNull final String sid,
                               @NotNull final String webAppId, @NotNull final VirtualFile virtualFile) {
        super(project, sid, webAppId, null, virtualFile);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new WebAppPropertyViewPresenter();
    }
}
