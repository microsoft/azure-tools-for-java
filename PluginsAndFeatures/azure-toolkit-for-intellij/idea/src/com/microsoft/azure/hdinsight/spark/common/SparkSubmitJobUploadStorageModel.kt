/**
 * Copyright (c) Microsoft Corporation
 *
 *
 * All rights reserved.
 *
 *
 * MIT License
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.common

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.microsoft.azure.hdinsight.common.AbfsUri
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStoragePanel

@Tag("job_upload_storage")
class SparkSubmitJobUploadStorageModel: ILogger, SparkSubmissionJobUploadStoragePanel.Model {
    @Attribute("storage_account")
    override var storageAccount: String? = null

    @Attribute("adls_gen2_account")
    override var gen2Account: String? = null

    // model for ADLS Gen 2 storage type
    @Attribute("adls_gen2_root_path", converter = AbfsUriConverter::class)
    override var gen2RootPath: AbfsUri? = null

    @Attribute("adls_gen2_root_path_raw_text")
    override var gen2RootPathRawText: String? = null

    @get:Transient @set:Transient
    override var storageKey: String? = null

    @get:Transient @set:Transient
    override var accessKey: String? = null

    @Attribute("upload_path")
    var uploadPath: String? = null

    // selectedContainer is saved to reconstruct a containersModel when we reopen the project
    @Attribute("selected_container")
    override var selectedContainer: String? = null

    @Attribute("selected_subscription")
    override var selectedSubscription: String? = null

    @Attribute("storage_account_type")
    override var storageAccountType: SparkSubmitStorageType? = null

    @get:Transient @set:Transient
    override var errorMsg: String? = null

    // model for ADLS Gen 1 storage type
    @Attribute("adl_root_path")
    override var adlsRootPath: String? = null

    // model for webhdfs  type
    @Attribute("webhdfs_root_path")
    override var webHdfsRootPath: String? = null
}

fun SparkSubmitStorageType.getSecureStoreServiceOf(account: String?): String? {
    if (account.isNullOrBlank()) {
        return null
    }

    return when (this) {
        SparkSubmitStorageType.BLOB -> "Azure IntelliJ Plugin Job Upload Storage Azure Blob - $account"
        SparkSubmitStorageType.ADLS_GEN2 -> "Azure IntelliJ Plugin Job Upload Storage Azure ADLSGen2 - $account"
        else -> null
    }
}