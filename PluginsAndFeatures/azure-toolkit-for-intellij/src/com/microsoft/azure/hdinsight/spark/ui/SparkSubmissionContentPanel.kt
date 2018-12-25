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
 */

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.jar.JarFileSystemImpl
import com.intellij.packaging.artifacts.Artifact
import com.intellij.packaging.impl.artifacts.ArtifactUtil
import com.intellij.packaging.impl.elements.ManifestFileUtil
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.uiDesigner.core.GridConstraints.*
import com.microsoft.azure.hdinsight.common.DarkThemeManager
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.mvc.SettableControl
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionJobConfigCheckStatus
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionJobConfigCheckStatus.Error
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionJobConfigCheckStatus.Warning
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitHelper
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.SubmissionTableModel
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.azurecommons.helpers.StringHelper
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.helpers.ManifestFileUtilsEx
import com.microsoft.intellij.lang.containsInvisibleChars
import com.microsoft.intellij.lang.tagInvisibleChars
import com.microsoft.intellij.rxjava.DisposableObservers
import com.microsoft.intellij.ui.util.findFirst
import org.apache.commons.lang3.StringUtils
import rx.Subscription
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.io.IOException
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.streams.toList

/**
 * Spark Batch Application Submission UI
 */
open class SparkSubmissionContentPanel(private val myProject: Project, val type: String = "HDInsight Spark") :
        SettableControl<SparkSubmitModel>, ILogger, Disposable {
    private enum class ErrorMessage {
        ClusterName,
        SystemArtifact,
        LocalArtifact,
        MainClass,
        JobConfiguration
        // Don't add more Error Message please, throw Configuration Exception in checkInputs()
    }

    private val documentValidationListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) = checkInputsWithErrorLabels()
        override fun removeUpdate(e: DocumentEvent) = checkInputsWithErrorLabels()
        override fun changedUpdate(e: DocumentEvent) = checkInputsWithErrorLabels()
    }

    private val isSignedIn: Boolean
        get() = try {
            AuthMethodManager.getInstance().isSignedIn
        } catch (ignored: IOException) {
            false
        }

    // All view components
    private val errorMessageLabels = arrayOf(
            JLabel(if (isSignedIn) "Cluster Name Should not be null, please choose one for submission"
            else "Can't list cluster, please login within Azure Explorer (View -> Tool Windows -> Azure Explorer) and refresh")
                    .apply { foreground = currentErrorColor },
            JLabel("Artifact should not be null!")
                    .apply { foreground = currentErrorColor },
            JLabel("Could not find the local jar package for Artifact")
                    .apply { foreground = currentErrorColor },
            JLabel("Main Class Name should not be null")
                    .apply {
                        foreground = currentErrorColor
                        isVisible = true
                    },
            JLabel().apply { foreground = currentErrorColor }
            // Don't add more we won't like to add more message labels
    )

    val clustersSelectionPrompt: JLabel = JLabel("Spark clusters(Linux only)").apply {
        toolTipText = "The $type cluster you want to submit your application to. Only Linux cluster is supported."
    }

    protected open val clustersSelection by lazy { SparkClusterListRefreshableCombo().apply {
        Disposer.register(this@SparkSubmissionContentPanel, this@apply)
    }}

    private val artifactSelectLabel: JLabel = JLabel("Select an Artifact to submit").apply {
        toolTipText = "The Artifact you want to use"
    }

    private val selectedArtifactComboBox: ComboBox<Artifact> = ComboBox<Artifact>(
            ArtifactUtil.getArtifactWithOutputPaths(myProject).toTypedArray()
    ).apply {
        toolTipText = artifactSelectLabel.toolTipText

        if (itemCount > 0) {
            selectedIndex = 0
        }

        renderer = object: ListCellRendererWrapper<Artifact>() {
            override fun customize(list: JList<*>?, artifact: Artifact?, index: Int, selected: Boolean, hasFocus: Boolean) {
                setText(artifact?.name)
            }
        }

        addItemListener {
            checkInputsWithErrorLabels()
        }
    }

    private val localArtifactTextField: TextFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        toolTipText = "Artifact from local jar package."
        isEnabled = false
        textField.document.addDocumentListener(documentValidationListener)

        button.addActionListener {
            val chooserDescriptor = FileChooserDescriptor(false, false, true, false, true, false).apply {
                title = "Select Local Artifact File"
            }

            val chooseFile = FileChooser.chooseFile(chooserDescriptor, null, null)
            val path = chooseFile?.path ?: return@addActionListener
            val normalizedPath = if (path.endsWith("!/")) path.substring(0, path.length - 2) else path

            text = normalizedPath
        }
    }

    private val ideaArtifactPrompt: JRadioButton = JRadioButton("Artifact from IntelliJ project:", true).apply {
        addItemListener {
            selectedArtifactComboBox.isEnabled = it.stateChange == ItemEvent.SELECTED
            checkInputsWithErrorLabels()
        }

        isSelected = true
    }

    private val localArtifactPrompt: JRadioButton = JRadioButton("Artifact from local disk:", false).apply {
        addItemListener {
            localArtifactTextField.isEnabled = it.stateChange == ItemEvent.SELECTED
            checkInputsWithErrorLabels()
        }

        isSelected = false
    }

    val artifactTypeGroup: ButtonGroup = ButtonGroup().apply {
        add(ideaArtifactPrompt)
        add(localArtifactPrompt)
    }

    private val mainClassPrompt: JLabel = JLabel("Main class name").apply {
        toolTipText = "Application's java/spark main class"
    }

    private val mainClassTextField: TextFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        toolTipText = mainClassPrompt.toolTipText
        textField.document.addDocumentListener(documentValidationListener)

        // Button actions
        addActionListener {
            val selected = if (localArtifactPrompt.isSelected)
                ManifestFileUtilsEx(myProject).selectMainClass(
                        JarFileSystemImpl().findFileByPath("${localArtifactTextField.text}!/"))
            else
                ManifestFileUtil.selectMainClass(myProject, text)

            if (selected != null) {
                setText(selected.qualifiedName)
            }
        }
    }

    private val jobConfigPrompt: JLabel = JLabel("Job configurations")

    private val jobConfigurationTable: JBTable = JBTable(SubmissionTableModel()).apply {
        preferredScrollableViewportSize = Dimension(580, 100)

        surrendersFocusOnKeystroke = true
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        columnSelectionAllowed = true
        fillsViewportHeight = true

        addPropertyChangeListener { checkInputsWithErrorLabels() }
    }

    private val jobConfTableScrollPane: JBScrollPane = JBScrollPane(jobConfigurationTable).apply {
        minimumSize = jobConfigurationTable.preferredScrollableViewportSize
    }

    private val commandLineArgsPrompt: JLabel = JLabel("Command line arguments").apply {
        toolTipText = "Command line arguments used in your main class; multiple arguments should be split by space."
    }

    private val commandLineTextField: JTextField = JTextField().apply {
        toolTipText = commandLineArgsPrompt.toolTipText
    }

    private val refJarsPrompt: JLabel = JLabel("Referenced Jars(spark.jars)").apply {
        toolTipText = "Files to be placed on the java classpath; The path needs to be an Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;)"
    }

    private val referencedJarsTextField: JTextField = JTextField().apply {
        toolTipText = refJarsPrompt.toolTipText
    }

    private val refFilesPrompt: JLabel = JLabel("Referenced Files(spark.files)").apply {
        toolTipText = "Files to be placed in executor working directory. The path needs to be an Azure Blob Storage Path (path started with wasb://); Multiple paths should be split by semicolon (;) "
    }

    private val referencedFilesTextField: JTextField = JTextField().apply {
        toolTipText = refFilesPrompt.toolTipText
    }

    private val storageWithUploadPathPanel: SparkSubmissionJobUploadStorageWithUploadPathPanel =
            SparkSubmissionJobUploadStorageWithUploadPathPanel().apply {
                Disposer.register(this@SparkSubmissionContentPanel, this@apply)
            }

    private val currentErrorColor
        get() = DarkThemeManager.getInstance().errorMessageColor

    private val currentWarningColor
        get() = DarkThemeManager.getInstance().warningMessageColor

    private fun setVisibleForFixedErrorMessage(label: ErrorMessage,
                                               isVisible: Boolean,
                                               overwriteMessage: String? = null,
                                               status: SparkSubmissionJobConfigCheckStatus = Error) {
        if (!StringHelper.isNullOrWhiteSpace(overwriteMessage)) {
            errorMessageLabels[label.ordinal].text = overwriteMessage
        }

        errorMessageLabels[label.ordinal].foreground = when (status) {
            Error -> currentErrorColor
            Warning -> currentWarningColor
        }

        errorMessageLabels[label.ordinal].isVisible = isVisible
    }

    private fun hideAllErrors() {
        for (errorMessageLabel in ErrorMessage.values()) {
            setVisibleForFixedErrorMessage(errorMessageLabel, false)
        }
    }

    @Synchronized
    private fun checkInputsWithErrorLabels() {
        ApplicationManager.getApplication().invokeLater({
            // Clean all error messages firstly
            hideAllErrors()

            // Check Cluster selection
            if ((viewModel.clusterSelection.toSelectClusterByIdBehavior.value as? String).isNullOrBlank()) {
                setVisibleForFixedErrorMessage(ErrorMessage.ClusterName, true)
            }

            if (ideaArtifactPrompt.isSelected) {
                // Check Intellij artifact
                if (selectedArtifactComboBox.selectedItem == null) {
                    setVisibleForFixedErrorMessage(ErrorMessage.SystemArtifact, true)
                }
            }

            if (localArtifactPrompt.isSelected) {
                // Check local jar artifact
                if (StringHelper.isNullOrWhiteSpace(localArtifactTextField.text)) {
                    setVisibleForFixedErrorMessage(ErrorMessage.LocalArtifact, true)
                }

                if (!SparkSubmitHelper.isLocalArtifactPath(localArtifactTextField.text)) {
                    setVisibleForFixedErrorMessage(ErrorMessage.LocalArtifact, true)
                }
            }

            // Check main class input
            if (StringUtils.isBlank(mainClassTextField.text)) {
                setVisibleForFixedErrorMessage(ErrorMessage.MainClass, true)
            }

            // Check job config table
            val confTableModel = jobConfigurationTable.model as SubmissionTableModel
            val result = confTableModel.firstCheckResults
            if (result != null) {
                setVisibleForFixedErrorMessage(ErrorMessage.JobConfiguration, true, result.messaqge, Warning)
            }
        }, ModalityState.any())
    }

    @Throws(ConfigurationException::class)
    fun validateInputs() {
        val confTableModel = jobConfigurationTable.model as SubmissionTableModel
        for (confEntry in confTableModel.jobConfigMap) {
            val entryKey = confEntry.first()

            if (StringUtils.isBlank(confEntry.first()) && StringUtils.isNotBlank(confEntry.second())) {
                throw ConfigurationException("The Spark config key shouldn't be empty for value: " + confEntry.second())
            }

            if (!StringUtils.isAlpha(entryKey.substring(0, 1)) && !StringUtils.startsWith(entryKey, "_")) {
                throw ConfigurationException("The Spark config key should start with a letter or underscore")
            }

            if (!StringUtils.containsOnly(entryKey.toLowerCase(), "abcdefghijklmnopqrstuvwxyz1234567890_-.")) {
                throw ConfigurationException("The Spark config key should only contains letters, digits, hyphens, underscores, and periods: ($entryKey)")
            }
        }

        // Check for command arguments invisible chars
        if (commandLineTextField.text.containsInvisibleChars()) {
            throw ConfigurationException("Found invisible chars (not space) in Command line arguments(tagged with []): " +
                    commandLineTextField.text.tagInvisibleChars("[]"))
        }
    }

    private val submissionPanel : JPanel by lazy {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = ANCHOR_WEST
                    fill = FILL_NONE
                }
                col {
                    anchor = ANCHOR_WEST
                    hSizePolicy = SIZEPOLICY_WANT_GROW
                    fill = FILL_HORIZONTAL
                }
            }
            row { c(clustersSelectionPrompt);             c(clustersSelection.component) }
            row { c();                                    c(errorMessageLabels[ErrorMessage.ClusterName.ordinal]) { fill = FILL_NONE } }
            row { c(artifactSelectLabel) }
            row {   c(ideaArtifactPrompt) { indent = 1 }; c(selectedArtifactComboBox) }
            row {   c();                                  c(errorMessageLabels[ErrorMessage.SystemArtifact.ordinal]) { fill = FILL_NONE } }
            row {   c(localArtifactPrompt){ indent = 1 }; c(localArtifactTextField) }
            row {   c();                                  c(errorMessageLabels[ErrorMessage.LocalArtifact.ordinal]) { fill = FILL_NONE }}
            row { c(mainClassPrompt);                     c(mainClassTextField) }
            row { c();                                    c(errorMessageLabels[ErrorMessage.MainClass.ordinal]) }
            row { c(jobConfigPrompt);                     c(jobConfTableScrollPane) }
            row { c();                                    c(errorMessageLabels[ErrorMessage.JobConfiguration.ordinal]) }
            row { c(commandLineArgsPrompt);               c(commandLineTextField) }
            row { c(refJarsPrompt);                       c(referencedJarsTextField) }
            row { c(refFilesPrompt);                      c(referencedFilesTextField) }
            row { c(storageWithUploadPathPanel) { colSpan = 2; fill = FILL_HORIZONTAL }; }
        }

        formBuilder.buildPanel()
    }

    open val component: JComponent
        get() = submissionPanel

    interface Control : SparkSubmissionJobUploadStorageWithUploadPathPanel.Control

    val control: Control by lazy { SparkSubmissionContentPanelControl(storageWithUploadPathPanel.control) }

    open inner class ViewModel: DisposableObservers() {
        val clusterSelection by lazy { clustersSelection.viewModel.apply {
            clusterIsSelected.subscribe {
                storageWithUploadPathPanel.viewModel.clusterSelectedSubject.onNext(it)

                if (it != null) {
                    storageWithUploadPathPanel.viewModel.uploadStorage.storageCheckSubject.onNext(
                            SparkSubmissionJobUploadStorageCtrl.StorageCheckSelectedClusterEvent(it,
                                    preSelectClusterName))
                }
                checkInputsWithErrorLabels()
            }
        }}
    }

    open val viewModel = ViewModel().apply { Disposer.register(this@SparkSubmissionContentPanel, this@apply) }

    override fun setData(data: SparkSubmitModel) {
        // Data -> Component

        ApplicationManager.getApplication().invokeAndWait({
            viewModel.clusterSelection.toSelectClusterByIdBehavior.onNext(data.clusterName)

            // TODO: Implement this in ClusterSelection ViewModel to take real effects
            clustersSelection.component.isEnabled = data.isClusterSelectable
            localArtifactPrompt.isSelected = data.isLocalArtifact

            localArtifactTextField.text = data.localArtifactPath
            mainClassTextField.text = data.mainClassName
            commandLineTextField.text = data.commandLineArgs.joinToString(" ")
            referencedJarsTextField.text = data.referenceJars.joinToString(";")
            referencedFilesTextField.text = data.referenceFiles.joinToString(";")

            // update job configuration table
            if (jobConfigurationTable.model != data.tableModel) {
                jobConfigurationTable.model = data.tableModel.apply {
                    if (jobConfigMap.isEmpty()) {
                        loadJobConfigMap(data.defaultParameters
                                .map { com.microsoft.azuretools.utils.Pair(it.first(), it.second()?.toString() ?: "") }
                                .toList())
                    }
                }
            }

            if (!data.artifactName.isNullOrBlank()) {
                selectedArtifactComboBox.model.apply { selectedItem = findFirst { it.name == data.artifactName } }
            }
        }, ModalityState.any())

        // set Job Upload Storage panel data
        storageWithUploadPathPanel.setData(data.jobUploadStorageModel)
    }

    override fun getData(data: SparkSubmitModel) {
        // Component -> Data

        val selectedArtifactName = (selectedArtifactComboBox.selectedItem as? Artifact)?.name ?: ""
        val className = mainClassTextField.text.trim()
        val selectedClusterName = viewModel.clusterSelection.toSelectClusterByIdBehavior.value as? String

        val referencedFileList = referencedFilesTextField.text.split(";").dropLastWhile { it.isEmpty() }
                .asSequence()
                .map { it.trim() }
                .filter { s -> !s.isEmpty() }
                .toList()

        val uploadedFilePathList = referencedJarsTextField.text.split(";").dropLastWhile { it.isEmpty() }
                .asSequence()
                .map { it.trim() }
                .filter { s -> !s.isEmpty() }
                .toList()

        val argsList = commandLineTextField.text.split(" ").dropLastWhile { it.isEmpty() }
                .asSequence()
                .map { it.trim() }
                .filter { s -> !s.isEmpty() }
                .toList()

        data.apply {
            // submission parameters
            clusterName = selectedClusterName
            isLocalArtifact = localArtifactPrompt.isSelected
            artifactName = selectedArtifactName
            localArtifactPath = localArtifactTextField.text
            filePath = null
            mainClassName = className
            referenceFiles = referencedFileList
            referenceJars = uploadedFilePathList
            commandLineArgs = argsList
            tableModel = jobConfigurationTable.model as SubmissionTableModel

            project = myProject
        }

        // get Job upload storage panel data
        storageWithUploadPathPanel.getData(data.jobUploadStorageModel)
        data.errors.apply {
            removeAll { true }
            add(control.resultMessage)
        }
    }

    override fun dispose() {
    }
}

class SparkSubmissionContentPanelControl(private val jobUploadStorageCtrl: SparkSubmissionJobUploadStorageWithUploadPathPanel.Control)
    : SparkSubmissionJobUploadStorageWithUploadPathPanel.Control by jobUploadStorageCtrl,
        SparkSubmissionContentPanel.Control, ILogger
