/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.coreTools

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import com.jetbrains.rd.util.warn
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.runner.functions.core.FunctionCliResolver
import java.io.File

data class FunctionsCoreToolsInfo(val coreToolsPath: String, var coreToolsExecutable: String)

object FunctionsCoreToolsInfoProvider {
    private val logger = getLogger<FunctionsCoreToolsInfoProvider>()

    fun retrieve(): FunctionsCoreToolsInfo? {
        val funcCoreToolsPath = PropertiesComponent.getInstance().getValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH)
        if (funcCoreToolsPath.isNullOrEmpty() || !File(funcCoreToolsPath).exists()) {
            return null
        }

        val coreToolsExecutablePath = if (SystemInfo.isWindows) {
            File(funcCoreToolsPath).resolve("func.exe")
        } else {
            File(funcCoreToolsPath).resolve("func")
        }

        if (!coreToolsExecutablePath.exists()) {
            return null
        }

        if (!coreToolsExecutablePath.canExecute()) {
            logger.warn { "Updating executable flag for $coreToolsExecutablePath..." }
            try {
                coreToolsExecutablePath.setExecutable(true)
            } catch (s: SecurityException) {
                logger.error("Failed setting executable flag for $coreToolsExecutablePath", s)
            }
        }

        return FunctionsCoreToolsInfo(funcCoreToolsPath, coreToolsExecutablePath.path)
    }

    fun detectFunctionCoreToolsPath(): String? {

        fun detectPluginDownloads(): File? {
            val pluginCoreToolsPath = FunctionsCoreToolsManager.determineLatestLocalCoreToolsPath() ?: return null
            val downloadFile = File(pluginCoreToolsPath)

            val toolNameWithExtension = FunctionsCoreToolsManager.getCoreToolsExecutableName()
            val toolFile = downloadFile.resolve(toolNameWithExtension)

            if (!toolFile.exists())
                return null

            return toolFile
        }

        fun detectManualDownloads(): File? {
            val coreToolsPath = FunctionCliResolver.resolveFunc() ?: return null

            val coreToolFile = File(coreToolsPath)
            if (!coreToolFile.exists())
                return null

            return coreToolFile
        }

        // Plugin tool downloads has higher priority then manual downloads.
        val pluginCoreToolsExecutable = detectPluginDownloads()
        if (pluginCoreToolsExecutable != null) {
            logger.info { "Found an existing download from the plugin for function core tool: '${pluginCoreToolsExecutable.canonicalPath}'" }
            return pluginCoreToolsExecutable.parentFile.canonicalPath
        }

        val manualCoreToolsExecutable = detectManualDownloads()
        if (manualCoreToolsExecutable != null) {
            logger.info { "Found an existing manual setup for function core tool: '${manualCoreToolsExecutable.canonicalPath}'" }
            return manualCoreToolsExecutable.parentFile.canonicalPath
        }

        return null
    }
}