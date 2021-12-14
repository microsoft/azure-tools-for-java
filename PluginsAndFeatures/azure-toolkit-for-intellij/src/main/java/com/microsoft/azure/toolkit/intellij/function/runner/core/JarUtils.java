/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.core;

import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationException;
import com.microsoft.azure.toolkit.lib.common.operation.SimpleOperation;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;

public class JarUtils {
    public static Path buildJarFileToStagingPath(String stagingFolder, Module module) throws IOException {
        final File stagingFolderFile = new File(stagingFolder);
        if (!stagingFolderFile.exists()) {
            stagingFolderFile.mkdirs();
        }
        final String moduleName = module.getName();
        final String path = CompilerPaths.getModuleOutputPath(module, false);
        final Path outputFile = Paths.get(stagingFolder, moduleName + ".jar");
        final JarArchiver jar = new JarArchiver();
        jar.setCompress(true);
        jar.setDestFile(outputFile.toFile());
        jar.addDirectory(new File(path));
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Created-By"), "Azure Intellij Plugin");
        try {
            jar.addConfiguredManifest(manifest);
        } catch (final ManifestException e) {
            final AzureString title = AzureOperationBundle.title("function.create_manifest");
            final SimpleOperation op = new SimpleOperation(title, AzureOperation.Type.TASK);
            throw new AzureOperationException(op, e);
        }
        jar.createArchive();
        return outputFile;
    }
}
