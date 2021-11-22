/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;

public class JarUtils {
    public static Path buildJarFileToStagingPath(String stagingFolder, MavenProject project) throws IOException {
        final File stagingFolderFile = new File(stagingFolder);
        if (!stagingFolderFile.exists()) {
            stagingFolderFile.mkdirs();
        }

        final String path = project.getBuild().getOutputDirectory();
        final Path outputFile = Paths.get(stagingFolder, project.getBuild().getFinalName() + ".jar");
        final JarArchiver jar = new JarArchiver();
        jar.setCompress(true);
        jar.setDestFile(outputFile.toFile());
        jar.addDirectory(new File(path));
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "Azure Intellij Plugin");
        try {
            jar.addConfiguredManifest(manifest);
        } catch (ManifestException e) {
            throw new AzureToolkitRuntimeException("Cannot create manifest for function jar.", e);
        }
        jar.createArchive();
        return outputFile;
    }
}
