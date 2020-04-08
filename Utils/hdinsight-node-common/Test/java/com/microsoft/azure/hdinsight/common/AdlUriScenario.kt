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

class AdlUriScenario {
    data class UrlUriEntry(val url: String, val uri: String)
    data class AdlUriParameterEntry(val uri: String, val path: String, val storageName: String)
    data class UrlUriEqualEntry(val src: String, val dest: String, val isEqualed: Boolean)
    data class UriPathResolveAsRootEntry(val uri: String, val path: String, val result: String)
    data class UriRelativizeCheckEntry(val src: String, val dest: String, val result: String)

    @Then("convert Gen ONE URL restful path to URI should be")
    fun checkGenOne2HttpConversion(checkTable: List<UrlUriEntry>) {
        checkTable.forEach {
            try {
                val adlUri = AdlUri.parse(it.url)

                assertEquals(it.uri, adlUri.uri.toString(), "Convert ${it.url} to ADL URI")
            } catch (ex: UnknownFormatConversionException) {
                assertEquals(it.uri, "<invalid_restful_path>", "Get ${ex.message} when converting ${it.url} to ADL URI")
            }
        }
    }

    @Then("convert ADL URI to Gen ONE URL restful path should be")
    fun checkHttp2GenOneConversion(checkTable: List<UrlUriEntry>) {
        checkTable.forEach {
            try {
                val adlUri = AdlUri.parse(it.uri)

                assertEquals(it.url, adlUri.url.toString(), "Convert ${it.uri} to GEN 1 HTTP Restful URL")
            } catch (ex: UnknownFormatConversionException) {
                assertEquals(it.url, "<invalid_Gen1_URI>", "Get ${ex.message} when converting ${it.url} to GEN 1 HTTP Restful URL")
            }
        }
    }

    @Then("check ADL URI parameters as below")
    fun checkAdlUriParameters(checkTable: List<AdlUriParameterEntry>) {
        checkTable.forEach {
            try {
                val adlUri = AdlUri.parse(it.uri)

                assertEquals(it.path, adlUri.getPath(), "Check ADL Gen1 URI ${it.uri} path parameter")
                assertEquals(it.storageName, adlUri.storageName.toString(), "Check ADL Gen1 URI ${it.uri} storage parameter")
            } catch (ex: UnknownFormatConversionException) {
                assertEquals(it.path, "<invalid>", "Get ${ex.message} when parsing ${it.uri} to GEN 1 AdlURI")
                assertEquals(it.storageName, "<invalid>", "Get ${ex.message} when parsing ${it.uri} to GEN 1 AdlURI")
            }
        }
    }

    @Then("check ADL URI equality as below")
    fun checkAdlUriEquality(checkTable: List<UrlUriEqualEntry>) {
        checkTable.forEach {
            val src = AdlUri.parse(it.src)
            val dest = AdlUri.parse(it.dest)

            if (it.isEqualed) {
                assertEquals(dest, src, "Check ADL URI ${it.src} should equal ${it.dest}")
            } else {
                assertNotEquals(dest, src, "Check ADL URI ${it.src} should not equal ${it.dest}")
            }
        }
    }

    @Then("check ADL URI resolve as root path as below")
    fun checkAdlUriResolveAsRoot(checkTable: List<UriPathResolveAsRootEntry>) {
        checkTable.forEach {
            val src = AdlUri.parse(it.uri)
            val dest = AdlUri.parse(it.result)

            assertEquals(dest, src.resolveAsRoot(it.path), "Check ADL URI ${it.uri} resolve ${it.path} as root path")
        }
    }

    @Then("check ADL URI relativize as below")
    fun checkAdlUriRelativize(checkTable: List<UriRelativizeCheckEntry>) {
        checkTable.forEach {
            val src = AdlUri.parse(it.src)
            val dest = AdlUri.parse(it.dest)

            assertEquals(
                    it.result.takeIf { result -> result != "<null>"},
                    src.relativize(dest),
                    "Check ADL URI ${it.src} relativite ${it.dest} as root path"
            )
        }
    }
}