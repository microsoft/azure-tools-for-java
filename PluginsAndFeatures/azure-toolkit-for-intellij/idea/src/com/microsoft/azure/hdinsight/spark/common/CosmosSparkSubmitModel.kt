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

import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.microsoft.azuretools.utils.Pair
import java.util.stream.Stream

open class CosmosSparkSubmitModel : SparkSubmitModel {
    @Attribute("tenant_id")
    var tenantId: String = "common"
    @Attribute("account_name")
    var accountName: String? = null
    @Attribute("cluster_id")
    var clusterId: String? = null
    @Attribute("livy_uri")
    var livyUri: String? = null

    constructor() : super()

    constructor(project: Project) : super(project)

    constructor(project: Project, submissionParameter: SparkSubmissionParameter): super(project, submissionParameter)

    override fun getDefaultParameters(): Stream<Pair<String, out Any>> {
        val list: List<Pair<String, out Any>> = listOf(
                Pair(SparkSubmissionParameter.DriverMemory, SparkSubmissionParameter.DriverMemoryDefaultValue),
                Pair(SparkSubmissionParameter.DriverCores, SparkSubmissionParameter.DriverCoresDefaultValue),
                Pair(SparkSubmissionParameter.ExecutorMemory, SparkSubmissionParameter.ExecutorMemoryDefaultValue),
                Pair(SparkSubmissionParameter.ExecutorCores, SparkSubmissionParameter.ExecutorCoresDefaultValue)
        )
        return list.stream()
    }

    override fun getSparkClusterTypeDisplayName(): String = "Apache Spark on Cosmos cluster"
}