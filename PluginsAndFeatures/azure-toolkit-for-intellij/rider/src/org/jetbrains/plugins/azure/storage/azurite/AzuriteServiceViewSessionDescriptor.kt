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

package org.jetbrains.plugins.azure.storage.azurite

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanelWithEmptyText
import icons.CommonIcons
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.storage.azurite.actions.CleanAzuriteAction
import org.jetbrains.plugins.azure.storage.azurite.actions.ShowAzuriteSettingsAction
import org.jetbrains.plugins.azure.storage.azurite.actions.StartAzuriteAction
import org.jetbrains.plugins.azure.storage.azurite.actions.StopAzuriteAction
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

class AzuriteServiceViewSessionDescriptor(private val project: Project)
    : SimpleServiceViewDescriptor(RiderAzureBundle.message("service.azurite.name"), CommonIcons.Azurite), @NotNull Disposable {

    companion object {
        val defaultToolbarActions = DefaultActionGroup(
                StartAzuriteAction(),
                StopAzuriteAction(),
                CleanAzuriteAction(),
                ShowAzuriteSettingsAction()
        )
    }

    private val azuriteService = service<AzuriteService>()
    private var processHandler: ColoredProcessHandler? = null
    private var workspace: String? = null

    private val consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project).apply { setViewer(true) }.console

    init {
        Disposer.register(project, this)
    }

    protected val panel = createEmptyComponent()

    override fun getToolbarActions() = defaultToolbarActions

    override fun getPresentation(): ItemPresentation {
        ensureConsoleView()

        val superPresentation = super.getPresentation()
        return object : ItemPresentation {
            override fun getLocationString(): String? = workspace
            override fun getIcon(p: Boolean) = superPresentation.getIcon(p)
            override fun getPresentableText() = superPresentation.presentableText
        }
    }

    override fun getContentComponent(): JComponent? {
        ensureConsoleView()
        return panel
    }

    private fun ensureConsoleView() {
        azuriteService.processHandler?.let { activeProcessHandler ->

            if (processHandler != activeProcessHandler) {
                processHandler?.detachProcess()
                processHandler = activeProcessHandler
                workspace = azuriteService.workspace

                consoleView.print(RiderAzureBundle.message("action.azurite.reattach.workspace", workspace.orEmpty()) + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
                consoleView.attachToProcess(activeProcessHandler)
                consoleView.print(RiderAzureBundle.message("action.azurite.reattach.finished") + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
            }
        }

        if (processHandler != null && panel.components.isEmpty()) {
            panel.add(consoleView.component, BorderLayout.CENTER)
        }
    }

    private fun createEmptyComponent(): JPanel {
        val panel: JPanel = JBPanelWithEmptyText(BorderLayout())
                .withEmptyText(RiderAzureBundle.message("service.azurite.not_started"))
                .withBorder(BorderFactory.createEmptyBorder())
        panel.isFocusable = true
        return panel
    }

    override fun dispose() {
        Disposer.dispose(consoleView)
    }
}