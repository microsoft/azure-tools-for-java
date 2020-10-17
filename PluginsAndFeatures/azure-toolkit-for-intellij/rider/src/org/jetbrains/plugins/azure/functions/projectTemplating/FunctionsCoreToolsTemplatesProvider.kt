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

package org.jetbrains.plugins.azure.functions.projectTemplating

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IOptProperty
import com.jetbrains.rd.util.reactive.OptProperty
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplate
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateGenerator
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateProvider
import com.jetbrains.rider.projectView.actions.projectTemplating.RiderProjectTemplateState
import com.jetbrains.rider.projectView.actions.projectTemplating.common.InfoProjectTemplateGeneratorBase
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateDialog
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateDialogContext
import com.jetbrains.rider.projectView.actions.projectTemplating.impl.ProjectTemplateTransferableModel
import icons.CommonIcons
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.Icon
import javax.swing.JComponent

class FunctionsCoreToolsTemplatesProvider : RiderProjectTemplateProvider {
    override val isReady = Property(true)

    override fun load(lifetime: Lifetime, context: ProjectTemplateDialogContext): IOptProperty<RiderProjectTemplateState> {
        val state = RiderProjectTemplateState(arrayListOf(), arrayListOf())

        if (!FunctionsCoreToolsTemplateManager.areRegistered()) {
            FunctionsCoreToolsTemplateManager.tryReload()
        }

        if (!FunctionsCoreToolsTemplateManager.areRegistered()) {
            state.new.add(InstallTemplates())
        }

        return OptProperty(state)
    }

    private class InstallTemplates : RiderProjectTemplate {
        override val group: String?
            get() = ".NET Core"
        override val name: String
            get() = "Azure Functions"
        override val icon: Icon
            get() = CommonIcons.AzureFunctions.TemplateAzureFunc

        override fun getKeywords() = arrayOf(name)

        override fun createGenerator(context: ProjectTemplateDialogContext, transferableModel: ProjectTemplateTransferableModel): RiderProjectTemplateGenerator {
            return object : InfoProjectTemplateGeneratorBase() {

                override val expandActionName: String
                    get() = message("template.project.function_app.actions.expand.reload")

                override fun expand(): Runnable? {
                    // just close dialog and show again to refresh templates
                    return Runnable { ProjectTemplateDialog.show(context.project, context.item) }
                }

                override fun getComponent(): JComponent {
                    return InstallFunctionsCoreToolsComponent(this.validationError).getView()
                }
            }
        }
    }
}