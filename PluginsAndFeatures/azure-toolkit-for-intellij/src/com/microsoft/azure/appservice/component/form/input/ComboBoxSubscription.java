package com.microsoft.azure.appservice.component.form.input;

import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

import java.util.List;

public class ComboBoxSubscription extends AzureComboBox<Subscription> {
    @NotNull
    @Override
    protected List<Subscription> loadItems() throws Exception {
        return AzureMvpModel.getInstance().getSelectedSubscriptions();
    }
}
