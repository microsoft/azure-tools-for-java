/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.mysql.component.PasswordDialog;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.toolkit.intellij.connector.mysql.MySQLConnectionUtils.ACCESS_DENIED_ERROR_CODE;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MySQLDatabaseResourceConnection implements Connection<MySQLDatabaseResource, ModuleResource> {
    public static final Key<Map<String, String>> ENV_VARS = Key.create("ConnectAzureResourceEnvVars");
    private static final String SPRING_BOOT_CONFIGURATION = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";
    private static final String AZURE_WEBAPP_CONFIGURATION = "com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration";
    @Getter
    @Nonnull
    private final MySQLDatabaseResource resource;
    @Getter
    @Nonnull
    @EqualsAndHashCode.Include // a module can only connect to one MySQL database
    private final ModuleResource consumer;
    private Map<String, String> env = new HashMap<>();

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
    @AzureOperation(name = "connector|mysql.before_run_task", type = AzureOperation.Type.ACTION)
    public boolean prepareBeforeRun(@Nonnull RunConfiguration configuration, DataContext dataContext) {
        this.env = this.initEnv();
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

    private Map<String, String> initEnv() {
        final Map<String, String> env = new HashMap<>();
        final Module module = this.consumer.getModule();
        final MySQLDatabaseResource mysql = this.resource;
        assert module != null : "loading password from unknown module";
        env.put(mysql.getEnvPrefix() + "URL", this.resource.getJdbcUrl().toString());
        env.put(mysql.getEnvPrefix() + "USERNAME", this.resource.getUsername());
        env.put(mysql.getEnvPrefix() + "PASSWORD", loadPassword(module, mysql).or(() -> inputPassword(module, mysql)).orElse(""));
        return env;
    }

    private static Optional<String> loadPassword(@Nonnull final Module module, @Nonnull final MySQLDatabaseResource mysql) {
        final String saved = PasswordStore.loadPassword(mysql.getId(), mysql.getUsername(), mysql.getPassword().saveType());
        final MySQLConnectionUtils.ConnectResult result = MySQLConnectionUtils.connectWithPing(mysql.getJdbcUrl(), mysql.getUsername(), saved);
        if (StringUtils.isNotBlank(saved) && result.isConnected()) {
            return Optional.of(saved);
        }
        if (result.getErrorCode() != ACCESS_DENIED_ERROR_CODE) {
            AzureMessager.getMessager().warning(result.getMessage(), "Azure Resource Connector");
        }
        return Optional.empty();
    }

    @Nonnull
    private static Optional<String> inputPassword(@Nonnull final Module module, @Nonnull final MySQLDatabaseResource mysql) {
        final AtomicReference<Password> passwordRef = new AtomicReference<>();
        final IAzureOperationTitle title = AzureOperationBundle.title("azure-mysql.azure-mysql-update-password");
        AzureTaskManager.getInstance().runAndWait(title, () -> {
            final PasswordDialog dialog = new PasswordDialog(module.getProject(), mysql.getUsername(), mysql.getJdbcUrl());
            if (dialog.showAndGet()) {
                final Password password = dialog.getData();
                mysql.getPassword().saveType(password.saveType());
                PasswordStore.savePassword(mysql.getId(), mysql.getUsername(), password.password(), password.saveType());
                passwordRef.set(password);
            }
        });
        return Optional.ofNullable(passwordRef.get()).map(c -> String.valueOf(c.password()));
    }

    @RequiredArgsConstructor
    public enum Definition implements ConnectionDefinition<MySQLDatabaseResource, ModuleResource> {
        MODULE_MYSQL;

        private static final String PROMPT_TITLE = "Azure Resource Connector";
        private static final String[] PROMPT_OPTIONS = new String[]{"Yes", "No"};

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

        @Nullable
        public MySQLDatabaseResourceConnection read(@Nonnull Element connectionEle) {
            final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
            // TODO: check if module exists
            final ModuleResource consumer = new ModuleResource(connectionEle.getChildTextTrim("consumer"));
            final MySQLDatabaseResource resource = (MySQLDatabaseResource) manager.getResourceById(connectionEle.getChildTextTrim("resource"));
            if (Objects.nonNull(resource)) {
                resource.setEnvPrefix(connectionEle.getAttributeValue("envPrefix"));
                return new MySQLDatabaseResourceConnection(resource, consumer);
            } else {
                // TODO: alert user to create new resource
                return null;
            }
        }

        @Override
        public boolean validate(Connection<MySQLDatabaseResource, ModuleResource> connection, Project project) {
            final MySQLDatabaseResource mysql = connection.getResource();
            final ModuleResource module = connection.getConsumer();
            final ConnectionManager connectionManager = project.getService(ConnectionManager.class);
            final ResourceManager resourceManager = ServiceManager.getService(ResourceManager.class);
            final var existedConnections = connectionManager.getConnectionsByConsumerId(module.getId());
            final var existedResource = (MySQLDatabaseResource) resourceManager.getResourceById(mysql.getId());
            if (Objects.nonNull(existedResource)) { // not new
                final boolean urlModified = !Objects.equals(mysql.getJdbcUrl(), existedResource.getJdbcUrl());
                final boolean usernameModified = !StringUtils.equals(mysql.getUsername(), existedResource.getUsername());
                final boolean passwordSaveTypeModified = mysql.getPassword().saveType() != existedResource.getPassword().saveType();
                if (urlModified || usernameModified || passwordSaveTypeModified) { // modified
                    // TODO: @qianjin what if only password is changed.
                    final String template = "MySQL database \"%s/%s\" with different configuration is found on your PC. \nDo you want to override it?";
                    final String msg = String.format(template, mysql.getServerId().name(), mysql.getDatabaseName());
                    return DefaultLoader.getUIHelper().showConfirmation(msg, PROMPT_TITLE, PROMPT_OPTIONS, null);
                }
            } else if (CollectionUtils.isNotEmpty(existedConnections)) {
                final MySQLDatabaseResource connectedMySQL = (MySQLDatabaseResource) existedConnections.get(0).getResource();
                final String template = "Module \"%s\" has already connected to MySQL database \"%s/%s\". \nDo you want to reconnect it to database \"%s/%s\"?";
                final String msg = String.format(template,
                                                 module.getModuleName(),
                                                 connectedMySQL.getServerId().name(),
                                                 connectedMySQL.getDatabaseName(),
                                                 mysql.getServerId().name(),
                                                 mysql.getDatabaseName());
                return DefaultLoader.getUIHelper().showConfirmation(msg, PROMPT_TITLE, PROMPT_OPTIONS, null);
            }

            return true; // is new or not modified.
        }
    }
}
