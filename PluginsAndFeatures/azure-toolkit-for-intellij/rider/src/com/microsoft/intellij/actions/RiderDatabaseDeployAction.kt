package com.microsoft.intellij.actions

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManagerEx
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.rider.projectView.nodes.ProjectModelNode
import com.jetbrains.rider.projectView.nodes.getBackendProjectModelNode
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.intellij.runner.db.dbconfig.RiderDatabaseConfigurationType
import java.util.*

// TODO: TO REMOVE - there is no option to deploy database from context.
// TODO:             Except the case when we deploy a REAL BIG database and we see it in the project explorer
class RiderDatabaseDeployAction : AzureAnAction() {

    companion object {
        private const val DIALOG_TITLE = "Azure Publish Database"
        private const val RUN_CONFIG_PREFIX = "Azure SQL Database"
    }

    private val configType = RiderDatabaseConfigurationType.instance

    override fun onActionPerformed(event: AnActionEvent?) {
        event ?: return
        val project = event.project ?: return
        val selectedProject = event.dataContext.getBackendProjectModelNode(false) ?: return

        try {
            if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return
            ApplicationManager.getApplication().invokeLater { runConfiguration(project, selectedProject) }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun runConfiguration(project: Project, node: ProjectModelNode) {

        val name = node.name
        val manager = RunManagerEx.getInstanceEx(project)
        val factory = configType.databaseConfigurationFactory

        val settings = manager.findConfigurationByName("$RUN_CONFIG_PREFIX: $name")
                ?: manager.createConfiguration("$RUN_CONFIG_PREFIX: $name", factory)

        if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
            val tasks = ArrayList(manager.getBeforeRunTasks(settings.configuration))
            manager.addConfiguration(settings, false, tasks, false)
            manager.selectedConfiguration = settings
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }
}
