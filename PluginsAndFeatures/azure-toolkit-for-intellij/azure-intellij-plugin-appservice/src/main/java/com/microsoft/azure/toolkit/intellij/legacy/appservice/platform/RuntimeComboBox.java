/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.platform;

import com.intellij.openapi.ui.popup.ListItemDescriptorAdapter;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;

import javax.annotation.Nonnull;
import java.util.*;

public class RuntimeComboBox extends AzureComboBox<Runtime> {

    private List<Runtime> platformList;

    public RuntimeComboBox() {
        this(Runtime.WEBAPP_RUNTIME);
    }

    public RuntimeComboBox(List<Runtime> platformList) {
        super();
        this.platformList = Collections.unmodifiableList(platformList.stream()
                .sorted(Comparator.comparing(o -> WebAppService.getInstance().getRuntimeDisplayName(o))).toList());
        setGroupRender();
    }

    public void setPlatformList(final List<Runtime> platformList) {
        this.platformList = Collections.unmodifiableList(platformList);
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Runtime ? WebAppService.getInstance().getRuntimeDisplayName((Runtime) item) : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Runtime> loadItems() {
        return platformList;
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    private void setGroupRender() {
        this.setRenderer(new GroupedItemsListRenderer<>(new RuntimeItemDescriptor()) {
            @Override
            protected boolean hasSeparator(Runtime value, int index) {
                return index >= 0 && super.hasSeparator(value, index);
            }
        });
    }

    private String getSeparatorCaption(Runtime item) {
        return item.isDocker() ? "Docker" : String.format("%s & %s", item.getOperatingSystem().toString(), item.getJavaVersion().toString());
    }

    class RuntimeItemDescriptor extends ListItemDescriptorAdapter<Runtime> {
        @Override
        public String getTextFor(Runtime value) {
            return WebAppService.getInstance().getRuntimeDisplayName(value);
        }

        @Override
        public String getCaptionAboveOf(Runtime value) {
            return getSeparatorCaption(value);
        }

        @Override
        public boolean hasSeparatorAboveOf(Runtime value) {
            final int index = platformList.indexOf(value);
            if (index <= 0) {
                return index == 0;
            }
            final String currentCaption = getSeparatorCaption(value);
            final String lastCaption = getSeparatorCaption(platformList.get(index - 1));
            return !Objects.equals(currentCaption, lastCaption);
        }
    }
}
