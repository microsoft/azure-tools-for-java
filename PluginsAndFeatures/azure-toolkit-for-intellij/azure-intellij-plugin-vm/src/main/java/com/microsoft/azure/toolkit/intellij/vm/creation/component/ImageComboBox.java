/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImage;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImageSku;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ImageComboBox extends AzureComboBox<VmImage> {
    private VmImageSku imageSku;

    public void setImageSku(VmImageSku imageSku) {
        this.imageSku = imageSku;
        this.clear();
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof VmImage ? String.format("%s:%s", ((VmImage) item).getSku(), ((VmImage) item).getVersion()) : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends VmImage> loadItems() throws Exception {
        return Optional.ofNullable(imageSku).map(VmImageSku::images).orElse(Collections.emptyList());
    }
}
