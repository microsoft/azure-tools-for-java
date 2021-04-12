/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;

import java.util.List;

public class VersionComboBox extends AzureComboBox<String> {

    @NotNull
    @Override
    @AzureOperation(
        name = "mysql|version.list.supported",
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends String> loadItems() {
        return MySQLMvpModel.listSupportedVersions();
    }
}
