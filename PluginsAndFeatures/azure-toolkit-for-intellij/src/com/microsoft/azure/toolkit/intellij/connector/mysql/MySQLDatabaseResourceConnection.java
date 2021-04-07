/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ModuleConnectionBeforeRunTaskProvider;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.mysql.component.PasswordDialog;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MySQLDatabaseResourceConnection implements Connection<MySQLDatabaseResource, ModuleResource> {
    @EqualsAndHashCode.Include
    private final MySQLDatabaseResource resource;
    @EqualsAndHashCode.Include
    private final ModuleResource consumer;

    @Override
    @AzureOperation(name = "connector|mysql.before_run_task", type = AzureOperation.Type.ACTION)
    public void beforeRun(@Nonnull RunConfiguration configuration, DataContext dataContext) {
        final Module module = this.consumer.getModule();
        final MySQLDatabaseResource mysql = this.resource;
        assert module != null : "loading password from unknown module";
        assert mysql != null : "loading password from unknown mysql database";
        final Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put(mysql.getEnvPrefix() + "URL", this.resource.getJdbcUrl().toString());
        environmentVariables.put(mysql.getEnvPrefix() + "USERNAME", this.resource.getUsername());
        environmentVariables.put(mysql.getEnvPrefix() + "PASSWORD", loadPassword(module, mysql).or(() -> inputPassword(module, mysql)).orElse(StringUtils.EMPTY));
        if (MapUtils.isNotEmpty(environmentVariables)) {
            if (configuration instanceof WebAppConfiguration) { // set envs for remote deploy
                final WebAppConfiguration webAppConfiguration = (WebAppConfiguration) configuration;
                webAppConfiguration.setApplicationSettings(environmentVariables);
            }
            if (ModuleConnectionBeforeRunTaskProvider.BeforeRunTask.isApplicableFor(configuration) && configuration instanceof UserDataHolder) {
                ((UserDataHolder) configuration).putUserData(ModuleConnectionBeforeRunTaskProvider.CONNECT_AZURE_RESOURCE_ENV_VARS, environmentVariables);
            }
        }
    }

    private static Optional<String> loadPassword(@Nonnull final Module module, @Nonnull final MySQLDatabaseResource mysql) {
        final String saved = PasswordStore.loadPassword(mysql.getId(), mysql.getUsername(), mysql.getPassword().saveType());
        if (StringUtils.isNotBlank(saved) && MySQLConnectionUtils.connect(mysql.getJdbcUrl(), mysql.getUsername(), saved)) {
            return Optional.of(saved);
        }
        return Optional.empty();
    }

    @NotNull
    private static Optional<String> inputPassword(@Nonnull final Module module, @Nonnull final MySQLDatabaseResource mysql) {
        final AtomicReference<Password> passwordConfigReference = new AtomicReference<>();
        final ActionConstants.ActionEntity operation = ActionConstants.parse(ActionConstants.MySQL.UPDATE_PASSWORD);
        final IAzureOperationTitle title = AzureOperationBundle.title(String.format("%s.%s", operation.getServiceName(), operation.getOperationName()));
        AzureTaskManager.getInstance().runAndWait(title, () -> {
            final PasswordDialog dialog = new PasswordDialog(module.getProject(), mysql.getUsername(), mysql.getJdbcUrl());
            dialog.setOkActionListener(data -> {
                dialog.close();
                if (MySQLConnectionUtils.connect(mysql.getJdbcUrl(), mysql.getUsername(), String.valueOf(data.password()))) {
                    PasswordStore.savePassword(mysql.getId(), mysql.getUsername(), data.password(), data.saveType());
                    if (mysql.getPassword().saveType() != data.saveType()) {
                        mysql.getPassword().saveType(data.saveType());
                    }
                }
                passwordConfigReference.set(data);
            });
            dialog.show();
        });
        return Optional.ofNullable(passwordConfigReference.get()).map(c -> String.valueOf(c.password()));
    }

    @RequiredArgsConstructor
    public enum Definition implements ConnectionDefinition<MySQLDatabaseResource, ModuleResource> {
        MODULE_MYSQL;

        private static final String PROMPT_TITLE = "Azure Resource Connector";
        private static final String[] PROMPT_OPTIONS = new String[]{"Yes", "No"};
        private static final String MESSAGE_TO_OVERRIDE = "This resource already existed in your local environment. Do you want to override it?";

        @Override
        public MySQLDatabaseResourceConnection create(MySQLDatabaseResource resource, ModuleResource consumer) {
            return new MySQLDatabaseResourceConnection(resource, consumer);
        }

        @Override
        public boolean write(@Nonnull Element connectionEle, @Nonnull Connection<? extends MySQLDatabaseResource, ? extends ModuleResource> connection) {
            final MySQLDatabaseResource resource = connection.getResource();
            final ModuleResource consumer = connection.getConsumer();
            if (StringUtils.isNotBlank(resource.getEnvPrefix())) {
                connectionEle.setAttribute("envPrefix", resource.getEnvPrefix());
            }
            connectionEle.addContent(new Element("resource").setAttribute("type", resource.getType()).setText(resource.getId()));
            connectionEle.addContent(new Element("consumer").setAttribute("type", consumer.getType()).setText(consumer.getId()));
            return true;
        }

        @Nonnull
        public MySQLDatabaseResourceConnection read(@Nonnull Element connectionEle) {
            final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
            // TODO: check if module exists
            final ModuleResource consumer = new ModuleResource(connectionEle.getChildTextTrim("consumer"));
            final MySQLDatabaseResource resource = (MySQLDatabaseResource) manager.getResourceById(connectionEle.getChildTextTrim("resource"));
            final String envPrefix = connectionEle.getAttributeValue("envPrefix");
            Optional.ofNullable(resource).ifPresent(d -> resource.setEnvPrefix(envPrefix));
            return new MySQLDatabaseResourceConnection(resource, consumer);
        }

        @Override
        public boolean validate(Connection<MySQLDatabaseResource, ModuleResource> connection, Project project) {
            final MySQLDatabaseResource resource = connection.getResource();
            final String resourceId = resource.getBizId();
            final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
            final MySQLDatabaseResource existed = (MySQLDatabaseResource) manager.getResourceByBizId(resourceId);
            if (Objects.nonNull(existed)) { // not new
                final boolean urlModified = !Objects.equals(resource.getJdbcUrl(), existed.getJdbcUrl());
                final boolean usernameModified = !StringUtils.equals(resource.getUsername(), existed.getUsername());
                final boolean passwordSaveTypeModified = resource.getPassword().saveType() != existed.getPassword().saveType();
                if (urlModified || usernameModified || passwordSaveTypeModified) { // modified
                    // TODO: @qianjin what if only password is changed.
                    return DefaultLoader.getUIHelper().showConfirmation(MESSAGE_TO_OVERRIDE, PROMPT_TITLE, PROMPT_OPTIONS, null);
                }
            }
            return true; // is new or not modified.
        }
    }
}
