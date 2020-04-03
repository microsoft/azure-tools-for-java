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

package com.microsoft.azuretools.plugins.tasks

import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.TaskAction

class UnzipOptTask extends BundleBuildIDEATask {
    private def unzipOptWinutils() {
        project.copy {
            from project.zipTree(new File(bundleConfig.downloadedOptsDir, new File(new URL(bundleConfig.winutilsUrl).file).name))
            into bundleConfig.optDir
            include "/*/" + bundleConfig.winutilsVer + "/**/*"
            eachFile { FileCopyDetails fileCopyDetail ->
                fileCopyDetail.relativePath = new RelativePath(true, fileCopyDetail.relativePath.segments.drop(1))
                if (bundleConfig.winutilsDir == null || bundleConfig.winutilsUrl.isEmpty()) {
                    bundleConfig.winutilsDir = new File(bundleConfig.optDir, fileCopyDetail.relativePath.segments[0]).toString()
                }
            }
            includeEmptyDirs = false
        }
    }

    private def unzipOpts() {
        // Fast unzip with default options
        def fastUnzipOptFilenames2Type = ["jdk", "scalaSdk"]
                .collectEntries { [ (new File(new URL(bundleConfig[it + "Url"]).file).name) : it ] }

        for (zipFile in project.file(bundleConfig.downloadedOptsDir).listFiles()) {
            // Only unzip selected zip files
            if (!fastUnzipOptFilenames2Type.containsKey(zipFile.name)) {
                continue
            }

            project.copy {
                from project.zipTree(zipFile.absolutePath)
                into bundleConfig.optDir
                eachFile { FileCopyDetails fileCopyDetail ->
                    def unzipDir = fastUnzipOptFilenames2Type[zipFile.name] + "Dir"

                    if (bundleConfig[unzipDir] == null || bundleConfig[unzipDir].isEmpty()) {
                        bundleConfig[unzipDir] =
                                new File(bundleConfig.optDir, fileCopyDetail.relativePath.segments[0]).toString()
                    }
                }
            }
        }
    }

    @TaskAction
    def unzip() {
        unzipOptWinutils()
        unzipOpts()
    }
}
