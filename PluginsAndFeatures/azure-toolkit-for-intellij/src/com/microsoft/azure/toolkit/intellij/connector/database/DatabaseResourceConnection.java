/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordDialog;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils.ACCESS_DENIED_ERROR_CODE;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DatabaseResourceConnection implements Connection<DatabaseResource, ModuleResource> {
    private static final String SPRING_BOOT_CONFIGURATION = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";
    private static final String AZURE_WEBAPP_CONFIGURATION = "com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration";
    @Getter
    @Nonnull
    private final DatabaseResource resource;
    @Getter
    @Nonnull
    @EqualsAndHashCode.Include
    private final ModuleResource consumer;
    private Map<String, String> env = new HashMap<>();

    @Override
    @EqualsAndHashCode.Include
    public String getType() {
        return Connection.super.getType();
    }

    @Override
    public boolean isApplicableFor(@Nonnull RunConfiguration configuration) {
        final boolean javaAppRunConfiguration = configuration instanceof ApplicationConfiguration;
        final boolean springbootAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), SPRING_BOOT_CONFIGURATION);
        final boolean azureWebAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), AZURE_WEBAPP_CONFIGURATION);
        if (javaAppRunConfiguration || azureWebAppRunConfiguration || springbootAppRunConfiguration) {
            final Module module = getTargetModule(configuration);
            return Objects.nonNull(module) && Objects.equals(module.getName(), this.consumer.getModuleName());
        }
        return false;
    }

    @Override
    @AzureOperation(name = "connector|mysql.prepare_before_run", type = AzureOperation.Type.ACTION)
    public boolean prepareBeforeRun(@Nonnull RunConfiguration configuration, DataContext dataContext) {
        this.env = this.initEnv(configuration.getProject());
        if (configuration instanceof WebAppConfiguration) { // set envs for remote deploy
            final WebAppConfiguration webAppConfiguration = (WebAppConfiguration) configuration;
            webAppConfiguration.setApplicationSettings(this.env);
        }
        return true;
    }

    @Override
    public void updateJavaParametersAtRun(@NotNull RunConfiguration configuration, @NotNull JavaParameters parameters) {
        if (Objects.nonNull(this.env)) {
            for (final Map.Entry<String, String> entry : this.env.entrySet()) {
                parameters.addEnv(entry.getKey(), entry.getValue());
            }
        }
    }

    @Nullable
    private static Module getTargetModule(@NotNull RunConfiguration configuration) {
        if (configuration instanceof ModuleBasedConfiguration) {
            return ((ModuleBasedConfiguration<?, ?>) configuration).getConfigurationModule().getModule();
        } else if (configuration instanceof WebAppConfiguration) {
            final WebAppConfiguration webAppConfiguration = (WebAppConfiguration) configuration;
            final AzureArtifact azureArtifact = AzureArtifactManager.getInstance(configuration.getProject())
                    .getAzureArtifactById(webAppConfiguration.getAzureArtifactType(),
                            webAppConfiguration.getArtifactIdentifier());
            return AzureArtifactManager.getInstance(configuration.getProject()).getModuleFromAzureArtifact(azureArtifact);
        }
        return null;
    }

    private Map<String, String> initEnv(@Nonnull final Project project) {
        final Map<String, String> env = new HashMap<>();
        final DatabaseResource mysql = this.resource;
        env.put(mysql.getEnvPrefix() + "URL", this.resource.getJdbcUrl().toString());
        env.put(mysql.getEnvPrefix() + "USERNAME", this.resource.getUsername());
        env.put(mysql.getEnvPrefix() + "PASSWORD", loadPassword(mysql).or(() -> inputPassword(project, mysql)).orElse(""));
        return env;
    }

    private static Optional<String> loadPassword(@Nonnull final DatabaseResource resource) {
        if (Objects.nonNull(resource.getPassword()) && resource.getPassword().saveType() == Password.SaveType.NEVER) {
            return Optional.empty();
        }
        final String saved = PasswordStore.loadPassword(resource.getId(), resource.getUsername(), resource.getPassword().saveType());
        final DatabaseConnectionUtils.ConnectResult result = DatabaseConnectionUtils.connectWithPing(resource.getJdbcUrl(), resource.getUsername(), saved);
        if (StringUtils.isNotBlank(saved) && result.isConnected()) {
            return Optional.of(saved);
        }
        if (result.getErrorCode() != ACCESS_DENIED_ERROR_CODE) {
            AzureMessager.getMessager().warning(result.getMessage(), "Azure Resource Connector");
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<String> inputPassword(@Nonnull final Project project, @Nonnull final DatabaseResource resource) {
        final AtomicReference<Password> passwordRef = new AtomicReference<>();
        final AzureString title = AzureOperationBundle.title("mysql.update_password");
        AzureTaskManager.getInstance().runAndWait(title, () -> {
            final PasswordDialog dialog = new PasswordDialog(project, resource);
            if (dialog.showAndGet()) {
                final Password password = dialog.getData();
                resource.getPassword().saveType(password.saveType());
                PasswordStore.savePassword(resource.getId(), resource.getUsername(), password.password(), password.saveType());
                passwordRef.set(password);
            }
        });
        return Optional.ofNullable(passwordRef.get()).map(c -> String.valueOf(c.password()));
    }

    @RequiredArgsConstructor
    public enum Definition implements ConnectionDefinition<DatabaseResource, ModuleResource> {
        MODULE_MYSQL,
        MODULE_SQL;

        private static final String PROMPT_TITLE = "Azure Resource Connector";
        private static final String[] PROMPT_OPTIONS = new String[]{"Yes", "No"};

        @Override
        public DatabaseResourceConnection create(DatabaseResource resource, ModuleResource consumer) {
            return new DatabaseResourceConnection(resource, consumer);
        }

        @Override
        public boolean write(@Nonnull Element connectionEle, @Nonnull Connection<? extends DatabaseResource, ? extends ModuleResource> connection) {
            final DatabaseResource resource = connection.getResource();
            final ModuleResource consumer = connection.getConsumer();
            if (StringUtils.isNotBlank(resource.getEnvPrefix())) {
                connectionEle.setAttribute("envPrefix", resource.getEnvPrefix());
            }
            connectionEle.addContent(new Element("resource").setAttribute("type", resource.getType()).setText(resource.getId()));
            connectionEle.addContent(new Element("consumer").setAttribute("type", consumer.getType()).setText(consumer.getId()));
            return true;
        }

        @Override
        @Nullable
        public DatabaseResourceConnection read(@Nonnull Element connectionEle) {
            final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
            // TODO: check if module exists
            final ModuleResource consumer = new ModuleResource(connectionEle.getChildTextTrim("consumer"));
            final DatabaseResource resource = (DatabaseResource) manager.getResourceById(connectionEle.getChildTextTrim("resource"));
            if (Objects.nonNull(resource)) {
                resource.setEnvPrefix(connectionEle.getAttributeValue("envPrefix"));
                return new DatabaseResourceConnection(resource, consumer);
            } else {
                // TODO: alert user to create new resource
                return null;
            }
        }

        @Override
        public boolean validate(Connection<DatabaseResource, ModuleResource> connection, Project project) {
            final ResourceManager resourceManager = ServiceManager.getService(ResourceManager.class);
            final DatabaseResource databaseResource = connection.getResource();
            final DatabaseResource existedResource = (DatabaseResource) resourceManager.getResourceById(databaseResource.getId());
            if (Objects.nonNull(existedResource)) { // not new
                final boolean urlModified = !Objects.equals(databaseResource.getJdbcUrl(), existedResource.getJdbcUrl());
                final boolean usernameModified = !StringUtils.equals(databaseResource.getUsername(), existedResource.getUsername());
                final boolean passwordSaveTypeModified = databaseResource.getPassword().saveType() != existedResource.getPassword().saveType();
                if (urlModified || usernameModified || passwordSaveTypeModified) { // modified
                    // TODO: @qianjin what if only password is changed.
                    final String template = "%s database \"%s/%s\" with different configuration is found on your PC. \nDo you want to override it?";
                    final String msg = String.format(template, DatabaseResource.Definition.getTitleByType(databaseResource.getType()),
                            databaseResource.getServerId().name(), databaseResource.getDatabaseName());
                    boolean validated = DefaultLoader.getUIHelper().showConfirmation(msg, PROMPT_TITLE, PROMPT_OPTIONS, null);
                    if (!validated) {
                        return false;
                    }
                }
            }
            final ConnectionManager connectionManager = project.getService(ConnectionManager.class);
            final ModuleResource module = connection.getConsumer();
            final List<Connection<? extends Resource, ? extends Resource>> existedConnections = connectionManager.getConnectionsByConsumerId(module.getId());
            if (CollectionUtils.isNotEmpty(existedConnections)) {
                Connection<? extends Resource, ? extends Resource> existedConnection = existedConnections.stream()
                        .filter(e -> StringUtils.equals(e.getConsumer().getId(), module.getId()) && e.getResource() instanceof DatabaseResource &&
                                StringUtils.equals(((DatabaseResource) e.getResource()).getEnvPrefix(), databaseResource.getEnvPrefix()))
                        .findFirst().orElse(null);
                if (Objects.nonNull(existedConnection)) { // modified
                    final DatabaseResource connectedDatabaseResource = (DatabaseResource) existedConnection.getResource();
                    final String template = "Module \"%s\" with \"%s\" env prefix has already connected to %s database \"%s/%s\". \n" +
                            "Do you want to reconnect it to %s database \"%s/%s\"?";
                    final String msg = String.format(template,
                            module.getModuleName(),
                            databaseResource.getEnvPrefix(),
                            DatabaseResource.Definition.getTitleByType(connectedDatabaseResource.getType()),
                            connectedDatabaseResource.getServerId().name(),
                            connectedDatabaseResource.getDatabaseName(),
                            DatabaseResource.Definition.getTitleByType(databaseResource.getType()),
                            databaseResource.getServerId().name(),
                            databaseResource.getDatabaseName());
                    return DefaultLoader.getUIHelper().showConfirmation(msg, PROMPT_TITLE, PROMPT_OPTIONS, null);
                }
            }

            return true; // is new or not modified.
        }

    }
}
