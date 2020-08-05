/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage

import com.microsoft.azuretools.utils.WebAppUtils
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener
import java.util.logging.Logger

class EmulatorStorageNode(parent: StorageModule) : ExternalStorageNode(parent, emulatorStorageAccount) {
    companion object {
        private val logger = Logger.getLogger(EmulatorStorageNode::class.java.name)

        // https://docs.microsoft.com/en-us/azure/storage/common/storage-use-emulator
        private const val connectionName = "devstoreaccount1"
        private const val protocol = "http"
        private const val primaryKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
        private const val blobsUri = "http://127.0.0.1:10000/devstoreaccount1"
        private const val tablesUri = "http://127.0.0.1:10002/devstoreaccount1"
        private const val queuesUri = "http://127.0.0.1:10001/devstoreaccount1"

        private val emulatorStorageAccount = ClientStorageAccount(connectionName)
                .apply {
                    isUseCustomEndpoints = true
                    protocol = Companion.protocol
                    primaryKey = Companion.primaryKey
                    blobsUri = Companion.blobsUri
                    tablesUri = Companion.tablesUri
                    queuesUri = Companion.queuesUri
                }
    }

    override fun getName(): String {
        return "Storage Emulator"
    }

    override fun initActions(): Map<String, Class<out NodeActionListener>>? {
        return emptyMap()
    }

    override fun refreshItems() {
        if (!isUrlAccessible(blobsUri) && !isUrlAccessible(tablesUri) && !isUrlAccessible(queuesUri)) {
            logger.info("Unable to get any of storage emulator URLs: '$blobsUri', '$tablesUri', '$queuesUri'.")
            DefaultLoader.getUIHelper().showError(
                    "Please make sure local storage emulator is running. Unable to connect to blob ($blobsUri), table ($tablesUri), queue ($queuesUri) storage emulator instances.",
                    "Unable to connect to local storage emulator"
            )
            return
        }
        super.refreshItems()
    }

    private fun isUrlAccessible(url: String): Boolean =
            WebAppUtils.isUrlAccessible(url)
}
