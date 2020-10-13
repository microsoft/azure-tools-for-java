package com.microsoft.azure.toolkit.appservice.intellij.view.component;

import com.microsoft.azure.toolkit.appservice.Platform;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

public class ComboBoxPlatform extends AzureComboBox<Platform> {
    @NotNull
    @Override
    protected List<? extends Platform> loadItems() throws Exception {
        return Platform.platforms;
    }
}
