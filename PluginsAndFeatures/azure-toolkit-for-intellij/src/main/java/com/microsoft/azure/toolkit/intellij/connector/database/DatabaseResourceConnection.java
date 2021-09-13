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
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.PasswordStore;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordDialog;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils.ACCESS_DENIED_ERROR_CODE;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DatabaseResourceConnection implements Connection<Database, String> {
    private static final String SPRING_BOOT_CONFIGURATION = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";
    private static final String AZURE_WEBAPP_CONFIGURATION = "com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig.WebAppConfiguration";
    @Getter
    @Nonnull
    @EqualsAndHashCode.Include
    private final DatabaseResource resource;
    @Getter
    @Nonnull
    @EqualsAndHashCode.Include
    private final ModuleResource consumer;
    @Getter
    private final Definition definition;
    private Map<String, String> env = new HashMap<>();

    @Override
    public boolean isApplicableFor(@Nonnull RunConfiguration configuration) {
        final boolean javaAppRunConfiguration = configuration instanceof ApplicationConfiguration;
        final boolean springbootAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), SPRING_BOOT_CONFIGURATION);
        final boolean azureWebAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), AZURE_WEBAPP_CONFIGURATION);
        if (javaAppRunConfiguration || azureWebAppRunConfiguration || springbootAppRunConfiguration) {
            final Module module = getTargetModule(configuration);
            return Objects.nonNull(module) && Objects.equals(module.getName(), this.consumer.getName());
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
    public void updateJavaParametersAtRun(@Nonnull RunConfiguration configuration, @Nonnull JavaParameters parameters) {
        if (Objects.nonNull(this.env)) {
            for (final Map.Entry<String, String> entry : this.env.entrySet()) {
                parameters.addEnv(entry.getKey(), entry.getValue());
            }
        }
    }

    @Nullable
    private static Module getTargetModule(@Nonnull RunConfiguration configuration) {
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
        final Map<String, String> envMap = new HashMap<>();
        final Database database = this.resource.getDatabase();
        envMap.put(database.getEnvPrefix() + "URL", database.getJdbcUrl().toString());
        envMap.put(database.getEnvPrefix() + "USERNAME", database.getUsername());
        envMap.put(database.getEnvPrefix() + "PASSWORD", loadPassword(this.resource).or(() -> inputPassword(project, resource)).orElse(""));
        return envMap;
    }

    private static Optional<String> loadPassword(@Nonnull final DatabaseResource resource) {
        final Database database = resource.getDatabase();
        if (Objects.nonNull(database.getPassword()) && database.getPassword().saveType() == Password.SaveType.NEVER) {
            return Optional.empty();
        }
        final String defName = resource.getDefName();
        if (database.getPassword().saveType() == Password.SaveType.FOREVER) {
            PasswordStore.migratePassword(database.getId(), database.getUsername(),
                    defName, database.getId(), database.getUsername());
        }
        final String saved = PasswordStore.loadPassword(defName, database.getId(), database.getUsername(), database.getPassword().saveType());
        if (database.getPassword().saveType() == Password.SaveType.UNTIL_RESTART && StringUtils.isBlank(saved)) {
            return Optional.empty();
        }
        final DatabaseConnectionUtils.ConnectResult result = DatabaseConnectionUtils.connectWithPing(database.getJdbcUrl(), database.getUsername(), saved);
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
        final Database database = resource.getDatabase();
        AzureTaskManager.getInstance().runAndWait(title, () -> {
            final PasswordDialog dialog = new PasswordDialog(project, resource);
            if (dialog.showAndGet()) {
                final Password password = dialog.getData();
                database.getPassword().saveType(password.saveType());
                PasswordStore.savePassword(resource.getDefName(), database.getId(), database.getUsername(), password.password(), password.saveType());
                passwordRef.set(password);
            }
        });
        return Optional.ofNullable(passwordRef.get()).map(c -> String.valueOf(c.password()));
    }

    @Getter
    public static class Definition extends ConnectionDefinition<Database, String> {
        public static final Definition MODULE_MYSQL = new Definition(DatabaseResource.Definition.AZURE_MYSQL, ModuleResource.Definition.IJ_MODULE);
        public static final Definition MODULE_SQL = new Definition(DatabaseResource.Definition.SQL_SERVER, ModuleResource.Definition.IJ_MODULE);

        private static final String PROMPT_TITLE = "Azure Resource Connector";
        private static final String[] PROMPT_OPTIONS = new String[]{"Yes", "No"};

        public Definition(ResourceDefinition<Database> rd, ResourceDefinition<String> cd) {
            super(rd, cd);
        }

        @Nonnull
        @Override
        public Connection<Database, String> define(Resource<Database> resource, Resource<String> consumer) {
            return new DatabaseResourceConnection((DatabaseResource) resource, (ModuleResource) consumer, this);
        }

        @Nullable
        @Override
        public Connection<Database, String> read(Element connectionEle) {
            final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
            // TODO: check if module exists
            final ModuleResource moduleConsumer = new ModuleResource(connectionEle.getChildTextTrim("consumer"));
            final DatabaseResource databaseResource = (DatabaseResource) manager.getResourceById(connectionEle.getChildTextTrim("resource"));
            if (Objects.nonNull(databaseResource)) {
                databaseResource.getDatabase().setEnvPrefix(connectionEle.getAttributeValue("envPrefix"));
                return new DatabaseResourceConnection(databaseResource, moduleConsumer, this);
            } else {
                // TODO: alert user to create new resource
                return null;
            }
        }

        @Override
        public boolean write(Element connectionEle, Connection<? extends Database, ? extends String> c) {
            final DatabaseResourceConnection connection = (DatabaseResourceConnection) c;
            final DatabaseResource resource = connection.getResource();
            final ModuleResource consumer = connection.getConsumer();

            if (StringUtils.isNotBlank(resource.getDatabase().getEnvPrefix())) {
                connectionEle.setAttribute("envPrefix", resource.getDatabase().getEnvPrefix());
            }
            connectionEle.addContent(new Element("resource").setAttribute("type", resource.getDefName()).setText(resource.getId()));
            connectionEle.addContent(new Element("consumer").setAttribute("type", consumer.getDefName()).setText(consumer.getId()));
            return true;
        }

        @Override
        public boolean validate(Connection<?, ?> c, Project project) {
            final DatabaseResourceConnection connection = (DatabaseResourceConnection) c;
            final ResourceManager resourceManager = ServiceManager.getService(ResourceManager.class);
            final DatabaseResource resource = connection.getResource();
            final DatabaseResource existedResource = (DatabaseResource) resourceManager.getResourceById(resource.getId());
            if (Objects.nonNull(existedResource)) { // not new
                final Database db = resource.getDatabase();
                final Database edb = existedResource.getDatabase();
                final boolean urlModified = !Objects.equals(db.getJdbcUrl(), edb.getJdbcUrl());
                final boolean usernameModified = !StringUtils.equals(db.getUsername(), edb.getUsername());
                final boolean passwordSaveTypeModified = db.getPassword().saveType() != edb.getPassword().saveType();
                if (urlModified || usernameModified || passwordSaveTypeModified) { // modified
                    // TODO: @qianjin what if only password is changed.
                    final String template = "Database \"%s\" with different configuration is found on your PC. \nDo you want to override it?";
                    final String msg = String.format(template, resource.getName());
                    if (!AzureMessager.getMessager().confirm(msg, PROMPT_TITLE)) {
                        return false;
                    }
                }
            }
            final ConnectionManager connectionManager = project.getService(ConnectionManager.class);
            final ModuleResource consumer = connection.getConsumer();
            final List<Connection<?, ?>> existedConnections = connectionManager.getConnectionsByConsumerId(consumer.getId());
            if (CollectionUtils.isNotEmpty(existedConnections)) {
                final Connection<?, ?> existedConnection = existedConnections.stream()
                        .filter(e -> StringUtils.equals(e.getConsumer().getId(), consumer.getId()) && e.getResource() instanceof DatabaseResource &&
                                StringUtils.equals(((DatabaseResource) e.getResource()).getDatabase().getEnvPrefix(), resource.getDatabase().getEnvPrefix()))
                        .findFirst().orElse(null);
                if (Objects.nonNull(existedConnection)) { // modified
                    final DatabaseResource connected = (DatabaseResource) existedConnection.getResource();
                    final String template = "Module \"%s\" has already connected to database \"%s\". \n" +
                            "Do you want to reconnect it to database \"%s\"?";
                    final String msg = String.format(template, consumer.getName(), connected.getName(), resource.getName());
                    return AzureMessager.getMessager().confirm(msg, PROMPT_TITLE);
                }
            }
            return true; // is new or not modified.
        }
    }
}
