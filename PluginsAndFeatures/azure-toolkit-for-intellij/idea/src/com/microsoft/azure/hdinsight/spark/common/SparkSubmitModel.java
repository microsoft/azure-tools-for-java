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

import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.*;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.Pair;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Tag(SparkSubmitModel.SUBMISSION_CONTENT_NAME)
public class SparkSubmitModel {
    static final String SUBMISSION_CONTENT_NAME = "spark_submission";
    private static final String SUBMISSION_CONTENT_JOB_CONF = "job_conf";

    @Transient
    @NotNull
    private Project project;

    @Transient
    @NotNull
    private SparkSubmissionParameter submissionParameter; // The parameters to packing submission related various

    @Nullable
    private String clusterMappedId;

    @NotNull
    @Property(surroundWithTag = false)
    final private SparkSubmitAdvancedConfigModel advancedConfigModel;

    @NotNull
    @Property(surroundWithTag = false)
    final private SparkSubmitJobUploadStorageModel jobUploadStorageModel;

    @Transient
    private boolean isClusterSelectable = true;

    @Transient
    private boolean isClustersRefreshable = true;

    @Transient
    @NotNull
    private SubmissionTableModel tableModel = new SubmissionTableModel();

    public SparkSubmitModel() {
        this(DummyProject.getInstance());
    }

    public SparkSubmitModel(@NotNull Project project) {
        this(project, new SparkSubmissionParameter());
    }

    public SparkSubmitModel(@NotNull Project project, @NotNull SparkSubmissionParameter submissionParameter) {
        this(project, submissionParameter, new SparkSubmitAdvancedConfigModel(), new SparkSubmitJobUploadStorageModel());
    }

    public SparkSubmitModel(@NotNull Project project, @NotNull SparkSubmissionParameter submissionParameter,
                            @NotNull SparkSubmitAdvancedConfigModel advancedConfigModel,
                            @NotNull SparkSubmitJobUploadStorageModel jobUploadStorageModel) {
        this.project = project;
        this.advancedConfigModel = advancedConfigModel;
        this.jobUploadStorageModel = jobUploadStorageModel;
        this.submissionParameter = submissionParameter;

        setTableModel(new SubmissionTableModel(submissionParameter.flatJobConfig()));
    }

    @Transient
    @NotNull
    public SparkSubmissionParameter getSubmissionParameter() {
        return submissionParameter;
    }

    @NotNull
    public SparkSubmitAdvancedConfigModel getAdvancedConfigModel() { return advancedConfigModel; }

    @NotNull
    public SparkSubmitJobUploadStorageModel getJobUploadStorageModel() {
        return jobUploadStorageModel;
    }

    @Transient
    public boolean isClusterSelectable() {
        return isClusterSelectable;
    }

    @Transient
    public void setClusterSelectable(boolean clusterSelectable) {
        isClusterSelectable = clusterSelectable;
    }

    @Transient
    public boolean isClustersRefreshable() {
        return isClustersRefreshable;
    }

    @Transient
    public void setClustersRefreshable(boolean clustersRefreshable) {
        isClustersRefreshable = clustersRefreshable;
    }

    @Attribute("job_name")
    public String getJobName() {
        return getSubmissionParameter().getName();
    }

    @Attribute("job_name")
    public void setJobName(String jobName) {
        getSubmissionParameter().setName(jobName);
    }

    @Attribute("cluster_name")
    public String getClusterName() {
        return getSubmissionParameter().getClusterName();
    }

    @Attribute("cluster_name")
    public void setClusterName(String clusterName) {
        getSubmissionParameter().setClusterName(clusterName);
    }

    @Attribute("cluster_mapped_id")
    public String getClusterMappedId() {
        return clusterMappedId;
    }

    @Attribute("cluster_mapped_id")
    public void setClusterMappedId(String clusterMappedId) {
        this.clusterMappedId = clusterMappedId;
    }

    @Attribute("is_local_artifact")
    public boolean getIsLocalArtifact() {
        return getSubmissionParameter().isLocalArtifact();
    }

    @Attribute("is_local_artifact")
    public void setIsLocalArtifact(boolean isLocalArtifact) {
        getSubmissionParameter().setLocalArtifact(isLocalArtifact);
    }

    @Attribute("artifact_name")
    public String getArtifactName() {
        return getSubmissionParameter().getArtifactName();
    }

    @Attribute("artifact_name")
    public void setArtifactName(String artifactName) {
        getSubmissionParameter().setArtifactName(artifactName);
    }

    @Attribute("local_artifact_path")
    public String getLocalArtifactPath() {
        return getSubmissionParameter().getLocalArtifactPath();
    }

    @Attribute("local_artifact_path")
    public void setLocalArtifactPath(String localArtifactPath) {
        getSubmissionParameter().setLocalArtifactPath(localArtifactPath);
    }
    @Attribute("file_path")
    public String getFilePath() {
        return getSubmissionParameter().getFile();
    }

    @Attribute("file_path")
    public void setFilePath(String filePath) {
        getSubmissionParameter().setFilePath(filePath);
    }

    @Attribute("classname")
    public String getMainClassName() {
        return getSubmissionParameter().getMainClassName();
    }

    @Attribute("classname")
    public void setMainClassName(String mainClassName) {
        getSubmissionParameter().setClassName(mainClassName);
    }

    @Tag("cmd_line_args")
    @XCollection(style = XCollection.Style.v2)
    public List<String> getCommandLineArgs() {
        return getSubmissionParameter().getArgs();
    }

    @Tag("cmd_line_args")
    @XCollection(style = XCollection.Style.v2)
    public void setCommandLineArgs(List<String> cmdArgs) {
        if (cmdArgs == getCommandLineArgs()) {
            return;
        }

        getSubmissionParameter().getArgs().clear();
        getSubmissionParameter().getArgs().addAll(cmdArgs);
    }

    @Tag("ref_jars")
    @XCollection(style = XCollection.Style.v2)
    public List<String> getReferenceJars() {
        return getSubmissionParameter().getReferencedJars();
    }

    @Tag("ref_jars")
    @XCollection(style = XCollection.Style.v2)
    public void setReferenceJars(List<String> refJars) {
        if (refJars == getReferenceJars()) {
            return;
        }

        getSubmissionParameter().getReferencedJars().clear();
        getSubmissionParameter().getReferencedJars().addAll(refJars);
    }

    @Tag("ref_files")
    @XCollection(style = XCollection.Style.v2)
    public List<String> getReferenceFiles() {
        return getSubmissionParameter().getReferencedFiles();
    }

    @Tag("ref_files")
    @XCollection(style = XCollection.Style.v2)
    public void setReferenceFiles(List<String> refFiles) {
        if (refFiles == getReferenceFiles()) {
            return;
        }

        getSubmissionParameter().getReferencedFiles().clear();
        getSubmissionParameter().getReferencedFiles().addAll(refFiles);
    }

    @XCollection(style = XCollection.Style.v2)
    public List<String[]> getJobConfigs() {
        return getTableModel()
                .getJobConfigMap()
                .stream()
                .map(p -> new String[] { p.first(), p.second() } )
                .collect(Collectors.toList());
    }

    @XCollection(style = XCollection.Style.v2)
    public void setJobConfigs(List<String[]> jobConf) {
        setTableModel(new SubmissionTableModel(
                jobConf.stream()
                       .map(kv -> new Pair<>(kv[0], kv[1]))
                       .collect(Collectors.toList())));
    }

    @Transient
    @NotNull
    public Project getProject() {
        return project;
    }

    @Transient
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Transient
    public SubmissionTableModel getTableModel() {
        return tableModel;
    }

    @Transient
    public synchronized void setTableModel(@NotNull SubmissionTableModel tableModel) {
        // Apply from table model
        getSubmissionParameter().applyFlattedJobConf(tableModel.getJobConfigMap());

        this.tableModel = tableModel;
    }

    @Transient
    public Optional<String> getArtifactPath() {
        return getIsLocalArtifact() ?
                Optional.ofNullable(getLocalArtifactPath()) :
                Optional.ofNullable(getArtifactName())
                        .map(name -> ArtifactManager.getInstance(project).findArtifact(name))
                        .map(Artifact::getOutputFilePath);
    }

    @NotNull
    @Transient
    public Stream<Pair<String, ? extends Object>> getDefaultParameters() {
        return Arrays.stream(SparkSubmissionParameter.defaultParameters);
    }

    public Element exportToElement() throws WriteExternalException {
        try {
            return XmlSerializer.serialize(this);
        } catch (Exception ex) {
            throw new WriteExternalException("Can't export Spark submit model to XML element", ex);
        }
    }

    public SparkSubmitModel applyFromElement(@NotNull Element rootElement) throws InvalidDataException{
        try {
            XmlSerializer.deserializeInto(this, rootElement);
        } catch (Exception ex) {
            throw new InvalidDataException("Configuration is broken or not compatible", ex);
        }

        return this;
    }

    @NotNull
    @Transient
    public String getSparkClusterTypeDisplayName() {
        return "HDInsight cluster";
    }
}
