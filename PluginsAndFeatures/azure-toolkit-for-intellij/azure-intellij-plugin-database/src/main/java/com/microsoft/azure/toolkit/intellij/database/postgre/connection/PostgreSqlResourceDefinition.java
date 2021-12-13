/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.connection;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlDatabase;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;


public class PostgreSqlResourceDefinition extends AzureServiceResource.Definition<PostgreSqlDatabase> implements SpringSupported<PostgreSqlDatabase> {
    public static final PostgreSqlResourceDefinition INSTANCE = new PostgreSqlResourceDefinition();


    private PostgreSqlResourceDefinition() {
        super("Azure.PostgreSQL", "Azure Database for PostgreSQL", "/icons/postgre.svg");
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<PostgreSqlDatabase> data, Project project) {
        PostgreSqlDatabaseResource resource = (PostgreSqlDatabaseResource) data;
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_URL", Connection.ENV_PREFIX), resource.getJdbcUrl().toString());
        env.put(String.format("%s_USERNAME", Connection.ENV_PREFIX), resource.getUsername());
        env.put(String.format("%s_PASSWORD", Connection.ENV_PREFIX), Optional.ofNullable(resource.loadPassword()).or(() -> Optional.ofNullable(resource.inputPassword(project))).orElse(""));
        return env;
    }

    @Override
    public List<Pair<String, String>> getSpringProperties() {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.datasource.url", String.format("${%s_URL}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.datasource.username", String.format("${%s_USERNAME}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.datasource.password", String.format("${%s_PASSWORD}", Connection.ENV_PREFIX)));
        return properties;
    }

    @Override
    public boolean write(@Nonnull Element resourceEle, @Nonnull Resource<PostgreSqlDatabase> paramResource) {
        PostgreSqlDatabaseResource resource = (PostgreSqlDatabaseResource) paramResource;
        final String defName = resource.getDefinition().getName();

        final Password.SaveType saveType = resource.getPassword().saveType();
        resourceEle.addContent(new Element("url").setText(resource.getJdbcUrl().toString()));
        resourceEle.addContent(new Element("username").setText(resource.getUsername()));
        resourceEle.addContent(new Element("passwordSave").setText(saveType.name()));
        final char[] password = resource.getPassword().password();
        final String storedPassword = PasswordStore.loadPassword(defName, resource.getDataId(), resource.getUsername(), saveType);
        if (ArrayUtils.isNotEmpty(password) && !StringUtils.equals(String.valueOf(password), storedPassword)) {
            PasswordStore.savePassword(defName, resource.getDataId(), resource.getUsername(), resource.getPassword().password(), saveType);
        }

        resourceEle.setAttribute(new Attribute("id", resource.getId()));
        resourceEle.addContent(new Element("dataId").addContent(resource.getDataId()));

        return true;
    }

    @Override
    public Resource<PostgreSqlDatabase> read(@Nonnull Element resourceEle) {
        final String dataId = resourceEle.getChildTextTrim("dataId");

        if (StringUtils.isBlank(dataId)) {
            throw new AzureToolkitRuntimeException("persistent data of resource connection is corrupted");
        }
        final PostgreSqlDatabaseResource resource = new PostgreSqlDatabaseResource(dataId, resourceEle.getChildTextTrim("username"), this);

        final String defName = this.getName();
        resource.setJdbcUrl(JdbcUrl.from(resourceEle.getChildTextTrim("url")));

        resource.setPassword(new Password().saveType(Password.SaveType.valueOf(resourceEle.getChildTextTrim("passwordSave"))));
        if (resource.getPassword().saveType() == Password.SaveType.FOREVER) {
            PasswordStore.migratePassword(resource.getId(), resource.getUsername(), defName, resource.getId(), resource.getUsername());
        }
        final String savedPassword = PasswordStore.loadPassword(defName, resource.getId(), resource.getUsername(), resource.getPassword().saveType());
        if (StringUtils.isNotBlank(savedPassword)) {
            resource.getPassword().password(savedPassword.toCharArray());
        }
        return resource;
    }

    @Override
    public Resource<PostgreSqlDatabase> define(PostgreSqlDatabase resource) {
        return new PostgreSqlDatabaseResource(resource, null, this);
    }

    @Override
    public Resource<PostgreSqlDatabase> define(String dataId) {
        return new AzureServiceResource<>(getResource(dataId), this);
    }

    @Override
    public PostgreSqlDatabase getResource(String dataId) {
        final ResourceId resourceId = ResourceId.fromString(dataId);
        final ResourceId serverId = resourceId.parent();
        final String databaseName = resourceId.name();
        final PostgreSqlServer postgreSqlServer = Azure.az(AzurePostgreSql.class).get(serverId.id());
        return postgreSqlServer.database(databaseName);
    }

    @Override
    public AzureFormJPanel<Resource<PostgreSqlDatabase>> getResourcePanel(Project project) {
        return new PostgreSqlResourcePanel();
    }

    public static class RegisterActivity extends PreloadingActivity {
        @Override
        public void preload(@NotNull ProgressIndicator progressIndicator) {
            ResourceManager.registerDefinition(INSTANCE);
        }
    }
}
