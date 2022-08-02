/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.platform;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;
import org.apache.commons.collections.ListUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
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

    @Override
    protected String getItemText(Object item) {
        return item instanceof Runtime ? WebAppService.getInstance().getRuntimeDisplayName((Runtime) item) : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Runtime> loadItems() throws Exception {
        return platformList;
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
