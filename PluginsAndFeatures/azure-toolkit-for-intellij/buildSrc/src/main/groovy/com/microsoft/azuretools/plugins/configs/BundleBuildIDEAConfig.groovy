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

package com.microsoft.azuretools.plugins.configs

import org.gradle.api.Project

class BundleBuildIDEAConfig implements JdkUrlConfigurable {
    // Downloading resources
    String jdkUrl
    String jbrUrl = "https://bintray.com/jetbrains/intellij-jbr/download_file?file_path=jbr_jfx_jcef-11_0_6-windows-x64-b765.25.tar.gz"
    String scalaSdkUrl = "https://downloads.lightbend.com/scala/2.11.12/scala-2.11.12.zip"
    String winutilsUrl = "https://github.com/steveloughran/winutils/archive/tag_2017-08=29-hadoop-2.8.1-native.zip"

    private String[] downloadUrls

    String adoptOpenJdkApi = "https://api.adoptopenjdk.net/v2/info/releases/openjdk8?os=windows&arch=x64&type=jdk&openjdk_impl=hotspot&release=latest"

    // Version selection
    String winutilsVer = "hadoop-2.7.1"
    String scalaVer = "2.11.12"

    // Dir configuration
    String bundleBuildDir
    String downloadedOptsDir
    String optDir
    String bundleResourceDir
    String bundleTemplateDir

    // Opt dir
    String jdkDir
    String scalaSdkDir
    String winutilsDir

    BundleBuildIDEAConfig(Project project) {
        bundleBuildDir = new File(project.buildDir, "bundle").toString()
        downloadedOptsDir = new File(project.buildDir, "bundle-opts").toString()
        optDir = new File(project.buildDir, "bundle/opt").toString()
        bundleResourceDir = project.file("resources/bundle").toString()
        bundleTemplateDir = project.file("resources/bundle/template").toString()
    }

    String[] getDownloadUrls() {
        return downloadUrls == null
                ? [ jdkUrl, jbrUrl, scalaSdkUrl, winutilsUrl ]
                : downloadUrls
    }
}
