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
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotPropertyViewPresenter;

public class DeploymentSlotPropertyView extends AppBasePropertyView {
    private static final String ID = "com.microsoft.intellij.helpers.webapp.DeploymentSlotPropertyView";

    /**
     * Initialize the Web App Property View and return it.
     */
    public static AppBasePropertyView create(@NotNull final Project project, @NotNull final String sid,
                                             @NotNull final String resId, @NotNull final String slotName,
                                             @NotNull final VirtualFile virtualFile) {
        final DeploymentSlotPropertyView view = new DeploymentSlotPropertyView(project, sid, resId, slotName, virtualFile);
        view.onLoadWebAppProperty(sid, resId, slotName);
        return view;
    }

    private DeploymentSlotPropertyView(@NotNull final Project project, @NotNull final String sid,
                                       @NotNull final String webAppId, @NotNull final String slotName,
                                       @NotNull final VirtualFile virtualFile) {
        super(project, sid, webAppId, slotName, virtualFile);
    }

    @Override
    protected String getId() {
        return ID;
    }

    @Override
    protected WebAppBasePropertyViewPresenter createPresenter() {
        return new DeploymentSlotPropertyViewPresenter();
    }
}
