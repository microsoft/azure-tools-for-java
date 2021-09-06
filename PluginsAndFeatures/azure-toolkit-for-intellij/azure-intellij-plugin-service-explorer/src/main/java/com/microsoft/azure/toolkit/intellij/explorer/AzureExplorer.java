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
import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.common.component.Tree;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.view.IView;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AzureExplorer extends Tree {
    private static final ExtensionPointName<IExplorerContributor> explorerExtensionPoint =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.azureExplorerContributor");
    public static final String ICON = "/icons/Common/Azure.svg";

    private AzureExplorer() {
        super();
        this.root = buildRoot();
        this.init(this.root);
    }

    private Node<Azure> buildRoot() {
        final List<Node<?>> modules = getModules();
        return new Node<>(Azure.az(), new IView.Label.Static(getTitle(), ICON)).lazy(false).addChildren(modules);
    }

    private String getTitle() {
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
        return explorerExtensionPoint.getExtensionList().stream()
                .map(IExplorerContributor::getModuleNode)
                .sorted(Comparator.comparing(Node::order))
                .collect(Collectors.toList());
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
}

