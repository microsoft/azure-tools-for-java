package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

import java.util.List;
import java.util.Objects;

public class ComboBoxSubscription extends AzureComboBox<Subscription> {
    @NotNull
    @Override
    protected List<Subscription> loadItems() throws Exception {
        return AzureMvpModel.getInstance().getSelectedSubscriptions();
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((Subscription) item).displayName();
    }
}
