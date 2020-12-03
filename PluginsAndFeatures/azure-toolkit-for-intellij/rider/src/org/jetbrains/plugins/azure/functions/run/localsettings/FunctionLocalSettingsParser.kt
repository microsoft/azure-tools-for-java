/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run.localsettings

import com.intellij.json.JsonUtil
import com.intellij.json.psi.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.azure.util.JsonParser.findBooleanProperty
import org.jetbrains.plugins.azure.util.JsonParser.findNumberProperty
import org.jetbrains.plugins.azure.util.JsonParser.findStringProperty
import org.jetbrains.plugins.azure.util.JsonParser.jsonFileFromVirtualFile

object FunctionLocalSettingsParser {

    const val PROPERTY_IS_ENCRYPTED = "IsEncrypted"

    const val OBJECT_VALUES = "Values"
    const val PROPERTY_VALUES_RUNTIME = "FUNCTIONS_WORKER_RUNTIME"
    const val PROPERTY_VALUES_WEB_JOBS_STORAGE = "AzureWebJobsStorage"
    const val PROPERTY_VALUES_WEB_JOBS_DASHBOARD = "AzureWebJobsDashboard"
    const val PROPERTY_VALUES_BINDING_CONNECTION = "MyBindingConnection"
    const val PROPERTY_VALUES_WEB_JOBS_HTTP_EXAMPLE_DISABLED = "AzureWebJobs.HttpExample.Disabled"

    const val OBJECT_HOST = "Host"
    const val PROPERTY_HOST_LOCAL_HTTP_PORT = "LocalHttpPort"
    const val PROPERTY_HOST_CORS = "CORS"
    const val PROPERTY_HOST_CORS_CREDENTIALS = "CORSCredentials"

    const val OBJECT_CONNECTION_STRINGS = "ConnectionStrings"

    fun readLocalSettingsJsonFrom(virtualFile: VirtualFile, project: Project): FunctionLocalSettings? {
        val jsonFile = ApplicationManager.getApplication().runReadAction(Computable {
            jsonFileFromVirtualFile(virtualFile, project)
        }) ?: return null

        return readFunctionLocalSettingsFrom(jsonFile)
    }

    fun readFunctionLocalSettingsFrom(jsonFile: JsonFile): FunctionLocalSettings? {
        val topLevelObject = JsonUtil.getTopLevelObject(jsonFile) ?: return null

        val isEncrypted = findBooleanProperty(topLevelObject, PROPERTY_IS_ENCRYPTED)

        val valuesObject = topLevelObject.findProperty(OBJECT_VALUES)?.value as? JsonObject
        val runtime = findStringProperty(valuesObject, PROPERTY_VALUES_RUNTIME)
        val webJobsStorage = findStringProperty(valuesObject, PROPERTY_VALUES_WEB_JOBS_STORAGE)
        val webJobsDashboard = findStringProperty(valuesObject, PROPERTY_VALUES_WEB_JOBS_DASHBOARD)
        val bindingConnection = findStringProperty(valuesObject, PROPERTY_VALUES_BINDING_CONNECTION)
        val webJobsHttpExampleDisabled = findBooleanProperty(valuesObject, PROPERTY_VALUES_WEB_JOBS_HTTP_EXAMPLE_DISABLED)

        val hostObject = topLevelObject.findProperty(OBJECT_HOST)?.value as? JsonObject
        val localHttpPort = findNumberProperty(hostObject, PROPERTY_HOST_LOCAL_HTTP_PORT)?.toInt()
        val cors = findStringProperty(hostObject, PROPERTY_HOST_CORS)
        val corsCredentials = findBooleanProperty(hostObject, PROPERTY_HOST_CORS_CREDENTIALS)

        val connectionStringsObject = topLevelObject.findProperty(OBJECT_CONNECTION_STRINGS)?.value as? JsonObject
        val connectionStrings: Map<String, String>? =
                connectionStringsObject?.propertyList?.mapNotNull { entry ->
                    val value = entry?.value as? JsonStringLiteral ?: return@mapNotNull null
                    entry.name to value.value
                }?.toMap()

        return FunctionLocalSettings(
            isEncrypted       = isEncrypted,
            values            = FunctionValuesModel(runtime, webJobsStorage, webJobsDashboard, webJobsHttpExampleDisabled, bindingConnection),
            host              = FunctionHostModel(localHttpPort, cors, corsCredentials),
            connectionStrings = connectionStrings
        )
    }
}
