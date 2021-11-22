/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;

import javax.annotation.Nonnull;
import java.io.File;

public class MavenUtils {

    private static final String CANNOT_FIND_POM = "Cannot find pom file.";
    private static final String CANNOT_GET_REG = "Cannot get Maven project registry.";
    private static final String CANNOT_CREATE_FACADE = "Cannot create Maven project facade.";
    private static final String CANNOT_GET_MAVEN_PROJ = "Cannot get Maven project.";

    public static boolean isMavenProject(@Nonnull IProject project) {
        try {
            if (project != null && project.exists() && project.isAccessible()
                    && (project.hasNature(IMavenConstants.NATURE_ID)
                            || project.getFile(IMavenConstants.POM_FILE_NAME).exists())) {
                return true;
            }
        } catch (CoreException e) {
            throw new AzureToolkitRuntimeException("Cannot detect maven project", e);
        }
        return false;
    }

    @Nonnull
    public static String getPackaging(@Nonnull IProject project) throws Exception {
        IFile pom = getPomFile(project);
        final MavenProject mavenProject = toMavenProject(pom);
        return mavenProject.getPackaging();
    }

    @Nonnull
    public static String getFinalName(@Nonnull IProject project) throws Exception {
        IFile pom = getPomFile(project);
        final MavenProject mavenProject = toMavenProject(pom);
        final Build build = mavenProject.getBuild();
        if (build != null) {
            return build.getFinalName();
        }
        return "";
    }

    @Nonnull
    public static String getTargetPath(@Nonnull IProject project) throws Exception {
        IFile pom = getPomFile(project);
        final MavenProject mavenProject = toMavenProject(pom);
        final Build build = mavenProject.getBuild();
        if (build != null) {
            return build.getDirectory() + File.separator + build.getFinalName() + "." + mavenProject.getPackaging();
        }
        return "";
    }

    @Nonnull
    public static IFile getPomFile(@Nonnull IProject project) {
        final IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
        if (pomResource != null && pomResource.exists()) {
            return pomResource;
        } else {
            throw new AzureToolkitRuntimeException(CANNOT_FIND_POM);
        }
    }

    @Nonnull
    public static MavenProject toMavenProject(@Nonnull IFile pom) throws Exception {
        final IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        final NullProgressMonitor monitor = new NullProgressMonitor();
        if (projectManager == null) {
            throw new Exception(CANNOT_GET_REG);
        }
        final IMavenProjectFacade mavenFacade = projectManager.create(pom, true, monitor);
        if (mavenFacade == null) {
            throw new Exception(CANNOT_CREATE_FACADE);
        }
        final MavenProject mavenProject = mavenFacade.getMavenProject(monitor);
        if (mavenProject == null) {
            throw new Exception(CANNOT_GET_MAVEN_PROJ);
        }
        final ResolverConfiguration configuration = mavenFacade.getResolverConfiguration();
        configuration.setResolveWorkspaceProjects(true);
        return mavenProject;
    }
}
