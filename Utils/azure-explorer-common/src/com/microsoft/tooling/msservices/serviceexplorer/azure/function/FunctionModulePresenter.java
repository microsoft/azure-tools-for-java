/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

public class FunctionModulePresenter<V extends FunctionModuleView> extends MvpPresenter<V> {

    public void onModuleRefresh() {
        final FunctionModuleView view = getMvpView();
        if (view != null) {
            view.renderChildren(AzureFunctionMvpModel.getInstance().listAllFunctions(true));
        }
    }

    public void onDeleteFunctionApp(String sid, String id) {
        AzureFunctionMvpModel.getInstance().deleteFunction(sid, id);
    }
}
