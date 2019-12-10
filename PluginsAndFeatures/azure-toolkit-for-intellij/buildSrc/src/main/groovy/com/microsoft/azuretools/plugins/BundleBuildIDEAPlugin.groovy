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

package com.microsoft.azuretools.plugins

import com.microsoft.azuretools.plugins.configs.BundleBuildIDEAConfig
import com.microsoft.azuretools.plugins.tasks.DownloadOptTask
import com.microsoft.azuretools.plugins.tasks.FetchAdoptOpenJdkAndSetJdkUrlTask
import com.microsoft.azuretools.plugins.tasks.InstallIdeaTask
import com.microsoft.azuretools.plugins.tasks.UnzipOptTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression

class BundleBuildIDEAPlugin implements Plugin<Project> {
    public static final String EXTENSION_NAME = "bundleBuildIdea"
    public static final String INSTALL_IDEA_TASK_NAME = "installIdea"
    public static final String FETCH_LATEST_JDK_URL_TASK_NAME = "fetchLatestJdkUrl"
    public static final String DOWNLOAD_OPT_TASK_NAME = "downloadOpt"
    public static final String UNZIP_OPT_TASK_NAME = "unzipOpt"
    public static final String COPY_TEMPLATE_FILES_TASK_NAME = "copyTemplateFiles"
    public static final String INSTALL_BUNDLE_PLUGINS_TASKK_NAME = "installBundlePlugins"
    public static final String BUNDLE_BUILD_IDEA_ZIP_TASK_NAME = "bundleBuildIdeaZip"
    public static final String INSTALL_JBR_TASK_NAME = "installJbr"

    @Override
    void apply(Project project) {
        def bundleBuildExtension = project.extensions.create(EXTENSION_NAME, BundleBuildIDEAConfig, project)

        configTasks(project, bundleBuildExtension)
    }

    private static void configTasks(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        configInstallIntelliJTask(project, bundleBuildConfig)
        configFetchLatestJdkUrlTask(project, bundleBuildConfig)
        configDownloadOptTask(project, bundleBuildConfig)
        configUnzipOptTask(project, bundleBuildConfig)
        configCopyTemplateFilesTask(project, bundleBuildConfig)
        configInstallBundlePluginsTask(project, bundleBuildConfig)
        configBuildBundleZipTask(project, bundleBuildConfig)
        configPrepareJbrTask(project, bundleBuildConfig)
    }

    private static void configPrepareJbrTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(INSTALL_JBR_TASK_NAME, Copy) {
                from tarTree(resources.gzip(new File(bundleBuildConfig.downloadedOptsDir, new File(new URL(bundleBuildConfig.jbrUrl).file).name)))
                into bundleBuildConfig.bundleBuildDir
                dependsOn DOWNLOAD_OPT_TASK_NAME
            }
        }
    }

    private static void configBuildBundleZipTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(BUNDLE_BUILD_IDEA_ZIP_TASK_NAME, Zip)  {
                from fileTree(dir: bundleBuildConfig.bundleBuildDir)
                archiveName "idea${project.intellij.type}-bundle-win-x64.zip"
                destinationDir file("$buildDir/distributions")
                exclude "bin/mac/**", "bin/linux/**", "MacOS/**", "plugins/android/**"
                entryCompression ZipEntryCompression.STORED
                dependsOn INSTALL_BUNDLE_PLUGINS_TASKK_NAME, COPY_TEMPLATE_FILES_TASK_NAME, UNZIP_OPT_TASK_NAME, INSTALL_IDEA_TASK_NAME, INSTALL_JBR_TASK_NAME
            }
        }
    }

    private static void configInstallBundlePluginsTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(INSTALL_BUNDLE_PLUGINS_TASKK_NAME, Copy) {
                from new File(intellij.sandboxDirectory, "plugins")
                into new File(bundleBuildConfig.bundleBuildDir, "config/plugins")
                dependsOn INSTALL_IDEA_TASK_NAME
            }
        }
    }

    private static void configCopyTemplateFilesTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(COPY_TEMPLATE_FILES_TASK_NAME) {
                dependsOn INSTALL_IDEA_TASK_NAME, UNZIP_OPT_TASK_NAME

                doLast {
                    copy {
                        from bundleBuildConfig.bundleTemplateDir
                        into bundleBuildConfig.bundleBuildDir
                        expand bundleBuildConfig.properties
                    }
                }
            }
        }
    }

    private static void configUnzipOptTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(UNZIP_OPT_TASK_NAME, UnzipOptTask) {
                bundleConfig = bundleBuildConfig
                dependsOn DOWNLOAD_OPT_TASK_NAME
            }
        }
    }

    private static void configDownloadOptTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(DOWNLOAD_OPT_TASK_NAME, DownloadOptTask) {
                bundleConfig = bundleBuildConfig
                dependsOn FETCH_LATEST_JDK_URL_TASK_NAME
            }
        }
    }

    private static void configFetchLatestJdkUrlTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(FETCH_LATEST_JDK_URL_TASK_NAME, FetchAdoptOpenJdkAndSetJdkUrlTask)  {
                conf = bundleBuildConfig
                adoptOpenJdkApi = bundleBuildConfig.adoptOpenJdkApi
            }
        }
    }

    private static void configInstallIntelliJTask(Project project, BundleBuildIDEAConfig bundleBuildConfig) {
        project.with {
            tasks.create(INSTALL_IDEA_TASK_NAME, InstallIdeaTask) {
                bundleConfig = bundleBuildConfig
            }
        }
    }
}
