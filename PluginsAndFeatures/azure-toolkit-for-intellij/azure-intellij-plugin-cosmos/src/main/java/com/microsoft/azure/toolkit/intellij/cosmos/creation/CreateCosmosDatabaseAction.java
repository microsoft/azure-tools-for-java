package com.microsoft.azure.toolkit.intellij.cosmos.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.applicationinsights.core.dependencies.javaxannotation.Nonnull;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabaseDraft;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiFunction;

public class CreateCosmosDatabaseAction {

    public static <T extends CosmosDBAccount> void create(@Nonnull Project project, @Nonnull T account,
                                                          @Nonnull BiFunction<T, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> draftSupplier,
                                                          @Nullable final DatabaseConfig data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final CosmosDatabaseCreationDialog dialog = new CosmosDatabaseCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            dialog.setOkActionListener((config) -> {
                dialog.close();
                doCreate(account, draftSupplier, config);
            });
            dialog.show();
        });
    }

    public static DatabaseConfig getDefaultDatabaseConfig() {
        final DatabaseConfig result = new DatabaseConfig();
        result.setName(String.format("database-%s", Utils.getTimestamp()));
        result.setMaxThroughput(4000);
        return result;
    }

    @AzureOperation(name = "cosmos.create_database.database|account", params = {"config.getName(), account.getName()"}, type = AzureOperation.Type.ACTION)
    private static <T extends CosmosDBAccount> void doCreate(@Nonnull T account,
                                                             @Nonnull BiFunction<T, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> draftSupplier,
                                                             @Nullable final DatabaseConfig config) {
        final AzureString title = OperationBundle.description("cosmos.create_database.database|account", Objects.requireNonNull(config).getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ICosmosDatabaseDraft<?, ?> draft = draftSupplier.apply(account, config);
            draft.setConfig(config);
            draft.commit();
        });
    }
}
