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

package com.microsoft.azuretools.core.utils;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

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

import java.io.File;

public class MavenUtils {

    private static final String CANNOT_FIND_POM = "Cannot find pom file.";
    private static final String CANNOT_GET_REG = "Cannot get Maven project registry.";
    private static final String CANNOT_CREATE_FACADE = "Cannot create Maven project facade.";
    private static final String CANNOT_GET_MAVEN_PROJ = "Cannot get Maven project.";

    public static boolean isMavenProject(@NotNull IProject project) throws CoreException {
        if (project != null && project.exists() && project.isAccessible()
                && (project.hasNature(IMavenConstants.NATURE_ID)
                        || project.getFile(IMavenConstants.POM_FILE_NAME).exists())) {
            return true;
        }
        return false;
    }

    @NotNull
    public static String getPackaging(@NotNull IProject project) throws Exception {
        IFile pom = getPomFile(project);
        final MavenProject mavenProject = getMavenProject(pom);
        return mavenProject.getPackaging();
    }

    @NotNull
    public static String getFinalName(@NotNull IProject project) throws Exception {
        IFile pom = getPomFile(project);
        final MavenProject mavenProject = getMavenProject(pom);
        final Build build = mavenProject.getBuild();
        if (build != null) {
            return build.getFinalName();
        }
        return "";
    }

    @NotNull
    public static String getTargetPath(@NotNull IProject project) throws Exception {
        IFile pom = getPomFile(project);
        final MavenProject mavenProject = getMavenProject(pom);
        final Build build = mavenProject.getBuild();
        if (build != null) {
            return build.getDirectory() + File.separator + build.getFinalName() + "." + mavenProject.getPackaging();
        }
        return "";
    }

    @NotNull
    public static IFile getPomFile(@NotNull IProject project) throws Exception {
        final IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
        if (pomResource != null && pomResource.exists()) {
            return pomResource;
        } else {
            throw new Exception(CANNOT_FIND_POM);
        }
    }

    @NotNull
    private static MavenProject getMavenProject(@NotNull IFile pom) throws Exception {
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
