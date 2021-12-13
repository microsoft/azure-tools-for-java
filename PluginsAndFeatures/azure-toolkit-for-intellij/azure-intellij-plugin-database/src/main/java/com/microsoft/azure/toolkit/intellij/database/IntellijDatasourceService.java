/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class IntellijDatasourceService {

    private static final String DATABASE_TOOLS_PLUGIN_ID = "com.intellij.database";
    private static final String DATABASE_PLUGIN_NOT_INSTALLED = "\"Database tools and SQL\" plugin is not detected.";
    private static final String NOT_SUPPORT_ERROR_ACTION = "\"Database tools and SQL\" plugin is only provided in IntelliJ Ultimate edition.";
    private static final String ERROR_MESSAGE_PATTERN = "Failed to open \"Data Sources and Drivers\" dialog for %s";
    private static final IntellijDatasourceService instance = new IntellijDatasourceService();

    public static IntellijDatasourceService getInstance() {
        return IntellijDatasourceService.instance;
    }

    private IntellijDatasourceService() {
    }

    public void openDataSourceManagerDialog(Project project, DatasourceProperties properties) {
        if (PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null) {
            throw new AzureToolkitRuntimeException(DATABASE_PLUGIN_NOT_INSTALLED);
        }
        try {
            final Object registry = getDataSourceRegistry(project, properties);
            final Object dbPsiFacade = getDbPsiFacade(project, properties);
            final Object builder = MethodUtils.invokeMethod(registry, "getBuilder");
            MethodUtils.invokeMethod(builder, true, "withName", properties.getName());
            MethodUtils.invokeMethod(builder, true, "withDriverClass", properties.getDriverClassName());
            MethodUtils.invokeMethod(builder, true, "withUrl", properties.getUrl());
            MethodUtils.invokeMethod(builder, true, "withUser", properties.getUsername());
            MethodUtils.invokeMethod(builder, true, "commit");
            showDataSourceManagerDialog(dbPsiFacade, registry, properties);
        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | InstantiationException e) {
            throw new AzureToolkitRuntimeException(String.format(ERROR_MESSAGE_PATTERN, properties.getName()));
        }
    }

    private Object getDataSourceRegistry(Project project, DatasourceProperties properties) throws ClassNotFoundException, NoSuchMethodException,
        InvocationTargetException, InstantiationException, IllegalAccessException {
        final Class<?>[] parameterTypes = {Project.class};
        final Class<?> dataSourceRegistryClazz = Class.forName("com.intellij.database.autoconfig.DataSourceRegistry");
        final Constructor<?> constructor = dataSourceRegistryClazz.getConstructor(parameterTypes);
        return constructor.newInstance(project);
    }

    private Object getDbPsiFacade(Object project, DatasourceProperties properties) throws ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, IllegalAccessException {
        final Class<?> dbPsiFacadeClass = Class.forName("com.intellij.database.psi.DbPsiFacade");
        return MethodUtils.invokeStaticMethod(dbPsiFacadeClass, "getInstance", project);
    }

    private void showDataSourceManagerDialog(Object dbPsiFacade, Object registry, DatasourceProperties properties) throws ClassNotFoundException,
        InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final Class<?> dataSourceManagerDialogClazz = Class.forName("com.intellij.database.view.ui.DataSourceManagerDialog");
        MethodUtils.invokeStaticMethod(dataSourceManagerDialogClazz, "showDialog", dbPsiFacade, registry);
    }

    @Builder
    @Getter
    public static class DatasourceProperties {
        private String name;
        @Builder.Default
        private String driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        private String url;
        private String username;
    }
}
