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

package com.microsoft.intellij.ui.forms.appservice.functionapp.slot

import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.intellij.ui.forms.appservice.slot.CreateDeploymentSlotDialog

class FunctionAppCreateDeploymentSlotDialog(lifetimeDef: LifetimeDefinition,
                                            project: Project,
                                            app: FunctionApp,
                                            onCreate: () -> Unit) :
        CreateDeploymentSlotDialog(lifetimeDef, project, app, { signal -> FunctionAppCreateDeploymentSlotComponent(app, lifetimeDef, signal) }, onCreate) {

    override fun createSlotAction(app: WebAppBase, slotName: String, sourceName: String?) {
        val functionApp = app as FunctionApp
        AzureFunctionAppMvpModel.createDeploymentSlot(functionApp, slotName, sourceName)
    }

    override fun checkSlotIsCreatedAction(app: WebAppBase, slotName: String): Boolean {
        val functionApp = app as FunctionApp
        val createdSlot =
                try {
                    AzureFunctionAppMvpModel.listDeploymentSlots(functionApp, true)
                            .find { it.name() == slotName }
                } catch (t: Throwable) { null }
        return createdSlot != null
    }
}
