package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.FieldSize;
import com.intellij.openapi.actionSystem.AnAction;
import com.microsoft.azure.toolkit.intellij.dbtools.AzureParamsEditorBase;
import com.microsoft.azure.toolkit.intellij.dbtools.NoResourceTipLabel;
import com.microsoft.azure.toolkit.intellij.dbtools.NotSignedInTipLabel;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class DatabaseServerParamEditor extends AzureParamsEditorBase<SqlDbServerComboBox, IDatabaseServer<?>> {
    public static final String KEY_DB_SERVER_ID = "AZURE_SQL_DB_SERVER";
    @Nonnull
    private final DatabaseServerClass databaseServerClass;

    public DatabaseServerParamEditor(
            @Nonnull DatabaseServerClass databaseServerClass,
            @Nullable String caption,
            @NotNull DataInterchange interchange,
            AnAction @NotNull ... actions) {
        super(new SqlDbServerComboBox(databaseServerClass), interchange, FieldSize.LARGE, caption, KEY_DB_SERVER_ID, actions);
        this.databaseServerClass = databaseServerClass;
        final LocalDataSource dataSource = getDataSourceConfigurable().getDataSource();

        final SqlDbServerComboBox comboBox = getEditorComponent();
        final boolean isModifying = StringUtils.isNotBlank(dataSource.getUsername());
        final String serverId = interchange.getProperty(KEY_DB_SERVER_ID);

        if (isModifying && Objects.nonNull(serverId)) {
            comboBox.setByResourceId(serverId);
        } else if (Objects.isNull(serverId)) {
            comboBox.setNull();
        }
    }

    @Override
    protected NotSignedInTipLabel createNotSignedInTipLabel() {
        return new DatabaseNotSignedInLabel(databaseServerClass);
    }

    @Override
    protected NoResourceTipLabel createNoResourceTipLabel() {
        return new NoServersTipLabel(databaseServerClass);
    }

    @Override
    @AzureOperation(name = "user/$database.select_server_dbtools.server", params = {"value.getName()"}, source = "value")
    protected void setResource(
            @Nonnull DataInterchange interchange,
            @Nullable Object fromBackground,
            @Nullable IDatabaseServer<?> value,
            @Nullable String oldResourceId,
            @Nullable String newResourceId) {
        // null will not be saved at com/intellij/database/dataSource/url/ui/DynamicJdbcUrlEditor#storeProperties
        interchange.putProperty(KEY_DB_SERVER_ID, Optional.ofNullable(newResourceId).orElse(NONE));
        if (Objects.isNull(value) || Objects.equals(oldResourceId, newResourceId)) {
            return;
        }
        final String user = value.getFullAdminName();
        LocalDataSource.setUsername(interchange.getDataSource(), user);
        this.setUsername(user);
        this.setJdbcUrl(value.getJdbcUrl().toString());
    }

    @Override
    protected String getHostFromValue(IDatabaseServer<?> value) {
        return value.getJdbcUrl().getServerHost();
    }
}



