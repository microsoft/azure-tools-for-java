/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.hdinsight.jobs.framework.JobViewEditorProvider;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewHDInsightReaderClusterAction;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewHDInsightReaderClusterForm;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.ijidea.ui.WarningMessageForm;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import javax.swing.*;

public class HDInsightHelperImpl implements HDInsightHelper {

    private static String instID = "";
    private static boolean isOptIn = true;
    static {
        String dataFile = PluginHelper.getTemplateFile(AzureBundle.message("dataFileName"));
        instID = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
        isOptIn = Boolean.parseBoolean(DataOperations.getProperty(dataFile, AzureBundle.message("prefVal")));
    }

    @Override
    public void closeJobViewEditor(@NotNull Object projectObject, @NotNull String uuid) {

    }

    @Override
    public String getPluginRootPath() {
        return PluginUtil.getPluginRootDirectory();
    }


    @Override
    public String getInstallationId() {
        if (isOptIn()) {
            return instID;
        } else {
            return "";
        }
    }

    public void openJobViewEditor(Object projectObject, String uuid) {
        IClusterDetail clusterDetail = JobViewManager.getCluster(uuid);

        Project project = (Project)projectObject;
        VirtualFile openedFile = getOpenedItem(project);
        if(openedFile == null || isNeedReopen(openedFile, clusterDetail)) {
            openItem(project,clusterDetail, uuid, openedFile);
        } else {
            openItem(project, openedFile, null);
        }
    }

    private boolean isNeedReopen(@NotNull VirtualFile virtualFile, @NotNull IClusterDetail myClusterDetail) {
        IClusterDetail detail = virtualFile.getUserData(JobViewEditorProvider.JOB_VIEW_KEY);
        return detail != null && !detail.getName().equalsIgnoreCase(myClusterDetail.getName());
    }

    private static VirtualFile getOpenedItem(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        for (VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            IClusterDetail detail = editedFile.getUserData(JobViewEditorProvider.JOB_VIEW_KEY);
            if (detail != null) {
                return editedFile;
            }
        }
        return null;
    }

    private void openItem(@NotNull final Project project, @NotNull final VirtualFile virtualFile, @Nullable final VirtualFile closeableVirtualFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if(closeableVirtualFile != null) {
                    FileEditorManager.getInstance(project).closeFile(closeableVirtualFile);
                }
                FileEditorManager.getInstance(project).openFile((VirtualFile) virtualFile, true, false);
            }
        });
    }

    private void openItem(@NotNull final Project project, @NotNull IClusterDetail myClusterDetail, @NotNull String uuid, @Nullable VirtualFile closeableFile) {
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
                return StreamUtil.getImageResourceFile(CommonConst.SPARK_JOBVIEW_ICONPATH);
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
            public String getCharset(@NotNull VirtualFile virtualFile, @NotNull byte[] bytes) {
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

    @Override
    public boolean isOptIn() {
        return isOptIn;
    }

    @NotNull
    @Override
    public NodeActionListener createAddNewHDInsightReaderClusterAction(
            @NotNull HDInsightRootModule module,
            @NotNull String clusterName) {
        return new AddNewHDInsightReaderClusterAction(module, clusterName);
    }

    @Override
    public void createRefreshHdiReaderClusterWarningForm(@NotNull HDInsightRootModule module, @NotNull String clusterName) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Project project = (Project)module.getProject();
                String title = "Read Only Cluster Warning";
                String warningText = "<html><pre>You only have READ ONLY permission for this cluster.<br>Would you like to link this cluster?</pre></html>";
                String okButtonText = "Link this cluster";
                WarningMessageForm form = new WarningMessageForm(project, title, warningText, okButtonText) {
                    @Override
                    protected void doOKAction() {
                        super.doOKAction();

                        AddNewHDInsightReaderClusterForm linkClusterForm = new AddNewHDInsightReaderClusterForm(project, module, clusterName);
                        linkClusterForm.show();
                    }
                };
                form.show();
            }
        }, ModalityState.any());
    }

    @Override
    public void createRefreshHdiReaderStorageAccountsWarningForm(@NotNull RefreshableNode node, @NotNull String aseDeepLink) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Project project = (Project)node.getProject();
                String title = "Storage Accounts Unavailable Warning";
                String warningText = "<html><pre>You only have READ ONLY permission for this cluster.<br>Would you like to see storage accounts in Azure Storage Explorer?</pre></html>";
                String okButtonText = "Open Azure Storage Explorer";
                WarningMessageForm form = new WarningMessageForm(project, title, warningText, okButtonText) {
                    @Override
                    protected void doOKAction() {
                        super.doOKAction();

                        try {
                            DefaultLoader.getIdeHelper().openLinkInBrowser(aseDeepLink);
                        } catch (Exception ex) {
                            DefaultLoader.getUIHelper().showError(ex.getMessage(), "HDInsight Explorer");
                        }

                    }
                };
                form.show();
            }
        }, ModalityState.any());
    }

    @Override
    public void createRefreshHdiLinkedClusterStorageAccountsWarningForm(@NotNull RefreshableNode node, @NotNull String aseDeepLink) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Project project = (Project)node.getProject();
                String title = "Storage Accounts Unavailable Warning";
                String warningText = "<html><pre>You don't have necessary credential to view storage accounts.<br>Would you like to see storage accounts in Azure Storage Explorer?</pre></html>";
                String okButtonText = "Open Azure Storage Explorer";
                WarningMessageForm form = new WarningMessageForm(project, title, warningText, okButtonText) {
                    @Override
                    protected void doOKAction() {
                        super.doOKAction();

                        try {
                            DefaultLoader.getIdeHelper().openLinkInBrowser(aseDeepLink);
                        } catch (Exception ex) {
                            DefaultLoader.getUIHelper().showError(ex.getMessage(), "HDInsight Explorer");
                        }
                    }
                };
                form.show();
            }
        }, ModalityState.any());
    }
}
