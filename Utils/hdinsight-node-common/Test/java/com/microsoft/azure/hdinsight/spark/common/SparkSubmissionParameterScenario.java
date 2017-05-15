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

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SparkSubmissionParameterScenario {

    String mockSelectedClusterName = "mockCluster";
    boolean mockIsLocalArtifactRadioButtionSelected = true;
    String mockArtifactName = "mockArtifact";
    String mockLocalArtifactPath = "mockLocalArtifactPath";
    String mockFilePath = "mockFilePath";
    String mockClassName = "mockClassName";
    List<String> mockReferencedFiles = new ArrayList<String>(Arrays.asList(
            "mock_reference_file1",
            "mock_reference_file2"));
    List<String> mockReferencedJars = new ArrayList<String>(Arrays.asList(
            "mock_reference_jar1",
            "mock_reference_jar2"));
    List<String> mockArgs = new ArrayList<String>(Arrays.asList(
            "mock_arg1",
            "mock_arg2"));

    SparkSubmissionParameter sparkSubmissionParameter;
    Map<String, Object> sparkConfig;

    private Map<String, Object>
    __getSparkSubmissionParameterMap(SparkSubmissionParameter sparkSubmissionParameter) throws
            Throwable {
        Method method = SparkSubmissionParameter.class.getDeclaredMethod("getSparkSubmissionParameterMap");
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(sparkSubmissionParameter);
    }

    @Given("^create SparkSubmissionParameter spark config parameter$")
    public void createSparkSubmissionParameterSparkConfigParameter(Map<String, Object> config) throws Throwable {
        sparkConfig = config;
    }

    @Given("^create SparkSubmissionParameter with the following job config$")
    public void createSparkSubmissionParameterWithJobConfig(Map<String, Object> jobConfig) throws Throwable {
        Map<String, Object> mergedJobConf = new HashMap<>(jobConfig);

        mergedJobConf.put("conf", sparkConfig);

        sparkSubmissionParameter = new SparkSubmissionParameter(
                mockSelectedClusterName,
                mockIsLocalArtifactRadioButtionSelected,
                mockArtifactName,
                mockLocalArtifactPath,
                mockFilePath,
                mockClassName,
                mockReferencedFiles,
                mockReferencedJars,
                mockArgs,
                mergedJobConf);
    }

    @Then("^the parameter map should include key '(.+)' with value '(.+)'$")
    public void verifyParameterKeyAndValueExist(String key, String value) throws Throwable{
        Map<String, Object> param = __getSparkSubmissionParameterMap(sparkSubmissionParameter);
        assertTrue(param.containsKey(key));
        assertEquals(value, param.get(key).toString());
    }

    @Then("^the serialized JSON should be '(.+)'$")
    public void verifySerializedJSON(String json) throws Throwable {
        assertEquals(sparkSubmissionParameter.serializeToJson(), json);
    }


    @And("^mock className to (.+)$")
    public void mockClassName(String className) throws Throwable {
        mockClassName = className;
    }

    @And("^mock reference jars to (.+)$")
    public void mockReferenceJars(String jars) throws Throwable {
        mockReferencedJars = Arrays.asList(jars.split(","));
    }

    @And("^mock args to (.+)$")
    public void mockArgs(String args) throws Throwable {
        mockArgs = Arrays.asList(args.split(","));
    }

    @And("^mock file to (.+)$")
    public void mockFilePath(String filePath) throws Throwable {
        mockFilePath = filePath;
    }

    @And("^mock reference files to (.+)$")
    public void mockReferencedFiles(String files) throws Throwable {
        mockReferencedFiles = Arrays.asList(files.split(","));
    }
}