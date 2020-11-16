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

package com.microsoft.intellij.configuration

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.storage.azurite.Azurite
import java.io.File

object AzureRiderSettings {

    // Dismiss notifications
    const val DISMISS_NOTIFICATION_AZURE_AD_REGISTER = "DismissAzureAdRegistration"

    // Web Apps
    const val PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME = "AzureOpenWebAppInBrowser"
    const val OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE = false

    // Functions
    const val PROPERTY_FUNCTIONS_CORETOOLS_PATH = "AzureFunctionsCoreToolsPath"
    const val PROPERTY_FUNCTIONS_CORETOOLS_ALLOW_PRERELEASE = "AzureFunctionsCoreToolsAllowPrerelease"
    const val PROPERTY_FUNCTIONS_CORETOOLS_CHECK_UPDATES = "AzureFunctionCoreToolsCheckUpdates"

    // Azurite
    const val PROPERTY_AZURITE_NODE_INTERPRETER = "AzureAzuriteNodeInterpreter"
    const val PROPERTY_AZURITE_NODE_PACKAGE = "AzureAzuriteNodePackage"

    const val PROPERTY_AZURITE_BLOB_HOST = "AzureAzuriteBlobHost"
    const val VALUE_AZURITE_BLOB_HOST_DEFAULT = "127.0.0.1"
    const val PROPERTY_AZURITE_BLOB_PORT = "AzureAzuriteBlobPort"
    const val VALUE_AZURITE_BLOB_PORT_DEFAULT = "10000"

    const val PROPERTY_AZURITE_QUEUE_HOST = "AzureAzuriteQueueHost"
    const val VALUE_AZURITE_QUEUE_HOST_DEFAULT = "127.0.0.1"
    const val PROPERTY_AZURITE_QUEUE_PORT = "AzureAzuriteQueuePort"
    const val VALUE_AZURITE_QUEUE_PORT_DEFAULT = "10001"

    const val PROPERTY_AZURITE_TABLE_HOST = "AzureAzuriteTableHost"
    const val VALUE_AZURITE_TABLE_HOST_DEFAULT = "127.0.0.1"
    const val PROPERTY_AZURITE_TABLE_PORT = "AzureAzuriteTablePort"
    const val VALUE_AZURITE_TABLE_PORT_DEFAULT = "10002"

    const val PROPERTY_AZURITE_LOCATION_MODE = "AzureAzuriteLocationMode"
    const val PROPERTY_AZURITE_LOCATION = "AzureAzuriteLocation"

    enum class AzuriteLocationMode(val description: String) {
        Managed(RiderAzureBundle.message("settings.azurite.row.general.workspace.use_managed")),
        Project(RiderAzureBundle.message("settings.azurite.row.general.workspace.use_project")),
        Custom(RiderAzureBundle.message("settings.azurite.row.general.workspace.use_custom"))
    }

    const val PROPERTY_AZURITE_LOOSE_MODE = "AzureAzuriteLooseMode"

    const val PROPERTY_AZURITE_CERT_PATH = "AzureAzuriteCertPath"
    const val PROPERTY_AZURITE_CERT_KEY_PATH = "AzureAzuriteCertKeyPath"
    const val PROPERTY_AZURITE_CERT_PASSWORD = "AzureAzuriteCertPassword"

    fun getAzuriteWorkspaceMode(properties: PropertiesComponent): AzuriteLocationMode {
        val mode = properties.getValue(PROPERTY_AZURITE_LOCATION_MODE)
                ?: return AzuriteLocationMode.Managed

        return AzuriteLocationMode.valueOf(mode)
    }

    fun getAzuriteWorkspacePath(properties: PropertiesComponent, project: Project): File =
        when(getAzuriteWorkspaceMode(properties)) {
            AzuriteLocationMode.Managed -> {
                val workspace = File(PathManager.getConfigPath(), Azurite.ManagedPathSuffix)
                workspace.mkdir()
                workspace
            }
            AzuriteLocationMode.Project -> {
                val workspace = File(project.basePath, Azurite.ProjectPathSuffix)
                workspace.mkdir()
                workspace
            }
            AzuriteLocationMode.Custom -> {
                val customPath = properties.getValue(PROPERTY_AZURITE_LOCATION)
                if (customPath.isNullOrEmpty()) {
                    val workspace = File(customPath)
                    workspace
                } else {
                    val workspace = File(PathManager.getConfigPath(), Azurite.ManagedPathSuffix)
                    workspace.mkdir()
                    workspace
                }
            }
        }
}