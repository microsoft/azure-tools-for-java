/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class SubmissionTableModelScenario {
    SubmissionTableModel tableModel = new SubmissionTableModel();

    @Given("^create the SparkSubmissionTable with the following config$")
    public void createSparkSubmissionTable(Map<String, Object> tableConfig) {
        tableConfig.entrySet()
                .forEach(entry -> tableModel.addRow(entry.getKey(), entry.getValue()));
    }

    @Then("^check to get config map should be '(.+)'$")
    public void checkGetConfigMapByJSON(String jsonString) throws Throwable {
        Map<String, Object> target = new Gson().fromJson(jsonString, new TypeToken<Map<String, Object>>(){}.getType());

        SparkSubmissionParameter parameter = new SparkSubmissionParameter();
        parameter.applyFlattedJobConf(tableModel.getJobConfigMap());

        assertEquals(target, parameter.getJobConfig());
    }
}
