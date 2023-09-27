/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterDestoryCtrlProvider;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterDestoryModel;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterNode;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterModule;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CosmosSparkClusterDestoryDialog extends DialogWrapper
        implements SettableControl<CosmosSparkClusterDestoryModel> {

    @NotNull
    private CosmosSparkClusterDestoryCtrlProvider ctrlProvider;

    @NotNull
    private Object clusterNode;

    @NotNull
    private AzureSparkCosmosCluster cluster;

    private JPanel destroyDialogPanel;
    private JTextField clusterNameField;
    private JTextField errorMessageField;
    private JLabel confimMessageLabel;

    public CosmosSparkClusterDestoryDialog(@NotNull CosmosSparkClusterNode clusterNode,
                                           @NotNull AzureSparkCosmosCluster cluster) {
        super((Project) clusterNode.getProject(), true);
        this.ctrlProvider = new CosmosSparkClusterDestoryCtrlProvider(
                this, new IdeaSchedulers((Project) clusterNode.getProject()), cluster);
        this.clusterNode = clusterNode;
        this.cluster = cluster;

        init();
        this.setTitle("Delete Spark Cluster");
        confimMessageLabel.setText(String.format("%s %s?", confimMessageLabel.getText(), clusterNode.getClusterName()));
        errorMessageField.setBackground(this.destroyDialogPanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());
        this.setModal(true);
    }

    public CosmosSparkClusterDestoryDialog(@NotNull SparkOnCosmosClusterNode clusterNode,
                                           Project project,
                                           @NotNull AzureSparkCosmosCluster cluster) {
        super(project, true);
        this.ctrlProvider = new CosmosSparkClusterDestoryCtrlProvider(
                this, new IdeaSchedulers(project), cluster);
        this.clusterNode = clusterNode;
        this.cluster = cluster;

        init();
        this.setTitle("Delete Spark Cluster");
        confimMessageLabel.setText(String.format("%s %s?", confimMessageLabel.getText(), cluster.getName()));
        errorMessageField.setBackground(this.destroyDialogPanel.getBackground());
        errorMessageField.setBorder(BorderFactory.createEmptyBorder());
        this.setModal(true);
    }

    // Components -> Data
    public void getData(@NotNull CosmosSparkClusterDestoryModel data) {
        data.setClusterName(clusterNameField.getText())
                .setErrorMessage(errorMessageField.getText());
    }

    // Data -> Components
    public void setData(@NotNull CosmosSparkClusterDestoryModel data) {
        clusterNameField.setText(data.getClusterName());
        errorMessageField.setText(data.getErrorMessage());
    }

    @Override
    protected void doOKAction() {
        if (!getOKAction().isEnabled()) {
            return;
        }

        getOKAction().setEnabled(false);

        ctrlProvider
                .validateAndDestroy(cluster.getName())
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(toUpdate -> {
                    if (this.clusterNode instanceof SparkOnCosmosClusterNode) {
                        SparkOnCosmosClusterNode cn = (SparkOnCosmosClusterNode) clusterNode;
                        SparkOnCosmosClusterModule module = (SparkOnCosmosClusterModule) cn.getModule();
                        module.getAdlAccountNode().refresh();
                    } else {
                        CosmosSparkClusterNode cn = (CosmosSparkClusterNode)clusterNode;
                        cn.getParent().removeDirectChildNode(cn);
                    }
                    super.doOKAction();
                });
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return destroyDialogPanel;
    }

}
