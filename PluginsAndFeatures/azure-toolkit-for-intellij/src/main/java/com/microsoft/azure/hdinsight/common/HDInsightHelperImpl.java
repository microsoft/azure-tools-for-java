/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.hdinsight.jobs.framework.JobViewEditorProvider;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewHDInsightReaderClusterForm;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ui.WarningMessageForm;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import org.apache.commons.lang3.ObjectUtils;

import javax.swing.Icon;

public class HDInsightHelperImpl implements HDInsightHelper {

    @Override
    public void closeJobViewEditor(@NotNull final Object projectObject, @NotNull final String uuid) {

    }

    @Override
    public String getPluginRootPath() {
        return PluginUtil.getPluginRootDirectory();
    }

    @Override
    public String getInstallationId() {
        if (isOptIn()) {
            return Azure.az().config().getMachineId();
        } else {
            return "";
        }
    }

    @Override
    public boolean isOptIn() {
        return ObjectUtils.firstNonNull(Azure.az().config().getTelemetryEnabled(), true);
    }

    public void openJobViewEditor(final Object projectObject, final String uuid) {
        final IClusterDetail clusterDetail = JobViewManager.getCluster(uuid);

        final Project project = (Project) projectObject;
        final VirtualFile openedFile = getOpenedItem(project);

        // TODO: Fix the issue of clusterDetail may be null
        if (openedFile == null || isNeedReopen(openedFile, clusterDetail)) {
            openItem(project, clusterDetail, uuid, openedFile);
        } else {
            openItem(project, openedFile, null);
        }
    }

    private boolean isNeedReopen(@NotNull final VirtualFile virtualFile,
                                 @NotNull final IClusterDetail myClusterDetail) {
        final IClusterDetail detail = virtualFile.getUserData(JobViewEditorProvider.JOB_VIEW_KEY);
        return detail != null && !detail.getName().equalsIgnoreCase(myClusterDetail.getName());
    }

    private static VirtualFile getOpenedItem(final Project project) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        for (final VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            final IClusterDetail detail = editedFile.getUserData(JobViewEditorProvider.JOB_VIEW_KEY);
            if (detail != null) {
                return editedFile;
            }
        }
        return null;
    }

    private void openItem(@NotNull final Project project,
                          @NotNull final VirtualFile virtualFile,
                          @Nullable final VirtualFile closeableVirtualFile) {
        AzureTaskManager.getInstance().runLater(() -> {
            if (closeableVirtualFile != null) {
                FileEditorManager.getInstance(project).closeFile(closeableVirtualFile);
            }
            FileEditorManager.getInstance(project).openFile(virtualFile, true, false);
        });
    }

    private void openItem(@NotNull final Project project,
                          @NotNull final IClusterDetail myClusterDetail,
                          @NotNull final String uuid,
                          @Nullable final VirtualFile closeableFile) {
        final LightVirtualFile virtualFile = new LightVirtualFile(myClusterDetail.getName() + ": Job View");
        virtualFile.putUserData(JobViewEditorProvider.JOB_VIEW_KEY, myClusterDetail);
        virtualFile.setFileType(new FileType() {
            @NotNull
            @Override
            public String getName() {
                return this.getClass().getName();
            }

            @NotNull
            @Override
            public String getDescription() {
                return "job view dummy file";
            }

            @NotNull
            @Override
            public String getDefaultExtension() {
                return "";
            }

            @Nullable
            @Override
            public Icon getIcon() {
                return PluginUtil.getIcon(CommonConst.SPARK_JOBVIEW_ICONPATH);
            }

            @Override
            public boolean isBinary() {
                return true;
            }

            @Override
            public boolean isReadOnly() {
                return true;
            }

            @Nullable
            @Override
            public String getCharset(@NotNull final VirtualFile virtualFile, @NotNull final byte[] bytes) {
                return "UTF8";
            }
        });
        virtualFile.putUserData(JobViewEditorProvider.JOB_VIEW_UUID, uuid);
        openItem(project, virtualFile, closeableFile);
    }

    @Override
    public boolean isIntelliJPlugin() {
        return true;
    }

    @NotNull
    @Override
    public NodeActionListener createAddNewHDInsightReaderClusterAction(@NotNull final HDInsightRootModule module,
                                                                       @NotNull final ClusterDetail clusterDetail) {
        return new NodeActionListener() {
            @Override
            protected void actionPerformed(final NodeActionEvent nodeActionEvent) {
                final AddNewHDInsightReaderClusterForm linkClusterForm =
                        new AddNewHDInsightReaderClusterForm((Project) module.getProject(), module, clusterDetail);
                linkClusterForm.show();
            }
        };
    }

    @Override
    public void createRefreshHdiReaderJobsWarningForm(@NotNull final HDInsightRootModule module,
                                                      @NotNull final ClusterDetail clusterDetail) {
        AzureTaskManager.getInstance().runLater(new Runnable() {
            @Override
            public void run() {
                final Project project = (Project) module.getProject();
                final String title = "Cluster Job Access Denied";
                final String warningText = "<html><pre>"
                        + "You have Read-only permission for this cluster. Please ask the cluster owner or<br>"
                        + "user access administrator to upgrade your role to HDInsight Cluster Operator in the "
                        + "Azure Portal, or<br>‘Link this cluster’ through Ambari credentials to view the "
                        + "corresponding jobs."
                        + "</pre></html>";
                final String okButtonText = "Link this cluster";
                final WarningMessageForm form = new WarningMessageForm(project, title, warningText, okButtonText) {
                    @Override
                    protected void doOKAction() {
                        super.doOKAction();

                        final AddNewHDInsightReaderClusterForm linkClusterForm =
                                new AddNewHDInsightReaderClusterForm(project, module, clusterDetail);
                        linkClusterForm.show();
                    }
                };
                form.show();
            }
        }, AzureTask.Modality.ANY);
    }

    @Override
    public void createRefreshHdiReaderStorageAccountsWarningForm(@NotNull final RefreshableNode node,
                                                                 @NotNull final String aseDeepLink) {
        AzureTaskManager.getInstance().runLater(new Runnable() {
            @Override
            public void run() {
                final Project project = (Project) node.getProject();
                final String title = "Storage Access Denied";
                final String warningText = "<html><pre>"
                        + "You have Read-only permission for this cluster. Please ask the cluster owner or <br>"
                        + "user access administrator to upgrade your role to HDInsight Cluster Operator in the "
                        + "Azure Portal, or <br>use 'Open Azure Storage Explorer' to access the storages "
                        + "associated with this cluster."
                        + "</pre></html>";
                final String okButtonText = "Open Azure Storage Explorer";
                final WarningMessageForm form = new WarningMessageForm(project, title, warningText, okButtonText) {
                    @Override
                    protected void doOKAction() {
                        super.doOKAction();

                        try {
                            DefaultLoader.getIdeHelper().openLinkInBrowser(aseDeepLink);
                        } catch (final Exception ex) {
                            DefaultLoader.getUIHelper().showError(ex.getMessage(), "HDInsight Explorer");
                        }

                    }
                };
                form.show();
            }
        }, AzureTask.Modality.ANY);
    }

    @Override
    public void createRefreshHdiLinkedClusterStorageAccountsWarningForm(@NotNull final RefreshableNode node,
                                                                        @NotNull final String aseDeepLink) {
        AzureTaskManager.getInstance().runLater(new Runnable() {
            @Override
            public void run() {
                final Project project = (Project) node.getProject();
                final String title = "Storage Access Denied";
                final String warningText =
                        "<html><pre>You are only linked to HDInsight cluster through Ambari credentials.<br>"
                                + "Please use 'Open Azure Storage Explorer' to access the storage associated "
                                + "with this cluster.</pre></html>";
                final String okButtonText = "Open Azure Storage Explorer";
                final WarningMessageForm form = new WarningMessageForm(project, title, warningText, okButtonText) {
                    @Override
                    protected void doOKAction() {
                        super.doOKAction();

                        try {
                            DefaultLoader.getIdeHelper().openLinkInBrowser(aseDeepLink);
                        } catch (final Exception ex) {
                            DefaultLoader.getUIHelper().showError(ex.getMessage(), "HDInsight Explorer");
                        }
                    }
                };
                form.show();
            }
        }, AzureTask.Modality.ANY);
    }
}
