/*
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
 */

package com.microsoft.azure.hdinsight.spark.ui;

import com.google.common.collect.ImmutableSortedSet;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.impl.jar.JarFileSystemImpl;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.metadata.ClusterMetaDataService;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.common.SubmissionTableModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.helpers.ManifestFileUtilsEx;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spark Batch Application Submission UI control class
 */
public class SparkSubmissionContentPanelConfigurable implements SettableControl<SparkSubmitModel>, ILogger {
    @NotNull
    private final Project myProject;

    private SparkSubmissionContentPanel submissionPanel;
    private SparkSubmissionJobUploadStorageWithUploadPathPanel storageWithUploadPathPanel;
    private SparkSubmissionJobUploadStorageCtrl jobUploadStorageCtrl;
    private JPanel myWholePanel;

    // Cluster refresh publish subject with preselected cluster name as event
    @Nullable
    private BehaviorSubject<String> clustersRefreshSub;

    public SparkSubmissionContentPanelConfigurable(@NotNull Project project, @NotNull SparkSubmissionContentPanel submissionPanel) {
        this.submissionPanel = submissionPanel;
        this.storageWithUploadPathPanel = submissionPanel.storageWithUploadPathPanel;
        this.myProject = project;

        this.jobUploadStorageCtrl = new SparkSubmissionJobUploadStorageCtrl(this.storageWithUploadPathPanel) {
            @Nullable
            @Override
            public String getClusterName() {
                IClusterDetail clusterDetail = getSelectedClusterDetail();
                return clusterDetail == null ? null : clusterDetail.getName();
            }
        };
        this.clustersRefreshSub = BehaviorSubject.create();
    }

    @NotNull
    protected ImmutableSortedSet<? extends IClusterDetail> getClusterDetails() {
        return ImmutableSortedSet.copyOf((x, y) -> x.getTitle().compareToIgnoreCase(y.getTitle()),
                                         ClusterMetaDataService.getInstance().getCachedClusterDetails());
    }

    @NotNull
    protected Observable<ImmutableSortedSet<? extends IClusterDetail>> getClusterDetailsWithRefresh() {
        return Observable.fromCallable(() -> ClusterManagerEx.getInstance().getClusterDetails())
                .map(list -> ImmutableSortedSet.copyOf((x, y) -> x.getTitle().compareToIgnoreCase(y.getTitle()), list));
    }

    protected void createUIComponents() {
        // Customized UI creation
        this.submissionPanel.getJobConfigurationTable().setModel(new SubmissionTableModel());
        this.submissionPanel.getClustersListComboBox().getComboBox().setRenderer(new ListCellRendererWrapper<IClusterDetail>() {
            @Override
            public void customize(JList jList, @Nullable IClusterDetail cluster, int i, boolean b, boolean b1) {
                this.setText(cluster == null ? null : cluster.getTitle());
            }
        });

        this.submissionPanel.getSelectedArtifactComboBox().setRenderer(new ListCellRendererWrapper<Artifact>() {
            @Override
            public void customize(JList jList, @Nullable Artifact artifact, int i, boolean b, boolean b1) {
                this.setText(artifact == null ? null : artifact.getName());
            }
        });

        submissionPanel.getMainClassTextField().addActionListener(e -> {
                    PsiClass selected = submissionPanel.getLocalArtifactRadioButton().isSelected() ?
                            new ManifestFileUtilsEx(myProject).selectMainClass(
                                    new JarFileSystemImpl().findFileByPath(
                                            submissionPanel.getLocalArtifactTextField().getText() + "!/")) :
                            ManifestFileUtil.selectMainClass(myProject, submissionPanel.getMainClassTextField().getText());
                    if (selected != null) {
                        submissionPanel.getMainClassTextField().setText(selected.getQualifiedName());
                    }
                }
        );

        this.submissionPanel.addClusterListRefreshActionListener(e -> {
            String clusterSelected = getSelectedClusterDetail() == null ? null : getSelectedClusterDetail().getName();

            refreshClusterListAsync(clusterSelected);
        });

        this.submissionPanel.getClustersListComboBox().getComboBox().addItemListener(e -> {
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    if (e.getItem() != null) {
                        IClusterDetail cluster = (IClusterDetail) e.getItem();
                        onClusterSelected(cluster);
                    }
                    break;
                default:
            }
        });

        this.submissionPanel.getIntelliJArtifactRadioButton().addActionListener(e ->
                refreshAndSelectArtifact(getSelectedArtifact() == null ? null : getSelectedArtifact().getName()));

        this.submissionPanel.updateTableColumn();
        this.submissionPanel.getClustersListComboBox().getComboBox().setModel(new DefaultComboBoxModel<>(getClusterDetails().toArray()));

        this.submissionPanel.addPropertyChangeListener("ancestor", event -> {
            if (event.getNewValue() != null) {
                // Being added
                if (clustersRefreshSub == null) {
                    clustersRefreshSub = BehaviorSubject.create();
                }

                clustersRefreshSub
                        .doOnNext(any -> setClusterRefreshEnabled(false))
                        .flatMap(preSelectedClusterName -> getClusterDetailsWithRefresh()
                                .subscribeOn(Schedulers.io())
                                .map(clusters -> Pair.of(preSelectedClusterName, clusters))
                                .onErrorReturn(err -> {
                                    log().warn(String.format("Project %s failed to refresh %s: %s",
                                            myProject.getName(), getType(), err));

                                    return Pair.of(preSelectedClusterName, ImmutableSortedSet.of());
                                }))
                        .doOnEach(each -> setClusterRefreshEnabled(true))
                        .subscribe(
                                selectedClustersPair -> {
                                    final DefaultComboBoxModel<IClusterDetail> clustersModel = getClusterComboBoxModel();

                                    clustersModel.removeAllElements();
                                    selectedClustersPair.getRight().forEach(clustersModel::addElement);

                                    if (selectedClustersPair.getLeft() != null) {
                                        selectCluster(selectedClustersPair.getLeft(), IClusterDetail::getName);
                                    }
                                },
                                err -> log().error(String.format("Project %s failed to process subject %s: %s",
                                        myProject.getName(), getType(), err)));
            } else if (clustersRefreshSub != null) {
                // Being removed
                clustersRefreshSub.onCompleted();

                clustersRefreshSub = null;
            }
        });
    }

    protected String getType() {
        return "HDInsight";
    }

    @NotNull
    public JComponent getComponent() {
        return submissionPanel;
    }

    @NotNull
    private DefaultComboBoxModel<IClusterDetail> getClusterComboBoxModel() {
        return (DefaultComboBoxModel<IClusterDetail>) (submissionPanel.getClustersListComboBox().getComboBox().getModel());
    }

    protected void onClusterSelected(@NotNull IClusterDetail cluster) {
        getSubmissionPanel().getClusterSelectedSubject().onNext(cluster.getName());
        jobUploadStorageCtrl.selectCluster(cluster.getName());
    }

    private synchronized void refreshClusterListAsync(@Nullable String preSelectedClusterName) {
        if (clustersRefreshSub != null) {
            clustersRefreshSub.onNext(preSelectedClusterName);
        }
    }

    private synchronized void refreshAndSelectArtifact(final @Nullable String artifactName) {
        DefaultComboBoxModel<Artifact> artifactModel = (DefaultComboBoxModel<Artifact>) submissionPanel.getSelectedArtifactComboBox().getModel();

        final List<Artifact> artifacts = ArtifactUtil.getArtifactWithOutputPaths(myProject);

        artifactModel.removeAllElements();

        for (int i = 0; i < artifacts.size(); i++) {
            if (StringUtils.equals(artifacts.get(i).getName(), artifactName)) {
                artifactModel.addElement(artifacts.get(i));         // Add with select it
            } else {
                artifactModel.insertElementAt(artifacts.get(i), i); // Insert without select it
            }
        }

        // If no element selected, select the first one as default
        if (StringUtils.isBlank(artifactName) && artifactModel.getSelectedItem() == null && artifactModel.getSize() > 0) {
            artifactModel.setSelectedItem(artifactModel.getElementAt(0));
        }
    }

    // Select cluster from the cluster combo box model
    // Returns true for find and select the cluster
    private boolean selectCluster(final @Nullable String clusterProperty,
                                  final @NotNull Function<? super IClusterDetail, String> clusterPropertyMapper) {
        for (int i = 0; i < getClusterComboBoxModel().getSize(); i++) {
            if (StringUtils.equals(clusterProperty, clusterPropertyMapper.apply(getClusterComboBoxModel().getElementAt(i)))) {
                getClusterComboBoxModel().setSelectedItem(getClusterComboBoxModel().getElementAt(i));

                return true;
            }
        }

        return false;
    }

    void setClusterRefreshEnabled(boolean enabled) {
        ApplicationManager.getApplication().invokeAndWait(() ->
                submissionPanel.setClustersListRefreshEnabled(enabled), ModalityState.any());
    }

    void setClusterSelectionEnabled(boolean enabled) {
        ApplicationManager.getApplication().invokeAndWait(() ->
                submissionPanel.getClustersListComboBox().setEnabled(enabled), ModalityState.any());
    }

    @Override
    public void setData(@NotNull SparkSubmitModel data) {
        // Data -> Component

        // The clusters combo box model and artifacts model are project context related,
        // so just refresh them if needed, rather than reading from the data

        // Scenarios
        // 1. Cluster refresh in progress, the list model have choice, select cluster by cluster name
        // 2. Cluster refresh in progress, the list model is empty, save cluster name in submit model
        // 3. Cluster list got, but no selection before, select cluster by cluster name
        ApplicationManager.getApplication().invokeAndWait(() -> {
            submissionPanel.getClustersListComboBox().getComboBox().setModel(data.getClusterComboBoxModel());
            submissionPanel.getSelectedArtifactComboBox().setModel(data.getArtifactComboBoxModel());

            if (!selectCluster(data.getClusterName(), IClusterDetail::getName)) {
                refreshClusterListAsync(data.getClusterName());
            }

            if (data.getIsLocalArtifact()) {
                submissionPanel.getLocalArtifactRadioButton().setSelected(true);
            }

            submissionPanel.getLocalArtifactTextField().setText(data.getLocalArtifactPath());
            submissionPanel.getMainClassTextField().setText(data.getMainClassName());
            submissionPanel.getCommandLineTextField().setText(String.join(" ", data.getCommandLineArgs()));
            submissionPanel.getReferencedJarsTextField().setText(String.join(";", data.getReferenceJars()));
            submissionPanel.getReferencedFilesTextField().setText(String.join(";", data.getReferenceFiles()));

            // update job configuration table
            submissionPanel.getJobConfigurationTable().setModel(data.getTableModel());

            refreshAndSelectArtifact(data.getArtifactName());
        }, ModalityState.any());

        // set Job Upload Storage panel data
        storageWithUploadPathPanel.setData(data.getJobUploadStorageModel());
    }

    @Override
    public void getData(@NotNull SparkSubmitModel data) {
        // Component -> Data

        String selectedArtifactName = Optional.ofNullable(getSelectedArtifact())
                .map(Artifact::getName)
                .orElse("");

        String className = submissionPanel.getMainClassTextField().getText().trim();

        String localArtifactPath = submissionPanel.getLocalArtifactTextField().getText();

        String selectedClusterName = Optional.ofNullable(getSelectedClusterDetail())
                .map(IClusterDetail::getName)
                .orElse("");

        List<String> referencedFileList = Arrays.stream(submissionPanel.getReferencedFilesTextField().getText().split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> uploadedFilePathList = Arrays.stream(submissionPanel.getReferencedJarsTextField().getText().split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> argsList = Arrays.stream(submissionPanel.getCommandLineTextField().getText().split(" "))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        boolean isLocalArtifact = submissionPanel.getLocalArtifactRadioButton().isSelected();
        SubmissionTableModel tableModel = (SubmissionTableModel) submissionPanel.getJobConfigurationTable().getModel();

        // submission parameters
        data.setClusterName(selectedClusterName);
        data.setIsLocalArtifact(isLocalArtifact);
        data.setArtifactName(selectedArtifactName);
        data.setLocalArtifactPath(localArtifactPath);
        data.setFilePath(null);
        data.setMainClassName(className);
        data.setReferenceFiles(referencedFileList);
        data.setReferenceJars(uploadedFilePathList);
        data.setCommandLineArgs(argsList);

        data.setTableModel(tableModel);

        // get Job upload storage panel data
        storageWithUploadPathPanel.getData(data.getJobUploadStorageModel());
    }

    @Nullable
    public IClusterDetail getSelectedClusterDetail() {
        return (IClusterDetail) getClusterComboBoxModel().getSelectedItem();
    }

    public void validate() throws ConfigurationException {
        getSubmissionPanel().checkInputs();

        if (!jobUploadStorageCtrl.isCheckPassed()) {
            throw new RuntimeConfigurationError("Can't save the configuration since "
                    + jobUploadStorageCtrl.getResultMessage().toLowerCase());
        }
    }

    public SparkSubmissionContentPanel getSubmissionPanel() {
        return submissionPanel;
    }

    @Nullable
    private Artifact getSelectedArtifact() {
        return (Artifact) submissionPanel.getSelectedArtifactComboBox().getSelectedItem();
    }
}
