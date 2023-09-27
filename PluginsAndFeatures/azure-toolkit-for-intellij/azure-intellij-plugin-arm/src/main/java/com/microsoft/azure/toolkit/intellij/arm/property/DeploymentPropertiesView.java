/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.property;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.azure.toolkit.ide.arm.DeploymentActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class DeploymentPropertiesView extends AzResourcePropertiesEditor<ResourceDeployment> {
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss a");

    private final ResourceDeployment deployment;

    private JPanel contentPane;
    private JPanel pnlOverviewHolder;
    private JPanel pnlOverview;
    private JLabel deploymentNameLabel;
    private JLabel lastModifiedLabel;
    private JLabel statusLabel;
    private JLabel deploymentModeLabel;
    private Tree templateTree;
    private JLabel statusReasonLabel;
    private AzureActionButton<ResourceDeployment> viewResourceTemplateButton;
    private AzureActionButton<ResourceDeployment> exportTemplateFileButton;
    private AzureActionButton<ResourceDeployment> exportParameterFileButton;
    private static final String PNL_OVERVIEW = "Overview";

    public DeploymentPropertiesView(@Nonnull Project project, @Nonnull ResourceDeployment deployment, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile, deployment, project);
        this.deployment = deployment;
        final HideableDecorator overviewDecorator = new HideableDecorator(pnlOverviewHolder, PNL_OVERVIEW, false);
        overviewDecorator.setContentComponent(pnlOverview);
        overviewDecorator.setOn(true);
        pnlOverview.setName(PNL_OVERVIEW);
        pnlOverview.setBorder(BorderFactory.createCompoundBorder());
        exportTemplateFileButton.setAction(DeploymentActionsContributor.EXPORT_TEMPLATE, this.deployment);
        exportParameterFileButton.setAction(DeploymentActionsContributor.EXPORT_PARAMETER, this.deployment);
        this.rerender();
    }

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> this.setData(this.deployment));
    }

    private void setData(ResourceDeployment deployment) {
        deploymentNameLabel.setText(deployment.getName());
        lastModifiedLabel.setText(Optional.ofNullable(deployment.getTimestamp()).map(t -> t.format(DATETIME_FORMATTER)).orElse(""));
        statusLabel.setText(deployment.getStatus());
        deploymentModeLabel.setText(deployment.getMode());
        final StringBuilder statusReason = new StringBuilder();
        deployment.getOperations().forEach(o -> {
            if (o.statusMessage() != null && o.statusMessage() instanceof Map) {
                final Map<String, String> msg = (Map<String, String>) o.statusMessage();
                statusReason.append(msg.containsKey("Message") ? msg.get("Message") : msg.toString());
            }
        });
        statusReasonLabel.setText(statusReason.toString());
        viewResourceTemplateButton.setAction(DeploymentActionsContributor.EDIT, this.deployment);

        final DefaultMutableTreeNode nodeRoot = new DefaultMutableTreeNode("Template");
        final TreeModel model = new DefaultTreeModel(nodeRoot);
        templateTree.setModel(model);
        templateTree.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        final DefaultMutableTreeNode nodeParameters = new DefaultMutableTreeNode("parameters");
        final DefaultMutableTreeNode nodeVariables = new DefaultMutableTreeNode("variables");
        final DefaultMutableTreeNode nodeResources = new DefaultMutableTreeNode("resources");
        nodeRoot.add(nodeParameters);
        nodeRoot.add(nodeVariables);
        nodeRoot.add(nodeResources);
        deployment.getParameters().stream().map(DefaultMutableTreeNode::new).forEach(nodeParameters::add);
        deployment.getVariables().stream().map(DefaultMutableTreeNode::new).forEach(nodeVariables::add);
        deployment.getResources().stream().map(DefaultMutableTreeNode::new).forEach(nodeResources::add);
        expandTree(templateTree, 0, templateTree.getRowCount());
    }

    private void expandTree(JTree tree, int startingIndex, int rowCount) {
        IntStream.range(0, rowCount).forEach(tree::expandRow);
        if (tree.getRowCount() != rowCount) {
            expandTree(tree, rowCount, tree.getRowCount());
        }
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return contentPane;
    }
}
