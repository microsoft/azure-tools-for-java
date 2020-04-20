/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.ZipUtil
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.string.printToString
import com.jetbrains.rd.util.warn
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.GitHubReleasesService
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

object FunctionsCoreToolsManager {
    private const val CORE_TOOLS_DIR = "azure-functions-coretools"
    private const val API_URL_RELEASES = "repos/Azure/azure-functions-core-tools/releases?per_page=100"

    private val downloadPath: String = PathManager.getConfigPath() + File.separator + CORE_TOOLS_DIR

    private val logger = getLogger<FunctionsCoreToolsManager>()

    fun downloadLatestRelease(allowPrerelease: Boolean, indicator: ProgressIndicator, onComplete: (String) -> Unit) {
        object : Task.Backgroundable(null, message("progress.function_app.core_tools.downloading_latest"), true) {
            override fun run(indicator: ProgressIndicator) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    downloadLatestReleaseInternal(allowPrerelease, indicator, onComplete)
                }
            }
        }.run(indicator)
    }

    fun downloadLatestRelease(project: Project, allowPrerelease: Boolean, onComplete: (String) -> Unit): Task {
        return object : Task.Backgroundable(project, message("progress.function_app.core_tools.downloading_latest"), true) {
            override fun run(pi: ProgressIndicator) {
                downloadLatestReleaseInternal(allowPrerelease, pi, onComplete)
            }
        }
    }

    private fun downloadLatestReleaseInternal(allowPrerelease: Boolean, indicator: ProgressIndicator, onComplete: (String) -> Unit) {
        if (!indicator.isRunning) indicator.start()

        // Grab latest URL
        indicator.text = message("progress.function_app.core_tools.determining_download_url")
        indicator.isIndeterminate = true

        val latestLocal = determineVersion(determineLatestLocalCoreToolsPath())
        val latestRemote = determineLatestRemote(allowPrerelease)
        if (latestRemote == null) {
            logger.error { "Could not determine latest remote version." }
        }
        if (latestRemote == null || latestLocal?.compareTo(latestRemote) == 0) {
            indicator.text = message("progress.common.finished")
            if (indicator.isRunning) indicator.stop()

            if (latestLocal != null) {
                onComplete(latestLocal.fullPath)
            }

            return
        }

        val tempFile = FileUtil.createTempFile(
                File(FileUtil.getTempDirectory()),
                latestRemote.fileName,
                "download", true, true)

        // Download
        indicator.text = message("progress.function_app.core_tools.preparing_to_download")
        indicator.isIndeterminate = false
        HttpRequests.request(latestRemote.downloadUrl)
                .productNameAsUserAgent()
                .connect {
                    indicator.text = message("progress.function_app.core_tools.downloading")
                    it.saveToFile(tempFile, indicator)
                }

        indicator.checkCanceled()

        // Extract
        val latestDirectory = File(downloadPath).resolve(latestRemote.version)

        ProgressManager.getInstance().executeNonCancelableSection {
            indicator.text = message("progress.function_app.core_tools.preparing_to_extract")
            indicator.isIndeterminate = true
            try {
                if (latestDirectory.exists()) latestDirectory.deleteRecursively()
            } catch (e: Exception) {
                logger.error("Error while removing latest directory $latestDirectory.path", e)
            }

            indicator.text = message("progress.function_app.core_tools.extracting")
            indicator.isIndeterminate = true
            try {
                ZipUtil.extract(tempFile, latestDirectory, null)
            } catch (e: Exception) {
                logger.error("Error while extracting $tempFile.path to $latestDirectory.path", e)
            }

            indicator.text = message("progress.function_app.core_tools.cleaning_up_older_versions")
            indicator.isIndeterminate = true
            if (latestLocal != null) {
                val latestLocalDirectory = File(latestLocal.fullPath)
                try {
                    if (latestLocalDirectory.exists()) latestLocalDirectory.deleteRecursively()
                } catch (e: Exception) {
                    logger.error("Error while removing older version directory $latestLocalDirectory.path", e)
                }
            }

            indicator.text = message("progress.function_app.core_tools.cleaning_up_temporary_files")
            indicator.isIndeterminate = true
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (e: Exception) {
                logger.error("Error while removing temporary file $tempFile.path", e)
            }

            indicator.text = message("progress.common.finished")
        }

        if (indicator.isRunning)
            indicator.stop()

        onComplete(latestDirectory.path)
    }

    fun getCoreToolsExecutableName(): String {
        return if (SystemInfo.isWindows) { "func.exe" }
        else { "func" }
    }

    fun determineVersion(coreToolsPath: String?): AzureFunctionsCoreToolsLocalAsset? {
        coreToolsPath ?: return null

        val coreToolsExecutableDir = File(coreToolsPath)
        val coreToolsExecutable = coreToolsExecutableDir.resolve(getCoreToolsExecutableName())
        if (!coreToolsExecutable.exists())
            return null


        try {
            if (!coreToolsExecutable.canExecute()) {
                logger.warn { "Updating executable flag for $coreToolsPath..." }
                try {
                    coreToolsExecutable.setExecutable(true)
                } catch (s: SecurityException) {
                    logger.error("Failed setting executable flag for $coreToolsPath", s)
                }
            }

            val commandLine = GeneralCommandLine()
                    .withExePath(coreToolsExecutable.path)
                    .withParameters("--version")

            val processHandler = CapturingProcessHandler(commandLine)
            val output = processHandler.runProcess(15000, true)
            val version = output.stdoutLines.firstOrNull()?.trim('\r', '\n')
            if (!version.isNullOrEmpty()) {
                return AzureFunctionsCoreToolsLocalAsset(version, coreToolsPath)
            }
        } catch (e: Exception) {
            logger.error("Error while determining version of tools in $coreToolsPath", e)
        }

        return null
    }

    fun determineLatestLocalCoreToolsPath(toolsDownloadPath: String = downloadPath): String? {
        val downloadDirectory = File(toolsDownloadPath)
        if (downloadDirectory.exists()) {
            val versionDirs = downloadDirectory.listFiles { file -> file != null && file.isDirectory } ?: return null
            val latestDirectory = versionDirs
                    .sortedWith(Comparator<File> { o1, o2 -> StringUtil.compareVersionNumbers(o1?.name, o2?.name) })
                    .lastOrNull()

            if (latestDirectory != null) {
                return latestDirectory.path
            }
        }

        return null
    }

    fun determineLatestRemote(allowPrerelease: Boolean): AzureFunctionsCoreToolsRemoteAsset? {
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

        try {
            val gitHubReleases = GitHubReleasesService.createInstance()
                    .getReleases(API_URL_RELEASES)
                    .execute()
                    .body()

            if (gitHubReleases != null) {
                for (gitHubRelease in gitHubReleases
                        .filter { !it.tagName.isNullOrEmpty() }
                        .filter { allowPrerelease || !it.prerelease }
                        .sortedWith(Comparator { o1, o2 ->
                            VersionComparatorUtil.compare(o2.tagName!!.trimStart('v'), o1.tagName!!.trimStart('v')) // latest versions on top
                        })) {
                    val latestReleaseVersion = gitHubRelease.tagName!!.trimStart('v')

                    val latestAsset = gitHubRelease.assets.firstOrNull {
                        it.name!!.startsWith(expectedFileNamePrefix, true) && it.name.endsWith(".zip")
                    }

                    if (latestAsset != null) {
                        return AzureFunctionsCoreToolsRemoteAsset(latestReleaseVersion, latestAsset.name!!, gitHubRelease.prerelease, latestAsset.browserDownloadUrl!!)
                    }
                }
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
    class AzureFunctionsCoreToolsRemoteAsset(version: String, val fileName: String, val isPrerelease: Boolean, val downloadUrl: String) : AzureFunctionsCoreToolsAsset(version)
}