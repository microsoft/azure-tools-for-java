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
package com.microsoft.azure.hdinsight.spark.common;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.spark.uihelper.InteractiveTableModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import com.microsoft.intellij.util.AppInsightsCustomEvent;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SparkSubmitModel {

    private static final String[] columns = {"Key", "Value", ""};
    private  static final String SparkYarnLogUrlFormat = "%s/yarnui/hn/cluster/app/%s";

    private static Map<Project, SparkSubmissionParameter> submissionParameterMap = new HashMap<>();

    private final Project project;
    private final List<IClusterDetail> cachedClusterDetails;

    private Map<String, IClusterDetail> mapClusterNameToClusterDetail = new HashMap<>();
    private Map<String, Artifact> artifactHashMap = new HashMap<>();

    private SparkSubmissionParameter submissionParameter;

    private DefaultComboBoxModel<String> clusterComboBoxModel;
    private DefaultComboBoxModel<String> artifactComboBoxModel;
    private SubmissionTableModel tableModel = new SubmissionTableModel(columns);
    private final Map<String, String> postEventProperty = new HashMap<>();


    public SparkSubmitModel(@NotNull Project project, @NotNull List<IClusterDetail> cachedClusterDetails) {
        this.cachedClusterDetails = cachedClusterDetails;
        this.project = project;

        this.clusterComboBoxModel = new DefaultComboBoxModel<>();
        this.artifactComboBoxModel = new DefaultComboBoxModel<>();
        this.submissionParameter = submissionParameterMap.get(project);

        setClusterComboBoxModel(cachedClusterDetails);
        int index = -1;
        if(submissionParameter != null) {
            String title = getCluserTitle(submissionParameter.getClusterName());
            index = clusterComboBoxModel.getIndexOf(title);
            if (index != -1) {
                clusterComboBoxModel.setSelectedItem(getCluserTitle(submissionParameter.getClusterName()));
            }
        }

        final List<Artifact> artifacts = ArtifactUtil.getArtifactWithOutputPaths(project);

        for (Artifact artifact : artifacts) {
            artifactHashMap.put(artifact.getName(), artifact);
            artifactComboBoxModel.addElement(artifact.getName());
            if (artifactComboBoxModel.getSize() == 0) {
                artifactComboBoxModel.setSelectedItem(artifact.getName());
            }
        }

        index = submissionParameter != null ? artifactComboBoxModel.getIndexOf(submissionParameter.getArtifactName()) : -1;
        if (index != -1) {
            artifactComboBoxModel.setSelectedItem(submissionParameter.getArtifactName());
        }

        initializeTableModel(tableModel);
    }

    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }

    @NotNull
    public IClusterDetail getSelectedClusterDetail() {
        return mapClusterNameToClusterDetail.get((String) clusterComboBoxModel.getSelectedItem());
    }

    public DefaultComboBoxModel getClusterComboBoxModel() {
        return clusterComboBoxModel;
    }

    public DefaultComboBoxModel getArtifactComboBoxModel() {
        return artifactComboBoxModel;
    }

    public boolean isLocalArtifact() {
        return submissionParameter.isLocalArtifact();
    }

    public Project getProject() {
        return project;
    }

    public InteractiveTableModel getTableModel() {
        return tableModel;
    }

    public void setClusterComboBoxModel(List<IClusterDetail> cachedClusterDetails) {
        clusterComboBoxModel.removeAllElements();
        mapClusterNameToClusterDetail.clear();

        for (IClusterDetail clusterDetail : cachedClusterDetails) {

            String title = getCluserTitle(clusterDetail);
            mapClusterNameToClusterDetail.put(title, clusterDetail);
            clusterComboBoxModel.addElement(title);
            if (clusterComboBoxModel.getSize() == 0) {
                clusterComboBoxModel.setSelectedItem(title);
            }
        }
    }

    public void action(@NotNull SparkSubmissionParameter submissionParameter) {
        HDInsightUtil.getJobStatusManager(project).setJobRunningState(true);
        this.submissionParameter = submissionParameter;
        submissionParameterMap.put(project, submissionParameter);
        postEventAction();

        if (isLocalArtifact()) {
            submit();
        } else {
            final List<Artifact> artifacts = new ArrayList<>();
            final Artifact artifact = artifactHashMap.get(submissionParameter.getArtifactName());
            artifacts.add(artifact);
            ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);

            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, true);
                    CompilerManager.getInstance(project).make(scope, new CompileStatusNotification() {
                        @Override
                        public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                            if (aborted || errors != 0) {
                                showCompilerErrorMessage(compileContext);
                                HDInsightUtil.getJobStatusManager(project).setJobRunningState(false);
                            } else {
                                postEventProperty.put("IsSubmitSucceed", "true");
                                postEventProperty.put("SubmitFailedReason", "CompileSuccess");
                                AppInsightsCustomEvent.create(HDInsightBundle.message("SparkProjectCompileSuccess"), null, postEventProperty);

                                HDInsightUtil.showInfoOnSubmissionMessageWindow(project, String.format("Info : Build %s successfully.", artifact.getOutputFile()));
                                submit();
                            }
                        }
                    });
                }
            }, ModalityState.defaultModalityState());
        }
    }

    private void showCompilerErrorMessage(@NotNull CompileContext compileContext) {
        postEventProperty.put("IsSubmitSucceed", "false");
        postEventProperty.put("SubmitFailedReason", "CompileFailed");
        AppInsightsCustomEvent.create(HDInsightBundle.message("SparkProjectCompileFailed"), null, postEventProperty);

        CompilerMessage[] errorMessages= compileContext.getMessages(CompilerMessageCategory.ERROR);
        for(CompilerMessage message : errorMessages) {
            HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project, message.toString());
        }
    }

    private String getCluserTitle(@NotNull IClusterDetail clusterDetail) {
        String sparkVersion = clusterDetail.getSparkVersion();
        return sparkVersion == null ? clusterDetail.getName() : StringHelper.concat(clusterDetail.getName(), "(Spark: ", sparkVersion, ")");
    }
    private String getCluserTitle(@NotNull String clusterName) {
        for(IClusterDetail clusterDetail : cachedClusterDetails) {
            if(clusterDetail.getName().equals(clusterName)) {
                return getCluserTitle(clusterDetail);
            }
        }
        return "unknow";
    }

    private void uploadFileToCluster(@NotNull final IClusterDetail selectedClusterDetail, @NotNull final String selectedArtifactName) throws Exception{
        String buildJarPath = submissionParameter.isLocalArtifact() ?
                submissionParameter.getLocalArtifactPath() : ((artifactHashMap.get(selectedArtifactName).getOutputFilePath()));

        String filePath = selectedClusterDetail.isEmulator() ?
                SparkSubmitHelper.uploadFileToEmulator(project, selectedClusterDetail, buildJarPath) :
                SparkSubmitHelper.uploadFileToHDFS(project, selectedClusterDetail, buildJarPath);
        submissionParameter.setFilePath(filePath);
    }

    private void tryToCreateBatchSparkJob(@NotNull final IClusterDetail selectedClusterDetail) throws HDIException,IOException {
        SparkBatchSubmission.getInstance().setCredentialsProvider(selectedClusterDetail.getHttpUserName(), selectedClusterDetail.getHttpPassword());
        HttpResponse response = SparkBatchSubmission.getInstance().createBatchSparkJob(SparkSubmitHelper.getLivyConnectionURL(selectedClusterDetail), submissionParameter);

        if (response.getCode() == 201 || response.getCode() == 200) {
            HDInsightUtil.showInfoOnSubmissionMessageWindow(project, "Info : Submit to spark cluster successfully.");
            postEventProperty.put("IsSubmitSucceed", "true");

            String jobLink = selectedClusterDetail.isEmulator() ?
                    ((EmulatorClusterDetail)selectedClusterDetail).getSparkHistoryEndpoint() :
                    String.format("%s/sparkhistory", selectedClusterDetail.getConnectionUrl());
            HDInsightUtil.getSparkSubmissionToolWindowManager(project).setHyperLinkWithText("See spark job view from ", jobLink, jobLink);
            SparkSubmitResponse sparkSubmitResponse = new Gson().fromJson(response.getMessage(), new TypeToken<SparkSubmitResponse>() {
            }.getType());

            // Set submitted spark application id and http request info for stopping running application
            HDInsightUtil.getSparkSubmissionToolWindowManager(project).setSparkApplicationStopInfo(selectedClusterDetail, sparkSubmitResponse.getId());
            HDInsightUtil.getSparkSubmissionToolWindowManager(project).setStopButtonState(true);
            HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().resetJobStateManager();
            SparkSubmitHelper.getInstance().printRunningLogStreamingly(project, sparkSubmitResponse.getId(), selectedClusterDetail, postEventProperty);
        } else {
            HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project,
                    String.format("Error : Failed to submit to spark cluster. error code : %d, reason :  %s.", response.getCode(), response.getContent()));
            postEventProperty.put("IsSubmitSucceed", "false");
            postEventProperty.put("SubmitFailedReason", response.getContent());
            AppInsightsCustomEvent.create(HDInsightBundle.message("SparkSubmissionButtonClickEvent"), null, postEventProperty);
        }
    }

    private void showFailedSubmitErrorMessage(Exception exception) {
        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error : Failed to submit application to spark cluster. Exception : " + exception.getMessage());
        postEventProperty.put("IsSubmitSucceed", "false");
        postEventProperty.put("SubmitFailedReason", exception.toString());
        AppInsightsCustomEvent.create(HDInsightBundle.message("SparkSubmissionButtonClickEvent"), null, postEventProperty);
    }

    private void writeJobLogToLocal() {
        String path = null;
        try {
            path = SparkSubmitHelper.getInstance().writeLogToLocalFile(project);
        } catch (IOException e) {
            HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project, e.getMessage());
        }

        if (!StringHelper.isNullOrWhiteSpace(path)) {
            String urlPath = StringHelper.concat("file:", path);
            HDInsightUtil.getSparkSubmissionToolWindowManager(project).setHyperLinkWithText("See detailed job log from local:", urlPath, path);
        }
    }

    private IClusterDetail getClusterConfiguration(@NotNull final IClusterDetail selectedClusterDetail, @NotNull final boolean isFirstSubmit) {
        try {
            if (!selectedClusterDetail.isConfigInfoAvailable()) {
                selectedClusterDetail.getConfigurationInfo(getProject());
            }
        } catch (AuthenticationException authenticationException) {
            if (isFirstSubmit) {
                HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project, "Error: Cluster Credentials Expired, Please sign in again...");
                //get new credentials by call getClusterDetails
                cachedClusterDetails.clear();
                cachedClusterDetails.addAll(ClusterManagerEx.getInstance().getClusterDetails(getProject()));

                for (IClusterDetail iClusterDetail : cachedClusterDetails) {
                    if (iClusterDetail.getName().equalsIgnoreCase(selectedClusterDetail.getName())) {
                        //retry get cluster info
                        return getClusterConfiguration(iClusterDetail, false);
                    }
                }
            } else {
                return null;
            }
        } catch (Exception exception) {
            showFailedSubmitErrorMessage(exception);
            return null;
        }

        return selectedClusterDetail;
    }

    private void submit() {
        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                IClusterDetail selectedClusterDetail = mapClusterNameToClusterDetail.get(clusterComboBoxModel.getSelectedItem());
                String selectedArtifactName = submissionParameter.getArtifactName();

                //may get a new clusterDetail reference if cluster credentials expired
                selectedClusterDetail = getClusterConfiguration(selectedClusterDetail, true);

                if (selectedClusterDetail == null) {
                    HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(project, "Selected Cluster can not found. Please login in first in HDInsight Explorer and try submit job again");
                    return;
                }

                try {
                    uploadFileToCluster(selectedClusterDetail, selectedArtifactName);
                    tryToCreateBatchSparkJob(selectedClusterDetail);
                } catch (Exception exception) {
                    showFailedSubmitErrorMessage(exception);
                } finally {
                    HDInsightUtil.getSparkSubmissionToolWindowManager(project).setStopButtonState(false);
                    HDInsightUtil.getSparkSubmissionToolWindowManager(project).setBrowserButtonState(false);

                    if (HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().isApplicationGenerated()) {
                        String applicationId = HDInsightUtil.getSparkSubmissionToolWindowManager(project).getJobStatusManager().getApplicationId();

                        // ApplicationYarnUrl example : https://sparklivylogtest.azurehdinsight.net/yarnui/hn/cluster/app/application_01_111
                        String applicationYarnUrl = String.format(SparkYarnLogUrlFormat, selectedClusterDetail.getConnectionUrl(), applicationId);
                        HDInsightUtil.getSparkSubmissionToolWindowManager(project).setHyperLinkWithText("See detailed job information from ", applicationYarnUrl, applicationYarnUrl);

                        writeJobLogToLocal();
                    }

                    HDInsightUtil.getJobStatusManager(project).setJobRunningState(false);
                }
            }
        });
    }

    private void postEventAction() {
        postEventProperty.clear();
        postEventProperty.put("ClusterName", submissionParameter.getClusterName());
        if (submissionParameter.getArgs() != null && submissionParameter.getArgs().size() > 0) {
            postEventProperty.put("HasCommandLine", "true");
        } else {
            postEventProperty.put("HasCommandLine", "false");
        }

        if (submissionParameter.getReferencedFiles() != null && submissionParameter.getReferencedFiles().size() > 0) {
            postEventProperty.put("HasReferencedFile", "true");
        } else {
            postEventProperty.put("HasReferencedFile", "false");
        }

        if (submissionParameter.getReferencedJars() != null && submissionParameter.getReferencedJars().size() > 0) {
            postEventProperty.put("HasReferencedJar", "true");
        } else {
            postEventProperty.put("HasReferencedJar", "false");
        }
    }

    public Map<String, Object> getJobConfigMap() {
        return tableModel.getJobConfigMap();
    }

    private void initializeTableModel(final InteractiveTableModel tableModel) {
        if (submissionParameter == null) {
            for (int i = 0; i < SparkSubmissionParameter.defaultParameters.length; ++i) {
                tableModel.addRow(SparkSubmissionParameter.defaultParameters[i].first(), "");
            }
        } else {
            Map<String, Object> configs = submissionParameter.getJobConfig();
            for (int i = 0; i < SparkSubmissionParameter.parameterList.length; ++i) {
                tableModel.addRow(SparkSubmissionParameter.parameterList[i], configs.containsKey(SparkSubmissionParameter.parameterList[i]) ?
                        configs.get(SparkSubmissionParameter.parameterList[i]) : "");
            }
        }

        if (!tableModel.hasEmptyRow()) {
            tableModel.addEmptyRow();
        }
    }
}
