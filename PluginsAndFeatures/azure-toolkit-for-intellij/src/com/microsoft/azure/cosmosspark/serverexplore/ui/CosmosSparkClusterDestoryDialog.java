/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.cosmosspark.serverexplore.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterDestoryCtrlProvider;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterDestoryModel;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterNode;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.cluster.DestroyableCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CosmosSparkClusterDestoryDialog extends DialogWrapper
        implements SettableControl<CosmosSparkClusterDestoryModel> {

    @NotNull
    private CosmosSparkClusterDestoryCtrlProvider ctrlProvider;

    @NotNull
    private CosmosSparkClusterNode clusterNode;

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
                .validateAndDestroy(clusterNode.getClusterName())
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(toUpdate -> {
                    clusterNode.getParent().removeDirectChildNode(clusterNode);
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
