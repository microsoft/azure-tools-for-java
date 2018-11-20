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

package com.microsoft.azure.sparkserverless.serverexplore.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.cluster.DestroyableCluster;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterDestoryCtrlProvider;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterDestoryModel;
import com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode.SparkServerlessClusterNode;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SparkServerlessClusterDestoryDialog extends DialogWrapper
        implements SettableControl<SparkServerlessClusterDestoryModel> {

    @NotNull
    private SparkServerlessClusterDestoryCtrlProvider ctrlProvider;

    @NotNull
    private SparkServerlessClusterNode clusterNode;

    @NotNull
    private DestroyableCluster cluster;

    private JPanel destroyDialogPanel;
    private JTextField clusterNameField;
    private JTextField errorMessageField;
    private JLabel confimMessageLabel;

    public SparkServerlessClusterDestoryDialog(@NotNull SparkServerlessClusterNode clusterNode,
                                               @NotNull DestroyableCluster cluster) {
        super((Project) clusterNode.getProject(), true);
        this.ctrlProvider = new SparkServerlessClusterDestoryCtrlProvider(
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
    public void getData(@NotNull SparkServerlessClusterDestoryModel data) {
        data.setClusterName(clusterNameField.getText())
                .setErrorMessage(errorMessageField.getText());
    }

    // Data -> Components
    public void setData(@NotNull SparkServerlessClusterDestoryModel data) {
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
