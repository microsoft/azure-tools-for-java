/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.utils;

import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.jar.Attributes;

public class JarUtils {
    public static File buildJarFileToTempFile(FunctionProject project) throws IOException {
        final File tempFile = File.createTempFile("AzureFunction_" + project.getName(), ".jar");
        final JarArchiver jar = new JarArchiver();
        jar.setCompress(true);
        jar.setDestFile(tempFile);
        if (Objects.nonNull(project.getClassesOutputDirectory()) && project.getClassesOutputDirectory().exists()) {
            jar.addDirectory(project.getClassesOutputDirectory());
        }
        if (Objects.nonNull(project.getResourceOutputDirectory()) && project.getResourceOutputDirectory().exists()) {
            jar.addDirectory(project.getResourceOutputDirectory());
        }
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "Azure Toolkit for Eclipse");
        try {
            jar.addConfiguredManifest(manifest);
        } catch (ManifestException e) {
            throw new AzureToolkitRuntimeException("Cannot create manifest for function jar.", e);
        }
        jar.createArchive();
        return tempFile;
    }
}
