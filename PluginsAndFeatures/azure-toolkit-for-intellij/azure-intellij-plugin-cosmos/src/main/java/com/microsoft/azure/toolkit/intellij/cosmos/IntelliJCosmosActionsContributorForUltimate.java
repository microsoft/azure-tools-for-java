/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos;

import com.intellij.database.autoconfig.DataSourceDetector;
import com.intellij.database.autoconfig.DataSourceRegistry;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.view.ui.DataSourceManagerDialog;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.FUSEventSource;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.cosmos.CosmosActionsContributor;
import com.microsoft.azure.toolkit.intellij.cosmos.dbtools.Dbms;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.intellij.cosmos.dbtools.AzureCosmosDbAccountParamEditor.KEY_COSMOS_ACCOUNT_ID;

public class IntelliJCosmosActionsContributorForUltimate implements IActionsContributor {
    private static final String DATABASE_TOOLS_PLUGIN_ID = "com.intellij.database";
    private static final String DATABASE_PLUGIN_NOT_INSTALLED = "\"Database tools and SQL\" plugin is not installed.";
    private static final String NOT_SUPPORT_ERROR_ACTION = "\"Database tools and SQL\" plugin is only provided in IntelliJ Ultimate edition.";
    private static final String ERROR_MESSAGE_PATTERN = "Failed to open \"Data Sources and Drivers\" dialog for %s";
    private static final String IDE_DOWNLOAD_URL = "https://www.jetbrains.com/idea/download/";

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiConsumer<CosmosDBAccount, AnActionEvent> openDatabaseHandler = (c, e) -> openDatabaseTool(e.getProject(), c);
        am.registerHandler(CosmosActionsContributor.OPEN_DATABASE_TOOL, (r, e) -> r instanceof MongoCosmosDBAccount || r instanceof CassandraCosmosDBAccount, openDatabaseHandler);
    }

    @AzureOperation(name = "cosmos.open_by_database_tools.account", params = {"account.getName()"}, type = AzureOperation.Type.ACTION)
    private void openDatabaseTool(Project project, CosmosDBAccount account) {
        if (PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null) {
            final Consumer<Object> handler = (r) -> FUSEventSource.NOTIFICATION.openDownloadPageAndLog(project, IDE_DOWNLOAD_URL);
            final ActionView.Builder view = new ActionView.Builder(IdeBundle.message("plugins.advertiser.action.try.ultimate", "IntelliJ IDEA Ultimate"));
            final Action.Id<Object> TRY_ULTIMATE = Action.Id.of("database.try_ultimate");
            final Action<Object> tryUltimate = new Action<>(TRY_ULTIMATE, handler, view);
            throw new AzureToolkitRuntimeException(DATABASE_PLUGIN_NOT_INSTALLED, NOT_SUPPORT_ERROR_ACTION, tryUltimate);
        }
        final DataSourceRegistry registry = new DataSourceRegistry(project);
        final DbPsiFacade dbPsiFacade = DbPsiFacade.getInstance(project);
        final DataSourceDetector.Builder builder = registry.getBuilder()
            .withDbms(getDbms(account))
            .withJdbcAdditionalProperty(KEY_COSMOS_ACCOUNT_ID, account.getId())
            .commit();
        AzureTaskManager.getInstance().runLater(() -> DataSourceManagerDialog.showDialog(dbPsiFacade, registry));
    }

    private static com.intellij.database.Dbms getDbms(CosmosDBAccount account) {
        if (account instanceof MongoCosmosDBAccount) {
            return Dbms.AZ_COSMOS_MONGO;
        } else if (account instanceof CassandraCosmosDBAccount) {
            return Dbms.AZ_COSMOS_CASSANDRA;
        }
        throw new AzureToolkitRuntimeException("not supported account type: " + account.getResourceTypeName());
    }

    @Override
    public int getOrder() {
        return CosmosActionsContributor.INITIALIZE_ORDER + 1;
    }
}
