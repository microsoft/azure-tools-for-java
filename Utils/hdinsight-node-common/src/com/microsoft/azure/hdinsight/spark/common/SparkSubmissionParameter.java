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

package com.microsoft.azure.hdinsight.spark.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SparkSubmissionParameter implements IConvertible {
    /**
     * For interactive spark job:
     * <p>
     * kind                The session kind (required)    session kind
     * proxyUser            The user to impersonate that will run this session (e.g. bob)                    string
     * jars                Files to be placed on the java classpath                                        list of paths
     * pyFiles                Files to be placed on the PYTHONPATH                                            list of paths
     * files                Files to be placed in executor working directory                                list of paths
     * driverMemory        Memory for driver (e.g. 1000M, 2G)                                                string
     * driverCores            Number of cores used by driver (YARN mode only)                                    int
     * executorMemory        Memory for executor (e.g. 1000M, 2G)                                            string
     * executorCores        Number of cores used by executor                                                int
     * numExecutors        Number of executors (YARN mode only)                                            int
     * archives            Archives to be uncompressed in the executor working directory (YARN mode only)    list of paths
     * queue                The YARN queue to submit too (YARN mode only)    string
     * name                Name of the application    string
     * conf             Spark configuration properties  Map of key=val
     */
    private String name = "";
    private String file = "";
    private String className = "";

    private String clusterName = "";
    private boolean isLocalArtifact = false;
    private String artifactName = "";
    private String localArtifactPath = "";
    private List<String> files = new ArrayList<>();
    private List<String> jars = new ArrayList<>();
    private List<String> args = new ArrayList<>();
    private Map<String, Object> jobConfig = new HashMap<>();

    private static final Pattern memorySizeRegex = Pattern.compile(    "\\d+(.\\d+)?[gGmM]");

    public static final String DriverMemory = "driverMemory";
    public static final String DriverMemoryDefaultValue = "4G";

    public static final String DriverCores = "driverCores";
    public static final int DriverCoresDefaultValue = 1;

    public static final String ExecutorMemory = "executorMemory";
    public static final String ExecutorMemoryDefaultValue = "4G";

    public static final String NumExecutors = "numExecutors";
    public static final int NumExecutorsDefaultValue = 5;

    public static final String ExecutorCores = "executorCores";
    public static final int ExecutorCoresDefaultValue = 1;

    public static final String Conf = "conf";   //     Spark configuration properties

    public static final String NAME = "name";

    public SparkSubmissionParameter() {
    }

    public SparkSubmissionParameter(String clusterName,
                                    boolean isLocalArtifact,
                                    String artifactName,
                                    String localArtifactPath,
                                    String filePath,
                                    String className,
                                    List<String> referencedFiles,
                                    List<String> referencedJars,
                                    List<String> args,
                                    Map<String, Object> jobConfig) {
        this.clusterName = clusterName;
        this.isLocalArtifact = isLocalArtifact;
        this.artifactName = artifactName;
        this.localArtifactPath = localArtifactPath;
        this.file = filePath;
        this.className = className;
        this.files = referencedFiles;
        this.jars = referencedJars;
        this.jobConfig = jobConfig;
        this.args = args;
    }

    public static SparkSubmissionParameter copyOf(SparkSubmissionParameter parameter) {
        SparkSubmissionParameter copiedParameter = new SparkSubmissionParameter(
                parameter.getClusterName(),
                parameter.isLocalArtifact(),
                parameter.getArtifactName(),
                parameter.getLocalArtifactPath(),
                parameter.getFile(),
                parameter.getMainClassName(),
                new ArrayList<>(parameter.getReferencedFiles()),
                new ArrayList<>(parameter.getReferencedJars()),
                new ArrayList<>(parameter.getArgs()),
                new HashMap<>(parameter.getJobConfig()));
        copiedParameter.setName(parameter.getName());
        return copiedParameter;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setLocalArtifact(boolean localArtifact) {
        isLocalArtifact = localArtifact;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    @JsonIgnore
    public String getClusterName() {
        return clusterName;
    }

    @JsonIgnore
    public boolean isLocalArtifact() {
        return isLocalArtifact;
    }

    @JsonIgnore
    public String getArtifactName() {
        return artifactName;
    }

    @JsonIgnore
    public String getLocalArtifactPath() {
        return localArtifactPath;
    }

    public void setLocalArtifactPath(String path) {
        localArtifactPath = path;
    }

    @JsonProperty(NAME)
    public String getName() {
        return name;
    }

    @JsonProperty("file")
    public String getFile() {
        return file;
    }

    @JsonProperty("className")
    public String getMainClassName() {
        return className;
    }

    @JsonProperty("files")
    public List<String> getReferencedFiles() {
        return files;
    }

    public void setReferencedFiles(List<String> files) {
        this.files = files;
    }

    @JsonProperty("jars")
    public List<String> getReferencedJars() {
        return jars;
    }

    public void setReferencedJars(List<String> jars) {
        this.jars = jars;
    }

    @JsonProperty("args")
    public List<String> getArgs() {
        return args;
    }

    @JsonIgnore
    public Map<String, Object> getJobConfig() {
        return jobConfig;
    }

    public void setFilePath(String filePath) {
        this.file = filePath;
    }

    @JsonProperty("driverMemory")
    public String getDriverMemory() {
        return (String) jobConfig.get(DriverMemory);
    }

    @JsonProperty("driverCores")
    public Integer getDriverCores() {
        return parseIntegerSafety(jobConfig.get(DriverCores));
    }

    @JsonProperty("executorMemory")
    public String getExecutorMemory() {
        return (String) jobConfig.get(ExecutorMemory);
    }

    @JsonProperty("executorCores")
    public Integer getExecutorCores() {
        return parseIntegerSafety(jobConfig.get(ExecutorCores));
    }

    @JsonProperty("numExecutors")
    public Integer getNumExecutors() {
        return parseIntegerSafety(jobConfig.get(NumExecutors));
    }

    @JsonProperty("conf")
    public Map<String, String> getConf() {
        Map<String, String> jobConf = new HashMap<>();

        Optional.ofNullable(jobConfig.get(Conf))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .ifPresent(conf -> conf.forEach((k, v) -> jobConf.put((String) k, (String) v)));

        return jobConf.isEmpty() ? null : jobConf;
    }

    @JsonIgnore
    @Nullable
    private Integer parseIntegerSafety(@Nullable Object maybeInteger) {
        if (maybeInteger == null) {
            return null;
        }

        if (maybeInteger instanceof Integer) {
            return (Integer) maybeInteger;
        }

        try {
            return Integer.parseInt(maybeInteger.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    public static List<SparkSubmissionJobConfigCheckResult> checkJobConfigMap(Map<String, String> jobConfigMap) {

        List<SparkSubmissionJobConfigCheckResult> messageList = new ArrayList<>();
        for (Map.Entry<String, String> entry : jobConfigMap.entrySet()) {
            String entryKey = entry.getKey();
            if (StringUtils.isBlank(entryKey)) {
                continue;
            }

            if (StringUtils.containsWhitespace(entryKey)) {
                messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Error,
                        "Error : The Spark config key with whitespace is not allowed"));

                continue;
            }

            if (StringUtils.isBlank(entry.getValue())) {
                messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Warning,
                        "Warning : Empty value(s) will be override by default value(s) of system"));

                continue;
            }

            if (entryKey.equals(DriverCores)
                    || entryKey.equals(NumExecutors)
                    || entryKey.equals(ExecutorCores)) {
                try {
                    Integer.parseInt(entry.getValue());
                } catch (NumberFormatException e) {
                    messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Error,
                            String.format("Error : Key \"%s\" failed to parse the value \"%s\", it should be an integer", entry.getKey(), entry.getValue())));
                }
            } else if (entryKey.equals(DriverMemory)
                    || entryKey.equals(ExecutorMemory)) {
                if (!memorySizeRegex.matcher(entry.getValue()).matches()) {
                    messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Error,
                            String.format("Error : Key \"%s\" failed to parse the value \"%s\" into the memory size, it should be like 1.5G, 500M or 4g", entry.getKey(), entry.getValue())));
                }
            }
        }

        Collections.sort(messageList);

        return messageList;
    }

    @NotNull
    public List<Pair<String, String>> flatJobConfig() {
        List<Pair<String, String>> flattedConfigs = new ArrayList<>();

        getJobConfig().forEach((key, value) -> {
            if (isSubmissionParameter(key)) {
                flattedConfigs.add(new Pair<>(key, value == null ? null : value.toString()));
            } else if (key.equals(Conf)) {
                new SparkConfigures(value).forEach((scKey, scValue) ->
                        flattedConfigs.add(new Pair<>(scKey, scValue == null ? null : scValue.toString())));
            }
        });

        return flattedConfigs;
    }

    public void applyFlattedJobConf(List<Pair<String, String>> jobConfFlatted) {
        jobConfig.clear();

        SparkConfigures sparkConfig = new SparkConfigures();

        jobConfFlatted.forEach(kvPair -> {
            if (isSubmissionParameter(kvPair.first())) {
                jobConfig.put(kvPair.first(), kvPair.second());
            } else {
                sparkConfig.put(kvPair.first(), kvPair.second());
            }
        });

        if (!sparkConfig.isEmpty()) {
            jobConfig.put(Conf, sparkConfig);
        }
    }

    public String serializeToJson() {
        return convertToJson().orElse("");
    }

    public static final String[] parameterList = new String[]{SparkSubmissionParameter.DriverMemory, SparkSubmissionParameter.DriverCores,
            SparkSubmissionParameter.ExecutorMemory, SparkSubmissionParameter.ExecutorCores, SparkSubmissionParameter.NumExecutors};

    //the first value in pair should be in same order with parameterList
    public static final Pair<String, Object>[] defaultParameters = new Pair[]{
            new Pair<>(SparkSubmissionParameter.DriverMemory, SparkSubmissionParameter.DriverMemoryDefaultValue),
            new Pair<>(SparkSubmissionParameter.DriverCores, SparkSubmissionParameter.DriverCoresDefaultValue),
            new Pair<>(SparkSubmissionParameter.ExecutorMemory, SparkSubmissionParameter.ExecutorMemoryDefaultValue),
            new Pair<>(SparkSubmissionParameter.ExecutorCores, SparkSubmissionParameter.ExecutorCoresDefaultValue),
            new Pair<>(SparkSubmissionParameter.NumExecutors, SparkSubmissionParameter.NumExecutorsDefaultValue)
    };

    /**
     * Checks whether the key is one of Spark Job submission parameters or not
     *
     * @param key the key string to check
     * @return true if the key is a member of submission parameters; false otherwise
     */
    public static boolean isSubmissionParameter(String key) {
        return Arrays.stream(SparkSubmissionParameter.parameterList).anyMatch(key::equals);
    }
}
