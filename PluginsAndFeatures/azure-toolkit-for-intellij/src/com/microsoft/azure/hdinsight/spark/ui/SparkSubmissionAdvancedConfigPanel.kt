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

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.icons.AllIcons.Actions.Help
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.InplaceButton
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.Docs
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.mvc.SettableControl
import com.microsoft.azure.hdinsight.common.viewmodels.swingPropertyDelegated
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.HDIException
import com.microsoft.azure.hdinsight.spark.common.DebugParameterDefinedException
import com.microsoft.azure.hdinsight.spark.common.SparkBatchDebugSession
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UseKeyFile
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UsePassword
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.rxjava.DisposableObservers
import com.microsoft.intellij.rxjava.IdeaSchedulers
import org.apache.commons.lang3.StringUtils
import rx.subjects.PublishSubject
import java.awt.Dimension
import java.awt.event.ItemEvent.DESELECTED
import java.awt.event.ItemEvent.SELECTED
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.event.DocumentEvent

// View and Control combined class
class SparkSubmissionAdvancedConfigPanel: JPanel(), SettableControl<SparkSubmitAdvancedConfigModel>, Disposable {
    data class CheckIndicatorState(val message: String? = null, val isRunning: Boolean = false)
    data class CheckResult(val model: SparkSubmitAdvancedConfigModel, val message: String)

    companion object {
        const val passedText = "passed"

        fun isParameterReady(model: SparkSubmitAdvancedConfigModel) =
                model.sshUserName.isNotBlank() && when (model.sshAuthType) {
                    UsePassword -> model.sshPassword.isNotBlank()
                    UseKeyFile -> model.sshKeyFile?.exists() == true
                    else -> false
                }

        fun probeAuth(modelToProbe: SparkSubmitAdvancedConfigModel, clusterToProbe: IClusterDetail? = null): CheckResult = try {
            val clusterDetail = clusterToProbe ?: ClusterManagerEx.getInstance()
                    .getClusterDetailByName(modelToProbe.clusterName)
                    .orElseThrow { HDIException( "No cluster name matched selection: ${modelToProbe.clusterName}") }

            // Verify the certificate
            val debugSession = SparkBatchDebugSession.factoryByAuth(clusterDetail.connectionUrl, modelToProbe)
                    .open()
                    .verifyCertificate()

            debugSession.close()

            CheckResult(modelToProbe, passedText)
        } catch (ex: SparkBatchDebugSession.SshPasswordExpiredException) {
            CheckResult(modelToProbe, "failed (password expired)")
        } catch (ex: Exception) {
            CheckResult(modelToProbe, "failed")
        }

        @Throws(DebugParameterDefinedException::class)
        fun checkSettings(model: SparkSubmitAdvancedConfigModel) {
            if (model.enableRemoteDebug && (!isParameterReady(model) || probeAuth(model).message != passedText)) {
                throw DebugParameterDefinedException("SSH Authentication is failed")
            }
        }
    }

    // FIXME!!! Since the Intellij has no locale setting, just set en-us here.
    private val helpUrl: String = Docs(Locale.US).getDocUrlByTopic(Docs.TOPIC_CONNECT_HADOOP_LINUX_USING_SSH)

    private val sshUserNameTip = "Secure shell (SSH) user name used in Spark remote debugging, by default using `sshuser`"
    private val enableRemoteDebugTip = "Enable Spark remote debug, use with caution since this might override data previously generated"
    private val authTypeTip = "Secure shell (SSH) authentication type used in Spark remote debugging, by default using the password"
    private val usePasswordTip = "For secure shell (SSH) password, use the password specified here"
    private val useKeyFileToolTip = "For secure shell (SSH) password, use the key file specified here"

    private val enableRemoteDebugCheckBox = JCheckBox("Enable Spark remote debug", true).apply {
        name = "enableRemoteDebugCheckBox"
        toolTipText = enableRemoteDebugTip
        isSelected = false
    }

    private val sshUserNameLabel = JLabel("Secure Shell (SSH) User Name:").apply {
        toolTipText = sshUserNameTip
    }
    private val sshUserNameTextField = JTextField("sshuser").apply {
        name = "sshUserNameTextField"
        toolTipText = sshUserNameTip
        preferredSize = Dimension(500, 0)
    }

    private val sshAuthTypeLabel = JLabel("Secure Shell (SSH) Auth Type").apply {
        toolTipText = authTypeTip
    }

    // Password inputs
    private val sshUsePasswordRadioButton = JRadioButton("Use SSH password:", false).apply {
        name = "sshUsePasswordRadioButton"
        toolTipText = usePasswordTip
        isSelected = true
    }
    private val sshPasswordField = JPasswordField().apply {
        name = "sshPasswordField"
        toolTipText = usePasswordTip
        // FIXME!!! The change won't take effects until IntelliJ fixes DarculaPasswordFieldUI::getMinimumSize() issue
        preferredSize = Dimension(500, 0)
    }

    // Key file inputs
    private val sshUseKeyFileRadioButton = JRadioButton("Use private key file:", false).apply {
        name = "sshUseKeyFileRadioButton"
        toolTipText = useKeyFileToolTip
        isSelected = false
    }
    private val sshKeyFileTextField = TextFieldWithBrowseButton().apply {
        textField.name = "sshKeyFileTextFieldText"
        button.name = "sshKeyFileTextFieldButton"
        toolTipText = useKeyFileToolTip
    }

    // Group password and key file inputs
    private val sshAuthGroup = ButtonGroup().apply {
        add(sshUsePasswordRadioButton)
        add(sshUseKeyFileRadioButton)
    }

    val checkSshCertIndicator = BackgroundTaskIndicator("Verify SSH Authentication...")

    private val helpButton = InplaceButton(IconButton("Help about connection to HDInsight using SSH", Help)) {
        BrowserUtil.browse(helpUrl)
    }.apply { name = "remoteDebugHelpButton" }

    private val formBuilder = panel {
        columnTemplate {
            col {   // Column 0
                anchor = ANCHOR_WEST
            }
            col {   // Column 1
                anchor = ANCHOR_WEST
            }
            col {   // Column 2
                hSizePolicy = SIZEPOLICY_WANT_GROW
                fill = FILL_HORIZONTAL
            }
        }
        row {
            c(enableRemoteDebugCheckBox) { indent = 2 };        c(helpButton) {};   c(checkSshCertIndicator) {}
        }
        row {
              c(sshUserNameLabel) { indent = 3 };               c(null) {};         c(sshUserNameTextField) {}
        }
        row {
              c(sshAuthTypeLabel) { indent = 3 }
        }
        row {
                c(sshUsePasswordRadioButton) { indent = 4 };    c(null) {};         c(sshPasswordField) {}
        }
        row {
                c(sshUseKeyFileRadioButton) { indent = 4 };     c(null) {};         c(sshKeyFileTextField) {}
        }
    }

    inner class ViewModel: DisposableObservers(), ILogger {
        val sshCheckSubject: PublishSubject<String> = disposableSubjectOf { PublishSubject.create() }
        val clusterSelectedSubject: PublishSubject<IClusterDetail> = disposableSubjectOf { PublishSubject.create() }

        private var checkStatus: CheckIndicatorState by swingPropertyDelegated(
                getter = { CheckIndicatorState(checkSshCertIndicator.text, false) },
                setterInDispatch = { _, v -> checkSshCertIndicator.setTextAndStatus(v.message, v.isRunning) })

        private val ideaSchedulers = IdeaSchedulers()

        init {
            rx.Observable
                    .combineLatest(
                            sshCheckSubject
                                    .throttleWithTimeout(500, TimeUnit.MILLISECONDS)
                                    .doOnNext { log().info("Receive checking message $it") },
                            clusterSelectedSubject) { _, cluster -> cluster }
                    .filter { it != null }
                    .map { it -> Pair(advConfModel.apply { clusterName = it!!.name }, it) }
                    .filter { (model, _) -> model.enableRemoteDebug && isParameterReady(model) }
                    .observeOn(ideaSchedulers.dispatchUIThread())
                    .doOnNext { (_, cluster) ->
                        log().info("Check SSH authentication for cluster ${cluster.name} ...")
                        checkStatus = CheckIndicatorState("SSH Authentication is checking...", true)
                    }
                    .observeOn(ideaSchedulers.dispatchPooledThread())
                    .map { (model, cluster) -> probeAuth(model, cluster) }
                    .observeOn(ideaSchedulers.dispatchUIThread())
                    .subscribe { (probedModel, message) ->
                        log().info("...Result: $message")

                        // Double check the the result is met the current user input
                        val checkingResultMessage = if (probedModel == advConfModel) {
                            // Checked parameter is matched with current content
                            "SSH Authentication is $message"
                        } else {
                            ""
                        }

                        checkStatus = CheckIndicatorState(checkingResultMessage, false)
                    }
        }
    }

    val viewModel = ViewModel().apply { Disposer.register(this@SparkSubmissionAdvancedConfigPanel, this@apply) }

    init {
        setSshAuthenticationUIEnabled(false)

        // Add all components according to the layout plan
        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }

        // To enable/disable all options
        enableRemoteDebugCheckBox.addItemListener {
            setSshAuthenticationUIEnabled(it.stateChange == SELECTED)

            if (it.stateChange == SELECTED) {
                viewModel.sshCheckSubject.onNext("Enabled remote debug")
            }
        }

        // To trigger SSH authentication background check
        val inputListener = object : DocumentAdapter() {
            override fun textChanged(ev: DocumentEvent) = viewModel.sshCheckSubject.onNext(ev.document.toString())
        }

        sshUserNameTextField.document.addDocumentListener(inputListener)
        sshPasswordField.document.addDocumentListener(inputListener)
        sshKeyFileTextField.textField.document.addDocumentListener(inputListener)

        sshUsePasswordRadioButton.addActionListener { viewModel.sshCheckSubject.onNext(it.toString()) }
        sshUseKeyFileRadioButton.addActionListener { viewModel.sshCheckSubject.onNext(it.toString()) }

        // To change SSH authentication type
        sshUsePasswordRadioButton.addItemListener { setSshPasswordInputEnabled(it.stateChange == SELECTED) }
        sshUseKeyFileRadioButton.addItemListener { setSshPasswordInputEnabled(it.stateChange == DESELECTED) }

        // To popup the key file chooser dialog
        sshKeyFileTextField.button.addActionListener { showSshKeyFileChooser() }
    }

    private fun setSshPasswordInputEnabled(isEnabled: Boolean) {
        sshPasswordField.isEnabled = isEnabled
        sshKeyFileTextField.isEnabled = !isEnabled
    }

    private fun setSshAuthenticationUIEnabled(isEnabled: Boolean) {
        sshUserNameTextField.isEnabled = isEnabled
        sshUserNameLabel.isEnabled = isEnabled
        sshAuthTypeLabel.isEnabled = isEnabled

        sshUsePasswordRadioButton.isEnabled = isEnabled
        sshUseKeyFileRadioButton.isEnabled = isEnabled

        sshPasswordField.isEnabled = isEnabled
        sshKeyFileTextField.isEnabled = isEnabled

        if (isEnabled) {
            val currentSelection = sshAuthGroup.selection
            sshAuthGroup.clearSelection()
            currentSelection.isSelected = true
        } else {
            checkSshCertIndicator.setTextAndStatus("", false)
            sshPasswordField.text = ""
            sshKeyFileTextField.text = ""
        }
    }

    val advConfModel: SparkSubmitAdvancedConfigModel
            get() = SparkSubmitAdvancedConfigModel().apply { getData(this) }

    private fun showSshKeyFileChooser() {
        val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .apply { title = "Select SSH Key File" }

        FileChooser.chooseFile(fileChooserDescriptor, null, null)?.run { sshKeyFileTextField.text = path }
    }

    override fun setData(data: SparkSubmitAdvancedConfigModel) {
        // Data -> Component
        val applyData: () -> Unit = {
            val current = advConfModel

            val password = data.sshPassword
            if (current.sshPassword != password) {
                sshPasswordField.text = password ?: ""
            }

            if (data.sshKeyFile != null && data.sshKeyFile.exists()) {
                if ((current.sshKeyFile?.absolutePath ?: "") != data.sshKeyFile.absolutePath) {
                    sshKeyFileTextField.text = data.sshKeyFile.absolutePath
                }
            }

            if (current.sshAuthType != data.sshAuthType) {
                when (data.sshAuthType) {
                    UsePassword -> {
                        sshUsePasswordRadioButton.isSelected = true
                        sshUseKeyFileRadioButton.isSelected = false
                    }
                    else -> {
                        sshUsePasswordRadioButton.isSelected = false
                        sshUseKeyFileRadioButton.isSelected = true
                    }
                }
            }

            if (current.sshUserName != data.sshUserName && data.sshUserName != null && data.sshUserName.isNotEmpty()) {
                sshUserNameTextField.text = data.sshUserName
            }

            // Keep this line at the ending
            if (current.enableRemoteDebug != data.enableRemoteDebug) {
                enableRemoteDebugCheckBox.isSelected = data.enableRemoteDebug
            }
        }

        ApplicationManager.getApplication().invokeLater(applyData, ModalityState.any())
    }

    override fun getData(data: SparkSubmitAdvancedConfigModel) {
        // Component -> Data
        if (StringUtils.isNotBlank(String(sshPasswordField.password?: CharArray(0)))) {
            data.sshPassword = String(sshPasswordField.password)
        }

        if (StringUtils.isNotBlank(sshKeyFileTextField.text)) {
            val keyFile = File(sshKeyFileTextField.text)
            if (keyFile.exists() && !keyFile.isDirectory) {
                data.sshKeyFile = keyFile
            }
        }

        if (sshUsePasswordRadioButton.isSelected) {
            data.sshAuthType = UsePassword
        } else if (sshUseKeyFileRadioButton.isSelected) {
            data.sshAuthType = UseKeyFile
        }

        if (StringUtils.isNotBlank(sshUserNameTextField.text)) {
            data.sshUserName = sshUserNameTextField.text
        }

        // Keep this line at the ending
        data.enableRemoteDebug = enableRemoteDebugCheckBox.isSelected
    }

    override fun dispose() {
    }
}
