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
 *
 */
package com.microsoft.azure.hdinsight.spark.run.configuration

import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType

class ArcadiaSparkSubmitModel(project: Project) : SparkSubmitModel(project) {
    @Attribute("livy_uri")
    var livyUri: String? = null
    @Attribute("spark_workspace")
    var sparkWorkspace: String? = null
    @Attribute("spark_compute")
    var sparkCompute: String? = null
    @Attribute("tenant_id")
    var tenantId: String? = null
    @Attribute("spark_app_type")
    var sparkApplicationType: SparkApplicationType = SparkApplicationType.None

    override fun getSparkClusterTypeDisplayName(): String = "Apache Spark Pool for Azure Synapse"
}