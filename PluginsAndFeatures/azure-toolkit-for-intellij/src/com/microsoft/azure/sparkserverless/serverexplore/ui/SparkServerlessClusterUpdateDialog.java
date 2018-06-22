package com.microsoft.azure.sparkserverless.serverexplore.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterUpdateCtrlProvider;
import com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode.SparkServerlessADLAccountNode;
import com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode.SparkServerlessClusterNode;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.rxjava.IdeaSchedulers;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

public class SparkServerlessClusterUpdateDialog extends SparkServerlessProvisionDialog {

    private void setFieldsUneditable() {
        Arrays.asList(
                clusterNameField, sparkEventsField, masterCoresField, masterMemoryField, workerCoresField, workerMemoryField)
                .forEach(field -> field.setEditable(false));
        sparkVersionComboBox.setEditable(false);
    }

    @NotNull
    private SparkServerlessClusterUpdateCtrlProvider ctrlProvider;
    public SparkServerlessClusterUpdateDialog(@NotNull SparkServerlessClusterNode clusterNode,
                                              @NotNull AzureSparkServerlessCluster cluster) {

        super((SparkServerlessADLAccountNode) clusterNode.getParent(), cluster.getAccount());
        this.setTitle("Update Cluster");
        setFieldsUneditable();
        getOKAction().setEnabled(false);
        ctrlProvider = new SparkServerlessClusterUpdateCtrlProvider(
                this, new IdeaSchedulers((Project)clusterNode.getProject()), cluster);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                ctrlProvider.initialize().subscribe(data -> getOKAction().setEnabled(true));
                super.windowOpened(e);
            }
        });
    }

    @Override
    protected void doOKAction() {
        if (!getOKAction().isEnabled()) {
            return;
        }

        getOKAction().setEnabled(false);
        ctrlProvider
                .validateAndUpdate()
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(toUpdate -> {
                    if (getOKAction().isEnabled()) {
                        close(OK_EXIT_CODE);
                    }
                });
    }
}
