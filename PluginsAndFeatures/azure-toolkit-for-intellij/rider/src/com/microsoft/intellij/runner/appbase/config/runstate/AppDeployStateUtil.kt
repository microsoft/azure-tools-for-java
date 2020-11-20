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

package com.microsoft.intellij.runner.appbase.config.runstate

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rd.util.spinUntil
import com.jetbrains.rd.util.threading.SpinWait
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.model.BuildResultKind
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.run.configurations.publishing.base.MsBuildPublishingService
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.utils.AzureUIRefreshCore
import com.microsoft.azuretools.utils.AzureUIRefreshEvent
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipOutputStream

object AppDeployStateUtil {

    private val logger = Logger.getInstance(AppDeployStateUtil::class.java)

    private const val COLLECT_ARTIFACTS_TIMEOUT_MS = 180000L

    private const val APP_START_TIMEOUT_MS = 20000L
    private const val APP_STOP_TIMEOUT_MS = 10000L
    private const val APP_LAUNCH_TIMEOUT_MS = 10000L

    private const val APP_STATE_RUNNING = "Running"

    private val activityNotifier = AzureDeploymentProgressNotification()

    var projectAssemblyRelativePath = ""

    fun appStart(app: WebAppBase, processHandler: RunProcessHandler, progressMessage: String, notificationTitle: String) {
        processHandler.setText(progressMessage)
        app.start()

        spinUntil(APP_START_TIMEOUT_MS) {
            val state = app.state() ?: return@spinUntil false
            WebAppBaseState.fromString(state) == WebAppBaseState.RUNNING
        }

        activityNotifier.notifyProgress(notificationTitle, Date(), app.defaultHostName(), 100, progressMessage)
    }

    fun appStop(app: WebAppBase, processHandler: RunProcessHandler, progressMessage: String, notificationTitle: String) {
        processHandler.setText(progressMessage)
        app.stop()

        spinUntil(APP_STOP_TIMEOUT_MS) {
            val state = app.state() ?: return@spinUntil false
            WebAppBaseState.fromString(state) == WebAppBaseState.STOPPED
        }

        activityNotifier.notifyProgress(notificationTitle, Date(), app.defaultHostName(), 100, progressMessage)
    }

    fun generateConnectionString(fullyQualifiedDomainName: String,
                                 database: SqlDatabase,
                                 adminLogin: String,
                                 adminPassword: CharArray) =
            "Data Source=tcp:$fullyQualifiedDomainName,1433;" +
                    "Initial Catalog=${database.name()};" +
                    "User Id=$adminLogin@${database.sqlServerName()};" +
                    "Password=${adminPassword.joinToString("")}"

    fun getAppUrl(app: WebAppBase): String {
        return "https://" + app.defaultHostName()
    }

    fun openAppInBrowser(app: WebAppBase, processHandler: RunProcessHandler) {
        val isStarted = SpinWait.spinUntil(APP_LAUNCH_TIMEOUT_MS) { app.state() == APP_STATE_RUNNING }

        if (!isStarted && !processHandler.isProcessTerminated && !processHandler.isProcessTerminating)
            processHandler.setText(message("process_event.publish.web_apps.app_not_started", app.state()))

        BrowserUtil.browse(getAppUrl(app))
    }

    /**
     * Collect a selected project artifacts and get a [File] with out folder
     * Note: For a DotNET Framework projects publish a copy of project folder as is
     *       For a DotNet Core projects generate an publishable output by [MsBuildPublishingService]
     *
     * @param project IDEA [Project] instance
     * @param publishableProject contains information about project to be published (isDotNetCore, path to project file)
     *
     * @return [File] to project content to be published
     */
    fun collectProjectArtifacts(project: Project, publishableProject: PublishableProjectModel): File {
        // Get out parameters
        val publishService = MsBuildPublishingService.getInstance(project)
        val (targetProperties, outPath) = publishService.getPublishToTempDirParameterAndPath()

        val event = AsyncPromise<BuildResultKind>()
        application.invokeLater {

            val onFinish: (result: BuildResultKind) -> Unit = {
                if (it == BuildResultKind.Successful || it == BuildResultKind.HasWarnings) {
                    requestRunWindowFocus(project)
                }

                event.setResult(it)
            }

            if (publishableProject.isDotNetCore) {
                publishService.invokeMsBuild(publishableProject, listOf(targetProperties), false, true, true, onFinish)
            } else {
                publishService.webPublishToFileSystem(publishableProject.projectFilePath, outPath, false, true, onFinish)
            }
        }

        val buildResult = event.get(COLLECT_ARTIFACTS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (buildResult != BuildResultKind.Successful && buildResult != BuildResultKind.HasWarnings) {
            val errorMessage = message("process_event.publish.project.artifacts.collecting_failed")
            logger.error(errorMessage)
            throw RuntimeException(errorMessage)
        }

        return outPath.toFile().canonicalFile
    }

    /**
     * Move focus to Run tool window to switch back to the running process
     */
    private fun requestRunWindowFocus(project: Project) {
        try {
            ToolWindowManager.getInstance(project).invokeLater(Runnable {
                val window = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)
                if (window != null && window.isAvailable)
                    window.show(null)
            })
        } catch (e: Throwable) {
            logger.error(e)
        }
    }

    /**
     * Get a relative path for an assembly name from a project output directory
     */
    fun getAssemblyRelativePath(publishableProject: PublishableProjectModel, outDir: File): String {
        val defaultPath = "${publishableProject.projectName}.dll"

        val assemblyFile = publishableProject.projectOutputs.find { output ->
            val exeFile = output.exePath.toIOFile()
            exeFile.exists()
        }?.exePath?.toIOFile() ?: return defaultPath

        return outDir.walk().find { it.isFile && it.name == assemblyFile.name }?.relativeTo(outDir)?.path ?: defaultPath
    }

    @Throws(FileNotFoundException::class)
    fun zipProjectArtifacts(fromFile: File,
                            processHandler: RunProcessHandler,
                            deleteOriginal: Boolean = true): File {

        if (!fromFile.exists())
            throw FileNotFoundException("Original file '${fromFile.path}' not found")

        try {
            val toZip = FileUtil.createTempFile(fromFile.nameWithoutExtension, ".zip", true)
            packToZip(fromFile, toZip)
            processHandler.setText(message("process_event.publish.zip_deploy.file_create_success", toZip.path))

            if (deleteOriginal)
                FileUtil.delete(fromFile)

            return toZip
        } catch (t: Throwable) {
            val errorMessage = "${message("process_event.publish.zip_deploy.file_not_created")}: $t"
            logger.error(errorMessage)
            processHandler.setText(errorMessage)
            throw RuntimeException(message("process_event.publish.zip_deploy.file_not_created"), t)
        }
    }

    fun refreshAzureExplorer(listenerId: String) {
        val listeners = AzureUIRefreshCore.listeners

        if (!listeners.isNullOrEmpty()) {
            val expectedListener = listeners.filter { it.key == listenerId }
            if (expectedListener.isNotEmpty()) {
                try {
                    AzureUIRefreshCore.listeners = expectedListener
                    AzureUIRefreshCore.execute(AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null))
                } catch (t: Throwable) {
                    logger.error("Error while refreshing Azure Explorer tree: $t")
                } finally {
                    AzureUIRefreshCore.listeners = listeners
                }
            }
        }
    }

    /**
     * @throws [FileNotFoundException] when ZIP file does not exists
     */
    @Throws(FileNotFoundException::class)
    private fun packToZip(fileToZip: File,
                          zipFileToCreate: File,
                          filter: FileFilter? = null) {
        if (!fileToZip.exists()) {
            val message = "Source file or directory '${fileToZip.path}' does not exist"
            logger.error(message)
            throw FileNotFoundException(message)
        }

        ZipOutputStream(FileOutputStream(zipFileToCreate)).use { zipOutput ->
            ZipUtil.addDirToZipRecursively(zipOutput, null, fileToZip, "", filter, null)
        }
    }
}
