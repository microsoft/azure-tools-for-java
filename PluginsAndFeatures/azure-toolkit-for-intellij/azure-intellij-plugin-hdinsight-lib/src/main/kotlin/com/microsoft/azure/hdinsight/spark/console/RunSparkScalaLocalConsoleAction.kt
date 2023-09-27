/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.azure.hdinsight.spark.console

import com.intellij.execution.ExecutionException
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.DispatchThreadProgressWindow
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.ProjectScope
import com.microsoft.azuretools.telemetry.TelemetryConstants
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.ui.ErrorWindow
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleRunConfigurationFactory
import javax.swing.Action
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoriesConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.microsoft.intellij.util.runInWriteAction

class RunSparkScalaLocalConsoleAction : RunSparkScalaConsoleAction() {
    private var isMockFs: Boolean = false
    private val replMain = "org.apache.spark.repl.Main"
    private val sparkCoreCoodRegex = """.*\b(org.apache.spark:spark-)(core)(_.+:.+)""".toRegex()

    override val selectedMenuActionId: String
        get() = "Actions.SparkRunLocalConsoleActionGroups"

    override val isLocalRunConfigEnabled: Boolean
        get() = true

    override val focusedTabIndex: Int
        get() = 0

    override val consoleRunConfigurationFactory: ScalaConsoleRunConfigurationFactory
        get() = SparkScalaLocalConsoleConfigurationType().sparkLocalConfFactory(isMockFs)

    override fun getNewSettingName(): String = "Spark Local Console(Scala)"

    override fun getOperationName(event: AnActionEvent?): String = TelemetryConstants.RUN_SPARK_LOCAL_CONSOLE

    override fun onActionPerformed(event: AnActionEvent, operation: Operation?): Boolean {
        val project = CommonDataKeys.PROJECT.getData(event.dataContext) ?: return true

        isMockFs = Messages.YES == Messages.showYesNoDialog(
            project,
            "Do you want to use a mocked file system?",
            "Setting file system",
            Messages.getQuestionIcon())

        val replLibraryCoord = findReplCoord(project) ?: throw ExecutionException("""
                The library org.apache.spark:spark-core is not in project dependencies.
                The project may not be a Spark Application project.
                Please create it from the wizard or add Spark related libraries into dependencies.
                ( Refer to https://www.jetbrains.com/help/idea/library.html#add-library-to-module-dependencies )
        """.trimIndent())

        // Check repl dependence and prompt the user to fix it
        checkReplDependenceAndTryToFix(replLibraryCoord,project)

        // Workaround for Spark 2.3 jline issue, refer to:
        // - https://github.com/Microsoft/azure-tools-for-java/issues/2285
        // - https://issues.apache.org/jira/browse/SPARK-13710
        val jlineLibraryCoord = "jline:jline:2.14.5"
        if (getLibraryByCoord(jlineLibraryCoord,project) == null) {
            promptAndFix(jlineLibraryCoord,project)
        }

        return super.onActionPerformed(event, operation)
    }

    private fun findReplCoord(project: Project): String? {
        val iterator = LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraryIterator

        while (iterator.hasNext()) {
            val libEntryName = iterator.next().name ?: continue

            // Replace `core` to `repl` with the title removed, such as:
            //     Maven: org.apache.spark:spark-core_2.11:2.1.0 => org.apache.spark:spark-repl_2.11:2.1.0
            //     ^^^^^^^                       ^^^^                                      ^^^^
            val replCoord = sparkCoreCoodRegex.replace(libEntryName) { "${it.groupValues[1]}repl${it.groupValues[3]}" }

            if (replCoord != libEntryName) {
                // Found and replaced
                return replCoord
            }
        }

        return null
    }

    private fun checkReplDependenceAndTryToFix(replLibraryCoord: String,project: Project) {

        if (getLibraryByCoord(replLibraryCoord,project) == null
            && JavaPsiFacade.getInstance(project).findClass(replMain, ProjectScope.getLibrariesScope(project)) == null) {
            // `repl.Main` is not in the project class path
            promptAndFix(replLibraryCoord,project)
        }
    }

    private fun getLibraryByCoord(libraryCoord: String,project: Project): Library? = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        .libraries.firstOrNull { it.name?.endsWith(libraryCoord) == true }

    private fun promptAndFix(libraryCoord: String,project: Project) {
        val toFixDialog = object : ErrorWindow(
            project,
            "The library $libraryCoord is not in project dependencies, would you like to auto fix it?",
            "Auto fix dependency issue to confirm") {
            init {
                setOKButtonText("Auto Fix")
            }

            override fun createActions(): Array<Action> {
                return arrayOf(okAction, cancelAction)
            }
        }

        val toFix = toFixDialog.showAndGet()

        if (toFix) {
            val progress = DispatchThreadProgressWindow(false, project).apply {
                setRunnable {
                    ProgressManager.getInstance().runProcess({
                        text = "Download $libraryCoord ..."
                        fixDependence(libraryCoord,project)
                    }, this@apply)
                }

                title = "Auto fix dependency $libraryCoord"
            }

            progress.start()
        }
    }

    private fun fixDependence(libraryCoord: String,project: Project) {
        runInWriteAction {
            val projectRepositories = RemoteRepositoriesConfiguration.getInstance(project).repositories
            val newLibConf: NewLibraryConfiguration = JarRepositoryManager.resolveAndDownload(
                project, libraryCoord, false, false, true, null, projectRepositories) ?: return@runInWriteAction
            val libraryType = newLibConf.libraryType
            val library = LibraryTablesRegistrar.getInstance().getLibraryTable(project).createLibrary("Apache Spark Console(auto-fix): $libraryCoord")

            val editor = NewLibraryEditor(libraryType, null)
            newLibConf.addRoots(editor)
            val model = library.modifiableModel
            editor.applyTo(model as LibraryEx.ModifiableModelEx)
            model.commit()
        }
    }

}