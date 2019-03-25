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

package org.jetbrains.plugins.azure.functions.run

import com.jetbrains.rider.model.RunnableProject

object AzureFunctionsRunnableProjectUtil {
    fun patchRunnableProjectOutputs(runnableProject: RunnableProject): RunnableProject {
        return RunnableProject(
                runnableProject.name,
                runnableProject.fullName,
                runnableProject.projectFilePath,
                runnableProject.kind,
                runnableProject.projectOutputs.map { it.copy(
                        // Azure Functions host needs the tfm folder, not the bin folder
                        workingDirectory = if (it.workingDirectory.endsWith("bin", true)) {
                            it.workingDirectory.substringBeforeLast("bin")
                        } else {
                            it.workingDirectory
                        },

                        // Add default arguments to start host
                        defaultArguments = mutableListOf("host", "start", "--port", "7071", "--pause-on-error"))
                }.toMutableList(),
                runnableProject.environmentVariables,
                runnableProject.problems,
                runnableProject.customAttributes)
    }
}