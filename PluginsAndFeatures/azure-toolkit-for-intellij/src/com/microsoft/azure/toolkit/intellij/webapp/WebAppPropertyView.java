/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppPropertyViewPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

public class WebAppPropertyView extends WebAppBasePropertyView {
    private static final String ID = "com.microsoft.intellij.helpers.webapp.WebAppBasePropertyView";

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppBasePropertyView create(@NotNull final Project project, @NotNull final String sid,
                                                @NotNull final String webAppId) {
        WebAppPropertyView view = new WebAppPropertyView(project, sid, webAppId);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }

    private WebAppPropertyView(@NotNull final Project project, @NotNull final String sid,
                               @NotNull final String webAppId) {
        super(project, sid, webAppId, null);
    }

    @Override
    protected String getId() {
        return this.ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new WebAppPropertyViewPresenter();
    }
}
