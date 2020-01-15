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

import cucumber.api.DataTable
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import java.util.*
import java.util.stream.Collectors
import kotlin.test.assertEquals

class AbfsUriScenario {
    data class AbfsUriAndProperties(val url: String,
                                    val accountName: String,
                                    val fileSystem: String,
                                    val rawPath: String,
                                    val path: String,
                                    val directoryParam: String)

    private var restfulGen2Paths: List<String> = emptyList()
    private var abfsUris: List<String> = emptyList()

    @Given("^Gen two restful path is$")
    fun initializeRestfulGen2Path(paths: DataTable) {
        this.restfulGen2Paths = paths.asList(String::class.java)
    }

    @Then("^convert the restful path to ABFS URI should be$")
    fun convertToGen2UriShouldBe(expectedConvertedUris: DataTable) {
        // calling stream() on a List returns an ordered stream
        val actualConvertedUris = this.restfulGen2Paths.stream()
            .map { path ->
                try {
                    AbfsUri.parse(path).uri.toString()
                } catch (ex: UnknownFormatConversionException) {
                    "invalid restful path"
                }
            }
            .collect(Collectors.toList())
        (0 until actualConvertedUris.size).forEach {
            assert(actualConvertedUris[it] == expectedConvertedUris.asList(String::class.java)[it])
        }
    }

    @Given("^ABFS URI is$")
    fun initializeAbfsUri(uris: DataTable) {
        this.abfsUris = uris.asList(String::class.java)
    }

    @Then("^convert the ABFS URI to restful path should be$")
    fun convertToGen2PathShouldBe(expectedRestfulPaths: DataTable) {
        val actualGen2Path = this.abfsUris.stream()
            .map { path ->
                try {
                    AbfsUri.parse(path).url.toString()
                } catch (ex: UnknownFormatConversionException) {
                    "invalid Gen2 URI"
                }
            }
            .collect(Collectors.toList())
        (0 until actualGen2Path.size).forEach {
            assert(actualGen2Path[it] == expectedRestfulPaths.asList(String::class.java)[it])
        }
    }

    @Then("^properties of abfs URI should be$")
    fun pathAndRawPathShouldBe(checkTable: List<AbfsUriAndProperties>) {
        checkTable.forEach {
            try {
                val abfsUri = AbfsUri.parse(it.url)
                assertEquals(abfsUri.accountName, it.accountName)
                assertEquals(abfsUri.fileSystem, it.fileSystem)
                assertEquals(abfsUri.rawPath, it.rawPath)
                assertEquals(abfsUri.path, it.path)
                assertEquals(abfsUri.directoryParam, it.directoryParam)
            } catch (ex: Throwable) {
                assertEquals("<invalid>", it.accountName, "Get error when parsing accountName from AbfsUri ${it.url}. ${ex.message}")
                assertEquals("<invalid>", it.fileSystem, "Get error when parsing fileSystem from AbfsUri ${it.url}. ${ex.message}")
                assertEquals("<invalid>", it.rawPath, "Get error when parsing rawPath from AbfsUri ${it.url}. ${ex.message}")
                assertEquals("<invalid>", it.path, "Get error when parsing path from AbfsUri ${it.url}. ${ex.message}")
                assertEquals("<invalid>", it.directoryParam, "Get error when parsing directoryParam from AbfsUri ${it.url}. ${ex.message}")
            }
        }
    }
}