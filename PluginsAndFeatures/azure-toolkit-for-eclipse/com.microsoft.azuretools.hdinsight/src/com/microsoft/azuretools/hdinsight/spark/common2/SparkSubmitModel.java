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
package com.microsoft.azuretools.hdinsight.spark.common2;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.internal.ui.jarpackager.JarFileExportOperation;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.swt.widgets.Display;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitResponse;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.hdinsight.SparkSubmissionToolWindowView;
import com.microsoft.azuretools.hdinsight.common2.HDInsightUtil;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class SparkSubmitModel {
    // private static final String[] columns = {"Key", "Value", ""};
    private  static final String SparkYarnLogUrlFormat = "%s/yarnui/hn/cluster/app/%s";

    private List<IClusterDetail> cachedClusterDetails;

    private SparkSubmissionParameter submissionParameter;

    private Map<String, String> postEventProperty = new HashMap<>();

    public SparkSubmitModel(@NotNull List<IClusterDetail> cachedClusterDetails) {
        this.cachedClusterDetails = cachedClusterDetails;
    }

    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }


    public boolean isLocalArtifact() {
        return submissionParameter.isLocalArtifact();
    }

    public void action(@NotNull SparkSubmissionParameter submissionParameter) {
        this.submissionParameter = submissionParameter;

        if (isLocalArtifact()) {
            submit();
        } else {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            IProject proj = root.getProject(submissionParameter.getArtifactName());
            JarExportJob job = new JarExportJob("Building jar", proj);
            job.schedule();
            job.addJobChangeListener(new JobChangeAdapter() {
                public void done(IJobChangeEvent event) {
                    if (event.getResult().isOK()) {
                        submit();
                    }
                };
            });
        }
    }

    private class JarExportJob extends Job {
        private IProject project;
        private String errorMessage;

        public JarExportJob(String name, IProject project) {
            super(name);
            this.project = project;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            monitor.beginTask("Begin building task", IProgressMonitor.UNKNOWN);
            try {
                // TODO： IncrementalProjectBuilder
                // build will new thread to run and we have no better way to get status of the thread
                //project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);

                super.setName("");
                final JarPackageData jarPackageData = new JarPackageData();
                jarPackageData.setElements(new Object[]{project});

                jarPackageData.setExportClassFiles(true);
                jarPackageData.setBuildIfNeeded(true);
                jarPackageData.setExportOutputFolders(true);
                jarPackageData.setExportJavaFiles(false);
                jarPackageData.setExportErrors(false);
                jarPackageData.setExportWarnings(false);
                jarPackageData.setRefactoringAware(false);
                jarPackageData.setCompress(true);
                jarPackageData.setIncludeDirectoryEntries(false);
                jarPackageData.setOverwrite(true);
                String dest = String.format("%s%s%s%s", project.getLocation(), File.separator, project.getName(), ".jar");
                IPath destPath = new Path(dest);

                jarPackageData.setJarLocation(destPath);
                monitor.worked(5);
                monitor.setTaskName("Creating Jar file...");
                Display.getDefault().syncExec(new Runnable() {

                    @SuppressWarnings("restriction")
                    @Override
                    public void run() {
                        try {
                            JarFileExportOperation jarFileExportOperation = new JarFileExportOperation(jarPackageData, Display.getDefault().getActiveShell());
                            jarFileExportOperation.run(SubMonitor.convert(monitor,"Compiler Spark project", 100));
                        } catch (InvocationTargetException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                });
            } catch (Exception ex) {
                errorMessage = ex.getMessage();
                return Status.CANCEL_STATUS;
            } finally {
                monitor.done();
            }
            if (monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            } else {
                return Status.OK_STATUS;
            }
        }

    }

    private void uploadFileToCluster(@NotNull final IClusterDetail selectedClusterDetail, @NotNull final String selectedArtifactName) throws Exception {
        String buildJarPath;
        if (submissionParameter.isLocalArtifact()) {
            buildJarPath = submissionParameter.getLocalArtifactPath();
        } else {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject proj = root.getProject(submissionParameter.getArtifactName());
            buildJarPath = String.format("%s%s%s%s", proj.getLocation(), File.separator, proj.getName(), ".jar");

        }

        String filePath = selectedClusterDetail.isEmulator() ?
                SparkSubmitHelper.uploadFileToEmulator(selectedClusterDetail, buildJarPath) :
                (selectedClusterDetail.getStorageAccount() == null ? SparkSubmitHelper.uploadFileToHDFS(selectedClusterDetail, buildJarPath) :
                    JobUtils.uploadFileToAzure(new File(buildJarPath), selectedClusterDetail.getStorageAccount(), selectedClusterDetail.getStorageAccount().getDefaultContainerOrRootPath(), JobUtils.getFormatPathByDate(), HDInsightUtil.getToolWindowMessageSubject(), null));
        submissionParameter.setFilePath(filePath);
    }


    private void tryToCreateBatchSparkJob(@NotNull final IClusterDetail selectedClusterDetail) throws HDIException,IOException {
        SparkBatchSubmission.getInstance().setUsernamePasswordCredential(selectedClusterDetail.getHttpUserName(), selectedClusterDetail.getHttpPassword());
        HttpResponse response = SparkBatchSubmission.getInstance().createBatchSparkJob(SparkSubmitHelper.getLivyConnectionURL(selectedClusterDetail), submissionParameter);

        if (response.getCode() == 201 || response.getCode() == 200) {
            HDInsightUtil.showInfoOnSubmissionMessageWindow("Info : Submit to spark cluster successfully.");
            postEventProperty.put("IsSubmitSucceed", "true");

            String jobLink = String.format("%s/sparkhistory", selectedClusterDetail.getConnectionUrl());

            HDInsightUtil.setHyperLinkWithText("See spark job view from ", jobLink, jobLink);
            @SuppressWarnings("serial")
            final SparkSubmitResponse sparkSubmitResponse = new Gson().fromJson(response.getMessage(), new TypeToken<SparkSubmitResponse>() {
            }.getType());

            // Set submitted spark application id and http request info for stopping running application
              Display.getDefault().syncExec(new Runnable() {
                  @Override
                  public void run() {
                      SparkSubmissionToolWindowView view = HDInsightUtil.getSparkSubmissionToolWindowView();
                      view.setSparkApplicationStopInfo(selectedClusterDetail.getConnectionUrl(), sparkSubmitResponse.getId());
                      view.setStopButtonState(true);
                      view.getJobStatusManager().resetJobStateManager();
                  }
              });
              SparkSubmitHelper.getInstance().printRunningLogStreamingly(sparkSubmitResponse.getId(), selectedClusterDetail, postEventProperty);
        } else {
            HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(
                    String.format("Error : Failed to submit to spark cluster. error code : %d, reason :  %s.", response.getCode(), response.getContent()));
            postEventProperty.put("IsSubmitSucceed", "false");
            postEventProperty.put("SubmitFailedReason", response.getContent());
            AppInsightsClient.create(Messages.SparkSubmissionButtonClickEvent, null, postEventProperty);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionButtonClickEvent, null);
        }
    }

    private void showFailedSubmitErrorMessage(Exception exception) {
        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error : Failed to submit application to spark cluster. Exception : " + exception.getMessage());
        postEventProperty.put("IsSubmitSucceed", "false");
        postEventProperty.put("SubmitFailedReason", exception.toString());
        AppInsightsClient.create(Messages.SparkSubmissionButtonClickEvent, null, postEventProperty);
        EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkSubmissionButtonClickEvent, null);
    }

    private void writeJobLogToLocal() {
        String path = null;
        try {
            path = SparkSubmitHelper.getInstance().writeLogToLocalFile(/*project*/);
        } catch (IOException e) {
            HDInsightUtil.showErrorMessageOnSubmissionMessageWindow(e.getMessage());
        }

        if (!StringHelper.isNullOrWhiteSpace(path)) {
            String urlPath = StringHelper.concat("file:", path);
            HDInsightUtil.setHyperLinkWithText("See detailed job log from local:", urlPath, path);
        }
    }

    private IClusterDetail getClusterConfiguration(@NotNull final IClusterDetail selectedClusterDetail,
                                                   final boolean isFirstSubmit) {
        try {
            if (!selectedClusterDetail.isConfigInfoAvailable()) {
                selectedClusterDetail.getConfigurationInfo();
            }
        } catch (AuthenticationException authenticationException) {
            if (isFirstSubmit) {
                HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error: Cluster Credentials Expired, Please sign in again...");
                cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetails();

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
        postEventAction();
        DefaultLoader.getIdeHelper().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                // Validate the selected cluster
                String selectedClusterName = submissionParameter.getClusterName();
                IClusterDetail selectedClusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(selectedClusterName).orElse(null);
                if (selectedClusterDetail == null) {
                    HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Selected Cluster can not found. "
                            + "Please login in first in HDInsight Explorer and try submit job again");
                    return;
                } else if (ClusterManagerEx.getInstance().isHdiReaderCluster(selectedClusterDetail)) {
                    if (((ClusterDetail)selectedClusterDetail).getHttpUserName() == null
                            || ((ClusterDetail)selectedClusterDetail).getHttpPassword() == null) {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("No Ambari permission to submit job to the selected cluster.");
                        return;
                    }
                } else {
                    //may get a new clusterDetail reference if cluster credentials expired
                    selectedClusterDetail = getClusterConfiguration(selectedClusterDetail, true);
                    if (selectedClusterDetail == null) {
                        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Selected Cluster can not found. "
                                + "Please login in first in HDInsight Explorer and try submit job again");
                        return;
                    }
                }

                String selectedArtifactName = submissionParameter.getArtifactName();

                try {
                    uploadFileToCluster(selectedClusterDetail, selectedArtifactName);
                    tryToCreateBatchSparkJob(selectedClusterDetail);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    showFailedSubmitErrorMessage(exception);
                } finally {
                    HDInsightUtil.getSparkSubmissionToolWindowView().setStopButtonState(false);
                    HDInsightUtil.getSparkSubmissionToolWindowView().setBrowserButtonState(false);

                    if (HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().isApplicationGenerated()) {
                        String applicationId = HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().getApplicationId();

                        // ApplicationYarnUrl example : https://sparklivylogtest.azurehdinsight.net/yarnui/hn/cluster/app/application_01_111
                        String applicationYarnUrl = String.format(SparkYarnLogUrlFormat, selectedClusterDetail.getConnectionUrl(), applicationId);
                        HDInsightUtil.setHyperLinkWithText("See detailed job information from ", applicationYarnUrl, applicationYarnUrl);

                        writeJobLogToLocal();
                    }
                    HDInsightUtil.getSparkSubmissionToolWindowView().getJobStatusManager().setJobRunningState(false);
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
}
