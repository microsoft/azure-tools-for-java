/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azuretools.utils.Pair;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

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
