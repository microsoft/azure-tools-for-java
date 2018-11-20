package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.uiDesigner.core.GridConstraints
import com.microsoft.intellij.forms.dsl.panel
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CosmosServerlessSparkSubmissionContentPanel : SparkSubmissionContentPanel {
    constructor() : super(false) {
        initialization()
    }

    private val sparkEventsPrompt = JLabel("Spark Events directory:").apply {
        toolTipText = "Directory Path for spark events"
    }

    private val sparkEventsDirectoryPrefixField = JLabel("adl://*.azuredatalakestore.net/").apply {
        toolTipText = "DirectoryPath for spark events"
    }

    private val sparkEventsDirectoryField = JTextField().apply {
        toolTipText = sparkEventsPrompt.toolTipText
    }

    var sparkEventsDirectory = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(sparkEventsDirectoryPrefixField)
        add(sparkEventsDirectoryField)
    }

    override fun initialization(): SparkSubmissionContentPanel {
        super.clustersSelectionPrompt.text = "ADL account"
        super.isStorageWithUploadPathPanelEnabled = false
        super.initialization()

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
            row { c(super.sparkSubmissionPanel) { colSpan = 2 } }
            row { c(sparkEventsPrompt); c(sparkEventsDirectory) }
        }

        this.add(formBuilder.buildPanel())
        return this
    }

    fun getEventsLocation() : String {
        return sparkEventsDirectoryField.text
    }

    fun setEventsLocationPrefix(account : String) {
        this.sparkEventsDirectoryPrefixField.text = "adl://$account.azuredatalakestore.net/"
    }

    fun setEventsLocation(path : String) {
        this.sparkEventsDirectoryField.text = path
    }
}