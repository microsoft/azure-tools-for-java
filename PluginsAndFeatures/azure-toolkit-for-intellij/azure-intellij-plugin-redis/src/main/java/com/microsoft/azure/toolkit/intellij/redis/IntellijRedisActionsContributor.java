/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis;

import com.intellij.database.Dbms;
import com.intellij.database.autoconfig.DataSourceDetector;
import com.intellij.database.autoconfig.DataSourceRegistry;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.view.ui.DataSourceManagerDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.redis.RedisActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager.AzureResourceFileType;
import com.microsoft.azure.toolkit.intellij.dbtools.DatabasePlugin;
import com.microsoft.azure.toolkit.intellij.dbtools.DatabaseTools;
import com.microsoft.azure.toolkit.intellij.redis.creation.CreateRedisCacheAction;
import com.microsoft.azure.toolkit.intellij.redis.dbtools.RedisCacheParamEditor;
import com.microsoft.azure.toolkit.intellij.redis.dbtools.RedisJdbcUrl;
import com.microsoft.azure.toolkit.intellij.redis.explorer.RedisCacheExplorerProvider;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import com.microsoft.azure.toolkit.redis.model.RedisConfig;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.ide.redis.RedisActionsContributor.REDIS_ACTIONS;
import static com.microsoft.azure.toolkit.intellij.redis.dbtools.RedisAccessKeyAuthProvider.RedisAccessKey;

public class IntellijRedisActionsContributor implements IActionsContributor {
    public static final Action.Id<AzResource> OPEN_DATABASE_TOOL = Action.Id.of("user/redis.open_database_tools.name");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_DATABASE_TOOL)
                .visibleWhen(s -> s instanceof RedisCache)
                .enableWhen(s -> s.getFormalStatus().isRunning())
                .withIcon(AzureIcons.Action.OPEN_DATABASE_TOOL.getIconPath())
                .withLabel("Open with Database Tools")
                .withIdParam(AzResource::getName)
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final IActionGroup group = am.getGroup(REDIS_ACTIONS);
        group.addAction("---");
        group.addAction(OPEN_DATABASE_TOOL);
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureRedis;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateRedisCacheAction.create(e.getProject(), null);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        final Icon icon = IntelliJAzureIcons.getIcon(AzureIcons.RedisCache.MODULE);
        final String name = RedisCacheExplorerProvider.TYPE;
        final AzureResourceFileType type = new AzureResourceFileType(name, icon);
        final AzureResourceEditorViewManager manager = new AzureResourceEditorViewManager((resource) -> type);
        am.<AzResource, AnActionEvent>registerHandler(RedisActionsContributor.OPEN_EXPLORER, (r, e) -> r instanceof RedisCache,
            (r, e) -> manager.showEditor(r, Objects.requireNonNull(e.getProject())));

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateServerHandler = (r, e) -> {
            final RedisConfig config = new RedisConfig();
            config.setSubscription(r.getSubscription());
            config.setRegion(r.getRegion());
            config.setResourceGroup(r);
            CreateRedisCacheAction.create(e.getProject(), config);
        };
        am.registerHandler(RedisActionsContributor.GROUP_CREATE_REDIS, (r, e) -> true, groupCreateServerHandler);

        final BiConsumer<AzResource, AnActionEvent> openDatabaseHandler = (c, e) -> openDatabaseTool(e.getProject(), (RedisCache) c);
        am.registerHandler(OPEN_DATABASE_TOOL, (r, e) -> true, openDatabaseHandler);
    }

    @Override
    public int getOrder() {
        return RedisActionsContributor.INITIALIZE_ORDER + 1;
    }

    public static void openDatabaseTool(Project project, @Nonnull RedisCache cache) {
        DatabaseTools.openDatabaseTool(project, cache, IntellijRedisActionsContributor::openDataSourceManagerDialog);
    }

    public static void openDataSourceManagerDialog(RedisCache cache, Project project) {
        final DataSourceRegistry registry = new DataSourceRegistry(project);
        final DbPsiFacade dbPsiFacade = DbPsiFacade.getInstance(project);
        final DataSourceDetector.Builder builder = registry.getBuilder()
                .withJdbcAdditionalProperty(RedisCacheParamEditor.KEY_REDIS_CACHE, cache.getId())
                .withDbms(Dbms.REDIS)
                .withUrl(RedisJdbcUrl.from(cache))
                .withAuthProviderId(RedisAccessKey)
                .commit();

        DataSourceManagerDialog.showDialog(dbPsiFacade, registry);
    }
}
