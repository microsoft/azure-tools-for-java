/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.hdinsight.projects;

import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;

public class SparkLibraryInfoForEclipse {
    private String localPath;
    private String title;
    private String version;

    private static final String TITLE_LABEL = "Implementation-Title";
    private static final String VERSION_LABEL = "Implementation-Version";

    public SparkLibraryInfoForEclipse(@NotNull String path) throws Exception{
        if(path.endsWith("!/")) {
            path = path.substring(0, path.length() - 2);
        }

        this.localPath = path.toLowerCase();
        JarFile jarFile = new JarFile(this.localPath);
        Manifest mainFest = jarFile.getManifest();
        if(mainFest != null) {
            title = jarFile.getManifest().getMainAttributes().getValue(TITLE_LABEL);
            version = jarFile.getManifest().getMainAttributes().getValue(VERSION_LABEL);
        }

        if(StringHelper.isNullOrWhiteSpace(title)) {
            title = "unkown";
        }

        if(StringHelper.isNullOrWhiteSpace(version)) {
            version = "unkown";
        }

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

        if(obj instanceof SparkLibraryInfoForEclipse) {
            SparkLibraryInfoForEclipse other = (SparkLibraryInfoForEclipse)obj;
            return this.title.equals(other.title) && this.version.equals(version);
        }

        return false;
    }
}
