/**
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
 *
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;

import java.util.List;
import java.util.Map;

/**
 * To leverage Livy(HDInsight) Spark Batch Job codes, some fields are commented
 * since they can be inherited from the base class SparkSubmissionParameter
 *
 * Parameters used to submit a new Data Lake Analytics spark batch job request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSparkBatchJobParameters extends SparkSubmissionParameter {
    public static final String DriverMemoryDefaultValue = "12G";

    public static final int DriverCoresDefaultValue = 4;

    public static final String ExecutorMemoryDefaultValue = "12G";

    public static final int ExecutorCoresDefaultValue = 4;

    @JsonIgnoreProperties
    private String adlAccountName;

    /**
     * ADLS directory path to store Spark events and logs.
     */
    @JsonProperty(value = "sparkEventsDirectoryPath", required = true)
    private String sparkEventsDirectoryPath;

    /**
     * File containing the application to run.
     */
//    @JsonProperty(value = "file", required = true)
//    private String file;

    /**
     * Application Java or Spark main class.
     */
//    @JsonProperty(value = "className", required = true)
//    private String className;

    /**
     * Command line arguments for the application.
     */
//    @JsonProperty(value = "args")
//    private List<String> args;

    /**
     * Jar files to be used in the session.
     */
//    @JsonProperty(value = "jars")
//    private List<String> jars;

    /**
     * Python files to be used in this session.
     */
//    @JsonProperty(value = "pyFiles")
//    private List<String> pyFiles;

    /**
     * Other files to be used in the session.
     */
//    @JsonProperty(value = "files")
//    private List<String> files;

    /**
     * Amount of memory to use for the driver process.
     */
//    @JsonProperty(value = "driverMemory", required = true)
//    private String driverMemory;

    /**
     * Number of Cores to use for the driver process.
     */
//    @JsonProperty(value = "driverCores", required = true)
//    private int driverCores;

    /**
     * Amount of memory to use for each executor process.
     */
//    @JsonProperty(value = "executorMemory", required = true)
//    private String executorMemory;

    /**
     * Number of cores to use for each executor.
     */
//    @JsonProperty(value = "executorCores", required = true)
//    private int executorCores;

    /**
     * Number of executors to launch for this session.
     */
//    @JsonProperty(value = "numExecutors", required = true)
//    private int numExecutors;

    /**
     * Archives to be used in this session.
     */
//    @JsonProperty(value = "archives")
//    private List<String> archives;

    /**
     * Name of the session.
     */
//    @JsonProperty(value = "name", required = true)
//    private String name;

    /**
     * Spark Configuration Properties.
     */
//    @JsonProperty(value = "conf")
//    private Map<String, String> conf;

    /**
     * Special properties that will allow choosing/targeting of features (runtime, gp version etc) on server side.
     */
    @JsonProperty(value = "extendedProperties")
    private Map<String, String> extendedProperties;

    public String adlAccountName() {
        return adlAccountName;
    }

    public CreateSparkBatchJobParameters withAdlAccountName(String adlAccountName) {
        this.adlAccountName = adlAccountName;
        return this;
    }

    public CreateSparkBatchJobParameters() {
    }

    public CreateSparkBatchJobParameters(String clusterName,
                                         boolean isLocalArtifact,
                                         String artifactName,
                                         String localArtifactPath,
                                         String filePath,
                                         String className,
                                         List<String> referencedFiles,
                                         List<String> referencedJars,
                                         List<String> args,
                                         Map<String, Object> jobConfig,
                                         String adlAccountName,
                                         String sparkEventsDirectoryPath,
                                         Map<String, String> extendedProperties) {
        super(clusterName, isLocalArtifact, artifactName, localArtifactPath, filePath, className, referencedFiles,
                referencedJars, args, jobConfig);
        this.adlAccountName = adlAccountName;
        this.sparkEventsDirectoryPath = sparkEventsDirectoryPath;
        this.extendedProperties = extendedProperties;
    }

    public static CreateSparkBatchJobParameters copyOf(CreateSparkBatchJobParameters parameter) {
        CreateSparkBatchJobParameters copiedParameter = new CreateSparkBatchJobParameters(
                parameter.getClusterName(),
                parameter.isLocalArtifact(),
                parameter.getArtifactName(),
                parameter.getLocalArtifactPath(),
                parameter.getFile(),
                parameter.getMainClassName(),
                ImmutableList.copyOf(parameter.getReferencedFiles()),
                ImmutableList.copyOf(parameter.getReferencedJars()),
                ImmutableList.copyOf(parameter.getArgs()),
                ImmutableMap.copyOf(parameter.getJobConfig()),
                parameter.adlAccountName(),
                parameter.sparkEventsDirectoryPath(),
                ImmutableMap.copyOf(parameter.extendedProperties()));
        copiedParameter.setName(parameter.getName());
        return copiedParameter;
    }

    /**
     * Get aDLS directory path to store Spark events and logs.
     *
     * @return the sparkEventsDirectoryPath value
     */
    public String sparkEventsDirectoryPath() {
        return this.sparkEventsDirectoryPath;
    }

    /**
     * Set aDLS directory path to store Spark events and logs.
     *
     * @param sparkEventsDirectoryPath the sparkEventsDirectoryPath value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
    public CreateSparkBatchJobParameters withSparkEventsDirectoryPath(String sparkEventsDirectoryPath) {
        this.sparkEventsDirectoryPath = sparkEventsDirectoryPath;
        return this;
    }

    /**
     * Get file containing the application to run.
     *
     * @return the file value
     */
//    public String file() {
//        return this.file;
//    }

    /**
     * Set file containing the application to run.
     *
     * @param file the file value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withFile(String file) {
//        this.file = file;
//        return this;
//    }

    /**
     * Get application Java or Spark main class.
     *
     * @return the className value
     */
//    public String className() {
//        return this.className;
//    }

    /**
     * Set application Java or Spark main class.
     *
     * @param className the className value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withClassName(String className) {
//        this.className = className;
//        return this;
//    }

    /**
     * Get command line arguments for the application.
     *
     * @return the args value
     */
//    public List<String> args() {
//        return this.args;
//    }

    /**
     * Set command line arguments for the application.
     *
     * @param args the args value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withArgs(List<String> args) {
//        this.args = args;
//        return this;
//    }

    /**
     * Get jar files to be used in the session.
     *
     * @return the jars value
     */
//    public List<String> jars() {
//        return this.jars;
//    }

    /**
     * Set jar files to be used in the session.
     *
     * @param jars the jars value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withJars(List<String> jars) {
//        this.jars = jars;
//        return this;
//    }

    /**
     * Get python files to be used in this session.
     *
     * @return the pyFiles value
     */
//    public List<String> pyFiles() {
//        return this.pyFiles;
//    }

    /**
     * Set python files to be used in this session.
     *
     * @param pyFiles the pyFiles value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withPyFiles(List<String> pyFiles) {
//        this.pyFiles = pyFiles;
//        return this;
//    }

    /**
     * Get other files to be used in the session.
     *
     * @return the files value
     */
//    public List<String> files() {
//        return this.files;
//    }

    /**
     * Set other files to be used in the session.
     *
     * @param files the files value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withFiles(List<String> files) {
//        this.files = files;
//        return this;
//    }

    /**
     * Get amount of memory to use for the driver process.
     *
     * @return the driverMemory value
     */
//    public String driverMemory() {
//        return this.driverMemory;
//    }

    /**
     * Set amount of memory to use for the driver process.
     *
     * @param driverMemory the driverMemory value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withDriverMemory(String driverMemory) {
//        this.driverMemory = driverMemory;
//        return this;
//    }

    /**
     * Get number of Cores to use for the driver process.
     *
     * @return the driverCores value
     */
//    public int driverCores() {
//        return this.driverCores;
//    }

    /**
     * Set number of Cores to use for the driver process.
     *
     * @param driverCores the driverCores value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withDriverCores(int driverCores) {
//        this.driverCores = driverCores;
//        return this;
//    }

    /**
     * Get amount of memory to use for each executor process.
     *
     * @return the executorMemory value
     */
//    public String executorMemory() {
//        return this.executorMemory;
//    }

    /**
     * Set amount of memory to use for each executor process.
     *
     * @param executorMemory the executorMemory value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withExecutorMemory(String executorMemory) {
//        this.executorMemory = executorMemory;
//        return this;
//    }

    /**
     * Get number of cores to use for each executor.
     *
     * @return the executorCores value
     */
//    public int executorCores() {
//        return this.executorCores;
//    }

    /**
     * Set number of cores to use for each executor.
     *
     * @param executorCores the executorCores value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withExecutorCores(int executorCores) {
//        this.executorCores = executorCores;
//        return this;
//    }

    /**
     * Get number of executors to launch for this session.
     *
     * @return the numExecutors value
     */
//    public int numExecutors() {
//        return this.numExecutors;
//    }

    /**
     * Set number of executors to launch for this session.
     *
     * @param numExecutors the numExecutors value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withNumExecutors(int numExecutors) {
//        this.numExecutors = numExecutors;
//        return this;
//    }

    /**
     * Get archives to be used in this session.
     *
     * @return the archives value
     */
//    public List<String> archives() {
//        return this.archives;
//    }

    /**
     * Set archives to be used in this session.
     *
     * @param archives the archives value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withArchives(List<String> archives) {
//        this.archives = archives;
//        return this;
//    }

    /**
     * Get name of the session.
     *
     * @return the name value
     */
//    public String name() {
//        return this.name;
//    }

    /**
     * Set name of the session.
     *
     * @param name the name value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withName(String name) {
//        this.name = name;
//        return this;
//    }

    /**
     * Get spark Configuration Properties.
     *
     * @return the conf value
     */
//    public Map<String, String> conf() {
//        return this.conf;
//    }

    /**
     * Set spark Configuration Properties.
     *
     * @param conf the conf value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
//    public CreateSparkBatchJobParameters withConf(Map<String, String> conf) {
//        this.conf = conf;
//        return this;
//    }

    /**
     * Get special properties that will allow choosing/targeting of features (runtime, gp version etc) on server side.
     *
     * @return the extendedProperties value
     */
    public Map<String, String> extendedProperties() {
        return this.extendedProperties;
    }

    /**
     * Set special properties that will allow choosing/targeting of features (runtime, gp version etc) on server side.
     *
     * @param extendedProperties the extendedProperties value to set
     * @return the CreateSparkBatchJobParameters object itself.
     */
    public CreateSparkBatchJobParameters withExtendedProperties(Map<String, String> extendedProperties) {
        this.extendedProperties = extendedProperties;
        return this;
    }
}
