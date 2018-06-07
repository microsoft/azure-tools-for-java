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

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SparkSubmissionContentPanelConfigurable implements SettableControl<SparkSubmitModel> {
    @NotNull
    private final Project myProject;

    private SparkSubmissionContentPanel submissionPanel;
    private JPanel myWholePanel;

    private CallBack updateCallback;

    @NotNull
    private SparkSubmitModel submitModel;

    public SparkSubmissionContentPanelConfigurable(@NotNull Project project,
                                                   @Nullable CallBack callBack,
                                                   @NotNull SparkSubmissionContentPanel submissionPanel) {
        this.myProject = project;
        this.updateCallback = callBack;
        this.submissionPanel = submissionPanel;

    }

    @NotNull
    protected ImmutableList<IClusterDetail> getClusterDetails() {
        return ClusterManagerEx.getInstance().getClusterDetails();
    }

    protected void createUIComponents() {
        // Customized UI creation
        this.submitModel = new SparkSubmitModel(myProject);
        this.submissionPanel.getClustersListComboBox().getComboBox().setModel(submitModel.getClusterComboBoxModel());

        ManifestFileUtil.setupMainClassField(myProject, submissionPanel.getMainClassTextField());

        this.submissionPanel.addClusterListRefreshActionListener(e -> refreshClusterListAsync());

        this.submissionPanel.addJobConfigurationLoadButtonActionListener(e -> {
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
                    true,
                    false,
                    false,
                    false,
                    false,
                    false);

            fileChooserDescriptor.setTitle("Select Spark Property File");

            Optional.ofNullable(FileChooser.chooseFile(fileChooserDescriptor, null, null))
                    .map(VirtualFile::getCanonicalPath)
                    .ifPresent(this::loadJobConfigMapFromPropertyFile);
        });

        this.submissionPanel.getSelectedArtifactComboBox().setModel(submitModel.getArtifactComboBoxModel());
        this.submissionPanel.getJobConfigurationTable().setModel(submitModel.getTableModel());

        this.submissionPanel.updateTableColumn();

        refreshClusterListAsync();
    }

    @NotNull
    public SparkSubmitModel getSubmitModel() {
        return submitModel;
    }

    @NotNull
    public JComponent getComponent() {
        return submissionPanel;
    }

    protected void refreshClusterSelection(@NotNull List<IClusterDetail> clusters) {
        Optional<String> selectedClusterTitle = submitModel.getSelectedClusterDetail()
                .map(IClusterDetail::getTitle);
        resetClusterDetailsToComboBoxModel(submitModel, clusters);
        if (selectedClusterTitle.isPresent()) {
            setSelectedClusterByTitle(submitModel, selectedClusterTitle.get());
        } else {
            setSelectedClusterByName(submitModel, submitModel.getSubmissionParameter().getClusterName());
        }
    }

    protected void refreshClusterListAsync() {
        submissionPanel.setClustersListRefreshEnabled(false);

        DefaultLoader.getIdeHelper().executeOnPooledThread(() -> {
            HDInsightUtil.showInfoOnSubmissionMessageWindow(myProject, "List spark clusters ...");
            List<IClusterDetail> cachedClusters = ClusterManagerEx.getInstance().getClusterDetailsWithoutAsync(true);

            if (!ClusterManagerEx.getInstance().isSelectedSubscriptionExist()) {
                HDInsightUtil.showWarningMessageOnSubmissionMessageWindow(myProject, "No selected subscription(s), Please go to HDInsight Explorer to sign in....");
            }
            if (ClusterManagerEx.getInstance().isListClusterSuccess()) {
                HDInsightUtil.showInfoOnSubmissionMessageWindow(myProject, "List spark clusters successfully");
            } else {
                HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(myProject, "Error : Failed to list spark clusters.");
            }
            if (ClusterManagerEx.getInstance().isLIstAdditionalClusterSuccess()) {
                HDInsightUtil.showInfoOnSubmissionMessageWindow(myProject, "List additional spark clusters successfully");
            } else {
                HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(myProject, "Error: Failed to list additional cluster");
            }

            refreshClusterSelection(cachedClusters);

            submissionPanel.setClustersListRefreshEnabled(true);
            submissionPanel.getClusterSelectedSubject().onNext((String) submissionPanel.getClustersListComboBox().getComboBox().getSelectedItem());
        });

    }

    protected void resetClusterDetailsToComboBoxModel(@NotNull SparkSubmitModel destSubmitModel, @NotNull List<IClusterDetail> cachedClusterDetails) {
        List<IClusterDetail> clusterDetails = new ArrayList<>();

        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
                clusterDetails = cachedClusterDetails;
            } else {
                clusterDetails = cachedClusterDetails.stream()
                        .filter(HDInsightAdditionalClusterDetail.class::isInstance)
                        .collect(Collectors.toList());
            }
        } catch (IOException ignored) { }

        destSubmitModel.setCachedClusterDetailsWithTitleMapping(clusterDetails);

        destSubmitModel.getClusterComboBoxModel().removeAllElements();
        clusterDetails.forEach(clusterDetail -> destSubmitModel.getClusterComboBoxModel().addElement(clusterDetail.getTitle()));
    }

    private void loadJobConfigMapFromPropertyFile(String propertyFilePath) {
        submitModel.getTableModel().loadJobConfigMapFromPropertyFile(propertyFilePath);
    }

    private void setSelectedClusterByTitle(SparkSubmitModel destSubmitModel, String clusterTitleToSelect) {
        final DefaultComboBoxModel<String> clusterComboBoxModel = destSubmitModel.getClusterComboBoxModel();

        destSubmitModel.getCachedClusterDetails().stream()
                .filter(clusterDetail -> clusterDetail.getTitle().equals(clusterTitleToSelect))
                .map(IClusterDetail::getTitle)
                .findFirst()
                .ifPresent(clusterTitle -> {
                    if (clusterComboBoxModel.getIndexOf(clusterTitle) >= 0) {
                        clusterComboBoxModel.setSelectedItem(clusterTitle);
                    }
                });
    }

    private void setSelectedClusterByName(SparkSubmitModel destSubmitModel, String clusterNameToSelect) {
        final DefaultComboBoxModel<String> clusterComboBoxModel = destSubmitModel.getClusterComboBoxModel();

        destSubmitModel.getCachedClusterDetails().stream()
                .filter(clusterDetail -> clusterDetail.getName().equals(clusterNameToSelect))
                .map(IClusterDetail::getTitle)
                .findFirst()
                .ifPresent(clusterTitle -> {
                    if (clusterComboBoxModel.getIndexOf(clusterTitle) >= 0) {
                        clusterComboBoxModel.setSelectedItem(clusterTitle);
                    }
                });
    }

    @Override
    public void setData(@NotNull SparkSubmitModel data) {
        // Data -> Component
        SparkSubmissionParameter parameter = data.getSubmissionParameter();

        submitModel.setSubmissionParameters(parameter);

        resetClusterDetailsToComboBoxModel(submitModel, data.getCachedClusterDetails());
        data.getSelectedClusterDetail()
            .map(IClusterDetail::getTitle)
            .ifPresent(selectedTitle -> setSelectedClusterByTitle(submitModel, selectedTitle));

        if (parameter.isLocalArtifact()) {
            submissionPanel.getLocalArtifactRadioButton().setSelected(true);
        }

        submissionPanel.getSelectedArtifactTextField().setText(parameter.getLocalArtifactPath());
        submissionPanel.getMainClassTextField().setText(parameter.getMainClassName());
        submissionPanel.getCommandLineTextField().setText(String.join(" ", parameter.getArgs()));
        submissionPanel.getReferencedJarsTextField().setText(String.join(";", parameter.getReferencedJars()));
        submissionPanel.getReferencedFilesTextField().setText(String.join(";", parameter.getReferencedFiles()));

        // update job configuration table
        submitModel.getTableModel().loadJobConfigMap(data.getTableModel().getJobConfigMap());

        if (updateCallback != null) {
            updateCallback.run();
        }
    }

    @Override
    public void getData(@NotNull SparkSubmitModel data) {
        // Component -> Data

        String selectedArtifactName = Optional.ofNullable(submissionPanel.getSelectedArtifactComboBox().getSelectedItem())
                .map(Object::toString)
                .orElse("");

        String className = submissionPanel.getMainClassTextField().getText().trim();

        String localArtifactPath = submissionPanel.getSelectedArtifactTextField().getText();

        String selectedClusterName = submitModel.getSelectedClusterDetail()
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

        Boolean isLocalArtifact = submissionPanel.getLocalArtifactRadioButton().isSelected();

        Map<String, Object> jobConfigMap = submitModel.getJobConfigMap();

        // submission parameters
        data.setSubmissionParameters(new SparkSubmissionParameter(selectedClusterName, isLocalArtifact,
                selectedArtifactName, localArtifactPath, null, className, referencedFileList, uploadedFilePathList, argsList, jobConfigMap));

        // Sub models
        resetClusterDetailsToComboBoxModel(data, submitModel.getCachedClusterDetails());
        submitModel.getSelectedClusterDetail()
                   .map(IClusterDetail::getTitle)
                   .ifPresent(selectedTitle -> setSelectedClusterByTitle(data, selectedTitle));

        data.getTableModel().loadJobConfigMap(submitModel.getTableModel().getJobConfigMap());

        data.getArtifactComboBoxModel().removeAllElements();
        final DefaultComboBoxModel<String> componentArtifactsModel = submitModel.getArtifactComboBoxModel();
        IntStream.range(0, componentArtifactsModel.getSize())
                .boxed()
                .map(componentArtifactsModel::getElementAt)
                .forEach(artifact -> data.getArtifactComboBoxModel().addElement(artifact));
    }

    public SparkSubmissionContentPanel getSubmissionPanel() {
        return submissionPanel;
    }
}
