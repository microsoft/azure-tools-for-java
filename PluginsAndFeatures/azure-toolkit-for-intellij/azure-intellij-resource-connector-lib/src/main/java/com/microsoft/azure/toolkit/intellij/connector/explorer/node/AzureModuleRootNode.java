package com.microsoft.azure.toolkit.intellij.connector.explorer.node;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;

public class AzureModuleRootNode extends AbstractTreeNode<AzureModule> implements IAzureProjectExplorerNode {
    private final AzureEventBus.EventListener listener;
    private final MessageBusConnection connection;
    private final ViewSettings viewSettings;

    public AzureModuleRootNode(final AzureModule module, ViewSettings settings) {
        super(module.getProject(), module);
        this.viewSettings = settings;
        this.listener = new AzureEventBus.EventListener(this::onEvent);
        this.connection = module.getProject().getMessageBus().connect();
        this.connection.subscribe(CONNECTION_CHANGED, (ConnectionTopics.ConnectionChanged) (p, conn, action) -> {
            if (conn.getConsumer().getId().equalsIgnoreCase(module.getName())) {
                refresh();
            }
        });
        AzureEventBus.on("connector.refreshed.module_root", listener);
    }

    private void onEvent(AzureEvent azureEvent) {
        final Object payload = azureEvent.getSource();
        if (payload instanceof AzureModule && Objects.equals(payload, getValue())) {
            refresh();
        }
    }

    private void refresh() {
        final AbstractProjectViewPane currentProjectViewPane = ProjectView.getInstance(getProject()).getCurrentProjectViewPane();
//            currentProjectViewPane.getTreeBuilder().updateFromRoot();
        currentProjectViewPane.updateFromRoot(true);
    }

    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final ArrayList<AbstractTreeNode<?>> result = new ArrayList<>();
        final AzureModule module = this.getValue();
        final List<Connection<?, ?>> connections = Optional.ofNullable(module.getDefaultProfile()).map(Profile::getConnections).orElse(Collections.emptyList());
        if (CollectionUtils.isNotEmpty(connections)) { // add back .azure file node
            final VirtualFile virtualFile = Optional.ofNullable(getValue()).map(AzureModule::getDotAzureDir).flatMap(op -> op).orElse(null);
            Optional.ofNullable(virtualFile)
                    .map(dir -> PsiManagerEx.getInstanceEx(getProject()).findDirectory(dir))
                    .map(dir -> new PsiDirectoryNode(getProject(), dir, viewSettings)).ifPresent(result::add);
        }
        result.add(CollectionUtils.isEmpty(connections) ? new AddResourceConnectorNode(module) : new LocalConnectionsNode(module));
        return result;
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final AzureModule value = getValue();
        final boolean connected = CollectionUtils.isNotEmpty(Optional.ofNullable(value.getDefaultProfile()).map(Profile::getConnections).orElse(Collections.emptyList()));
        presentation.addText("Azure" + StringUtils.SPACE, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.addText(connected ? "Connected Azure resources" : "No Azure resources connected yet", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
        presentation.setTooltip("Manage connected Azure resources here.");
        presentation.setIcon(connected ? IntelliJAzureIcons.getIcon("/icons/Common/AzureResourceConnector.svg") : IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, "ACTION_SOURCE") ? this.getValue() : null;
    }

    @javax.annotation.Nullable
    @Override
    public IActionGroup getActionGroup() {
        return AzureActionManager.getInstance().getGroup(ResourceConnectionActionsContributor.EXPLORER_MODULE_ROOT_ACTIONS);
    }
}
