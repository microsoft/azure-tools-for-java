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

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

public class FunctionAppNodePresenter<V extends FunctionAppNodeView> extends MvpPresenter<V> {
    public void onStartFunctionApp(String subscriptionId, String appId) {
        AzureFunctionMvpModel.getInstance().startFunction(subscriptionId, appId);
        renderFunctionStatus(subscriptionId, appId);
    }

    public void onRestartFunctionApp(String subscriptionId, String appId) {
        AzureFunctionMvpModel.getInstance().restartFunction(subscriptionId, appId);
        renderFunctionStatus(subscriptionId, appId);
    }

    public void onStopFunctionApp(String subscriptionId, String appId) {
        AzureFunctionMvpModel.getInstance().stopFunction(subscriptionId, appId);
        renderFunctionStatus(subscriptionId, appId);
    }

    private void renderFunctionStatus(String subscriptionId, String appId) {
        final FunctionAppNodeView view = getMvpView();
        if (view != null) {
            final FunctionApp target = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, appId);
            view.renderNode(WebAppBaseState.fromString(target.state()));
        }
    }

    public void onNodeRefresh() {
        final FunctionAppNodeView view = getMvpView();
        if (view != null) {
            view.renderSubModules();
        }
    }
}
