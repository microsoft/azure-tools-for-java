/*
 * Copyright (c) Microsoft Corporation
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
package com.microsoft.intellij.helpers.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.intellij.helpers.webapp.WebAppBasePropertyView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class FunctionAppPropertyView extends WebAppBasePropertyView {
    private static final String ID = "com.microsoft.intellij.helpers.function.FunctionAppPropertyView";

    public static WebAppBasePropertyView create(@NotNull final Project project, @NotNull final String sid,
                                                @NotNull final String webAppId) {
        final FunctionAppPropertyView view = new FunctionAppPropertyView(project, sid, webAppId);
        view.onLoadWebAppProperty(sid, webAppId, null);
        return view;
    }


    protected FunctionAppPropertyView(@NotNull Project project, @NotNull String sid, @NotNull String resId) {
        super(project, sid, resId, null);
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
