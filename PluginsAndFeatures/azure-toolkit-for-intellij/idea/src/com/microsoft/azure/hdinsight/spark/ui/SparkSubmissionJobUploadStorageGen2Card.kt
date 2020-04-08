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
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST
import com.microsoft.azure.hdinsight.common.AbfsUri
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.MfaEspCluster
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.ADLS_GEN2
import com.microsoft.azure.hdinsight.spark.common.getSecureStoreServiceOf
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageBasicCard.StorageCheckEvent.PathInputFocusLostEvent
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.ui.HintTextField
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.ui.util.UIUtils
import org.apache.commons.lang3.StringUtils
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JLabel

class SparkSubmissionJobUploadStorageGen2Card : SparkSubmissionJobUploadStorageBasicCard(ADLS_GEN2.description) {
    interface Model: SparkSubmissionJobUploadStorageBasicCard.Model {
        var gen2RootPath: AbfsUri?
        var gen2Account: String?
        var accessKey: String?
    }

    private val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)
    private val storageKeyTip = "The access key of the default storage account, which can be found from HDInsight cluster storage accounts of Azure portal."
    private val storageKeyLabel = JLabel("Access Key").apply { toolTipText = storageKeyTip }
    val storageKeyField = ExpandableTextField().apply { toolTipText = storageKeyTip; name = "gen2CardstorageKeyField" }
    private val gen2RootPathTip = "e.g. abfs://<file_system>@<account_name>.dfs.core.windows.net/<path>"
    private val gen2RootPathLabel = JLabel("ADLS GEN2 Root Path")
    val gen2RootPathField = HintTextField (gen2RootPathTip).apply {
        name = "gen2CardRootPathField"
        preferredSize = Dimension(500, 0)

        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                viewModel.storageCheckSubject.onNext(PathInputFocusLostEvent(ADLS_GEN2))
            }
        })
    }

    override val view by lazy {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = ANCHOR_WEST
                }
                col {
                    anchor = ANCHOR_WEST
                    hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                    fill = GridConstraints.FILL_HORIZONTAL
                }
            }
            row {
                c(gen2RootPathLabel.apply { labelFor = gen2RootPathField });    c(gen2RootPathField)
            }
            row {
                c(storageKeyLabel.apply { labelFor = storageKeyField });        c(storageKeyField)
            }
        }

        formBuilder.buildPanel()
    }

    inner class ViewModel: SparkSubmissionJobUploadStorageBasicCard.ViewModel() {
        val rootUri: AbfsUri?
            get() {
                val rootPathText = gen2RootPathField.text?.trim()
                return if (AbfsUri.isType(rootPathText)) AbfsUri.parse(rootPathText) else null
            }

        override var cluster: IClusterDetail?
            get() = super.cluster
            set(cluster) {
                val gen2Account = cluster?.storageAccount?.name ?: return

                if (!storageKeyField.text.isNullOrBlank()) {
                    val viewModel = viewModel as? ViewModel ?: return
                    val credentialAccount = ADLS_GEN2.getSecureStoreServiceOf(viewModel.rootUri?.accountName) ?: return
                    storageKeyField.text = secureStore?.loadPassword(credentialAccount, gen2Account) ?: ""
                }

                super.cluster = cluster
            }

        override fun getValidatedStorageUploadPath(config: SparkSubmissionJobUploadStorageBasicCard.Model)
                : String {
            if (config !is Model) {
                return INVALID_UPLOAD_PATH
            }

            // There are IO operations
            UIUtils.assertInPooledThread()

            val rootPath = config.gen2RootPath ?: throw RuntimeConfigurationError("ADLS GEN2 Root Path is invalid")

            if (!AuthMethodManager.getInstance().isSignedIn) {
                throw RuntimeConfigurationError("Need to use azure account to login in first")
            }

            val cluster = this.cluster
            val homeUri = if (cluster is MfaEspCluster) rootPath.resolveAsRoot(cluster.userPath) else rootPath
            return homeUri.resolveAsRoot("SparkSubmission/").toString()
        }
    }

    override fun createViewModel(): SparkSubmissionJobUploadStorageBasicCard.ViewModel = ViewModel()

    override fun readWithLock(to: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (to !is Model) {
            return
        }

        to.gen2RootPath = (viewModel as ViewModel).rootUri
        to.gen2Account = to.gen2RootPath?.accountName
        to.accessKey = storageKeyField.text?.trim()
    }

    override fun writeWithLock(from: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (from !is Model) {
            return
        }

        if (from.gen2RootPath?.toString() != gen2RootPathField.text.trim()) {
            gen2RootPathField.text = from.gen2RootPath?.uri?.toString() ?: ""
        }

        val credentialAccount = ADLS_GEN2.getSecureStoreServiceOf(from.gen2RootPath?.accountName)
        storageKeyField.text =
                if (StringUtils.isBlank(from.accessKey)) {
                    credentialAccount?.let { secureStore?.loadPassword(credentialAccount, from.gen2Account) ?: "" }
                } else {
                    from.accessKey
                }
    }
}