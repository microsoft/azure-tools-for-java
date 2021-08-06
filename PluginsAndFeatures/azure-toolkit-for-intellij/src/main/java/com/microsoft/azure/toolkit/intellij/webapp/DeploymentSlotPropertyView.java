/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotPropertyViewPresenter;

import javax.annotation.Nonnull;

public class DeploymentSlotPropertyView extends WebAppBasePropertyView {
    private static final String ID = "com.microsoft.intellij.helpers.webapp.DeploymentSlotPropertyView";

    /**
     * Initialize the Web App Property View and return it.
     */
    public static WebAppBasePropertyView create(@NotNull final Project project, @NotNull final String sid,
                                                @NotNull final String resId, @NotNull final String slotName, @Nonnull VirtualFile virtualFile) {
        DeploymentSlotPropertyView view = new DeploymentSlotPropertyView(project, sid, resId, slotName, virtualFile);
        view.onLoadWebAppProperty(sid, resId, slotName);
        return view;
    }

    private DeploymentSlotPropertyView(@NotNull final Project project, @NotNull final String sid,
                                       @NotNull final String webAppId, @NotNull final String slotName, @Nonnull VirtualFile virtualFile) {
        super(project, sid, webAppId, slotName, virtualFile);
    }

    @Override
    protected String getId() {
        return this.ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new DeploymentSlotPropertyViewPresenter();
    }
}
