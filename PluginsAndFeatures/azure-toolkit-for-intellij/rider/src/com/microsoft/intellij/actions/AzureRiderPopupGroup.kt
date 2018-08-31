package com.microsoft.intellij.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rider.ideaInterop.fileTypes.sln.SolutionFileType
import com.jetbrains.rider.projectView.projectTypes.RiderProjectTypesManager

// TODO: This can be removed to get rid of [Tools > Azure] context menu group
class AzureRiderPopupGroup : DefaultActionGroup(), DumbAware {

    override fun update(e: AnActionEvent) {
        val dataContext = e.dataContext
        val project = e.project

        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val selectedFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext)
        e.presentation.isEnabledAndVisible = isProjectFile(selectedFile)
    }

    private fun isProjectFile(file: VirtualFile?): Boolean {
        return if (file == null || !file.isValid || file.isDirectory) false
               else SolutionFileType.isSolutionFile(file) || RiderProjectTypesManager.getInstance().isKnownProjectFile(file)
    }
}