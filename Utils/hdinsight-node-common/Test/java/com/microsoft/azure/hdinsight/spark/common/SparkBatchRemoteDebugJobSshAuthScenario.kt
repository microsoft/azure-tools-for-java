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

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import java.io.File
import kotlin.test.assertEquals

class SparkBatchRemoteDebugJobSshAuthScenario {
    @Then("^checking isValid should match the following combinations$")
    fun checkIsValidCombinations(comboTable: DataTable) {
        comboTable.asMaps(String::class.java, String::class.java)
                .forEach {
                    val sshAuth = SparkBatchRemoteDebugJobSshAuth()

                    sshAuth.sshUserName = it["userName"]
                    sshAuth.sshAuthType = SparkBatchRemoteDebugJobSshAuth.SSHAuthType.valueOf(it["authType"]!!)
                    sshAuth.sshPassword = it["password"]

                    if (!it["keyFile"].isNullOrBlank()) {
                        sshAuth.sshKeyFile = File(it["keyFile"])
                    }

                    assertEquals(
                            it["isValid"]!!.toBoolean(),
                            sshAuth.isValid,
                            "Current working directory is ${System.getProperty("user.dir")}")
                }

    }
}