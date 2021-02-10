/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

public interface MySQLPropertyMvpView extends MvpView {

    void onReadProperty(String sid, String resourceGroupName, String name);

    void showProperty(MySQLProperty property);
}
