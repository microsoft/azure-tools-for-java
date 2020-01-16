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

package com.microsoft.azure.hdinsight.common

import cucumber.api.java.en.Then
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WasbUriScenario {
    data class UrlUriEntry(val url: String, val uri: String)
    data class UriParameterEntry(val uri: String, val container: String, val path: String, val account: String, val endpointSuffix: String)
    data class UrlUriEqualEntry(val src: String, val dest: String, val isEqualed: Boolean)
    data class UriPathResolveAsRootEntry(val uri: String, val path: String, val result: String)
    data class UriRelativizeCheckEntry(val src: String, val dest: String, val result: String)
    data class rawPathEncodedPathEntry(val rawPath: String, val encodedPath: String)

    @Then("convert Wasb URL restful path to URI should be")
    fun checkWasb2HttpConversion(checkTable: List<UrlUriEntry>) {
        checkTable.forEach {
            try {
                val wasbUri = WasbUri.parse(it.url)

                assertEquals(it.uri, wasbUri.uri.toString(), "Convert ${it.url} to Wasb URI")
            } catch (ex: UnknownFormatConversionException) {
                assertEquals(it.uri, "<invalid_restful_path>", "Get ${ex.message} when converting ${it.url} to Wasb URI")
            }
        }
    }

    @Then("convert Wasb URI to URL restful path should be")
    fun checkHttp2WasbConversion(checkTable: List<UrlUriEntry>) {
        checkTable.forEach {
            try {
                val wasbUri = WasbUri.parse(it.uri)

                assertEquals(it.url, wasbUri.url.toString(), "Convert ${it.uri} to HTTP Restful URL")
            } catch (ex: UnknownFormatConversionException) {
                assertEquals(it.url, "<invalid_uri>", "Get ${ex.message} when converting ${it.url} to HTTP Restful URL")
            }
        }
    }

    @Then("check Wasb URI parameters as below")
    fun checkWasbUriParameters(checkTable: List<UriParameterEntry>) {
        checkTable.forEach {
            try {
                val wasbUri = WasbUri.parse(it.uri)

                assertEquals(it.path, wasbUri.getPath(), "Check Wasb URI ${it.uri} path parameter")
                assertEquals(it.account, wasbUri.storageAccount.toString(), "Check Wasb URI ${it.uri} account parameter")
                assertEquals(it.container, wasbUri.container.toString(), "Check Wasb URI ${it.uri} container parameter")
                assertEquals(it.endpointSuffix, wasbUri.endpointSuffix.toString(), "Check Wasb URI ${it.uri} endpointSuffix parameter")
            } catch (ex: UnknownFormatConversionException) {
                assertEquals(it.path, "<invalid>", "Get ${ex.message} when parsing ${it.uri} to WasbURI")
                assertEquals(it.account, "<invalid>", "Get ${ex.message} when parsing ${it.uri} to WasbURI")
                assertEquals(it.container, "<invalid>", "Get ${ex.message} when parsing ${it.uri} to WasbURI")
                assertEquals(it.endpointSuffix, "<invalid>", "Get ${ex.message} when parsing ${it.uri} to WasbURI")
            }
        }
    }

    @Then("check Wasb URI equality as below")
    fun checkWasbUriEquality(checkTable: List<UrlUriEqualEntry>) {
        checkTable.forEach {
            val src = WasbUri.parse(it.src)
            val dest = WasbUri.parse(it.dest)

            if (it.isEqualed) {
                assertEquals(dest, src, "Check Wasb URI ${it.src} should equal ${it.dest}")
            } else {
                assertNotEquals(dest, src, "Check Wasb URI ${it.src} should not equal ${it.dest}")
            }
        }
    }

    @Then("check Wasb URI resolve as root path as below")
    fun checkWasbUriResolveAsRoot(checkTable: List<UriPathResolveAsRootEntry>) {
        checkTable.forEach {
            val src = WasbUri.parse(it.uri)
            val dest = WasbUri.parse(it.result)

            assertEquals(dest, src.resolveAsRoot(it.path), "Check Wasb URI ${it.uri} resolve ${it.path} as root path")
        }
    }

    @Then("check Wasb URI relativize as below")
    fun checkWasbUriRelativize(checkTable: List<UriRelativizeCheckEntry>) {
        checkTable.forEach {
            val src = WasbUri.parse(it.src)
            val dest = WasbUri.parse(it.dest)

            assertEquals(
                    it.result.takeIf { result -> result != "<null>"},
                    src.relativize(dest),
                    "Check Wasb URI ${it.src} relativite ${it.dest} as root path"
            )
        }
    }

    @Then("^check the encoded path as below$")
    fun checkEncodedPath(checkTable: List<rawPathEncodedPathEntry>) {
        checkTable.forEach {
            val rawPath = it.rawPath
            val expectedEncodedPath = it.encodedPath
            try {
                assertEquals(AzureStorageUri.encodeAndNormalizePath(rawPath), expectedEncodedPath, "Check encode path $rawPath")
            } catch (ex: Throwable) {
                assertEquals("<invalid>", expectedEncodedPath, "Get error when encode path $rawPath. ${ex.message}")
            }
        }
    }
}