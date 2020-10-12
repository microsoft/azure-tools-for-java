package com.microsoft.azure.appservice.component.form.input;

import com.microsoft.azure.appservice.Platform;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

public class ComboBoxPlatform extends AzureComboBox<Platform> {
    @NotNull
    @Override
    protected List<? extends Platform> loadItems() throws Exception {
        return Platform.platforms;
    }
}
