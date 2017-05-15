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

import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.utils.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SparkSubmissionParameter {
    /**
     * For interactive spark job:
     * <p>
     * kind	            The session kind (required)	session kind
     * proxyUser	        The user to impersonate that will run this session (e.g. bob)	                string
     * jars	            Files to be placed on the java classpath	                                    list of paths
     * pyFiles	            Files to be placed on the PYTHONPATH	                                        list of paths
     * files	            Files to be placed in executor working directory	                            list of paths
     * driverMemory	    Memory for driver (e.g. 1000M, 2G)	                                            string
     * driverCores	        Number of cores used by driver (YARN mode only)	                                int
     * executorMemory	    Memory for executor (e.g. 1000M, 2G)	                                        string
     * executorCores	    Number of cores used by executor	                                            int
     * numExecutors	    Number of executors (YARN mode only)	                                        int
     * archives	        Archives to be uncompressed in the executor working directory (YARN mode only)	list of paths
     * queue	            The YARN queue to submit too (YARN mode only)	string
     * name	            Name of the application	string
     * conf             Spark configuration properties  Map of key=val
     */
    private String file = "";
    private String className = "";

    private String clusterName;
    private boolean isLocalArtifact;
    private String artifactName;
    private String localArtifactPath;
    private List<String> files;
    private List<String> jars;
    private List<String> args;
    private Map<String, Object> jobConfig;

    public static final String DriverMemory = "driverMemory";
    public static final String DriverMemoryDefaultValue = "4G";

    public static final String DriverCores = "driverCores";
    public static final String DriverCoresDefaultValue = "1";

    public static final String ExecutorMemory = "executorMemory";
    public static final String ExecutorMemoryDefaultValue = "4G";

    public static final String NumExecutors = "numExecutors";
    public static final String NumExecutorsDefaultValue = "";

    public static final String ExecutorCores = "executorCores";
    public static final String ExecutorCoresDefaultValue = "1";

    public static final String Conf = "conf";   // 	Spark configuration properties
    public static final Map<String, Object> ConfDefaultValue = new HashMap<String, Object>();

    public static final String NAME = "name";

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

    public String getClusterName() {
        return clusterName;
    }

    public boolean isLocalArtifact() {
        return isLocalArtifact;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public String getLocalArtifactPath() {
        return localArtifactPath;
    }

    public String getFile() {
        return file;
    }

    public String getMainClassName() {
        return className;
    }

    public List<String> getReferencedFiles() {
        return files;
    }

    public List<String> getReferencedJars() {
        return jars;
    }

    public List<String> getArgs() {
        return args;
    }

    public Map<String, Object> getJobConfig() {
        return jobConfig;
    }

    public void setFilePath(String filePath) {
        this.file = filePath;
    }

    public static List<SparkSubmissionJobConfigCheckResult> checkJobConfigMap(Map<String, Object> jobConfigMap) {

        List<SparkSubmissionJobConfigCheckResult> messageList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : jobConfigMap.entrySet()) {
            String entryKey = entry.getKey();
            if (StringHelper.isNullOrWhiteSpace(entryKey)) {
                continue;
            }

            if (entryKey.equals(DriverCores)
                    || entryKey.equals(NumExecutors)
                    || entryKey.equals(ExecutorCores)) {
                if (StringHelper.isNullOrWhiteSpace(entry.getValue().toString())) {
                    messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Warning,
                            "Warning : Empty value(s) will be override by default value(s) of system"));
                    continue;
                }

                try {
                    Integer.parseInt(entry.getValue().toString());
                } catch (NumberFormatException e) {
                    messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Error,
                            String.format("Error : Failed to parse \"%s\", it should be an integer", entry.getValue())));
                }
            } else if (entryKey.equals(DriverMemory)
                    || entryKey.equals(ExecutorMemory)
                    || entryKey.equals(NAME)) {
                if (StringHelper.isNullOrWhiteSpace(entry.getValue().toString())) {
                    messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Warning,
                            "Warning : Empty value(s) will be override by default value(s) of system"));
                }
            } else {
                messageList.add(new SparkSubmissionJobConfigCheckResult(SparkSubmissionJobConfigCheckStatus.Error,
                        String.format("Error : Key \"%s\" is invalid. It should be one of \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"",
                                entryKey, DriverMemory, DriverCores, ExecutorMemory, NumExecutors, ExecutorCores, NAME)));
            }
        }

        return messageList;
    }

    public String serializeToJson() {

        JSONObject jsonObject = new JSONObject(getSparkSubmissionParameterMap());
        try {
            Object driverCoresValue = jsonObject.get(DriverCores);
            jsonObject.put(DriverCores, Integer.parseInt(driverCoresValue.toString()));
        } catch (JSONException e) {
        }

        try {
            Object driverCoresValue = jsonObject.get(ExecutorCores);
            jsonObject.put(ExecutorCores, Integer.parseInt(driverCoresValue.toString()));
        } catch (JSONException e) {
        }

        try {
            Object driverCoresValue = jsonObject.get(NumExecutors);
            jsonObject.put(NumExecutors, Integer.parseInt(driverCoresValue.toString()));
        } catch (JSONException e) {
        }

        return jsonObject.toString();
    }

    private Map<String, Object> getSparkSubmissionParameterMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("file", file);
        map.put("className", className);
        if (files != null && files.size() > 0) {
            map.put("files", files);
        }

        if (jars != null && jars.size() > 0) {
            map.put("jars", jars);
        }

        if (args != null && args.size() > 0) {
            map.put("args", args);
        }

        if (jobConfig != null) {
            if (jobConfig.containsKey(DriverMemory) && !StringHelper.isNullOrWhiteSpace(jobConfig.get(DriverMemory).toString())) {
                map.put(DriverMemory, jobConfig.get(DriverMemory));
            }

            if (jobConfig.containsKey(DriverCores) && !StringHelper.isNullOrWhiteSpace(jobConfig.get(DriverCores).toString())) {
                map.put(DriverCores, jobConfig.get(DriverCores));
            }

            if (jobConfig.containsKey(ExecutorMemory) && !StringHelper.isNullOrWhiteSpace(jobConfig.get(ExecutorMemory).toString())) {
                map.put(ExecutorMemory, jobConfig.get(ExecutorMemory));
            }

            if (jobConfig.containsKey(ExecutorCores) && !StringHelper.isNullOrWhiteSpace(jobConfig.get(ExecutorCores).toString())) {
                map.put(ExecutorCores, jobConfig.get(ExecutorCores));
            }

            if (jobConfig.containsKey(NumExecutors) && !StringHelper.isNullOrWhiteSpace(jobConfig.get(NumExecutors).toString())) {
                map.put(NumExecutors, jobConfig.get(NumExecutors));
            }

            if (jobConfig.containsKey(Conf) &&
                    jobConfig.get(Conf) != null &&
                    !StringHelper.isNullOrWhiteSpace(jobConfig.get(Conf).toString())) {
                map.put(Conf, jobConfig.get(Conf));
            }

            if (jobConfig.containsKey(NAME)) {
                map.put(NAME, jobConfig.get(NAME));
            }
        }

        return map;
    }

    public static final String[] parameterList = new String[]{SparkSubmissionParameter.DriverMemory, SparkSubmissionParameter.DriverCores,
            SparkSubmissionParameter.ExecutorMemory, SparkSubmissionParameter.ExecutorCores, SparkSubmissionParameter.NumExecutors};

    //the first value in pair should be in same order with parameterList
    public static final Pair<String, String>[] defaultParameters = new Pair[]{
            new Pair<String, String>(SparkSubmissionParameter.DriverMemory, SparkSubmissionParameter.DriverMemoryDefaultValue),
            new Pair<String, String>(SparkSubmissionParameter.DriverCores, SparkSubmissionParameter.DriverCoresDefaultValue),
            new Pair<String, String>(SparkSubmissionParameter.ExecutorMemory, SparkSubmissionParameter.ExecutorMemoryDefaultValue),
            new Pair<String, String>(SparkSubmissionParameter.ExecutorCores, SparkSubmissionParameter.ExecutorCoresDefaultValue),
            new Pair<String, String>(SparkSubmissionParameter.NumExecutors, SparkSubmissionParameter.NumExecutorsDefaultValue)
    };
}