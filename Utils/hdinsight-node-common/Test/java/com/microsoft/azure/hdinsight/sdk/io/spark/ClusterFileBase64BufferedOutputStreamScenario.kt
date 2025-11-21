/*
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

package com.microsoft.azure.hdinsight.sdk.io.spark

import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.Session
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession
import io.cucumber.java.Before
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import rx.Observable
import java.io.ByteArrayInputStream
import java.net.URI

class ClusterFileBase64BufferedOutputStreamScenario {
    var clusterFileBase64OutputStream: ClusterFileBase64BufferedOutputStream? = null
    var sessionMock: Session? = null
    var runCodesArg: ArgumentCaptor<String>? = null

    @Before
    fun setUp() {
        runCodesArg = ArgumentCaptor.forClass(String::class.java)
    }

    @Then("^uploading the following BASE64 string$")
    fun uploadFileToCluster(encodes: List<String>) {
        val base64Code = encodes.joinToString("")
        clusterFileBase64OutputStream.use {   // autoclose resource
            IOUtils.copy(ByteArrayInputStream(base64Code.toByteArray(Charsets.UTF_8)), it)
        }
    }

    @Given("^create a mocked Livy session for ClusterFileBase64KBBufferedOutputStream$")
    fun mockLivySessionForClusterFileBase64OutputStream() {
        sessionMock = mock(SparkSession::class.java)
        doReturn(Observable.just(hashMapOf("text/plain" to ""))).`when`(sessionMock!!)
                .runCodes(runCodesArg!!.capture())
    }

    @And("^create a Spark cluster file BASE64 output stream '(.+)' with page size (\\d+)KB$")
    fun createClusterFileBase64OutputStream(dest: String, pageSize: Int) {
        clusterFileBase64OutputStream = ClusterFileBase64BufferedOutputStream(sessionMock, URI.create(dest), pageSize)
    }

    @Then("^check the statements send to Livy session should be:$")
    fun checkStetementsOutput(codesExpect: String) {
        val codeLinesExpect = codesExpect.split("###__CMD_END__###\r?\n?".toRegex()).dropLastWhile { it.isEmpty() }
        codeLinesExpect.zip(runCodesArg!!.allValues).forEach { assertThat(it.second).isEqualToNormalizingNewlines(it.first) }

        verify(sessionMock!!, times(codeLinesExpect.size)).runCodes(ArgumentMatchers.anyString())
    }
}