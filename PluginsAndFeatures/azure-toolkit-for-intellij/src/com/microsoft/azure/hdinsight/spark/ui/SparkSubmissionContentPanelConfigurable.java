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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.SettableControl;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public SparkSubmissionContentPanelConfigurable(@NotNull Project project, @Nullable CallBack callBack) {
        this.myProject = project;
        this.updateCallback = callBack;
    }

    private void createUIComponents() {
        this.submitModel = new SparkSubmitModel(myProject);
        this.submissionPanel = new SparkSubmissionContentPanel(updateCallback);
        this.submissionPanel.getClustersListComboBox().getComboBox().setModel(submitModel.getClusterComboBoxModel());

        ManifestFileUtil.setupMainClassField(myProject, submissionPanel.getMainClassTextField());

        this.submissionPanel.addClusterListRefreshActionListener(e -> {
            List<IClusterDetail> clusterDetails = ClusterManagerEx.getInstance().getClusterDetails();
            resetClusterDetailsToComboBoxModel(submitModel, clusterDetails);
        });

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

        final SparkSubmissionAdvancedConfigDialog advConfDialog = this.submissionPanel.getAdvancedConfigDialog();
        advConfDialog.setData(submitModel.getAdvancedConfigModel());
        advConfDialog.addCallbackOnOk(() -> advConfDialog.getData(submitModel.getAdvancedConfigModel()));

        this.submissionPanel.addAdvancedConfigurationButtonActionListener(e -> {
            // Read the current panel setting into current model

            advConfDialog.setAuthenticationAutoVerify(submitModel.getSelectedClusterDetail().map(IClusterDetail::getName)
                                                                                            .orElse(null));
            advConfDialog.setModal(true);
            advConfDialog.setVisible(true);
        });

        this.submissionPanel.updateTableColumn();

        refreshClusterListAsync();
    }
    @NotNull
    public JComponent getComponent() {
        return submissionPanel;
    }

    private void refreshClusterListAsync() {
        submissionPanel.setClustersListRefreshEnabled(false);

        DefaultLoader.getIdeHelper().executeOnPooledThread(() -> {
            HDInsightUtil.showInfoOnSubmissionMessageWindow(myProject, "List spark clusters ...", true);
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

            resetClusterDetailsToComboBoxModel(submitModel, cachedClusters);
            submissionPanel.setClustersListRefreshEnabled(true);
            submissionPanel.getClusterSelectedSubject().onNext((String) submissionPanel.getClustersListComboBox().getComboBox().getSelectedItem());
        });

    }

    private void resetClusterDetailsToComboBoxModel(SparkSubmitModel destSubmitModel, List<IClusterDetail> cachedClusterDetails) {
        destSubmitModel.setCachedClusterDetailsWithTitleMapping(cachedClusterDetails);

        destSubmitModel.getClusterComboBoxModel().removeAllElements();
        cachedClusterDetails.forEach(clusterDetail -> destSubmitModel.getClusterComboBoxModel().addElement(clusterDetail.getTitle()));

        setSelectedClusterByName(destSubmitModel.getSubmissionParameter().getClusterName());
    }

    private void loadJobConfigMapFromPropertyFile(String propertyFilePath) {
        submitModel.getTableModel().loadJobConfigMapFromPropertyFile(propertyFilePath);
    }

    private void setSelectedClusterByName(String clusterName) {
        final DefaultComboBoxModel<String> clusterComboBoxModel = submitModel.getClusterComboBoxModel();

        submitModel.getCachedClusterDetails().stream()
                .filter(clusterDetail -> clusterDetail.getName().equals(clusterName))
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

        setSelectedClusterByName(parameter.getClusterName());

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

        // Advanced Configuration Dialog
        submissionPanel.getAdvancedConfigDialog().setData(data.getAdvancedConfigModel());

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
        data.getTableModel().loadJobConfigMap(submitModel.getTableModel().getJobConfigMap());

        data.getArtifactComboBoxModel().removeAllElements();
        final DefaultComboBoxModel<String> componentArtifactsModel = submitModel.getArtifactComboBoxModel();
        IntStream.range(0, componentArtifactsModel.getSize())
                .boxed()
                .map(componentArtifactsModel::getElementAt)
                .forEach(artifact -> data.getArtifactComboBoxModel().addElement(artifact));

        // Advanced Configuration Dialog
        submissionPanel.getAdvancedConfigDialog().getData(data.getAdvancedConfigModel());
    }

}
