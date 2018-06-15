package com.microsoft.azure.sparkserverless.serverexplore.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azure.sparkserverless.common.JXHyperLinkWithUri;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterStatesCtrlProvider;
import com.microsoft.azure.sparkserverless.serverexplore.SparkServerlessClusterStatesModel;
import com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode.SparkServerlessClusterNode;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import rx.Subscription;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.TimeUnit;

public class SparkServerlessClusterMonitorDialog extends DialogWrapper
        implements SettableControl<SparkServerlessClusterStatesModel> {
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

    @Nullable
    private Subscription refreshSub;
    @NotNull
    private SparkServerlessClusterStatesCtrlProvider ctrlProvider;
    @NotNull
    private AzureSparkServerlessCluster cluster;

    private static final int REFRESH_INTERVAL = 1;

    public SparkServerlessClusterMonitorDialog(@NotNull SparkServerlessClusterNode clusterNode,
                                               @NotNull AzureSparkServerlessCluster cluster) {
        super((Project) clusterNode.getProject(), true);
        this.ctrlProvider = new SparkServerlessClusterStatesCtrlProvider(
                this, new IdeaSchedulers((Project) clusterNode.getProject()), cluster);
        this.cluster = cluster;

        init();
        this.setTitle("Cluster Status");
        this.setModal(true);
        this.getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                refreshSub = ctrlProvider.updateAll()
                        .repeatWhen(ob -> ob.delay(REFRESH_INTERVAL, TimeUnit.SECONDS))
                        .subscribe();
                super.windowOpened(e);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                refreshSub.unsubscribe();
                super.windowClosing(e);
            }
        });
    }

    // Data -> Components
    @Override
    public void setData(SparkServerlessClusterStatesModel data) {
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
    }

    // Components -> Data
    @Override
    public void getData(@NotNull SparkServerlessClusterStatesModel data) {
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
