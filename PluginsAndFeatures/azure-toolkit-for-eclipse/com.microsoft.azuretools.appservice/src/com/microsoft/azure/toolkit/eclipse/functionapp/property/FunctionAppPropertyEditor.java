/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp.property;

import java.util.Map;
import java.util.Set;

import com.microsoft.azure.toolkit.eclipse.appservice.property.AppServiceBasePropertyEditor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureFunction;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

public class FunctionAppPropertyEditor extends AppServiceBasePropertyEditor {
    public static final String ID = "com.microsoft.azure.toolkit.eclipse.functionapp.property.FunctionAppPropertyEditor";

    public FunctionAppPropertyEditor() {
        super(new FunctionAppPropertyViewPresenter());
    }

    static class FunctionAppPropertyViewPresenter extends WebAppBasePropertyViewPresenter {

        @Override
        protected FunctionApp getWebAppBase(String subscriptionId, String functionAppId, String name) {
            return Azure.az(AzureFunction.class).subscription(subscriptionId).get(functionAppId);
        }

        @Override
        protected void updateAppSettings(String subscriptionId, String functionAppId, String name, Map toUpdate, Set toRemove) {
            final FunctionApp functionApp = getWebAppBase(subscriptionId, functionAppId, name);
            final IAppServiceUpdater appServiceUpdater = functionApp.update();
            appServiceUpdater.withAppSettings(toUpdate);
            toRemove.forEach(key -> appServiceUpdater.withoutAppSettings((String) key));
            appServiceUpdater.commit();
        }
    }

}
