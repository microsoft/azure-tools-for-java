/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.projectTemplating

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.rider.projectView.actions.projectTemplating.backend.ReSharperProjectTemplateProvider
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import java.io.File

object FunctionsCoreToolsTemplateManager {
    private val logger = Logger.getInstance(FunctionsCoreToolsTemplateManager::class.java)

    private fun isFunctionsProjectTemplate(file: File?): Boolean =
            file != null && file.isFile && file.name.startsWith("projectTemplates.", true) && file.name.endsWith(".nupkg", true)

    fun areRegistered(): Boolean =
            ReSharperProjectTemplateProvider.getUserTemplateSources().any { isFunctionsProjectTemplate(it) }

    fun tryReload() {
        val coreToolsInfo = FunctionsCoreToolsInfoProvider.retrieve() ?: return

        // Remove previous templates
        ReSharperProjectTemplateProvider.getUserTemplateSources().forEach {
            if (isFunctionsProjectTemplate(it)) {
                ReSharperProjectTemplateProvider.removeUserTemplateSource(it)
            }
        }

        // Add available templates
        val templatesToBeRegisteredFolder = File(coreToolsInfo.coreToolsPath).resolve("templates")
        if (templatesToBeRegisteredFolder.exists()) {
            try {
                val templateFiles = templatesToBeRegisteredFolder.listFiles { f: File? -> isFunctionsProjectTemplate(f) }
                        ?: emptyArray<File>()

                logger.info("Found ${templateFiles.size} function template(s)")

                templateFiles.forEach { file ->
                    ReSharperProjectTemplateProvider.addUserTemplateSource(file)
                }
            } catch (e: Exception) {
                logger.error("Could not register project templates from ${templatesToBeRegisteredFolder.path}", e)
            }
        }
    }
}