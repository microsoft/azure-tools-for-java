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

package org.jetbrains.plugins.azure.functions.coreTools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.ZipUtil
import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.warn
import org.jetbrains.plugins.azure.functions.GitHubReleasesService
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

object FunctionsCoreToolsManager {
    private const val DOWNLOADTASK_TITLE = "Downloading latest Azure Functions Core Tools..."
    private const val CORETOOLS_DIR = "azure-functions-coretools"
    private const val LATEST_RELEASE_URL = "repos/Azure/azure-functions-core-tools/releases/latest"

    private val downloadPath = PathManager.getConfigPath() + File.separator + FunctionsCoreToolsManager.CORETOOLS_DIR

    private val logger = getLogger<FunctionsCoreToolsManager>()

    fun downloadLatestRelease(indicator: ProgressIndicator, completed: (String) -> Unit) {
        object : Task.Backgroundable(null, DOWNLOADTASK_TITLE, true) {
            override fun run(pi: ProgressIndicator) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    downloadLatestReleaseInternal(pi, completed)
                }
            }
        }.run(indicator)
    }

    fun downloadLatestRelease(project: Project, completed: (String) -> Unit): Task {
        return object : Task.Backgroundable(project, DOWNLOADTASK_TITLE, true) {
            override fun run(pi: ProgressIndicator) {
                downloadLatestReleaseInternal(pi, completed)
            }
        }
    }

    private fun downloadLatestReleaseInternal(pi: ProgressIndicator, completed: (String) -> Unit) {
        if (!pi.isRunning) pi.start()

        // Grab latest URL
        pi.text = "Determining download URL..."
        pi.isIndeterminate = true

        val latestLocal = determineVersion(determineLatestLocalCoreToolsPath())
        val latestRemote = determineLatestRemote()
        if (latestRemote == null) {
            logger.error { "Could not determine latest remote version." }
        }
        if (latestRemote == null || latestLocal?.compareTo(latestRemote) == 0) {
            pi.text = "Finished."
            if (pi.isRunning) pi.stop()

            if (latestLocal != null) {
                completed(latestLocal.fullPath)
            }

            return
        }

        val tempFile = FileUtil.createTempFile(
                File(FileUtil.getTempDirectory()),
                latestRemote.fileName,
                "download", true, true)

        // Download
        pi.text = "Preparing to download..."
        pi.isIndeterminate = false
        HttpRequests.request(latestRemote.downloadUrl)
                .productNameAsUserAgent()
                .connect {
                    pi.text = "Downloading..."
                    it.saveToFile(tempFile, pi)
                }

        pi.checkCanceled()

        // Extract
        pi.startNonCancelableSection()
        pi.text = "Preparing to extract..."
        pi.isIndeterminate = true
        val latestDirectory = File(downloadPath).resolve(latestRemote.version)
        try {
            if (latestDirectory.exists()) latestDirectory.deleteRecursively()
        } catch (e: Exception) {
            logger.error("Error while removing latest directory $latestDirectory.path", e)
        }

        pi.text = "Extracting..."
        pi.isIndeterminate = true
        try {
            ZipUtil.extract(tempFile, latestDirectory, null)
        } catch (e: Exception) {
            logger.error("Error while extracting $tempFile.path to $latestDirectory.path", e)
        }

        pi.text = "Cleaning up older versions..."
        pi.isIndeterminate = true
        if (latestLocal != null) {
            val latestLocalDirectory = File(latestLocal.fullPath)
            try {
                if (latestLocalDirectory.exists()) latestLocalDirectory.deleteRecursively()
            } catch (e: Exception) {
                logger.error("Error while removing older version directory $latestLocalDirectory.path", e)
            }
        }

        pi.text = "Cleaning up temporary files..."
        pi.isIndeterminate = true
        try {
            if (tempFile.exists()) tempFile.delete()
        } catch (e: Exception) {
            logger.error("Error while removing temporary file $tempFile.path", e)
        }

//      // TODO: Should we register project templates?
//      indicator.text = "Registering templates..."
//      val templatesFolder = File(latestDirectory.path + File.separator + "templates")
//      if (templatesFolder.exists()) {
//          templatesFolder
//              .listFiles { f: File? -> f != null && f.isFile && f.name.contains("projectTemplates", true) }
//              .forEach {
//                  ReSharperProjectTemplateProvider.addUserTemplateSource(it)
//              }
//       }

        pi.finishNonCancelableSection()
        pi.text = "Finished."
        if (pi.isRunning) pi.stop()

        completed(latestDirectory.path)
    }

    fun determineVersion(coreToolsPath: String?): AzureFunctionsCoreToolsLocalAsset? {
        if (coreToolsPath == null) return null

        val coreToolsExecutablePath = if (SystemInfo.isWindows) {
            File(coreToolsPath).resolve("func.exe")
        } else {
            File(coreToolsPath).resolve("func")
        }

        try {
            if (coreToolsExecutablePath.exists()) {
                if (!coreToolsExecutablePath.canExecute()) {
                    logger.warn { "Updating executable flag for $coreToolsPath..." }
                    try {
                        coreToolsExecutablePath.setExecutable(true)
                    } catch (s: SecurityException) {
                        logger.error("Failed setting executable flag for $coreToolsPath", s)
                    }
                }

                val commandLine = GeneralCommandLine()
                        .withExePath(coreToolsExecutablePath.path)
                        .withParameters("--version")

                val processHandler = CapturingProcessHandler(commandLine)
                val output = processHandler.runProcess(15000, true)
                val version = output.stdoutLines.firstOrNull()?.trim('\r', '\n')
                if (!version.isNullOrEmpty()) {
                    return AzureFunctionsCoreToolsLocalAsset(version!!, coreToolsPath)
                }
            }
        } catch (e: Exception) {
            logger.error("Error while determining version of tools in $coreToolsPath", e)
        }

        return null
    }

    private fun determineLatestLocalCoreToolsPath(): String? {
        val downloadDirectory = File(downloadPath)
        if (downloadDirectory.exists()) {
            val latestDirectory = downloadDirectory
                    .listFiles { f: File? -> f != null && f.isDirectory }
                    .sortedWith(Comparator<File> { o1, o2 -> StringUtil.compareVersionNumbers(o1?.name, o2?.name) })
                    .lastOrNull()

            if (latestDirectory != null) {
                return latestDirectory.path
            }
        }

        return null
    }

    fun determineLatestRemote(): AzureFunctionsCoreToolsRemoteAsset? {
        try {
            val latestGitHubRelease = GitHubReleasesService.createInstance()
                    .getLatestRelease(LATEST_RELEASE_URL)
                    .execute()
                    .body()

            if (latestGitHubRelease != null) {
                val expectedFileNamePrefix = "Azure.Functions.Cli." + if (SystemInfo.isWindows && SystemInfo.is64Bit) {
                    "win-x64"
                } else if (SystemInfo.isWindows) {
                    "win-x86"
                } else if (SystemInfo.isMac) {
                    "osx-x64"
                } else if (SystemInfo.isLinux) {
                    "linux-x64"
                } else {
                    "unknown"
                }

                val latestReleaseVersion = latestGitHubRelease.tagName!!.trimStart('v')

                val latestAsset = latestGitHubRelease.assets.first {
                    it.name!!.startsWith(expectedFileNamePrefix, true) && it.name.endsWith(".zip")
                }

                return AzureFunctionsCoreToolsRemoteAsset(latestReleaseVersion, latestAsset.name!!, latestAsset.browserDownloadUrl!!)
            }
        } catch (e: UnknownHostException) {
            logger.warn { "Could not determine latest remote: " + e.printToString() }
        } catch (e: IOException) {
            logger.warn { "Could not determine latest remote: " + e.printToString() }
        }

        return null
    }

    open class AzureFunctionsCoreToolsAsset(val version: String) : Comparable<AzureFunctionsCoreToolsAsset> {
        override fun compareTo(other: AzureFunctionsCoreToolsAsset): Int {
            return StringUtil.compareVersionNumbers(version, other.version)
        }
    }

    class AzureFunctionsCoreToolsLocalAsset(version: String, val fullPath: String) : AzureFunctionsCoreToolsAsset(version)
    class AzureFunctionsCoreToolsRemoteAsset(version: String, val fileName: String, val downloadUrl: String) : AzureFunctionsCoreToolsAsset(version)
}