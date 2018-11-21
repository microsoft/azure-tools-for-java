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
 */

package com.microsoft.azure.hdinsight.spark.common

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import groovy.text.SimpleTemplateEngine

class MockHttpService {
    val livyServerMock: WireMockServer = WireMockServer(wireMockConfig().dynamicPort())

    val port: Int
        get() = this.livyServerMock.port()

    val templateProperties: Map<String, String>
        get() = hashMapOf("port" to port.toString())

    fun stub(action: String, uri: String, statusCode: Int, response: String) {
        WireMock.configureFor(port)
        WireMock.stubFor(WireMock.request(
                                action, WireMock.urlEqualTo(uri))
                        .willReturn(WireMock.aResponse()
                                .withStatus(statusCode).withBody(normalizeResponse(response))))
    }

    fun stubWithHeader(action: String, uri: String, statusCode: Int, response: String, header: Map<String, String>) {
        WireMock.configureFor(port)
        WireMock.stubFor(WireMock.request(
                action, WireMock.urlEqualTo(uri))
                .willReturn(WireMock.aResponse()
                        .withStatus(statusCode)
                        .withHeaders(HttpHeaders(header.map { HttpHeader(it.key, it.value) }))
                        .withBody(normalizeResponse(response))))
    }

    fun stubWithBody(action: String, uri: String, body: String, statusCode: Int, response: String) {
        WireMock.configureFor(port)
        WireMock.stubFor(
            WireMock.request(action, WireMock.urlEqualTo(uri))
                .withRequestBody(WireMock.equalToJson(body))
                .willReturn(WireMock.aResponse()
                    .withStatus(statusCode)
                    .withBody(normalizeResponse(response))))
    }

    fun normalizeResponse(rawResponse: String): String {
        val engine = SimpleTemplateEngine()
        return engine.createTemplate(rawResponse).make(templateProperties).toString()
    }

    fun completeUrl(absoluteUri: String): String {
        return "http://localhost:$port/${absoluteUri.trimStart('/')}"
    }

    init {
        this.livyServerMock.start()
    }
}