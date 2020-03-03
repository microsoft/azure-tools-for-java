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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST
import com.microsoft.azure.hdinsight.common.AdlUri
import com.microsoft.azure.hdinsight.common.StreamUtil
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.viewmodels.ComboBoxSelectionDelegated
import com.microsoft.azure.hdinsight.common.viewmodels.ImmutableComboBoxModelDelegated
import com.microsoft.azure.hdinsight.sdk.common.AzureSparkClusterManager
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitJobUploadStorageModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType.ADLS_GEN1
import com.microsoft.azure.hdinsight.spark.ui.SparkSubmissionJobUploadStorageBasicCard.StorageCheckEvent.PathInputFocusLostEvent
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.ui.HintTextField
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.rxjava.IdeaSchedulers
import com.microsoft.intellij.ui.util.UIUtils
import com.microsoft.intellij.ui.util.findFirst
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import rx.schedulers.Schedulers
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ItemEvent
import javax.swing.ComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

class SparkSubmissionJobUploadStorageAdlsCard
    : SparkSubmissionJobUploadStorageBasicCard(ADLS_GEN1.description), ILogger {
    interface Model : SparkSubmissionJobUploadStorageBasicCard.Model {
        var adlsRootPath: String?
        var selectedSubscription: String?
    }

    private val refreshButtonIconPath = "/icons/refresh.png"
    private val adlsRootPathTip = "e.g. adl://myaccount.azuredatalakestore.net/<root path>"
    private val adlsRootPathLabel = JLabel("ADLS Root Path").apply { toolTipText = adlsRootPathTip }
    private val adlsRootPathField = HintTextField(adlsRootPathTip).apply {
        name = "adlsCardRootPathField"
        preferredSize = Dimension(500, 0)

        addFocusListener( object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                viewModel.storageCheckSubject.onNext(PathInputFocusLostEvent(ADLS_GEN1))
            }
        })
    }
    private val authMethodLabel = JLabel("Authentication Method")
    private val authMethodComboBox = ComboBox<String>(arrayOf("Azure Account")).apply { name = "adlsCardAuthMethodComboBox" }
    private val subscriptionsLabel = JLabel("Subscription List")
    private val subscriptionsComboBox  = ComboboxWithBrowseButton(JComboBox(ImmutableComboBoxModel.empty<String>())).apply {
        comboBox.name = "adlsCardSubscriptionsComboBoxCombo"
        button.name = "adlsCardSubscriptionsComboBoxButton"
        button.toolTipText = "Refresh"
        button.icon = StreamUtil.getImageResourceFile(refreshButtonIconPath)
        button.addActionListener {
            //refresh subscriptions after refresh button is clicked
            if (button.isEnabled) {
                button.isEnabled = false
                (viewModel as ViewModel).refreshSubscriptions()
                        .doOnEach { button.isEnabled = true }
                        .subscribe(
                                { },
                                { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
            }
        }

        // after container is selected or new model is set, update upload path
        comboBox.addPropertyChangeListener("model") {
            if ((it.oldValue as? ComboBoxModel<*>)?.selectedItem != (it.newValue as? ComboBoxModel<*>)?.selectedItem) {
                viewModel.storageCheckSubject.onNext(StorageCheckEvent.InputChangedEvent(comboBox))
            }
        }

        comboBox.addItemListener { itemEvent ->
            if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                viewModel.storageCheckSubject.onNext(StorageCheckEvent.InputChangedEvent(comboBox))
            }
        }

        comboBox.renderer = object : SimpleListCellRenderer<Any>() {
            override fun customize(list: JList<out Any>?, value: Any?, index: Int, selected: Boolean, hasFocus: Boolean) {
                font = if (value != null) {
                    text = value.toString()
                    font.deriveFont(Font.PLAIN)
                } else {
                    text = (viewModel as ViewModel).refreshSubscriptionError
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "<$it>" }
                            ?: "<No selection>"
                    font.deriveFont(Font.ITALIC)
                }
            }
        }
    }

    private val signInCard = SparkSubmissionJobUploadStorageAdlsSignInCard().apply {
        signInLink.addActionListener {
            viewModel.storageCheckSubject.onNext(StorageCheckEvent.SignInOutEvent(true))
        }
    }
    private val signOutCard = SparkSubmissionJobUploadStorageAdlsSignOutCard().apply {
        signOutLink.addActionListener {
            viewModel.storageCheckSubject.onNext(StorageCheckEvent.SignInOutEvent(false))
        }
    }
    private val azureAccountCards = JPanel(CardLayout()).apply {
        add(signInCard, signInCard.title)
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
                c(adlsRootPathLabel); c(adlsRootPathField)
            }
            row {
                c(authMethodLabel); c(authMethodComboBox)
            }
            row {
                c(); c(azureAccountCards)
            }
            row {
                c(subscriptionsLabel); c(subscriptionsComboBox)
            }
        }

        formBuilder.buildPanel()
    }

    inner class ViewModel: SparkSubmissionJobUploadStorageBasicCard.ViewModel() {
        var subscriptionSelection: Any? by ComboBoxSelectionDelegated(subscriptionsComboBox.comboBox)
        var subscriptionsModel: ImmutableComboBoxModel<Any> by ImmutableComboBoxModelDelegated(subscriptionsComboBox.comboBox)
        var refreshSubscriptionError: String? = null

        override fun getValidatedStorageUploadPath(config: SparkSubmissionJobUploadStorageBasicCard.Model)
                : String {
            if (config !is Model) {
                return INVALID_UPLOAD_PATH;
            }

            // There are IO operations
            UIUtils.assertInPooledThread()

            if (!AzureSparkClusterManager.getInstance().isSignedIn) {
                throw RuntimeConfigurationError("ADLS Gen 1 storage type requires user to sign in first")
            }

            if (config.adlsRootPath != null && !AdlUri.isType(config.adlsRootPath)) {
                // basic validation for ADLS root path
                // pattern for adl root path. e.g. adl://john.azuredatalakestore.net/root/path/
                throw RuntimeConfigurationError("ADLS Root Path is invalid")
            }

            val adlUri = AdlUri.parse(config.adlsRootPath)
            return adlUri.resolveAsRoot("SparkSubmission/").toString()
        }

        override fun onSelected() {
            // show sign in/out panel based on whether user has signed in or not
            val curLayout = azureAccountCards.layout as CardLayout
            if (AzureSparkClusterManager.getInstance().isSignedIn) {
                curLayout.show(azureAccountCards, signOutCard.title)
                signOutCard.azureAccountLabel.text = AzureSparkClusterManager.getInstance().azureAccountEmail
            } else {
                curLayout.show(azureAccountCards, signInCard.title)
            }
        }

        private val ideaSchedulers = IdeaSchedulers()

        fun refreshSubscriptions(): Observable<ImmutableComboBoxModel<Any>> {
            ApplicationManager.getApplication().assertIsDispatchThread()

            return Observable.just(SparkSubmitJobUploadStorageModel())
                    .doOnNext { getData(it) }
                    // set error message to prevent user from applying the change when refreshing is not completed
                    .observeOn(Schedulers.io())
                    .map { config ->
                        if (!AzureSparkClusterManager.getInstance().isSignedIn) {
                            throw RuntimeConfigurationError("ADLS Gen 1 storage type requires user to sign in first")
                        }

                        val subscriptionManager = AuthMethodManager.getInstance().azureManager.subscriptionManager
                        val subscriptionNameList = subscriptionManager.selectedSubscriptionDetails
                                .sortedBy { it.subscriptionName }
                                .map { subDetail -> subDetail.subscriptionName as Any }
                                .toTypedArray()

                        if (subscriptionNameList.isEmpty()) {
                            throw RuntimeConfigurationError("No subscriptions found in this storage account")
                        }

                        refreshSubscriptionError = null
                        ImmutableComboBoxModel(subscriptionNameList).apply {
                            findFirst { subscriptionName -> subscriptionName == config.selectedSubscription }
                                    ?.let { found -> selectedItem = found}
                        }
                    }
                    .doOnError {
                        log().info("Refresh subscriptions error. $it")

                        refreshSubscriptionError = it.message
                        subscriptionsModel = ImmutableComboBoxModel.empty()

                        storageCheckSubject.onNext(StorageCheckEvent.InputFocusLostEvent(subscriptionsComboBox.comboBox))
                    }
                    .observeOn(ideaSchedulers.dispatchUIThread())
                    .doOnNext { comboModel -> subscriptionsModel = comboModel }
        }
    }

    override fun createViewModel(): ViewModel = ViewModel()

    override fun readWithLock(to: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (to !is Model) {
            return
        }

        to.adlsRootPath = adlsRootPathField.text?.trim()
        to.selectedSubscription = (viewModel as ViewModel).subscriptionSelection?.toString()
    }

    override fun writeWithLock(from: SparkSubmissionJobUploadStorageBasicCard.Model) {
        if (from !is Model) {
            return
        }

        // Only set for changed
        if (adlsRootPathField.text != from.adlsRootPath) {
            adlsRootPathField.text = from.adlsRootPath
        }

        (viewModel as ViewModel).apply {
            val found = subscriptionsModel.findFirst { it == from.selectedSubscription }
            if (found != null) {
                subscriptionSelection = found
            } else {
                subscriptionsModel = ImmutableComboBoxModel<Any>(arrayOf(from.selectedSubscription ?: StringUtils.EMPTY))
            }
        }
    }
}