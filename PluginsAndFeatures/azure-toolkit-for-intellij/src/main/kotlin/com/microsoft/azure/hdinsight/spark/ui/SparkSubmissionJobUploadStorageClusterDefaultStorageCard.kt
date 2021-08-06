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

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.intellij.ui.util.UIUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.*

class SparkSubmissionJobUploadStorageClusterDefaultStorageCard
    : SparkSubmissionJobUploadStorageBasicCard(SparkSubmitStorageType.DEFAULT_STORAGE_ACCOUNT.description) {
    override fun createViewModel(): ViewModel = object : ViewModel() {
        override fun getValidatedStorageUploadPath(config: Model): String {
            // There are IO operations
            UIUtils.assertInPooledThread()

            log().info("Current selected cluster: ${cluster?.title}")
            val cluster = this.cluster ?: throw RuntimeConfigurationError("No selected cluster")

            try {
                cluster.getConfigurationInfo()
            } catch (ex: Exception) {
                val errorMsg = "Error getting cluster storage configuration"
                log().warn("$errorMsg. ${ex.message}. ${ExceptionUtils.getRootCauseStackTrace(ex)}")

                throw RuntimeConfigurationError(errorMsg)
            }

            if (cluster.storageAccount == null) {
                log().warn("Cluster ${cluster.title} has no storage account")
                throw RuntimeConfigurationError("Cluster ${cluster.title} has no storage account")
            }

            // TODO: need to test whether this has block issue
            // Here is a bug before this commit. If cluster.defaultStorageRootPath is not
            // null, the path will not add "/SparkSubmission" as suffix. This is because
            // operator + is calculated ahead of operator ?:
            // To address issue https://github.com/microsoft/azure-tools-for-java/issues/3856
            val path = (cluster.defaultStorageRootPath?.trimEnd('/') ?: "") +
                    "/${SparkSubmissionContentPanel.Constants.submissionFolder}/"

            try {
                return StorageAccountType.parseUri(path).parse(path).uri.toString()
            } catch (ex: UnknownFormatConversionException) {
                throw RuntimeConfigurationError(ex.message)
            }
        }
    }
}