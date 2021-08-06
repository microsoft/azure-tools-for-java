package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.uiDesigner.core.GridConstraints
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkSubmitModel
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager
import com.microsoft.intellij.forms.dsl.panel
import org.apache.commons.lang3.exception.ExceptionUtils
import javax.swing.JComponent

open class CosmosServerlessSparkSubmissionPanel(private val project: Project)
    : SparkSubmissionContentPanel(project, "Cosmos Serverless Spark") {

    override val clustersSelection: SparkClusterListRefreshableCombo by lazy { CosmosServerlessSparkAccountsCombo().apply {
        Disposer.register(this@CosmosServerlessSparkSubmissionPanel, this@apply)
    }}

    override fun getData(data: SparkSubmitModel) {
        super.getData(data)

        (data as? CosmosServerlessSparkSubmitModel)?.apply {
            setSparkEventsDirectoryPath(additionalFieldsPanel.sparkEventsDirectoryField.text)
            sparkEventsDirectoryPrefix = additionalFieldsPanel.sparkEventsDirectoryPrefixField.text
            setExtendedProperties(additionalFieldsPanel.extendedPropertiesField.envs)
        }
    }

    override fun setData(data: SparkSubmitModel) {
        super.setData(data)
        AzureTaskManager.getInstance().runAndWait ({
            (data as? CosmosServerlessSparkSubmitModel)?.apply {
                additionalFieldsPanel.sparkEventsDirectoryField.text = getSparkEventsDirectoryPath()
                additionalFieldsPanel.sparkEventsDirectoryPrefixField.text = sparkEventsDirectoryPrefix
                additionalFieldsPanel.extendedPropertiesField.envs = getExtendedProperties()
            }
        }, AzureTask.Modality.ANY)
    }

    private val additionalFieldsPanel = CosmosServerlessSparkAdditionalFieldsPanel()

    override fun getErrorMessageClusterNameNull(isSignedIn : Boolean) : String {
        return when {
            isSignedIn -> "Account name should not be null, please choose one for submission"
            else -> "Can't list account, please login within Azure Explorer (View -> Tool Windows -> Azure Explorer) and refresh"
        }
    }

    override val component: JComponent by lazy {
        clustersSelectionPrompt.text = "Account name"
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                    hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                    fill = GridConstraints.FILL_HORIZONTAL
                }
            }
            row { c(super.component) }
            row { c(additionalFieldsPanel) }
        }

        formBuilder.buildPanel()
    }

    inner class ViewModel: SparkSubmissionContentPanel.ViewModel() {
        init {
            clusterSelection.clusterIsSelected
                    .subscribe({ cluster ->
                        if (cluster == null) {
                            return@subscribe
                        }

                        val model = CosmosServerlessSparkSubmitModel().apply {
                            getData(this)
                        }

                        setData(model.apply {
                            sparkEventsDirectoryPrefix = (cluster as? AzureSparkServerlessAccount)?.storageRootPath
                                    ?: "adl://*.azuredatalakestore.net/"
                        })
                    }, { err -> log().warn(ExceptionUtils.getStackTrace(err)) })
        }
    }

    override val viewModel = ViewModel().apply { Disposer.register(this@CosmosServerlessSparkSubmissionPanel, this@apply) }
}
