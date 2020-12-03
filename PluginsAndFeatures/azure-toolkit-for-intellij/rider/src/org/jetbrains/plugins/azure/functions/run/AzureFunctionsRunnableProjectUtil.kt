/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run

import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.model.RunnableProject

object AzureFunctionsRunnableProjectUtil {
    fun patchRunnableProjectOutputs(runnableProject: RunnableProject): RunnableProject {
        return RunnableProject(
                runnableProject.name,
                runnableProject.fullName,
                runnableProject.projectFilePath,
                runnableProject.kind,
                runnableProject.projectOutputs
                        .map { patchProjectOutput(it) }.toMutableList(),
                runnableProject.environmentVariables,
                runnableProject.problems,
                runnableProject.customAttributes)
    }

    fun patchProjectOutput(projectOutput: ProjectOutput): ProjectOutput =
            projectOutput.copy(
                    // Azure Functions host needs the tfm folder, not the bin folder
                    workingDirectory = if (projectOutput.workingDirectory.endsWith("bin", true)) {
                        projectOutput.workingDirectory.substringBeforeLast("bin")
                    } else {
                        projectOutput.workingDirectory
                    },

                    // Add default arguments to start host
                    defaultArguments = mutableListOf("host", "start", "--pause-on-error")
            )
}
