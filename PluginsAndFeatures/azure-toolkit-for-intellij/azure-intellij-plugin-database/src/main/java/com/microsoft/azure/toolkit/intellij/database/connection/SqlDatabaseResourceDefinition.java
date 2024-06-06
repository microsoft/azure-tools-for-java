/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public abstract class SqlDatabaseResourceDefinition<T extends IDatabase> extends AzureServiceResource.Definition<T> {
    protected SqlDatabaseResourceDefinition(String name, String title, String icon) {
        super(name, title, icon);
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<T> data, Project project) {
        final SqlDatabaseResource<T> resource = (SqlDatabaseResource<T>) data;
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_URL", Connection.ENV_PREFIX), resource.getJdbcUrl().toString());
        env.put(String.format("%s_USERNAME", Connection.ENV_PREFIX), resource.getUsername());
        env.put(String.format("%s_PASSWORD", Connection.ENV_PREFIX), Optional.ofNullable(resource.loadPassword())
            .filter(StringUtils::isNotBlank)
            .orElse(""));
        return env;
    }

    @Override
    public boolean write(@Nonnull Element resourceEle, @Nonnull Resource<T> paramResource) {
        final SqlDatabaseResource<T> resource = (SqlDatabaseResource<T>) paramResource;
        final String defName = resource.getDefinition().getName();
        resourceEle.setAttribute(new Attribute("id", resource.getId()));
        resourceEle.addContent(new Element("url").setText(resource.getJdbcUrl().toString()));
        resourceEle.addContent(new Element("username").setText(resource.getUsername()));
        resourceEle.addContent(new Element("resourceId").addContent(resource.getDataId()));
        return true;
    }

    @Override
    public Resource<T> read(@Nonnull Element resourceEle) {
        final String dataId = Optional.ofNullable(resourceEle.getChildTextTrim("resourceId")).orElseGet(() -> resourceEle.getChildTextTrim("dataId"));
        final String url = resourceEle.getChildTextTrim("url");
        final String username = resourceEle.getChildTextTrim("username");
        if (StringUtils.isBlank(dataId)) {
            throw new AzureToolkitRuntimeException("Missing required dataId for database in service link.");
        }
        final SqlDatabaseResource<T> resource = new SqlDatabaseResource<>(dataId, username, this);
        resource.setJdbcUrl(JdbcUrl.from(url));
        return resource;
    }

    @Override
    public Resource<T> define(@Nonnull T resource, String id) {
        return new SqlDatabaseResource<>(resource, null, this);
    }

    @Override
    public abstract AzureFormJPanel<Resource<T>> getResourcePanel(Project project);
}
