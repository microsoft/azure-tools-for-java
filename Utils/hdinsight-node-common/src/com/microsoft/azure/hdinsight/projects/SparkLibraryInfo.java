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

package com.microsoft.azure.hdinsight.projects;

import java.util.jar.JarFile;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class SparkLibraryInfo {
    private String localPath;
    private String title;
    private String version;

    private static final String TITLE_LABEL = "Implementation-Title";
    private static final String VERSION_LABEL = "Implementation-Version";

    public SparkLibraryInfo(@NotNull String path) throws Exception{
        if(path.endsWith("!/")) {
            path = path.substring(0, path.length() - 2);
        }

        this.localPath = path.toLowerCase();
        JarFile jarFile = new JarFile(this.localPath);
        title = jarFile.getManifest().getMainAttributes().getValue(TITLE_LABEL);
        version = jarFile.getManifest().getMainAttributes().getValue(VERSION_LABEL);
        jarFile.close();
    }

    @NotNull
    public String getLocalPath() {
        return localPath;
    }

    @Override
    public int hashCode() {
        return title.hashCode() + version.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj instanceof SparkLibraryInfo) {
            SparkLibraryInfo other = (SparkLibraryInfo)obj;
            return this.title.equals(other.title) && this.version.equals(version);
        }

        return false;
    }
}
