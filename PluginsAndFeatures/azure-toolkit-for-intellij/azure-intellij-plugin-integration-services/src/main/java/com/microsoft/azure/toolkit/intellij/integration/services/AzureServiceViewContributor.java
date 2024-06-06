/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.execution.services.ServiceViewLazyContributor;
import com.intellij.execution.services.SimpleServiceViewDescriptor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.ActionNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.intellij.explorer.TypeGroupedServicesRootNode;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractConnectionStringAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.integration.services.AzureResourceActionsContributor.CONNECTION_STRING_RESOURCES;

public class AzureServiceViewContributor implements ServiceViewContributor<NodeViewContributor>, ServiceViewLazyContributor {
    public AzureServiceViewContributor() {
        AzureEventBus.on("account.logged_in.account", new AzureEventBus.EventListener((e) -> AzureResourceManagerListener.resourceChanged()));
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener((e) -> AzureResourceManagerListener.resourceChanged()));
    }

    @Override
    public @Nonnull ServiceViewDescriptor getViewDescriptor(@Nonnull final Project project) {
        return new SimpleServiceViewDescriptor("Azure", IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)); // hidden root
    }

    @Override
    public @Nonnull List<NodeViewContributor> getServices(@Nullable final Project project) {
        final NodeViewContributor o = new NodeViewContributor(new RootNode() // visible root
            .withChildrenLoadLazily(true)
            .withActions(new ActionGroup(
                IAccountActions.SIGN_IN,
                AzureResourceActionsContributor.REFRESH,
                IAccountActions.SELECT_SUBS,
                "----",
                AzureResourceActionsContributor.ADD_RESOURCE,
                AzureResourceActionsContributor.CONNECT_RESOURCE,
                "----",
                IAccountActions.SIGN_OUT
            ))
            .addChildren((a) -> buildChildrenNodes()));
        return Collections.singletonList(o);
    }

    @Override
    public @Nonnull ServiceViewDescriptor getServiceDescriptor(@Nonnull final Project project, @Nonnull final NodeViewContributor contributor) {
        return contributor.getViewDescriptor(project);
    }

    @Nonnull
    public static List<Node<?>> buildChildrenNodes() {
        final AzureResourceManager resourceManager = AzureResourceManager.getInstance();
        final List<? extends AbstractAzResource<?, ?, ?>> connectionStringResources = loadConnectionStringResources();

        final List<AbstractAzResource<?, ?, ?>> resources = new ArrayList<>(connectionStringResources);
        final ActionNode<Object> connectResource = new ActionNode<>(AzureResourceActionsContributor.CONNECT_RESOURCE);
        final boolean notSignedIn = !Azure.az(AzureAccount.class).isLoggedIn();
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            resources.addAll(loadNonConnectionStringResources());
        }
        if (resources.isEmpty()) {
            if (Azure.az(AzureAccount.class).isLoggedIn()) {
                final ActionNode<Object> addResource = new ActionNode<>(AzureResourceActionsContributor.ADD_RESOURCE);
                return Arrays.asList(addResource, connectResource);
            } else {
                final Action<Object> signIn = AzureActionManager.getInstance().getAction(Action.SIGN_IN).bind(new Object()).withLabel("Sign in to manage resources in your account...");
                return Arrays.asList(new ActionNode<>(signIn), connectResource);
            }
        }
        return buildResourceNodes(resources);
    }

    @Nonnull
    public static List<Node<?>> buildResourceNodes(final List<? extends AbstractAzResource<?, ?, ?>> resources) {
        final AzureExplorer.AzureExplorerNodeProviderManager manager = AzureExplorer.getManager();
        return resources.stream()
            .map(r -> manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
            .sorted(Comparator.comparing(Node::getLabel))
            .collect(Collectors.toList());
    }

    @Nonnull
    private static List<? extends AbstractAzResource<?, ?, ?>> loadNonConnectionStringResources() {
        return AzureResourceManager.getInstance().getResources().stream()
            .map(String::toLowerCase).distinct()
            .filter(id -> !AbstractConnectionStringAzResourceModule.CONNECTION_STRING_SUBSCRIPTION_ID.equalsIgnoreCase(ResourceId.fromString(id).subscriptionId()))
            .parallel().map(id -> {
                try {
                    return Azure.az().getById(id);
                } catch (final Throwable e) {
                    //noinspection ReturnOfNull
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Nonnull
    private static List<? extends AbstractAzResource<?, ?, ?>> loadConnectionStringResources() {
        return AzureResourceManager.getInstance().getResources().stream()
            .map(String::toLowerCase).distinct()
            .filter(id -> AbstractConnectionStringAzResourceModule.CONNECTION_STRING_SUBSCRIPTION_ID.equalsIgnoreCase(ResourceId.fromString(id).subscriptionId()))
            .map(id -> {
                try {
                    final String string = AzureStoreManager.getInstance().getSecureStore().loadPassword(CONNECTION_STRING_RESOURCES, id, null);
                    if (StringUtils.isNotBlank(string)) {
                        return Azure.az().getOrInitByIdAndConnectionString(id, string.trim());
                    }
                    //noinspection ReturnOfNull
                    return null;
                } catch (final Throwable e) {
                    //noinspection ReturnOfNull
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static class RootNode extends TypeGroupedServicesRootNode {
        protected void onAuthEvent() {
            final AzureAccount az = Azure.az(AzureAccount.class);
            String desc = "";
            if (az.isLoggingIn()) {
                desc = "Signing In...";
                this.withTips("Signing in...");
            }
            this.withDescription(" " + desc);
            this.refreshViewLater();
        }
    }
}
