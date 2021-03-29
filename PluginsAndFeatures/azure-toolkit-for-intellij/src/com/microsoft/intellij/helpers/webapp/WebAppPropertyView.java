/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2021 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers.webapp;

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
