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

package com.microsoft.azure.hdinsight.spark.common

import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.jdom.input.SAXBuilder
import org.jdom.output.XMLOutputter
import java.io.StringReader
import kotlin.test.assertEquals

class SparkSubmitModelScenario {
    lateinit var submitModel: SparkSubmitModel

    @Before
    fun setup() {
        submitModel = SparkSubmitModel()
    }

    @Given("^set SparkSubmitModel properties as following$")
    fun initSparkSubmitModel(properties: Map<String, String>) {
        properties.forEach { key, value -> when (key) {
            "cluster_name" -> submitModel.clusterName = value
            "is_local_artifact" -> submitModel.isLocalArtifact = value.toBoolean()
            "local_artifact_path" -> submitModel.localArtifactPath = value
            "classname" -> submitModel.mainClassName = value
            "cmd_line_args" -> submitModel.commandLineArgs = value.split(" ")
            "ref_jars" -> submitModel.referenceJars = value.split(";")
            "ref_files" -> submitModel.referenceFiles = value.split(";")
        } }
    }

    @Then("^checking XML serialized should to be '(.*)'$")
    fun checkSparkSubmitModelExportedXML(expect: String) {
        val element = submitModel.exportToElement()
        val actual = XMLOutputter().outputString(element)

        assertEquals(expect, actual)

    }

    @Given("^the SparkSubmitModel XML input '(.*)' to deserialize$")
    fun importSparkSubmitModelFromXML(xml: String) {
        val element = SAXBuilder().build(StringReader(xml)).rootElement

        submitModel.applyFromElement(element)
    }

    @Then("^check SparkSubmitModel properties as following$")
    fun checkSparkSubmitMode(expect: Map<String, String>) {
        expect.forEach { key, value -> when (key) {
            "cluster_name" -> assertEquals(value, submitModel.clusterName)
            "is_local_artifact" -> assertEquals(value.toBoolean(), submitModel.isLocalArtifact)
            "local_artifact_path" -> assertEquals(value, submitModel.localArtifactPath)
            "classname" -> assertEquals(value, submitModel.mainClassName)
            "cmd_line_args" -> assertEquals(value, submitModel.commandLineArgs.joinToString(" "))
            "ref_jars" -> assertEquals(value, submitModel.referenceJars.joinToString(";"))
            "ref_files" -> assertEquals(value, submitModel.referenceFiles.joinToString(";"))
        } }
    }
}