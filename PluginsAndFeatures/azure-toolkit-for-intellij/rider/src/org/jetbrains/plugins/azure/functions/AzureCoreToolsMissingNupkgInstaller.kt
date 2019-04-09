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

package org.jetbrains.plugins.azure.functions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.jetbrains.rider.nuget.RiderNuGetHost
import com.jetbrains.rider.projectView.ProjectModelViewHost
import com.jetbrains.rider.projectView.nodes.ProjectModelNode
import com.jetbrains.rider.projectView.nodes.ProjectModelNodeVisitor
import com.jetbrains.rider.projectView.nodes.containingProject
import com.jetbrains.rider.util.idea.getComponent

class AzureCoreToolsMissingNupkgInstaller : StartupActivity {
    companion object {
        private const val enableAzureFunctionsInstallMissingNupkg = "rider.enable.azure.functions.install.missing.nupkg"

        private fun shouldProcess(projectFileIndex: ProjectFileIndex, file: VirtualFile): Boolean =
                file.exists() && projectFileIndex.isInContent(file)

        private fun hasKnownFileSuffix(file: VirtualFile): Boolean =
                file.extension.equals("cs", true) ||
                file.extension.equals("vb", true) ||
                file.extension.equals("fs", true)

        private val knownMarkerWords = listOf(
                "FunctionName",
                "Microsoft.Azure.WebJobs")

        private val triggerMap = mapOf(
            Pair("BlobTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage", "3.0.0")),
            Pair("QueueTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage", "3.0.0")),
            Pair("CosmosDBTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.CosmosDB", "3.0.1")),
            Pair("OrchestrationTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.DurableTask", "1.6.2")),
            Pair("EventGridTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventGrid", "2.0.0")),
            Pair("EventHubTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "3.0.0")),
            Pair("IoTHubTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "3.0.0")),
            Pair("ServiceBusTrigger", PackageDependency("Microsoft.Azure.WebJobs.Extensions.ServiceBus", "3.0.0"))
        )
    }

    private data class PackageDependency(val id: String, val version: String)

    override fun runActivity(project: Project) {
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                if (!Registry.`is`(enableAzureFunctionsInstallMissingNupkg, true)) return

                val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project) ?: return

                for (event in events) {
                    val file = event.file ?: continue

                    if (shouldProcess(projectFileIndex, file) &&
                            hasKnownFileSuffix(file) && // Only process certain file types
                            event.requestor !is FileDocumentManagerImpl && // Don't process when typing in document
                            file.length > 0) {

                        ApplicationManager.getApplication().runReadAction {
                            val text = LoadTextUtil.loadText(file, 4096)

                            // Check for known marker words
                            if (knownMarkerWords.any { text.contains(it, true) }) {
                                // Determine project(s) to install into
                                val installableProjects = project.getComponent<ProjectModelViewHost>()
                                        .findProjectsWithVirtualFile(file)

                                // For every known trigger name, verify required dependencies are installed
                                for ((triggerName, dependency) in triggerMap) {
                                    if (text.contains(triggerName, true)) {
                                        for (installableProject in installableProjects) {
                                            val riderNuGetFacade = RiderNuGetHost.getInstance(installableProject.project).facade

                                            val isInstalled = riderNuGetFacade.host.projectInfos[installableProject.id]?.
                                                    packages?.any { it.id.equals(dependency.id, ignoreCase = true) } ?: false

                                            if (!isInstalled) {
                                                riderNuGetFacade.installForProject(
                                                        installableProject.name, dependency.id, dependency.version)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}

fun ProjectModelViewHost.findProjectsWithVirtualFile(virtualFile: VirtualFile): List<ProjectModelNode> {
    val visitor = object : ProjectModelNodeVisitor() {
        val projects: HashSet<ProjectModelNode> = hashSetOf()

        override fun visitNode(node: ProjectModelNode): Result {
            if (node.getVirtualFile() == virtualFile) {
                node.containingProject()?.let { projects.add(it) }
            }

            return Result.Continue
        }
    }

    visitor.visit(solutionNode)
    return visitor.projects.toList()
}