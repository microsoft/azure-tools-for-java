package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.uiDesigner.core.GridConstraints
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkSubmitModel
import com.microsoft.intellij.forms.dsl.panel
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

open class CosmosServerlessSparkSubmissionPanel(private val project: Project)
    : SparkSubmissionContentPanel(project, "Cosmos Serverless Spark") {

    override val clustersSelection: SparkClusterListRefreshableCombo by lazy { CosmosServerlessSparkAccountsCombo().apply {
        Disposer.register(this@CosmosServerlessSparkSubmissionPanel, this@apply)
    }}

    override fun getData(data: SparkSubmitModel) {
        super.getData(data)

        (data as? CosmosServerlessSparkSubmitModel)?.apply {
            setSparkEventsDirectoryPath(sparkEventsDirectoryField.text)
            sparkEventsDirectoryPrefix = sparkEventsDirectoryPrefixField.text
        }
    }

    override fun setData(data: SparkSubmitModel) {
        super.setData(data)
        ApplicationManager.getApplication().invokeAndWait({
            (data as? CosmosServerlessSparkSubmitModel)?.apply {
                sparkEventsDirectoryField.text = getSparkEventsDirectoryPath()
                sparkEventsDirectoryPrefixField.text = sparkEventsDirectoryPrefix
            }
        }, ModalityState.any())
    }

    private val sparkEventsPrompt = JLabel("Spark Events:").apply {
        toolTipText = "Spark events root path"
    }

    private val sparkEventsDirectoryPrefixField = JLabel("<Root Path>/").apply {
        toolTipText = "Spark events root path"
    }

    private val sparkEventsDirectoryField = JTextField("spark-events/").apply {
        toolTipText = sparkEventsPrompt.toolTipText
    }

    private var sparkEventsDirectory = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(sparkEventsPrompt)
        add(sparkEventsDirectoryPrefixField)
        add(sparkEventsDirectoryField)
    }

    override val component: JComponent by lazy {
        clustersSelectionPrompt.text = "ADL account"
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                    fill = GridConstraints.FILL_NONE
                }
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                    hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                    fill = GridConstraints.FILL_HORIZONTAL
                }
            }
            row { c(super.component);}
            row { c(sparkEventsDirectory) }
        }

        formBuilder.buildPanel()
    }

    inner class ViewModel: SparkSubmissionContentPanel.ViewModel() {
        init {
            clusterSelection.clusterIsSelected
                .flatMap { cluster ->
                    if (cluster == null) {
                        Observable.empty<CosmosServerlessSparkSubmitModel>()
                    } else {
                        Observable.just(CosmosServerlessSparkSubmitModel())
                            .doOnNext { getData(it) }
                            .map { model ->
                                model.apply {
                                    sparkEventsDirectoryPrefix = (cluster as? AzureSparkServerlessAccount)?.storageRootPath
                                            ?: "<Root Path>/"
                                }
                            }
                            .doOnNext { setData(it) }
                    }
                }
                .subscribe({}, {err -> ExceptionUtils.getStackTrace(err)})
        }
    }

    override val viewModel = ViewModel().apply { Disposer.register(this@CosmosServerlessSparkSubmissionPanel, this@apply) }
}
