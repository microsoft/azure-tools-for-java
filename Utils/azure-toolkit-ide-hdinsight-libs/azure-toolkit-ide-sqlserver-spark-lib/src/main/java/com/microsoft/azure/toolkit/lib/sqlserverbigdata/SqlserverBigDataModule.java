package com.microsoft.azure.toolkit.lib.sqlserverbigdata;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorite;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class SqlserverBigDataModule extends AbstractAzResourceModule<SqlserverBigDataNode, SqlserverBigDataServiceSubscription, IClusterDetail> {

    public static final String NAME = "sqlserverbigdata";

    @Getter
    private static final SqlserverBigDataModule instance = new SqlserverBigDataModule();

    private SqlserverBigDataModule() {
        super(NAME, new SqlserverBigDataServiceSubscription());
    }


    @NotNull
    @Override
    protected SqlserverBigDataNode newResource(@NotNull IClusterDetail iClusterDetail) {
        return null;
    }

    @NotNull
    @Override
    protected SqlserverBigDataNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return null;
    }

    public boolean isAuthRequiredForListing() {
        return false;
    }

    @Override
    public boolean isAuthRequiredForCreating() {
        return false;
    }

    @Nonnull
    @Override
    public List<SqlserverBigDataNode> list() {
        return Collections.emptyList();
    }

    @Override
    public IClusterDetail getClient() {
        return null;
    }

}
