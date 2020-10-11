package com.microsoft.azure.appservice.component.form.input;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AzureComboBoxSimple<T> extends AzureComboBox<T> {

    private DataProvider<? extends List<? extends T>> provider;

    public AzureComboBoxSimple() {
        super();
    }

    public AzureComboBoxSimple(final DataProvider<? extends List<? extends T>> provider) {
        this();
        this.provider = provider;
    }

    @NotNull
    protected List<? extends T> loadItems() throws Exception {
        if(Objects.nonNull(this.provider)){
            return this.provider.loadData();
        }
        return Collections.emptyList();
    }

    @FunctionalInterface
    public interface DataProvider<T>{
        T loadData() throws Exception;
    }
}
