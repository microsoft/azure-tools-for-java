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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rider.model.RunnableProject
import java.io.File

object FunctionLocalSettingsUtil {

    fun readFunctionLocalSettings(project: Project, runnableProject: RunnableProject): FunctionLocalSettings? =
            readFunctionLocalSettings(project, File(runnableProject.projectFilePath).parent)

    fun readFunctionLocalSettings(project: Project, basePath: String): FunctionLocalSettings? {
        val localSettingsFile = getLocalSettingsVirtualFile(basePath) ?: return null
        return FunctionLocalSettingsParser.readLocalSettingsJsonFrom(localSettingsFile, project)
    }

    fun getLocalSettingsVirtualFile(basePath: String): VirtualFile? {
        val localSettingsFile = getLocalSettingFile(basePath)
        if (!localSettingsFile.exists()) return null

        return VfsUtil.findFileByIoFile(localSettingsFile, true)
    }

    fun getLocalSettingFile(basePath: String): File = File(basePath).resolve("local.settings.json")
}