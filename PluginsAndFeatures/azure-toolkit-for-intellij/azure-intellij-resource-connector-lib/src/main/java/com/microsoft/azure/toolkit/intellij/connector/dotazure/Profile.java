/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.connector.*;
import com.microsoft.azure.toolkit.intellij.facet.AzureFacet;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import io.github.cdimascio.dotenv.internal.DotenvParser;
import io.github.cdimascio.dotenv.internal.DotenvReader;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;
import static com.microsoft.azure.toolkit.intellij.connector.IManagedIdentitySupported.*;
import static com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule.DOT_ENV;

@Getter
public class Profile {
    public static final String NO_PERMISSION_MESSAGE = "The managed identity <a href=\"%s\">%s</a> (%s) doesn't have enough permission to access resource %s.";
    @Nonnull
    private final String name;
    @Nonnull
    private final VirtualFile profileDir;
    @Nonnull
    private final AzureModule module;
    @Nonnull
    private final ConnectionManager connectionManager;
    @Nonnull
    private final ResourceManager resourceManager;
    @Nonnull
    private final DeploymentTargetManager deploymentTargetManager;
    @Nullable
    private VirtualFile dotEnvFile;

    public Profile(@Nonnull String name, @Nonnull VirtualFile profileDir, @Nonnull AzureModule module) {
        this.name = name;
        this.module = module;
        this.profileDir = profileDir;
        this.resourceManager = new ResourceManager(this);
        this.connectionManager = new ConnectionManager(this);
        this.deploymentTargetManager = new DeploymentTargetManager(this);
        this.dotEnvFile = this.profileDir.findChild(DOT_ENV);
    }

    public List<Pair<String, String>> load() {
        return Optional.ofNullable(this.dotEnvFile).map(Profile::load).orElseGet(Collections::emptyList);
    }

    public static List<Pair<String, String>> load(@Nonnull VirtualFile dotEnv) {
        final DotenvReader reader = new DotenvReader(dotEnv.getParent().getPath(), dotEnv.getName());
        final DotenvParser parser = new DotenvParser(reader, false, false);
        return parser.parse().stream().map(e -> Pair.of(e.getKey(), e.getValue())).toList();
    }

    public synchronized Profile addApp(@Nonnull final AbstractAzResource<?, ?, ?> app) {
        this.getDeploymentTargetManager().addTarget(app.getId());
        final Project project = this.module.getProject();
        project.getMessageBus().syncPublisher(DeploymentTargetTopics.TARGET_APP_CHANGED).appChanged(this.module, app, DeploymentTargetTopics.Action.ADD);
        return this;
    }

    public synchronized Profile removeApp(@Nonnull final AbstractAzResource<?, ?, ?> app) {
        this.getDeploymentTargetManager().removeTarget(app.getId());
        final Project project = this.module.getProject();
        project.getMessageBus().syncPublisher(DeploymentTargetTopics.TARGET_APP_CHANGED).appChanged(this.module, app, DeploymentTargetTopics.Action.REMOVE);
        return this;
    }

    public synchronized Future<?> addConnection(@Nonnull Connection<?, ?> connection) {
        AzureFacet.addTo(this.module.getModule());
        final Resource<?> resource = connection.getResource();
        this.resourceManager.addResource(resource);
        this.connectionManager.addConnection(connection);
        // handle user assigned managed identity
        final Resource<Identity> identityResource = connection.getUserAssignedManagedIdentity();
        if (Objects.nonNull(identityResource)) {
            this.resourceManager.addResource(identityResource);
            AzureTaskManager.getInstance().runInBackground("Validating connection", () -> {
                final String identity = identityResource.getData().getPrincipalId();
                if (resource instanceof AzureServiceResource<?> serviceResource && !checkPermission(serviceResource, identity)) {
                    final String message = String.format(NO_PERMISSION_MESSAGE, identityResource.getData().getPortalUrl(), identityResource.getName(),identityResource.getData().getPrincipalId(), resource.getName());
                    final Action<?> openIdentityConfigurationAction = getOpenIdentityConfigurationAction(serviceResource);
                    final Action<?> grantPermissionAction = getGrantPermissionAction(serviceResource, identity);
                    AzureMessager.getMessager().warning(message, grantPermissionAction, openIdentityConfigurationAction);
                }
            });
        }
        return this.addConnectionToDotEnv(connection);
    }

    public synchronized Profile removeConnection(@Nonnull Connection<?, ?> connection) {
        this.removeConnectionFromDotEnv(connection);
        this.connectionManager.removeConnection(connection);
        final Project project = this.module.getProject();
        project.getMessageBus().syncPublisher(CONNECTION_CHANGED).connectionChanged(project, connection, ConnectionTopics.Action.REMOVE);
        return this;
    }

    public synchronized Future<?> createOrUpdateConnection(@Nonnull Connection<?, ?> connection) {
        // Remove old connection
        this.getConnections().stream().filter(c -> c.getId().equals(connection.getId())).findFirst().ifPresent(this::removeConnection);
        return this.addConnection(connection);
    }

    public synchronized void reload() {
        this.resourceManager.reload();
        this.deploymentTargetManager.reload();
        this.connectionManager.reload();
    }

    public synchronized void save() {
        try {
            this.connectionManager.save();
            this.resourceManager.save();
            this.deploymentTargetManager.save();
            this.profileDir.refresh(true, true);
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @AzureOperation(value = "internal/connector.generate_env_variables.resource", params = "connection.getResource().getName()")
    private static List<String> generateEnvLines(@Nonnull final Project project, @Nonnull final Connection<?, ?> connection) {
        final ArrayList<String> lines = new ArrayList<>();
        lines.add("# connection.id=" + connection.getId());
        lines.addAll(connection.getEnvironmentVariables(project).entrySet().stream()
            .map((e) -> String.format("%s=\"%s\"", e.getKey(), e.getValue()))
            .toList());
        return lines;
    }

    @SneakyThrows(IOException.class)
    @AzureOperation(value = "boundary/connector.remove_connection_from_dotenv.resource", params = "connection.getResource().getName()")
    private void removeConnectionFromDotEnv(@Nonnull Connection<?, ?> connection) {
        if (Objects.isNull(this.dotEnvFile) || !this.dotEnvFile.isValid()) {
            // users may not have env file when they clone project from repo, so just return here
            return;
        }
        final List<String> lines = Files.readAllLines(this.dotEnvFile.toNioPath());
        final String startMark = "# connection.id=" + connection.getId();
        boolean started = false;
        final Iterator<String> each = lines.iterator();
        while (each.hasNext()) {
            final String line = each.next();
            started = started || line.equalsIgnoreCase(startMark);
            final boolean ended = started && !line.equalsIgnoreCase(startMark) && (StringUtils.isBlank(line.trim()) || line.trim().startsWith("# connection.id="));
            if (started && (!ended || StringUtils.isBlank(line.trim()))) {
                each.remove();
            }
            if (ended) {
                break;
            }
        }
        FileUtils.write(this.dotEnvFile.toNioPath().toFile(), lines.stream().collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    @SneakyThrows(IOException.class)
    @AzureOperation(value = "boundary/connector.add_connection_to_dotenv.resource", params = "connection.getResource().getName()")
    private Future<?> addConnectionToDotEnv(@Nonnull Connection<?, ?> connection) {
        if (!this.profileDir.isValid()) {
            throw new AzureToolkitRuntimeException(String.format("'.azure/%s' doesn't exist.", this.name));
        }
        WriteAction.run(() -> this.dotEnvFile = this.profileDir.findOrCreateChildData(this, DOT_ENV));
        Objects.requireNonNull(this.dotEnvFile, String.format("'.azure/%s/.env' can not be created.", this.name));
        final AzureString description = OperationBundle.description("boundary/connector.load_env.resource", connection.getResource().getDataId());
        return ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final String envVariables = generateEnvLines(module.getProject(), connection).stream().collect(Collectors.joining(System.lineSeparator()));
                Files.writeString(this.dotEnvFile.toNioPath(), envVariables + System.lineSeparator() + System.lineSeparator(), StandardOpenOption.APPEND);
                this.profileDir.refresh(true, true);
                final String message = String.format("The connection between %s and %s has been successfully created/updated.", connection.getResource().getName(), connection.getConsumer().getName());
                AzureMessager.getMessager().success(message);
                final Project project = this.module.getProject();
                project.getMessageBus().syncPublisher(CONNECTION_CHANGED).connectionChanged(project, connection, ConnectionTopics.Action.ADD);
            } catch (final Throwable e) {
                AzureMessager.getMessager().error(e);
            }
        });
    }

    public boolean isEnvFileValid() {
        return Optional.ofNullable(dotEnvFile).map(VirtualFile::isValid).orElse(false);
    }

    @Nonnull
    @SneakyThrows(IOException.class)
    public List<Pair<String, String>> getGeneratedEnvironmentVariables(@Nonnull Connection<?, ?> connection) {
        if (Objects.isNull(this.dotEnvFile) || !this.dotEnvFile.isValid()) {
            return Collections.emptyList();
        }
        final List<String> lines = Files.readAllLines(this.dotEnvFile.toNioPath());
        final String startMark = "# connection.id=" + connection.getId();
        boolean started = false;
        final List<String> generated = new ArrayList<>();
        for (final String line : lines) {
            started = started || line.equalsIgnoreCase(startMark);
            final boolean ended = started && !line.equalsIgnoreCase(startMark) && (StringUtils.isBlank(line.trim()) || line.trim().startsWith("# connection.id="));
            if (started && !ended && !line.equalsIgnoreCase(startMark) && StringUtils.isNotBlank(line)) {
                generated.add(line);
            }
            if (ended) {
                break;
            }
        }
        return generated.stream().map(g -> g.split("=", 2)).map(a -> Pair.of(a[0], a[1])).toList();
    }

    public List<Connection<?, ?>> getConnections() {
        return this.connectionManager.getConnections();
    }

    public List<String> getTargetAppIds() {
        return this.deploymentTargetManager.getTargets();
    }
}
