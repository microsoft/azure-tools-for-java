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
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST
import com.microsoft.azure.hdinsight.common.AbfsUri
import com.microsoft.azure.hdinsight.sdk.cluster.MfaEspCluster
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.ADLS_GEN2_FOR_OAUTH
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageBasicCard.StorageCheckEvent.PathInputFocusLostEvent
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.ui.HintTextField
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.ui.util.UIUtils
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JLabel

class SparkSubmissionJobUploadStorageGen2OAuthCard
    : SparkSubmissionJobUploadStorageBasicCard(ADLS_GEN2_FOR_OAUTH.description) {
    interface Model: SparkSubmissionJobUploadStorageBasicCard.Model {
        var gen2RootPath: AbfsUri?
    }

    private val gen2RootPathTip = "e.g. abfs://<file_system>@<account_name>.dfs.core.windows.net/<path>"
    private val gen2RootPathLabel = JLabel("ADLS GEN2 Root Path")
    private val gen2RootPathField = HintTextField (gen2RootPathTip).apply {
        name = "gen2OAuthCardRootPathField"
        preferredSize = Dimension(500, 0)

        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                viewModel.storageCheckSubject.onNext(PathInputFocusLostEvent(ADLS_GEN2_FOR_OAUTH))
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
        }

        formBuilder.buildPanel()
    }

    override fun createViewModel(): ViewModel = object : ViewModel() {
        override fun getValidatedStorageUploadPath(config: SparkSubmissionJobUploadStorageBasicCard.Model)
                : String {
            if (config !is SparkSubmissionJobUploadStorageGen2Card.Model) {
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

    override fun readWithLock(to: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (to !is Model) {
            return
        }

        val rootPathText = gen2RootPathField.text?.trim()
        to.gen2RootPath = if (AbfsUri.isType(rootPathText)) AbfsUri.parse(rootPathText) else null
    }

    override fun writeWithLock(from: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (from !is Model) {
            return
        }

        val gen2PathText = gen2RootPathField.text?.trim()
        val parsedGen2Path = if (AbfsUri.isType(gen2PathText)) AbfsUri.parse(gen2PathText) else null
        if (from.gen2RootPath != parsedGen2Path) {
            gen2RootPathField.text = from.gen2RootPath?.uri?.toString() ?: ""
        }
    }
}