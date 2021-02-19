/**
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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

@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.azure.functions

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rider.nuget.RiderNuGetHost
import com.jetbrains.rider.projectView.workspace.containingProjectEntity
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.projectView.workspace.getProjectModelEntities
import com.jetbrains.rider.projectView.workspace.impl.WorkspaceProjectRootsTracker

class AzureCoreToolsMissingNupkgInstaller : StartupActivity {
    companion object {
        private const val enableAzureFunctionsInstallMissingNupkg = "rider.enable.azure.functions.install.missing.nupkg"
        private val markedForProcessing = Key<Boolean>("AzureCoreToolsMissingNupkgInstaller_MarkedForProcessing")

        private fun hasKnownFileSuffix(file: VirtualFile): Boolean =
                file.extension.equals("cs", true) ||
                file.extension.equals("vb", true) ||
                file.extension.equals("fs", true)

        private fun existsInCurrentProject(project: Project, file: VirtualFile): Boolean =
                WorkspaceProjectRootsTracker.getInstance(project).contains(file)

        private fun isNewOrMarkedForProcessing(file: VirtualFile, event: VFileEvent) = when (event) {
            is VFileCreateEvent -> true
            is VFileContentChangeEvent -> file.getUserData(markedForProcessing) ?: false
            else -> false
        }

        private val knownMarkerWords = listOf(
                "FunctionName",
                "Microsoft.Azure.WebJobs")

        private val triggerMap = mapOf(
            Pair("BlobTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage", "3.0.4")),
            Pair("QueueTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage", "3.0.4")),
            Pair("CosmosDBTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.CosmosDB", "3.0.5")),
            Pair("OrchestrationTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.DurableTask", "2.1.1")),
            Pair("EventGridTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventGrid", "2.1.0")),
            Pair("EventHubTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "4.1.1")),
            Pair("IoTHubTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "4.1.1")),
            Pair("ServiceBusTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.ServiceBus", "4.1.0"))
        )
    }

    private data class PackageDependency(val id: String, val version: String)

    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                if (!Registry.`is`(enableAzureFunctionsInstallMissingNupkg, true)) return

                for (event in events) {
                    val file = event.file ?: continue

                    if (hasKnownFileSuffix(file) &&
                            isNewOrMarkedForProcessing(file, event) &&
                            existsInCurrentProject(project, file)) {

                        // First pass(es), no content will be in the file.
                        // If that is the case, mark the file for processing on later changes.
                        if (file.length < 25 || event is VFileCreateEvent) {
                            file.putUserData(markedForProcessing, true)
                            continue
                        }

                        application.invokeLater {
                            application.runReadAction {
                                var keepMarker = false
                                val text = LoadTextUtil.loadText(file, 4096)

                                // Check for known marker words
                                if (knownMarkerWords.any { text.contains(it, true) }) {
                                    // Determine project(s) to install into
                                    val installableProjects = WorkspaceModel.getInstance(project)
                                            .getProjectModelEntities(file, project)
                                            .mapNotNull { it.containingProjectEntity() }

                                    if (installableProjects.isEmpty()) {
                                        keepMarker = true
                                    }

                                    // For every known trigger name, verify required dependencies are installed
                                    for ((triggerName, dependency) in triggerMap) {
                                        if (text.contains(triggerName, true)) {
                                            for (installableProject in installableProjects) {
                                                val riderNuGetFacade = RiderNuGetHost.getInstance(project)
                                                        .facade

                                                val isInstalled = riderNuGetFacade.host.nuGetProjectModel
                                                        .projects[installableProject.getId(project)]
                                                        ?.explicitPackages?.any { it.id.equals(dependency.id, ignoreCase = true) }
                                                            ?: false

                                                if (!isInstalled) {
                                                    riderNuGetFacade.installForProject(
                                                            installableProject.name, dependency.id, dependency.version)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Remove marker
                                file.putUserData(markedForProcessing, keepMarker)
                            }
                        }
                    }
                }
            }
        })
    }
}