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

package org.jetbrains.plugins.azure.functions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import java.io.File

/**
 * Check and detect Azure Function Core Tool path.
 */
class AzureCoreToolsDetector : StartupActivity {

    companion object {
        private val logger = Logger.getInstance(AzureCoreToolsDetector::class.java)
    }

    override fun runActivity(project: Project) {
        val properties = PropertiesComponent.getInstance()

        val existingCoreToolPath = properties.getValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH, "")
        if (existingCoreToolPath.isNotEmpty() && File(existingCoreToolPath).exists()) {
            logger.info("Azure Function Core Tool is setup: '$existingCoreToolPath'")
            return
        }

        logger.info("Detecting Azure Function Core Tool...")
        val functionCoreToolPath = FunctionsCoreToolsInfoProvider.detectFunctionCoreToolsPath()

        if (functionCoreToolPath == null) {
            logger.info("Unable to detect Azure Core Tool path")
            return
        }

        properties.setValue(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH, functionCoreToolPath)
    }
}
