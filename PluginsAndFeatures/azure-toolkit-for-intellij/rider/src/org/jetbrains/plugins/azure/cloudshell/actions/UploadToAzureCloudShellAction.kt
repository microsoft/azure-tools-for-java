/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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
 * SOFTWARE.
 */
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
import org.jetbrains.plugins.azure.RiderAzureBundle
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
            descriptor.title = RiderAzureBundle.message("action.cloud_shell.upload_to_azure.title")
            FileChooser.chooseFiles(descriptor, project, null, null, object : FileChooser.FileChooserConsumer {
                override fun consume(files: List<VirtualFile>) {
                    files.forEach {
                        object : Task.Backgroundable(project, RiderAzureBundle.message("progress.cloud_shell.upload_to_azure.uploading", it.presentableName), true, PerformInBackgroundOption.DEAF)
                        {
                            override fun run(indicator: ProgressIndicator)
                            {
                                logger.info("Uploading ${it.name} to Azure Cloud Shell...")
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