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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SubmissionTableModelScenario {
    SubmissionTableModel tableModel = new SubmissionTableModel(new String[] {"Key", "Value", ""});

    @Given("^create the SparkSubmissionTable with the following config$")
    public void createSparkSubmissionTable(Map<String, Object> tableConfig) {
        tableConfig.entrySet()
                .forEach(entry -> tableModel.addRow(entry.getKey(), entry.getValue()));
    }


    @Then("^check to get config map should be '(.+)'$")
    public void checkGetConfigMapByJSON(String jsonString) throws Throwable {
        Map<String, Object> target = new Gson().fromJson(jsonString, new TypeToken<Map<String, Object>>(){}.getType());

        assertEquals(target, tableModel.getJobConfigMap());
    }
}
