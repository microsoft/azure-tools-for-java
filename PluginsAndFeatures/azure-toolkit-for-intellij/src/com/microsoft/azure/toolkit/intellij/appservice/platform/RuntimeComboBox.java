/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.platform;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.collections.ListUtils;

import java.util.List;

public class RuntimeComboBox extends AzureComboBox<Runtime> {

    private List<Runtime> platformList;

    public RuntimeComboBox() {
        this(Runtime.WEBAPP_RUNTIME);
    }

    public RuntimeComboBox(List<Runtime> platformList) {
        this.platformList = ListUtils.unmodifiableList(platformList);
    }

    public void setPlatformList(final List<Runtime> platformList) {
        this.platformList = ListUtils.unmodifiableList(platformList);
    }

    @NotNull
    @Override
    protected List<? extends Runtime> loadItems() throws Exception {
        return platformList;
    }
}
