package org.jetbrains.plugins.azure.cloudshell.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rider.util.idea.getComponent
import org.jetbrains.plugins.azure.cloudshell.CloudShellComponent

class UploadToAzureCloudShellAction : AnAction() {
    private val logger = Logger.getInstance(UploadToAzureCloudShellAction::class.java)

    override fun update(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)
        val cloudShellComponent = project?.getComponent<CloudShellComponent>()

        e.presentation.isEnabled = CommonDataKeys.PROJECT.getData(e.dataContext) != null
                && cloudShellComponent != null
                && cloudShellComponent.activeConnector() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return
        val activeConnector = project.getComponent<CloudShellComponent>().activeConnector() ?: return

        ApplicationManager.getApplication().invokeLater {
            val descriptor = FileChooserDescriptor(true, false, false, true, false, true)
            descriptor.title = "Select file(s) to upload to Azure Cloud Shell"
            FileChooser.chooseFiles(descriptor, project, null, project.baseDir, object : FileChooser.FileChooserConsumer {
                override fun consume(files: List<VirtualFile>) {
                    files.forEach {
                        object : Task.Backgroundable(project, "Uploading file " + it.presentableName + " to Azure Cloud Shell...", true, PerformInBackgroundOption.DEAF)
                        {
                            override fun run(indicator: ProgressIndicator)
                            {
                                activeConnector.uploadFile(it.name, it)
                            }
                        }.queue()
                    }
                }

                override fun cancelled() {}
            })
        }
    }
}