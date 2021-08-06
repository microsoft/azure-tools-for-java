/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

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
    private static final String NOT_SUPPORT_ERROR_MESSAGE = "detect database plugin in your IDE.";
    private static final String NOT_SUPPORT_ERROR_ACTION = "note this action is only supported in Intellij Ultimate edition.";
    private static final String ERROR_MESSAGE_PATTERN = "Failed to open datasource management dialog for %s";
    private static final String ERROR_ACTION = "please try again.";
    private static final IntellijDatasourceService instance = new IntellijDatasourceService();

    public static IntellijDatasourceService getInstance() {
        return IntellijDatasourceService.instance;
    }

    private IntellijDatasourceService() {
    }

    public void openDataSourceManagerDialog(Project project, DatasourceProperties properties) {
        if (PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null) {
            throw new AzureToolkitRuntimeException(NOT_SUPPORT_ERROR_MESSAGE, NOT_SUPPORT_ERROR_ACTION);
        }
        Object registry = getDataSourceRegistry(project, properties);
        Object dbPsiFacade = getDbPsiFacade(project, properties);
        try {
            Object builder = MethodUtils.invokeMethod(registry, "getBuilder");
            MethodUtils.invokeMethod(builder, true, "withName", properties.getName());
            MethodUtils.invokeMethod(builder, true, "withDriverClass", properties.getDriverClassName());
            MethodUtils.invokeMethod(builder, true, "withUrl", properties.getUrl());
            MethodUtils.invokeMethod(builder, true, "withUser", properties.getUsername());
            MethodUtils.invokeMethod(builder, true, "commit");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new AzureToolkitRuntimeException(String.format(ERROR_MESSAGE_PATTERN, properties.getName()), ERROR_ACTION);
        }
        showDataSourceManagerDialog(dbPsiFacade, registry, properties);
    }

    private Object getDataSourceRegistry(Project project, DatasourceProperties properties) {
        try {
            Class[] parameterTypes = {Project.class};
            Class dataSourceRegistryClazz = Class.forName("com.intellij.database.autoconfig.DataSourceRegistry");
            Constructor constructor = dataSourceRegistryClazz.getConstructor(parameterTypes);
            return constructor.newInstance(project);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new AzureToolkitRuntimeException(String.format(ERROR_MESSAGE_PATTERN, properties.getName()), ERROR_ACTION);
        }
    }

    private Object getDbPsiFacade(Object project, DatasourceProperties properties) {
        try {
            Class dbPsiFacadeClass = Class.forName("com.intellij.database.psi.DbPsiFacade");
            return MethodUtils.invokeStaticMethod(dbPsiFacadeClass, "getInstance", project);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new AzureToolkitRuntimeException(String.format(ERROR_MESSAGE_PATTERN, properties.getName()), ERROR_ACTION);
        }
    }

    private void showDataSourceManagerDialog(Object dbPsiFacade, Object registry, DatasourceProperties properties) {
        try {
            Class dataSourceManagerDialogClazz = Class.forName("com.intellij.database.view.ui.DataSourceManagerDialog");
            MethodUtils.invokeStaticMethod(dataSourceManagerDialogClazz, "showDialog", dbPsiFacade, registry);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new AzureToolkitRuntimeException(String.format(ERROR_MESSAGE_PATTERN, properties.getName()), ERROR_ACTION);
        }
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
