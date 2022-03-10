/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.explorer;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzureModuleLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.ide.common.favorite.FavoriteNodeView;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.intellij.common.component.Tree;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureExplorer extends Tree {
    private static final AzureExplorerNodeProviderManager manager = new AzureExplorerNodeProviderManager();
    public static final String AZURE_ICON = "/icons/Common/Azure.svg";
    public static final String FAVORITE_ICON = "/icons/Common/favorite.svg";

    private AzureExplorer() {
        super();
        this.root = buildAzureRoot();
        this.init(this.root);
    }

    private static Node<Azure> buildAzureRoot() {
        final List<Node<?>> modules = getModules();
        return new Node<>(Azure.az(), new NodeView.Static(getTitle(), AZURE_ICON)).lazy(false).addChildren(modules);
    }

    public static Node<Favorites> buildFavoriteRoot() {
        final AzureModuleLabelView<Favorites> view = new AzureModuleLabelView<>(Favorites.getInstance(), "Favorites", FAVORITE_ICON);
        AzureEventBus.on("account.subscription_changed.account", (e) -> {
            view.refreshChildren();
        });
        return new Node<>(Favorites.getInstance(), view).lazy(false)
            .addChildren(Favorites::list, (o, parent) -> {
                final Node<?> node = manager.createNode(o.getResource(), parent);
                if (Objects.nonNull(node) && node.view() instanceof AzureResourceLabelView) {
                    node.view(new FavoriteNodeView((AzureResourceLabelView<?>) node.view()));
                }
                return node;
            });
    }

    private static String getTitle() {
        try {
            final AzureAccount az = Azure.az(AzureAccount.class);
            final Account account = az.account();
            final List<Subscription> subscriptions = account.getSelectedSubscriptions();
            if (subscriptions.size() == 1) {
                return String.format("Azure(%s)", subscriptions.get(0).getName());
            }
        } catch (final Exception ignored) {
        }
        return "Azure";
    }

    @Nonnull
    public static List<Node<?>> getModules() {
        return manager.getRootNodes();
    }

    public static class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory {
        public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
            final SimpleToolWindowPanel windowPanel = new SimpleToolWindowPanel(true, true);
            windowPanel.setContent(new AzureExplorer());
            final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            final Content content = contentFactory.createContent(windowPanel, null, false);
            toolWindow.getContentManager().addContent(content);
        }
    }

    private static class AzureExplorerNodeProviderManager implements IExplorerNodeProvider.Manager {
        private static final ExtensionPointName<IExplorerNodeProvider> providers =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.explorerNodeProvider");

        @Nonnull
        public List<Node<?>> getRootNodes() {
            return providers.getExtensionList().stream()
                .map(p -> p.getModuleNode(null, this))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Node::order))
                .collect(Collectors.toList());
        }

        @Nullable
        @Override
        public Node<?> createNode(@Nonnull Object o, Node<?> parent) {
            return providers.getExtensionList().stream()
                .filter(p -> p.accept(o, parent))
                .findAny().map(p -> p.createNode(o, parent, this))
                .orElse(null);
        }
    }
}

