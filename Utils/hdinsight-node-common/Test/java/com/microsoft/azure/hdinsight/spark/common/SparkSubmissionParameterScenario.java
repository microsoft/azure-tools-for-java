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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azuretools.utils.Pair;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class SparkSubmissionParameterScenario {
    static ObjectMapper mapper = new ObjectMapper();
    String mockSelectedClusterName = "";
    boolean mockIsLocalArtifactRadioButtionSelected = true;
    String mockArtifactName = "";
    String mockLocalArtifactPath = "";
    String mockFilePath = "";
    String mockClassName = "";
    List<String> mockReferencedFiles = new ArrayList<>();
    List<String> mockReferencedJars = new ArrayList<>();
    List<String> mockArgs = new ArrayList<>();

    SparkSubmissionParameter sparkSubmissionParameter = new SparkSubmissionParameter();
    Map<String, String> sparkConfig;

    @Given("^apply spark configs$")
    public void applySparkConfigures(Map<String, String> config) throws Throwable {
        List<com.microsoft.azuretools.utils.Pair<String, String>> mergedJobConf = sparkSubmissionParameter.flatJobConfig();
        config.forEach( (k, v) -> mergedJobConf.add(new Pair<>(k, v)));
        sparkSubmissionParameter.applyFlattedJobConf(mergedJobConf);
    }


    @Given("^create SparkSubmissionParameter with the following job config$")
    public void createSparkSubmissionParameterWithJobConfig(Map<String, String> jobConfig) throws Throwable {
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
                new HashMap<>());

        applySparkConfigures(jobConfig);
    }

    @Then("^the parameter map should include key '(.+)' with value '(.+)'$")
    public void verifyParameterKeyAndValueExist(String key, String value) throws Throwable{
        Map<String, Object> param = sparkSubmissionParameter.getJobConfig();
        assertTrue(param.containsKey(key));
        assertEquals(value, param.get(key).toString());
    }

    @Then("^the serialized JSON should be '(.+)'$")
    public void verifySerializedJSON(String json) throws Throwable {
        String actualJson = sparkSubmissionParameter.serializeToJson();

        assertThat(mapper.readValue(actualJson, HashMap.class)).isEqualTo(mapper.readValue(json, HashMap.class));
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
