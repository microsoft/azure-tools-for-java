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

package com.microsoft.azure.hdinsight.sdk.storage.adlsgen2

import cucumber.api.DataTable
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import java.net.URI
import java.util.stream.Collectors

class ADLSGen2FSOperationScenario {
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
                    ADLSGen2FSOperation.convertToGen2Uri(URI.create(path))
                } catch (ex: IllegalArgumentException) {
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
                    ADLSGen2FSOperation.convertToGen2Path(URI.create(path))
                } catch (ex: IllegalArgumentException) {
                    "invalid Gen2 URI"
                }
            }
            .collect(Collectors.toList())
        (0 until actualGen2Path.size).forEach {
            assert(actualGen2Path[it] == expectedRestfulPaths.asList(String::class.java)[it])
        }
    }

    @Then("^the Gen two base restful path should be$")
    fun gen2BaseRestfulPathShouldBe(expectedBaseRestfulPath: DataTable) {
        val actualBaseRestfulPath = this.restfulGen2Paths.stream()
            .map { path ->
                try {
                    ADLSGen2FSOperation.getGen2BaseRestfulPath(URI.create(path))
                } catch (ex: IllegalArgumentException) {
                    "invalid Gen2 URI"
                }
            }
            .collect(Collectors.toList())

        (0 until actualBaseRestfulPath.size).forEach {
            assert(actualBaseRestfulPath[it] == expectedBaseRestfulPath.asList(String::class.java)[it])
        }
    }

    @Then("^the Gen two directory param should be$")
    fun gen2SubPathShouldBe(expectedSubPath: DataTable) {
        val actualSubPath = this.abfsUris.stream()
            .map { path ->
                try {
                    ADLSGen2FSOperation.getDirectoryParam(URI.create(path))
                } catch (ex: IllegalArgumentException) {
                    "invalid Gen2 URI"
                }
            }
            .collect(Collectors.toList())

        (0 until actualSubPath.size).forEach {
            assert(actualSubPath[it] == expectedSubPath.asList(String::class.java)[it])
        }
    }
}