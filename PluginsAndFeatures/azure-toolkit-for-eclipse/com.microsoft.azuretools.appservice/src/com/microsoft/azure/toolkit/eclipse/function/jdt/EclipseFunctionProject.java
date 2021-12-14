/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.jdt;

import com.microsoft.azure.toolkit.eclipse.function.utils.JarUtils;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;
import com.microsoft.azuretools.core.utils.MavenUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EclipseFunctionProject extends FunctionProject {
    public static final String FUNCTION_JAVA_LIBRARY_ARTIFACT_ID = "azure-functions-java-library";
    private final IJavaProject eclipseProject;

    public EclipseFunctionProject(IJavaProject project, File stagingFolder) {
        this.eclipseProject = project;
        setName(project.getElementName());
        setBaseDirectory(project.getProject().getLocation().toFile());
        setStagingFolder(stagingFolder);
    }

    public void buildJar() throws Exception {
        IFile pom = MavenUtils.getPomFile(eclipseProject.getProject());
        final MavenProject mavenProject = MavenUtils.toMavenProject(pom);

        final List<File> jarFiles = new ArrayList<>();
        mavenProject.getArtifacts().forEach(t -> {
            if (!StringUtils.equals(t.getScope(), "test") && !StringUtils.contains(t.getArtifactId(), FUNCTION_JAVA_LIBRARY_ARTIFACT_ID)) {
                jarFiles.add(t.getFile());
            }
        });
        setClassesOutputDirectory(new File(mavenProject.getBuild().getOutputDirectory()));
        setDependencies(jarFiles);
        final File jarFile = JarUtils.buildJarFileToTempFile(this);
        this.setArtifactFile(jarFile);
    }

    public IJavaProject getEclipseProject() {
        return eclipseProject;
    }
}
