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
package com.microsoft.azuretools.hdinsight.spark.common2;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitResponse;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.hdinsight.SparkSubmissionToolWindowView;
import com.microsoft.azuretools.hdinsight.common2.HDInsightUtil;
import com.microsoft.azuretools.hdinsight.util.Messages;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.internal.ui.jarpackager.JarFileExportOperation;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class SparkSubmitModel {
    private static final String[] columns = {"Key", "Value", ""};
    private  static final String SparkYarnLogUrlFormat = "%s/yarnui/hn/cluster/app/%s";

//    private static Map<Project, SparkSubmissionParameter> submissionParameterMap = new HashMap<>();

//    private Project project;
    private List<IClusterDetail> cachedClusterDetails;

    private Map<String, IClusterDetail> mapClusterNameToClusterDetail = new HashMap<>();
//    private Map<String, Artifact> artifactHashMap = new HashMap<>();

    private SparkSubmissionParameter submissionParameter;

//    private DefaultComboBoxModel<String> clusterComboBoxModel;
//    private DefaultComboBoxModel<String> artifactComboBoxModel;
//    private SubmissionTableModel tableModel = new SubmissionTableModel(columns);
    private Map<String, String> postEventProperty = new HashMap<>();

    public SparkSubmitModel(/*@NotNull IProject project,*/ @NotNull List<IClusterDetail> cachedClusterDetails) {
        this.cachedClusterDetails = cachedClusterDetails;
        
        
//
//        this.clusterComboBoxModel = new DefaultComboBoxModel<>();
//        this.artifactComboBoxModel = new DefaultComboBoxModel<>();
//        this.submissionParameter = submissionParameterMap.get(project);
//
        setClusterDetailsMap(cachedClusterDetails);
//        int index = submissionParameter != null ? clusterComboBoxModel.getIndexOf(submissionParameter.getClusterName()) : -1;
//        if (index != -1) {
//            clusterComboBoxModel.setSelectedItem(submissionParameter.getClusterName());
//        }
//
//        final List<Artifact> artifacts = ArtifactUtil.getArtifactWithOutputPaths(project);
//
//        for (Artifact artifact : artifacts) {
//            artifactHashMap.put(artifact.getName(), artifact);
//            artifactComboBoxModel.addElement(artifact.getName());
//            if (artifactComboBoxModel.getSize() == 0) {
//                artifactComboBoxModel.setSelectedItem(artifact.getName());
//            }
//        }
//
//        index = submissionParameter != null ? artifactComboBoxModel.getIndexOf(submissionParameter.getArtifactName()) : -1;
//        if (index != -1) {
//            artifactComboBoxModel.setSelectedItem(submissionParameter.getArtifactName());
//        }
//
//        initializeTableModel(tableModel);
    }

    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }
//
//    @NotNull
//    public IClusterDetail getSelectedClusterDetail() {
//        return mapClusterNameToClusterDetail.get((String) clusterComboBoxModel.getSelectedItem());
//    }

    public boolean isLocalArtifact() {
        return submissionParameter.isLocalArtifact();
    }

    public void setClusterDetailsMap(List<IClusterDetail> cachedClusterDetails) {
        mapClusterNameToClusterDetail.clear();

        for (IClusterDetail clusterDetail : cachedClusterDetails) {
            mapClusterNameToClusterDetail.put(clusterDetail.getName(), clusterDetail);
        }
    }
    public void action(@NotNull SparkSubmissionParameter submissionParameter) {
//        PluginUtil.getJobStatusManager(project).setJobRunningState(true);
        this.submissionParameter = submissionParameter;
//        submissionParameterMap.put(project, submissionParameter);
//        postEventAction();

        
        
        
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
        	

//           
//        	
//        	
////            List<Artifact> artifacts = new ArrayList<>();
//            final Artifact artifact = artifactHashMap.get(submissionParameter.getArtifactName());
////            artifacts.add(artifact);
////            ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);
//
////            final CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, true);
//
//            
//            
////            CompilerManager.getInstance(project).make(scope, new CompileStatusNotification() {
////                @Override
////                public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
////                    if (aborted || errors != 0) {
////                        postEventProperty.put("IsSubmitSucceed", "false");
////                        postEventProperty.put("SubmitFailedReason", "CompileFailed");
////                        TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);
////                        PluginUtil.getJobStatusManager(project).setJobRunningState(false);
////                        return;
////                    } else {
////                        CompilerManager.getInstance(project).make(new CompileStatusNotification() {
////                            @Override
////                            public void finished(boolean aborted1, int errors1, int warnings1, CompileContext compileContext1) {
////                                PluginUtil.showInfoOnSubmissionMessageWindow(project, String.format("Info : Build %s successfully.", artifact.getOutputFile()));
////                                submit();
////                            }
////                        });
////                    }
////                }
////            });
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
				// build will new thread to run and we have no better way to get status of the thread
				// project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
				
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
					
					@Override
					public void run() {
						try {
							new JarFileExportOperation(jarPackageData, Display.getDefault().getActiveShell())
							.run(new SubProgressMonitor(monitor, 100, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
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
                SparkSubmitHelper.uploadFileToHDFS(selectedClusterDetail, buildJarPath);
        submissionParameter.setFilePath(filePath);
    }
    
    
    private void tryToCreateBatchSparkJob(@NotNull final IClusterDetail selectedClusterDetail) throws HDIException,IOException {
        SparkBatchSubmission.getInstance().setCredentialsProvider(selectedClusterDetail.getHttpUserName(), selectedClusterDetail.getHttpPassword());
        HttpResponse response = SparkBatchSubmission.getInstance().createBatchSparkJob(SparkSubmitHelper.getLivyConnectionURL(selectedClusterDetail), submissionParameter);

        if (response.getCode() == 201 || response.getCode() == 200) {
            HDInsightUtil.showInfoOnSubmissionMessageWindow("Info : Submit to spark cluster successfully.");
            postEventProperty.put("IsSubmitSucceed", "true");

            String jobLink = String.format("%s/sparkhistory", selectedClusterDetail.getConnectionUrl());
            
            HDInsightUtil.setHyperLinkWithText("See spark job view from ", jobLink, jobLink);
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
        }
    }

    private void showFailedSubmitErrorMessage(Exception exception) {
        HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error : Failed to submit application to spark cluster. Exception : " + exception.getMessage());
        postEventProperty.put("IsSubmitSucceed", "false");
        postEventProperty.put("SubmitFailedReason", exception.toString());
        AppInsightsClient.create(Messages.SparkSubmissionButtonClickEvent, null, postEventProperty);
//        TelemetryManager.postEvent(TelemetryCommon.SparkSubmissionButtonClickEvent, postEventProperty, null);
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
//
    private IClusterDetail getClusterConfiguration(@NotNull final IClusterDetail selectedClusterDetail, @NotNull final boolean isFirstSubmit) {
        try {
            if (!selectedClusterDetail.isConfigInfoAvailable()) {
                selectedClusterDetail.getConfigurationInfo(null);
            }
        } catch (AuthenticationException authenticationException) {
            if (isFirstSubmit) {
                HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Error: Cluster Credentials Expired, Please sign in again...");
                //get new credentials by call getClusterDetails
                cachedClusterDetails = ClusterManagerEx.getInstance().getClusterDetails(null);

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
                IClusterDetail selectedClusterDetail = mapClusterNameToClusterDetail.get(submissionParameter.getClusterName());
                String selectedArtifactName = submissionParameter.getArtifactName();

                //may get a new clusterDetail reference if cluster credentials expired
                selectedClusterDetail = getClusterConfiguration(selectedClusterDetail, true);

                if (selectedClusterDetail == null) {
                    HDInsightUtil.showErrorMessageOnSubmissionMessageWindow("Selected Cluster can not found. Please login in first in HDInsight Explorer and try submit job again");
                    return;
                }

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
//
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
//
//    public Map<String, Object> getJobConfigMap() {
//        return tableModel.getJobConfigMap();
//    }
//
//    private void initializeTableModel(final InteractiveTableModel tableModel) {
//        if (submissionParameter == null) {
//            for (int i = 0; i < SparkSubmissionParameter.defaultParameters.length; ++i) {
//                tableModel.addRow(SparkSubmissionParameter.defaultParameters[i].getFirst(), "");
//            }
//        } else {
//            Map<String, Object> configs = submissionParameter.getJobConfig();
//            for (int i = 0; i < SparkSubmissionParameter.parameterList.length; ++i) {
//                tableModel.addRow(SparkSubmissionParameter.parameterList[i], configs.containsKey(SparkSubmissionParameter.parameterList[i]) ?
//                        configs.get(SparkSubmissionParameter.parameterList[i]) : "");
//            }
//        }
//
//        if (!tableModel.hasEmptyRow()) {
//            tableModel.addEmptyRow();
//        }
//    }
}
