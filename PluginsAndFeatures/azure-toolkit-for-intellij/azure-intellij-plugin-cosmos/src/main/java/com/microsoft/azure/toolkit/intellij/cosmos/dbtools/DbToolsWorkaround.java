/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.database.Dbms;
import com.intellij.database.dataSource.DatabaseDriverImpl;
import com.intellij.database.dataSource.DatabaseDriverManager;
import com.intellij.database.dataSource.url.ui.UrlPropertiesPanel;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

public class DbToolsWorkaround extends PreloadingActivity {
    private static final String PARAM_NAME = "account";

    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
            Optional.ofNullable(manager.getDriver("az_cosmos_cassandra")).map(d -> ((DatabaseDriverImpl) d))
                .ifPresent(manager::removeDriver);
            Optional.ofNullable(manager.getDriver("az_cosmos_mongo")).map(d -> ((DatabaseDriverImpl) d))
                .ifPresent(d -> {
                    d.setForcedDbms(Dbms.MONGO);
                    d.setIcon(IntelliJAzureIcons.getIcon("icons/Microsoft.DocumentDB/databaseAccounts/mongo.svg"));
                });
            DbToolsWorkaround.makeAccountShowAtTop();
        });
    }

    @SuppressWarnings("unchecked")
    private static void makeAccountShowAtTop() {
        try {
            final Field HEADS = FieldUtils.getField(UrlPropertiesPanel.class, "HEADS", true);
            final List<String> heads = (List<String>) FieldUtils.readStaticField(HEADS, true);
            if (!heads.contains(PARAM_NAME)) {
                final Object[] old = heads.toArray();
                heads.set(0, PARAM_NAME);
                for (int i = 0; i < old.length - 1; i++) {
                    heads.set(i + 1, (String) old[i]);
                }
            }
        } catch (final Throwable ignored) {
        }
    }
}
