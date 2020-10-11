package com.microsoft.azure.appservice.component.form.input;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;

public class AzureComboBoxSimple<T> extends AzureComboBox<T> {

    private final DataProvider<? extends List<? extends T>> provider;

    public AzureComboBoxSimple(final DataProvider<? extends List<? extends T>> provider) {
        super();
        this.provider = provider;
    }

    @NotNull
    protected List<? extends T> loadItems() throws Exception {
        return this.provider.loadData();
    }

    @FunctionalInterface
    public interface DataProvider<T>{
        T loadData() throws Exception;
    }
}
