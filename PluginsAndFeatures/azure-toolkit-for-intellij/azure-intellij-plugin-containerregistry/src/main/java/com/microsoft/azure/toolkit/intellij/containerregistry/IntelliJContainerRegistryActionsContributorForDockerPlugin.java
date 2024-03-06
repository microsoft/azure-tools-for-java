/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry;

import com.intellij.docker.registry.DockerRegistryConfiguration;
import com.intellij.docker.registry.DockerRegistryManager;
import com.intellij.docker.view.registry.DockerRegistryServiceViewContributor;
import com.intellij.docker.view.registry.node.DockerRegistryRoot;
import com.intellij.execution.services.ServiceEventListener;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.containerregistry.servicesview.AzureContainerRegistryProvider;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static com.microsoft.azure.toolkit.intellij.containerregistry.servicesview.AzureContainerRegistryConfigurator.ENABLE_ADMIN_USER_DOC_LINK;

@SuppressWarnings("UnstableApiUsage")
public class IntelliJContainerRegistryActionsContributorForDockerPlugin implements IActionsContributor {
    public static final String NULL_SERVICE_MESSAGE = "Failed to get Docker Registries root in \"Services\" view, please check whether \"Docker\" plugin is correctly installed.";

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.registerHandler(ResourceCommonActionsContributor.OPEN_IN_SERVICES_VIEW, (r, e) -> r instanceof ContainerRegistry,
            (AbstractAzResource<?, ?, ?> r, AnActionEvent e) -> {
                final ContainerRegistry registry = (ContainerRegistry) r;
                final AzureString msg = AzureString.format("<html><body>Admin user is not enabled for (%s), but it is required to login a Azure Container Registry. <b>Do you want to enable Admin user and then open it in \"services\" view?</b> <br>You can learn more about admin user <a href='" + ENABLE_ADMIN_USER_DOC_LINK + "'>here</a>.</body></html>", registry.getName());
                final IAzureMessage message = AzureMessager.getMessager().buildConfirmMessage(msg, "Enable Admin User");
                final boolean[] result = new boolean[1];
                if (!registry.isAdminUserEnabled()) {
                    ApplicationManager.getApplication().invokeAndWait(() -> result[0] = MessageDialogBuilder.yesNo("Enable Admin User", message.getContent()).guessWindowAndAsk());
                    if (result[0]) {
                        final Action<ContainerRegistry> enableAdminUser = AzureActionManager.getInstance().getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER);
                        enableAdminUser.handleSync(registry);
                    }
                }
                if (registry.isAdminUserEnabled()) {
                    AzureTaskManager.getInstance().runLater(() -> openRegistryInServicesView(registry, Objects.requireNonNull(e.getProject())));
                }
            }
        );
    }

    private static void openRegistryInServicesView(@Nonnull final ContainerRegistry data, @Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Services");
        if (Objects.nonNull(toolWindow) && !toolWindow.isActive()) {
            toolWindow.activate(() -> openRegistryInServicesView(data, project));
            return;
        }
        final ServiceViewContributor<?> root = ServiceViewContributor.CONTRIBUTOR_EP_NAME.findExtension(DockerRegistryServiceViewContributor.class);
        if (Objects.isNull(root)) {
            final Action<String> installAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SEARCH_MARKETPLACE_PLUGIN).bind("Docker").withLabel("Install");
            AzureMessager.getMessager().warning(NULL_SERVICE_MESSAGE, installAction);
            return;
        }
        final ServiceViewContributor<?> registry = getRegistryService(data, project, root).orElse(null);
        if (Objects.nonNull(registry)) {
            focusInServicesView(registry, root, project);
        } else {
            addAndFocusInServicesView(data, root, project);
        }
    }

    private static void addAndFocusInServicesView(@Nonnull final ContainerRegistry data, ServiceViewContributor<?> root, @Nonnull final Project project) {
        final DockerRegistryManager registryManager = DockerRegistryManager.getInstance();
        final Predicate<DockerRegistryConfiguration> exists = r -> r.getAddress().equals(data.getLoginServerUrl()) && r.getUsername().equalsIgnoreCase(data.getUserName());
        if (registryManager.getRegistries().stream().noneMatch(exists)) {
            final MessageBus messageBus = project.getMessageBus();
            final MessageBusConnection connect = messageBus.connect();
            connect.subscribe(ServiceEventListener.TOPIC, (ServiceEventListener) serviceEvent -> {
                final boolean isDockerRegistryEvent = serviceEvent.contributorClass == root.getClass();
                if (isDockerRegistryEvent) {
                    getRegistryService(data, project, root).ifPresent(service -> {
                        focusInServicesView(service, root, project);
                        connect.disconnect();
                    });
                }
            });
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                final DockerRegistryConfiguration config = new DockerRegistryConfiguration().withName(data.getName());
                config.setRegistryProviderId(AzureContainerRegistryProvider.ID);
                config.setAddress(data.getLoginServerUrl());
                config.setUsername(data.getUserName());
                config.setPasswordSafe(data.getPrimaryCredential());

                registryManager.addRegistry(config);
            });
        }
    }

    private static void focusInServicesView(@Nonnull final ServiceViewContributor<?> service,
                                            @Nonnull final ServiceViewContributor<?> contributor, final Project project) {
        ServiceViewManager.getInstance(project).select(service, contributor.getClass(), true, true);
        ServiceViewManager.getInstance(project).expand(service, contributor.getClass());
    }

    private static Optional<DockerRegistryRoot> getRegistryService(final @Nonnull ContainerRegistry data, final @Nonnull Project project, final ServiceViewContributor<?> root) {
        return root.getServices(project).stream()
            .filter(c -> c instanceof DockerRegistryRoot)
            .map(c -> (DockerRegistryRoot) c)
            .filter(r -> StringUtils.equals(r.getConfiguration().getAddress(), data.getLoginServerUrl()) && StringUtils.equals(r.getConfiguration().getUsername(), data.getUserName()))
            .findFirst();
    }

}
