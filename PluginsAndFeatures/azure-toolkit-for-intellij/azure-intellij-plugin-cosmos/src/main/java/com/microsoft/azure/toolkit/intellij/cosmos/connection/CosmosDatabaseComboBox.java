package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabase;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class CosmosDatabaseComboBox<T extends ICosmosDatabase, E extends CosmosDBAccount> extends AzureComboBox<T> {

    @Getter
    private E account;

    public CosmosDatabaseComboBox(@Nonnull Function<E, ? extends List<? extends T>> itemsLoader) {
        super();
        this.setItemsLoader(() -> Objects.isNull(account) ? Collections.emptyList() : itemsLoader.apply(this.getAccount()));
    }

    public void setAccount(E account) {
        if (Objects.equals(account, this.account)) {
            return;
        }
        this.account = account;
        if (account == null) {
            this.clear();
            return;
        }
        this.setValidationInfo(null);
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return Objects.nonNull(item) && item instanceof ICosmosDatabase ? ((ICosmosDatabase) item).getName() : super.getItemText(item);
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
