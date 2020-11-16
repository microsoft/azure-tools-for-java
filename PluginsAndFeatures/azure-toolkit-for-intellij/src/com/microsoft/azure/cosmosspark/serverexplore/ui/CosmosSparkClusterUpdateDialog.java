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
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterUpdateCtrlProvider;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkADLAccountNode;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterNode;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.rxjava.IdeaSchedulers;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CosmosSparkClusterUpdateDialog extends CosmosSparkProvisionDialog implements ILogger {
    @NotNull
    private CosmosSparkClusterUpdateCtrlProvider ctrlProvider;
    private static final String AU_WARNING_TIP = "Currently, there are not enough available AU for your serverless cluster Updating. Please adjust your cluster configuration or the updating request will be submitted into the queue with PENDING status.";

    public CosmosSparkClusterUpdateDialog(@NotNull CosmosSparkClusterNode clusterNode,
                                          @NotNull AzureSparkCosmosCluster cluster) {
        super((CosmosSparkADLAccountNode) clusterNode.getParent(), cluster.getAccount());
        this.setTitle("Update Cluster");
        this.auWarningLabel.setToolTipText(AU_WARNING_TIP);
        disableUneditableFields();

        // Hide extended properties field
        extendedPropertiesLabel.setVisible(false);
        extendedPropertiesField.setVisible(false);

        ctrlProvider = new CosmosSparkClusterUpdateCtrlProvider(
                this, new IdeaSchedulers((Project)clusterNode.getProject()), cluster);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                ctrlProvider.initialize()
                        .subscribe(complete -> {}, err -> log().warn("Error initialize update dialog. " + err.toString()));
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
        ctrlProvider
                .validateAndUpdate()
                .doOnEach(notification -> getOKAction().setEnabled(true))
                .subscribe(
                        toUpdate -> AzureTaskRunner.getInstance().runAndWait(()->close(OK_EXIT_CODE)),
                        err -> log().warn("Error update a cluster. " + err.toString())
                );
    }
}
