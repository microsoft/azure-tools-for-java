/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.azure.storage.azurite.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.orWhenNullOrEmpty
import org.jetbrains.plugins.azure.storage.azurite.Azurite
import org.jetbrains.plugins.azure.storage.azurite.AzuriteService

class StartAzuriteAction
    : AnAction(
        RiderAzureBundle.message("action.azurite.start.name"),
        RiderAzureBundle.message("action.azurite.start.description"),
        AllIcons.Actions.Execute) {

    private val azuriteService = service<AzuriteService>()

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return

        if (azuriteService.isRunning) {
            e.presentation.isEnabled = false
            return
        }

        val properties = PropertiesComponent.getInstance(project)
        val packagePath = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_PACKAGE) ?: return
        val azuritePackage = Azurite.PackageDescriptor.createPackage(packagePath)
        e.presentation.isEnabled = !azuritePackage.isEmptyPath
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (azuriteService.isRunning) {
            return
        }

        val properties = PropertiesComponent.getInstance(project)
        val nodeJsInterpreterRef = NodeJsInterpreterRef.create(properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_INTERPRETER) ?: "project")
        val nodeJsInterpreter = nodeJsInterpreterRef.resolve(project)
        if (nodeJsInterpreter == null) {
            Azurite.showSettings(project)
            return
        }

        val nodeJsLocalInterpreter = NodeJsLocalInterpreter.castAndValidate(nodeJsInterpreter)

        val packagePath = properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_NODE_PACKAGE)
        if (packagePath.isNullOrEmpty()) {
            Azurite.showSettings(project)
            return
        }

        val azuritePackage = Azurite.PackageDescriptor.createPackage(packagePath)

        ApplicationManager.getApplication().runWriteAction {

            val azuriteJsFile = azuritePackage.findBinFile("azurite", null)!!
            val azuriteWorkspaceLocation = AzureRiderSettings.getAzuriteWorkspacePath(properties, project).absolutePath

            val commandLine = GeneralCommandLine(
                    nodeJsLocalInterpreter.interpreterSystemDependentPath,
                    azuriteJsFile.absolutePath,

                    "--blobHost",
                    properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_HOST_DEFAULT),
                    "--blobPort",
                    properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_BLOB_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_BLOB_PORT_DEFAULT),

                    "--queueHost",
                    properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_HOST).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_HOST_DEFAULT),
                    "--queuePort",
                    properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_QUEUE_PORT).orWhenNullOrEmpty(AzureRiderSettings.VALUE_AZURITE_QUEUE_PORT_DEFAULT),

                    "--location",
                    azuriteWorkspaceLocation)

            if (properties.getBoolean(AzureRiderSettings.PROPERTY_AZURITE_LOOSE_MODE))
                commandLine.addParameter("--loose")

            properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PATH)?.let {
                if (it.isNotEmpty()) {
                    commandLine.addParameter("--cert")
                    commandLine.addParameter(it)
                }
            }
            properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_KEY_PATH)?.let {
                if (it.isNotEmpty()) {
                    commandLine.addParameter("--key")
                    commandLine.addParameter(it)
                }
            }
            properties.getValue(AzureRiderSettings.PROPERTY_AZURITE_CERT_PASSWORD)?.let {
                if (it.isNotEmpty()) {
                    commandLine.addParameter("--pwd")
                    commandLine.addParameter(it)
                }
            }

            azuriteService.start(commandLine, azuriteWorkspaceLocation)
        }

    }

}