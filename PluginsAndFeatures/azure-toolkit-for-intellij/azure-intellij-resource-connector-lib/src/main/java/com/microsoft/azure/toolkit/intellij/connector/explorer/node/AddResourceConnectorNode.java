package com.microsoft.azure.toolkit.intellij.connector.explorer.node;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

public class AddResourceConnectorNode extends AbstractTreeNode<AzureModule>
        implements IAzureProjectExplorerNode {

    protected AddResourceConnectorNode(AzureModule value) {
        super(value.getProject(), value);
    }

    @Override
    public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText("Click to Connect to Azure Resources");
        presentation.setTooltip("Connect your project to Azure");
        presentation.setForcedTextForeground(UIManager.getColor("Hyperlink.linkColor"));
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, "ACTION_SOURCE") ? this.getValue() : null;
    }

    @javax.annotation.Nullable
    @Override
    public IActionGroup getActionGroup() {
        return null;
    }

    @Override
    public void triggerClickAction(Object event) {
        final Action<AzureModule> action = IntellijAzureActionManager.getInstance().getAction(ResourceConnectionActionsContributor.CONNECT_TO_MODULE);
        action.handle(getValue(), event);
    }

}
