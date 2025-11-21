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

package com.microsoft.azure.hdinsight.projects

import io.cucumber.java.en.Then
import org.assertj.core.api.Assertions.assertThat

class SparkVersionScenario {
    @Then("^Spark version '(.+)' should large than '(.+)'")
    fun sparkVersionShouldLargeThan(version1: String, version2: String) {
        assertThat(SparkVersion.sparkVersionComparator.compare(SparkVersion.parseString(version1), SparkVersion.parseString(version2)))
                .isGreaterThan(0)
    }

    @Then("^Spark version '(.+)' should equal to '(.+)'")
    fun sparkVersionShouldEqualTo(version1: String, version2: String) {
        assertThat(SparkVersion.sparkVersionComparator.compare(SparkVersion.parseString(version1), SparkVersion.parseString(version2)))
                .isEqualTo(0)
    }
}