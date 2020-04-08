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
import com.intellij.openapi.ui.ComboBox
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.WEBHDFS
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageBasicCard.StorageCheckEvent.PathInputFocusLostEvent
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail
import com.microsoft.azuretools.ijidea.ui.HintTextField
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.ui.util.UIUtils
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JLabel
import javax.swing.JPanel

class SparkSubmissionJobUploadStorageWebHdfsCard: SparkSubmissionJobUploadStorageBasicCard(WEBHDFS.description) {
    interface Model: SparkSubmissionJobUploadStorageBasicCard.Model {
        var webHdfsRootPath: String?
    }

    private val webHdfsRootPathTip = "e.g. http(s)://hdfsnamenode:port/webhdfs/v1/<cluster root directory>"
    private val webHdfsRootPathLabel = JLabel("WEBHDFS Root Path").apply { toolTipText = webHdfsRootPathTip }
    private val webHdfsRootPathField = HintTextField(webHdfsRootPathTip).apply {
        name = "webHdfsCardRootPathTextField"
        preferredSize = Dimension(500, 0)

        addFocusListener( object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                viewModel.storageCheckSubject.onNext(PathInputFocusLostEvent(WEBHDFS))
            }
        })
    }

    private val authMethodLabel = JLabel("Authentication Method")
    private val authMethodComboBox = ComboBox<String>(arrayOf("Basic Authorization")).apply { name = "webHdfsCardAuthMethodComboBox" }
    private val signOutCard = SparkSubmissionJobUploadWebHdfsSignOutCard()
    private val authAccountForWebHdfsCards = JPanel(CardLayout()).apply {
        add(signOutCard, signOutCard.title)
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
                c(webHdfsRootPathLabel); c(webHdfsRootPathField)
            }
            row {
                c(authMethodLabel); c(authMethodComboBox)
            }
            row {
               c(); c(authAccountForWebHdfsCards)
            }
        }

        formBuilder.buildPanel()
    }

    override fun createViewModel(): ViewModel = object : ViewModel() {
        override var cluster: IClusterDetail?
            get() = super.cluster
            set(value) {
                signOutCard.authUserNameLabel.text = when (value) {
                    is SqlBigDataLivyLinkClusterDetail -> value.httpUserName
                    else -> SparkSubmissionJobUploadWebHdfsSignOutCard.defaultAuthUser
                }

                super.cluster = value
            }

        override fun getValidatedStorageUploadPath(config: SparkSubmissionJobUploadStorageBasicCard.Model)
                : String {
            if (config !is Model) {
                return INVALID_UPLOAD_PATH
            }

            // There are IO operations
            UIUtils.assertInPooledThread()

            //pattern for webhdfs root path.e.g http://host/webhdfs/v1/
            val rootPath = config.webHdfsRootPath?.trim()
                    ?: throw RuntimeConfigurationError("Webhdfs root path is blank")

            if (!SparkBatchJob.WebHDFSPathPattern.toRegex().matches(rootPath)) {
                throw RuntimeConfigurationError("Webhdfs root path is not valid")
            }

            val formatWebHdfsRootPath = if (rootPath.endsWith("/")) rootPath.trimEnd('/') else rootPath
            return "$formatWebHdfsRootPath/SparkSubmission/"
        }
    }

    override fun readWithLock(to: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (to !is Model) {
            return
        }

        to.webHdfsRootPath = webHdfsRootPathField.text?.trim()
    }

    override fun writeWithLock(from: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (from !is Model) {
            return
        }

        if (webHdfsRootPathField.text != from.webHdfsRootPath) {
            webHdfsRootPathField.text = from.webHdfsRootPath
        }

        // show sign in/out panel based on whether user has signed in or not
        val curLayout = authAccountForWebHdfsCards.layout as CardLayout
        curLayout.show(authAccountForWebHdfsCards, signOutCard.title)
    }
}