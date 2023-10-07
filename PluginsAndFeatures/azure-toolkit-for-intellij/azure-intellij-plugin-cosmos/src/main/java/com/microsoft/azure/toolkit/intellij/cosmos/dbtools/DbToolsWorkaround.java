/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.database.dataSource.DatabaseDriverImpl;
import com.intellij.database.dataSource.DatabaseDriverManager;
import com.intellij.database.dataSource.DatabaseDriverManagerImpl;
import com.intellij.database.dataSource.url.ui.ParametersLayoutUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.registry.Registry;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DbToolsWorkaround implements ProjectActivity, DumbAware {
    private static final String PARAM_NAME = "account";
    public static final String COSMOS_MONGO_ICON = "icons/Microsoft.DocumentDB/databaseAccounts/mongo.svg";
    public static final String COSMOS_MONGO_DRIVER_ID = "az_cosmos_mongo";
    public static final String COSMOS_MONGO_DRIVER_CONFIG = "databaseDrivers/azure-cosmos-mongo-drivers.xml";
    public static final String COSMOS_CASSANDRA_ICON = "icons/Microsoft.DocumentDB/databaseAccounts/cassandra.svg";
    public static final String COSMOS_CASSANDRA_DRIVER_ID = "az_cosmos_cassandra";
    public static final String COSMOS_CASSANDRA_DRIVER_CONFIG = "databaseDrivers/azure-cosmos-cassandra-drivers.xml";

    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                DbToolsWorkaround.makeAccountShowAtTop();
                loadMongoDriver();
                loadCassandraDriver();
            } catch (final Throwable t) {
                // swallow exception for preloading workarounds
                AzureTelemeter.log(AzureTelemetry.Type.ERROR, new HashMap<>(), t);
            }
        });
        return null;
    }

    private static void loadMongoDriver() {
        final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
        final DatabaseDriverImpl oldDriver = (DatabaseDriverImpl) manager.getDriver(COSMOS_MONGO_DRIVER_ID);
        final boolean legacy = Optional.ofNullable(oldDriver).map(DatabaseDriverImpl::getUrlTemplates)
            .flatMap(ts -> ts.stream().findFirst())
            .filter(t -> StringUtils.containsIgnoreCase(t.getTemplate(), "retrywrites"))
            .isEmpty();
        if (legacy) { // remove if old driver is not user defined.
            Optional.ofNullable(oldDriver).ifPresent(manager::removeDriver);
            addAsUserDriver(COSMOS_MONGO_DRIVER_CONFIG);
        } else {
            oldDriver.setIcon(IntelliJAzureIcons.getIcon(COSMOS_MONGO_ICON));
        }
    }

    private static void loadCassandraDriver() {
        final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
        final DatabaseDriverImpl oldDriver = (DatabaseDriverImpl) manager.getDriver(COSMOS_CASSANDRA_DRIVER_ID);
        if (Registry.is("azure.toolkit.cosmos_cassandra.dbtools.enabled")) {
            if (Objects.isNull(oldDriver) || !"Azure Cosmos DB API for Cassandra".equals(oldDriver.getName())) { // remove if old driver is not user defined.
                Optional.ofNullable(oldDriver).ifPresent(manager::removeDriver);
                addAsUserDriver(COSMOS_CASSANDRA_DRIVER_CONFIG);
            } else {
                oldDriver.setIcon(IntelliJAzureIcons.getIcon(COSMOS_CASSANDRA_ICON));
            }
        } else {
            Optional.ofNullable(oldDriver).ifPresent(manager::removeDriver);
        }
    }

    @SneakyThrows
    private static void addAsUserDriver(@Nonnull final String configUri) {
        final DatabaseDriverManagerImpl manager = (DatabaseDriverManagerImpl) DatabaseDriverManager.getInstance();
        final URL driverUrl = DbToolsWorkaround.class.getClassLoader().getResource(configUri);
        final Element config = JDOMUtil.load(Objects.requireNonNull(driverUrl)).getChildren().get(0);
        final String id = config.getAttributeValue("id");
        final String icon = config.getAttributeValue("icon-path");
        final String baseId = DatabaseDriverImpl.getDriverBaseId(config);
        final DatabaseDriverImpl driver = new DatabaseDriverImpl(id, false);
        manager.loadBaseState(baseId, driver);
        manager.updateDriver(driver);
        driver.loadState(config, true, false, 221, null);
        driver.setIcon(IntelliJAzureIcons.getIcon(icon));
    }

    @SuppressWarnings("unchecked")
    private static void makeAccountShowAtTop() {
        try {
            final Field HEADS = FieldUtils.getField(ParametersLayoutUtils.class, "HEADS", true);
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
