/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.explorer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.hover.TreeHoverListener;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.ide.common.genericresource.GenericResourceActionsContributor;
import com.microsoft.azure.toolkit.ide.common.genericresource.GenericResourceNode;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.Tree;
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.tree.DefaultTreeModel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS;
import static com.microsoft.azure.toolkit.intellij.common.component.TreeUtils.KEY_SCROLL_PANE;
import static com.microsoft.azure.toolkit.lib.common.action.Action.PLACE;

public class AzureExplorer extends Tree {
    public static final String TOOLWINDOW_ID = "Azure Explorer";
    @Getter
    public static final AzureExplorerNodeProviderManager manager = new AzureExplorerNodeProviderManager();
    public static final String AZURE_ICON = AzureIcons.Common.AZURE.getIconPath();

    private AzureExplorer() {
        super();
        this.putClientProperty(PLACE, ResourceCommonActionsContributor.AZURE_EXPLORER);
        this.root = new Node<>("Azure")
            .withChildrenLoadLazily(false)
            .addChild(buildFavoriteRoot())
            .addChild(buildAppGroupedResourcesRoot())
            .addChild(buildTypeGroupedResourcesRoot())
            .addChildren(buildNonAzServiceNodes());
        this.init(this.root);
        this.setRootVisible(false);
        //noinspection UnstableApiUsage
        TreeHoverListener.DEFAULT.addTo(this);
        this.setCellRenderer(new InlineActionSupportedNodeRenderer());
        AzureEventBus.on("azure.explorer.highlight_resource", new AzureEventBus.EventListener(e -> TreeUtils.highlightResource(this, e.getSource())));
        AzureEventBus.on("resource.creation_started.resource", new AzureEventBus.EventListener(e -> {
            if (e.getSource() instanceof AbstractAzResource<?, ?, ?>) {
                TreeUtils.focusResource(this, (AbstractAzResource<?, ?, ?>) e.getSource());
            }
        }));
        AzureEventBus.on("azure.explorer.focus_resource", new AzureEventBus.EventListener(e -> {
            if (e.getSource() instanceof AbstractAzResource<?, ?, ?>) {
                TreeUtils.focusResource(this, (AbstractAzResource<?, ?, ?>) e.getSource());
            }
        }));
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener(e -> {
            final DefaultTreeModel model = (DefaultTreeModel) this.getModel();
            final TreeNode<?> root = (TreeNode<?>) model.getRoot();
            final TreeNode<?> appGroupedResourcesRoot = (TreeNode<?>) root.getChildAt(1);
            final TreeNode<?> typeGroupedResourcesRoot = (TreeNode<?>) root.getChildAt(2);
            appGroupedResourcesRoot.clearChildren();
            typeGroupedResourcesRoot.clearChildren();
        }));
    }

    private Node<Azure> buildTypeGroupedResourcesRoot() {
        return new TypeGroupedServicesRootNode().addChildren((a) -> buildAzServiceNodes());
    }

    public Node<?> buildAppGroupedResourcesRoot() {
        final AzureResources resources = Azure.az(AzureResources.class);
        return manager.createNode(resources, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
    }

    public Node<?> buildFavoriteRoot() {
        return Favorites.buildFavoriteRoot(manager);
    }

    @Nonnull
    public List<Node<?>> buildAzServiceNodes() {
        return manager.getAzServices().stream()
            .map(r -> manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
            .sorted(Comparator.comparing(Node::getLabel))
            .collect(Collectors.toList());
    }

    @Nonnull
    public List<Node<?>> buildNonAzServiceNodes() {
        return manager.getNonAzServices().stream()
            .map(r -> manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
            .sorted(Comparator.comparing(Node::getLabel))
            .collect(Collectors.toList());
    }

    public void refreshAll() {
        manager.getAzServices().stream().filter(r -> r instanceof AbstractAzResourceModule)
            .forEach(r -> ((AbstractAzResourceModule<?, ?, ?>) r).refresh());
        Favorites.getInstance().refresh();
    }

    public static class ToolWindowFactory implements com.intellij.openapi.wm.ToolWindowFactory, DumbAware {
        public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
            final SimpleToolWindowPanel windowPanel = new SimpleToolWindowPanel(true, true);
            final AzureExplorer explorer = new AzureExplorer();
            final JBScrollPane scrollPane = new JBScrollPane(explorer);
            explorer.putClientProperty(KEY_SCROLL_PANE, scrollPane);
            windowPanel.setContent(scrollPane);
            this.addToolbarActions(toolWindow, project, explorer);
            final ContentFactory contentFactory = ContentFactory.getInstance();
            final Content content = contentFactory.createContent(windowPanel, null, false);
            toolWindow.getContentManager().addContent(content);
        }

        private void addToolbarActions(ToolWindow toolWindow, final Project project, AzureExplorer explorer) {
            final AnAction refreshAction = new AnAction("Refresh All", "Refresh Azure nodes list", AllIcons.Actions.Refresh) {
                @Override
                public void actionPerformed(@NotNull final AnActionEvent e) {
                    explorer.refreshAll();
                }

                @Override
                public void update(@NotNull final AnActionEvent e) {
                    e.getPresentation().setEnabled(Azure.az(AzureAccount.class).isLoggedIn());
                }

                @Override
                public ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.BGT;
                }
            };
            final AnAction feedbackAction = ActionManager.getInstance().getAction("Actions.ProvideFeedback");
            final AnAction getStartAction = ActionManager.getInstance().getAction("Actions.GettingStart");
            final AnAction signInAction = ActionManager.getInstance().getAction("AzureToolkit.AzureSignIn");
            final AnAction selectSubscriptionsAction = ActionManager.getInstance().getAction("AzureToolkit.SelectSubscriptions");
            toolWindow.setTitleActions(Arrays.asList(getStartAction, refreshAction, selectSubscriptionsAction, signInAction, Separator.create(), feedbackAction));
            if (toolWindow instanceof ToolWindowEx) {
                final AnAction devBlogsAction = ActionManager.getInstance().getAction("AzureToolkit.ViewDevBlogs");
                final AnAction documentAction = ActionManager.getInstance().getAction("AzureToolkit.ViewToolingDocument");
                final AnAction whatsNewAction = ActionManager.getInstance().getAction("Actions.WhatsNew");
                final AnAction reportIssueAction = ActionManager.getInstance().getAction("AzureToolkit.GithubIssue");
                final AnAction featureRequestAction = ActionManager.getInstance().getAction("AzureToolkit.FeatureRequest");
                final AnAction openSdkReferenceBookAction = ActionManager.getInstance().getAction("user/sdk.OpenSdkReferenceBook");
                final AnAction openResourceConnectionExplorerAction = ActionManager.getInstance().getAction("AzureToolkit.OpenResourceConnectionExplorerAction");
                final AnAction openAzureSettingsAction = ActionManager.getInstance().getAction(OPEN_AZURE_SETTINGS.getId());
                (toolWindow).setAdditionalGearActions(new DefaultActionGroup(openSdkReferenceBookAction, openAzureSettingsAction, openResourceConnectionExplorerAction,
                    Separator.create(), reportIssueAction, featureRequestAction, feedbackAction, Separator.create(), devBlogsAction, documentAction));
            }
        }
    }

    public static class AzureExplorerNodeProviderManager implements IExplorerNodeProvider.Manager {
        private static final ExtensionPointName<IExplorerNodeProvider> providers =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.explorerNodeProvider");

        @Nonnull
        public List<Object> getAzServices() {
            return providers.getExtensionList().stream()
                .filter(IExplorerNodeProvider::isAzureService)
                .map(IExplorerNodeProvider::getRoot)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        @Nonnull
        public List<Object> getNonAzServices() {
            return providers.getExtensionList().stream()
                .filter(p -> !p.isAzureService())
                .map(IExplorerNodeProvider::getRoot)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        @Nonnull
        @Override
        public Node<?> createNode(@Nonnull Object o, Node<?> parent, IExplorerNodeProvider.ViewType type) {
            return providers.getExtensionList().stream()
                .filter(p -> p.accept(o, parent, type)).findAny()
                .map(p -> p.createNode(o, parent, this))
                .or(() -> Optional.of(o).filter(r -> r instanceof AbstractAzResource).map(AzureExplorerNodeProviderManager::createGenericNode))
                .orElseThrow(() -> new AzureToolkitRuntimeException(String.format("failed to render %s", o.toString())));
        }

        private static <U> U createGenericNode(Object o) {
            //noinspection unchecked
            return (U) new GenericResourceNode((AbstractAzResource<?, ?, ?>) o)
                .onDoubleClicked(ResourceCommonActionsContributor.OPEN_PORTAL_URL)
                .withActions(GenericResourceActionsContributor.GENERIC_RESOURCE_ACTIONS);
        }
    }
}

