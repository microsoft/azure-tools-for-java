package com.microsoft.azuretools.core.utils;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenUtils {

    private static final String MAVEN_PACKAGE = "package";

    public static boolean isMavenProject(@NotNull IProject project) throws CoreException {
        if (project != null && project.exists() && project.isAccessible()
                && (project.hasNature(IMavenConstants.NATURE_ID)
                        || project.getFile(IMavenConstants.POM_FILE_NAME).exists())) {
            return true;
        }
        return false;
    }

    @NotNull
    public static String getFinalName(@NotNull IProject project) {
        IFile pom = getPomFile(project);
        if (pom != null) {
            final MavenProject mavenProject = getMavenProject(pom);
            final Build build = mavenProject.getBuild();
            if (build != null) {
                return build.getFinalName();
            }
        }
        return "";
    }

    @NotNull
    public static String getTargetPath(@NotNull IProject project) {
        IFile pom = getPomFile(project);
        if (pom != null) {
            final MavenProject mavenProject = getMavenProject(pom);
            final Build build = mavenProject.getBuild();
            if (build != null) {
                return build.getDirectory() + File.separator + build.getFinalName() + "." + mavenProject.getPackaging();
            }
        }
        return "";
    }

    public static void executePackageGoal(@NotNull IProject project) throws CoreException {
        final IFile pom = getPomFile(project);
        if (pom != null) {
            final MavenProject mavenProject = getMavenProject(pom);
            if (mavenProject == null) {
                return;
            }
            final List<String> goals = Arrays.asList(MAVEN_PACKAGE);
            final IMaven maven = MavenPlugin.getMaven();
            if (maven == null) {
                return;
            }
            final NullProgressMonitor monitor = new NullProgressMonitor();
            final MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject, goals, true, monitor);
            if (plan == null) {
                return;
            }
            final List<MojoExecution> mojos = plan.getMojoExecutions();
            if (mojos == null) {
                return;
            }
            for(MojoExecution mojo : mojos) {
                maven.execute(mavenProject, mojo, monitor);
            }
        }
    }

    @Nullable
    private static IFile getPomFile(@NotNull IProject project) {
        final IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);
        if (pomResource != null && pomResource.exists()) {
            return pomResource;
        } else {
            return null;
        }
    }

    @Nullable
    private static MavenProject getMavenProject(@NotNull IFile pom) {
        final IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        if (projectManager == null) {
            return null;
        }
        final IMavenProjectFacade mavenFacade = projectManager.create(pom, true, new NullProgressMonitor());
        if (mavenFacade == null) {
            return null;
        }
        return mavenFacade.getMavenProject();
    }
}
