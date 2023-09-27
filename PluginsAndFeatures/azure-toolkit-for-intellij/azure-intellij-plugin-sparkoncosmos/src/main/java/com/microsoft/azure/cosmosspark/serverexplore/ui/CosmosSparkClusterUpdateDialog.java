/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterProvisionSettingsModel;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterUpdateCtrlProvider;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkADLAccountNode;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterNode;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import rx.functions.Action1;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CosmosSparkClusterUpdateDialog extends CosmosSparkProvisionDialog implements ILogger {

    @NotNull
    private CosmosSparkClusterUpdateCtrlProvider ctrlProvider;
    private static final String AU_WARNING_TIP = "Currently, there are not enough available AU for your serverless cluster Updating. "
        + "Please adjust your cluster configuration or the updating request will be submitted into the queue with PENDING status.";

    public CosmosSparkClusterUpdateDialog(@NotNull CosmosSparkClusterNode clusterNode,
                                          @NotNull AzureSparkCosmosCluster cluster) {
        super((CosmosSparkADLAccountNode) clusterNode.getParent(), cluster.getAccount());
        this.setTitle("Update Cluster");
        this.auWarningLabel.setToolTipText(AU_WARNING_TIP);
        disableUneditableFields();

        // Hide extended properties field
        extendedPropertiesLabel.setVisible(false);
        extendedPropertiesField.setVisible(false);
        ctrlProvider = new CosmosSparkClusterUpdateCtrlProvider(this, new IdeaSchedulers((Project) clusterNode.getProject()), cluster);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                final Action1<CosmosSparkClusterProvisionSettingsModel> onNext = complete -> {
                };
                final Action1<Throwable> onError = err -> log().warn("Error initialize update dialog. " + err.toString());
                ctrlProvider.initialize().subscribe(onNext, onError);
                super.windowOpened(e);
            }
        });
    }

    public CosmosSparkClusterUpdateDialog(SparkOnCosmosClusterNode clusterNode,
                                          Project project,
                                          AzureSparkCosmosCluster cluster) {
        super(project,clusterNode.getParent(), cluster.getAccount());
        this.setTitle("Update Cluster");
        this.auWarningLabel.setToolTipText(AU_WARNING_TIP);
        disableUneditableFields();

        // Hide extended properties field
        extendedPropertiesLabel.setVisible(false);
        extendedPropertiesField.setVisible(false);
        ctrlProvider = new CosmosSparkClusterUpdateCtrlProvider(this, new IdeaSchedulers(project), cluster);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                final Action1<CosmosSparkClusterProvisionSettingsModel> onNext = complete -> {
                };
                final Action1<Throwable> onError = err -> log().warn("Error initialize update dialog. " + err.toString());
                ctrlProvider.initialize().subscribe(onNext, onError);
                super.windowOpened(e);
            }
        });
    }

    private void disableUneditableFields() {
        clusterNameField.setEditable(false);
        sparkEventsField.setEditable(false);
        masterCoresField.setEditable(false);
        masterMemoryField.setEditable(false);
        workerCoresField.setEditable(false);
        workerMemoryField.setEditable(false);
        sparkVersionComboBox.setEditable(false);
    }

    @Override
    protected void enableClusterNameUniquenessCheck() {
        // To avoid cluster already exists tooltips
        clusterNameField.setNotAllowedValues(null);

        sparkEventsField.setPatternAndErrorMessage(null);
        // The text setting is necessary. By default, '/' is not allowed for TextWithErrorHintedField, leading to
        // error tooltip. We have to set the text to trigger the validator of the new pattern.
        sparkEventsField.setText("spark-events/");
    }

    @Override
    protected void doOKAction() {
        if (!getOKAction().isEnabled()) {
            return;
        }

        getOKAction().setEnabled(false);
        final Action1<CosmosSparkClusterProvisionSettingsModel> onNext =
            toUpdate -> AzureTaskManager.getInstance().runAndWait(() -> close(OK_EXIT_CODE), AzureTask.Modality.ANY);
        final Action1<Throwable> onError = err -> log().warn("Error update a cluster. " + err.toString());
        ctrlProvider
                .validateAndUpdate()
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(onNext, onError);
    }
}
