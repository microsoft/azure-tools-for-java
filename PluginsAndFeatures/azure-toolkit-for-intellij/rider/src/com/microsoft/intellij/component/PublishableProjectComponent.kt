/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.component

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.LayeredIcon
import com.intellij.ui.ListCellRendererWrapper
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.projectView.ProjectModelViewHost
import com.jetbrains.rider.projectView.nodes.isProject
import com.jetbrains.rider.projectView.nodes.isUnloadedProject
import com.microsoft.intellij.component.extension.fillComboBox
import com.microsoft.intellij.component.extension.getSelectedValue
import com.microsoft.intellij.helpers.validator.ProjectValidator
import net.miginfocom.swing.MigLayout
import java.io.File
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

class PublishableProjectComponent(private val project: Project) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    companion object {
        private const val PROJECTS_EMPTY_MESSAGE = "No projects to publish"
    }

    private val lblProject = JLabel("Project")
    val cbProject = ComboBox<PublishableProjectModel>()

    var listenerAction: (PublishableProjectModel) -> Unit = {}
    var lastSelectedProject: PublishableProjectModel? = null

    var canBePublishedAction: (PublishableProjectModel) -> Boolean = { false }

    init {
        initProjectsComboBox()

        add(lblProject)
        add(cbProject, "growx")

        initComponentValidation()
    }

    fun fillProjectComboBox(publishableProjects: List<PublishableProjectModel>,
                            defaultProject: PublishableProjectModel? = null) {

        val projectsToProcess = publishableProjects.sortedBy { it.projectName }
        cbProject.fillComboBox(projectsToProcess, defaultProject)

        // Find first project that can be published to Azure and pre-select it if there was no project was selected by user
        val projectCanBePublished = projectsToProcess.find { canBePublishedAction(it) }
        if (defaultProject == null && projectCanBePublished != null) {
            cbProject.selectedItem = projectCanBePublished
            lastSelectedProject = projectCanBePublished
        }
    }

    private fun initProjectsComboBox() {

        cbProject.renderer = object : ListCellRendererWrapper<PublishableProjectModel>() {
            override fun customize(list: JList<*>,
                                   publishableProject: PublishableProjectModel?,
                                   index: Int,
                                   isSelected: Boolean,
                                   cellHasFocus: Boolean) {
                if (project.isDisposed) return

                if (publishableProject == null) {
                    setText(PROJECTS_EMPTY_MESSAGE)
                    return
                }

                setText(publishableProject.projectName)

                val projectVf = VfsUtil.findFileByIoFile(File(publishableProject.projectFilePath), false) ?: return
                val projectArray = ProjectModelViewHost.getInstance(project).getItemsByVirtualFile(projectVf)
                val projectNodes = projectArray.filter { it.isProject() || it.isUnloadedProject() }

                if (projectNodes.isEmpty()) return
                val itemIcon = projectNodes[0].getIcon()
                val icon =
                        if (canBePublishedAction(publishableProject)) itemIcon
                        else LayeredIcon.create(IconLoader.getDisabledIcon(itemIcon), AllIcons.RunConfigurations.InvalidConfigurationLayer)
                setIcon(icon)
            }
        }

        cbProject.addActionListener {
            val publishableProject = cbProject.getSelectedValue() ?: return@addActionListener
            if (publishableProject == lastSelectedProject) return@addActionListener

            lastSelectedProject = publishableProject
            listenerAction(publishableProject)
        }
    }
}
