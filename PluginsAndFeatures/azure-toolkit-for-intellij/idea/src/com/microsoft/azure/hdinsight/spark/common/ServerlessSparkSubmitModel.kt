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
import com.microsoft.azuretools.utils.Pair
import org.jdom.Element
import java.net.URI

class ServerlessSparkSubmitModel(project: Project) : SparkSubmitModel(project) {
    var tenantId: String = "common"
    var accountName: String? = null
    var clusterId: String? = null
    var livyUri: URI? = null

    companion object {
        @JvmStatic val SERVERLESS_SUBMISSION_ATTRIBUTE_TENANT_ID = "tenant_id"
        @JvmStatic val SERVERLESS_SUBMISSION_ATTRIBUTE_ACCOUNT_NAME = "account_name"
        @JvmStatic val SERVERLESS_SUBMISSION_ATTRIBUTE_CLUSTER_ID= "cluster_id"
        @JvmStatic val SERVERLESS_SUBMISSION_ATTRIBUTE_LIVY_URI = "livy_uri"
    }

    override fun getDefaultParameters(): Array<Pair<String, String>> {
        return arrayOf(
                Pair(SparkSubmissionParameter.DriverMemory, SparkSubmissionParameter.DriverMemoryDefaultValue),
                Pair(SparkSubmissionParameter.DriverCores, SparkSubmissionParameter.DriverCoresDefaultValue),
                Pair(SparkSubmissionParameter.ExecutorMemory, SparkSubmissionParameter.ExecutorMemoryDefaultValue),
                Pair(SparkSubmissionParameter.ExecutorCores, SparkSubmissionParameter.ExecutorCoresDefaultValue))
    }

    override fun exportToElement(): Element {
        val root = super.exportToElement()
                .setAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_TENANT_ID, tenantId)

        if (accountName != null) {
            root.setAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_ACCOUNT_NAME, accountName)
        }

        if (clusterId != null) {
            root.setAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_CLUSTER_ID, clusterId)
        }

        if (livyUri != null) {
            root.setAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_LIVY_URI, livyUri.toString())
        }

        return root
    }

    override fun applyFromElement(rootElement: Element): SparkSubmitModel {
        super.applyFromElement(rootElement)

        if (rootElement.name == SUBMISSION_CONTENT_NAME) {
            tenantId = rootElement.getAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_TENANT_ID)?.value ?: "common"
            accountName = rootElement.getAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_ACCOUNT_NAME)?.value
            clusterId = rootElement.getAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_CLUSTER_ID)?.value
            livyUri = rootElement.getAttribute(SERVERLESS_SUBMISSION_ATTRIBUTE_LIVY_URI)?.value?.let { URI.create(it) }
        }

        return this
    }
}