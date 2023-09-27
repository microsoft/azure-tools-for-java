/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.cosmosspark.common.JXHyperLinkWithUri;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterStatesCtrlProvider;
import com.microsoft.azure.cosmosspark.serverexplore.CosmosSparkClusterStatesModel;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterNode;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import rx.Subscription;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.TimeUnit;

public class CosmosSparkClusterMonitorDialog extends DialogWrapper
        implements SettableControl<CosmosSparkClusterStatesModel> {
    private JLabel masterStateLabel;
    private JLabel workerStateLabel;
    private JLabel masterTargetLabel;
    private JLabel masterRunningLabel;
    private JLabel masterFailedLabel;
    private JLabel masterOutstandingLabel;
    private JLabel workerTargetLabel;
    private JLabel workerRunningLabel;
    private JLabel workerFailedLabel;
    private JLabel workerOutStandingLabel;
    private JLabel clusterStateLabel;
    private JXHyperLinkWithUri sparkHistoryHyperLink;
    private JXHyperLinkWithUri sparkMasterHyperLink;
    private JPanel monitorDialogPanel;
    private JTextField clusterIDField;

    @Nullable
    private Subscription refreshSub;
    @NotNull
    private final CosmosSparkClusterStatesCtrlProvider ctrlProvider;

    private static final int REFRESH_INTERVAL = 2;

    public CosmosSparkClusterMonitorDialog(@NotNull final CosmosSparkClusterNode clusterNode,
                                           @NotNull final AzureSparkCosmosCluster cluster) {
        super((Project) clusterNode.getProject(), true);
        this.ctrlProvider = new CosmosSparkClusterStatesCtrlProvider(
                this, new IdeaSchedulers((Project) clusterNode.getProject()), cluster);

        init();
        this.setTitle(String.format("Cluster Status(%s)", cluster.getName()));
        this.setModal(true);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(final WindowEvent e) {
                refreshSub = ctrlProvider.updateAll()
                                         .retryWhen(ob -> ob.delay(REFRESH_INTERVAL, TimeUnit.SECONDS))
                                         .repeatWhen(ob -> ob.delay(REFRESH_INTERVAL, TimeUnit.SECONDS))
                                         .subscribe();
                super.windowOpened(e);
            }

            @Override
            public void windowClosing(final WindowEvent e) {
                if (refreshSub != null) {
                    refreshSub.unsubscribe();
                }
                super.windowClosing(e);
            }
        });
    }

    public CosmosSparkClusterMonitorDialog(@NotNull final Project project,
                                           @NotNull final AzureSparkCosmosCluster cluster) {
        super(project, true);
        this.ctrlProvider = new CosmosSparkClusterStatesCtrlProvider(
                this, new IdeaSchedulers(project), cluster);

        init();
        this.setTitle(String.format("Cluster Status(%s)", cluster.getName()));
        this.setModal(true);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(final WindowEvent e) {
                refreshSub = ctrlProvider.updateAll()
                        .retryWhen(ob -> ob.delay(REFRESH_INTERVAL, TimeUnit.SECONDS))
                        .repeatWhen(ob -> ob.delay(REFRESH_INTERVAL, TimeUnit.SECONDS))
                        .subscribe();
                super.windowOpened(e);
            }

            @Override
            public void windowClosing(final WindowEvent e) {
                if (refreshSub != null) {
                    refreshSub.unsubscribe();
                }
                super.windowClosing(e);
            }
        });
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return clusterIDField;
    }

    // Data -> Components
    @Override
    public void setData(@NotNull final CosmosSparkClusterStatesModel data) {
        AzureTaskManager.getInstance().runAndWait(() -> {
            masterStateLabel.setText(data.getMasterState());
            workerStateLabel.setText(data.getWorkerState());

            masterTargetLabel.setText(String.valueOf(data.getMasterTarget()));
            workerTargetLabel.setText(String.valueOf(data.getWorkerTarget()));

            masterRunningLabel.setText(String.valueOf(data.getMasterRunning()));
            workerRunningLabel.setText(String.valueOf(data.getWorkerRunning()));

            masterFailedLabel.setText(String.valueOf(data.getMasterFailed()));
            workerFailedLabel.setText(String.valueOf(data.getWorkerFailed()));

            masterOutstandingLabel.setText(String.valueOf(data.getMasterOutstanding()));
            workerOutStandingLabel.setText(String.valueOf(data.getWorkerOutstanding()));

            sparkHistoryHyperLink.setURI(data.getSparkHistoryUri());
            sparkMasterHyperLink.setURI(data.getSparkMasterUri());

            clusterStateLabel.setText(data.getClusterState());
            clusterIDField.setText(data.getClusterID());
            clusterIDField.setBorder(null);
        }, AzureTask.Modality.ANY);
    }

    // Components -> Data
    @Override
    public void getData(@NotNull final CosmosSparkClusterStatesModel data) {
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[0];
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return monitorDialogPanel;
    }

}
