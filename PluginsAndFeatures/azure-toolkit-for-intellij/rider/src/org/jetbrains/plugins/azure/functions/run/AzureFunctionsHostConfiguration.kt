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

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.rider.debugger.IDotNetDebuggable
import com.jetbrains.rider.run.configurations.RiderRunConfiguration
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.util.idea.getComponent
import org.jdom.Element

class AzureFunctionsHostConfiguration(
        name: String,
        project: Project,
        factory: ConfigurationFactory,
        val parameters: AzureFunctionsHostConfigurationParameters
) : RiderRunConfiguration(
        name,
        project,
        factory,
        { AzureFunctionsHostSettingsEditorGroup(it) },
        AzureFunctionsHostExecutorFactory(
                parameters
        )
), IDotNetDebuggable {

    private val riderDotNetActiveRuntimeHost = project.getComponent<RiderDotNetActiveRuntimeHost>()

    override fun checkConfiguration() {
        super.checkConfiguration()
        parameters.validate(riderDotNetActiveRuntimeHost)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        parameters.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        parameters.writeExternal(element)
    }
}