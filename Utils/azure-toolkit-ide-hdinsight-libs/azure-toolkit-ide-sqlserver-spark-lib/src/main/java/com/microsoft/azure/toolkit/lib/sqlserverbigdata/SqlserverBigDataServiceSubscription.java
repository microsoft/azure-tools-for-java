package com.microsoft.azure.toolkit.lib.sqlserverbigdata;

import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlserverBigDataServiceSubscription extends AbstractAzServiceSubscription<SqlserverBigDataServiceSubscription, SqlserverBigDataManager> {

    public SqlserverBigDataServiceSubscription() {
        super("[LinkedCluster]",new SqlserverBigDataService());
    }

    protected SqlserverBigDataServiceSubscription(@NotNull String name, @NotNull AbstractAzResourceModule<SqlserverBigDataServiceSubscription, None, SqlserverBigDataManager> module) {
        super(name, module);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return null;
    }
}
