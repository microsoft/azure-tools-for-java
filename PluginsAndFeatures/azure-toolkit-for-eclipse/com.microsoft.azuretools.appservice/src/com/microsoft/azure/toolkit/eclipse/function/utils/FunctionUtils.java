/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.utils;

import com.microsoft.azure.toolkit.eclipse.function.jdt.EclipseFunctionStagingContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.core.actions.MavenExecuteAction;
import com.microsoft.azuretools.core.utils.MavenUtils;
import lombok.Lombok;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.refactoring.CollectingSearchRequestor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FunctionUtils {
    private static final String AZURE_FUNCTIONS = "azure-functions-for-eclipse";

    public static List<IJavaProject> listJavaProjects() {
        return listProjects(project -> {
            try {
                return project.hasNature(JavaCore.NATURE_ID);
            } catch (CoreException e) {
                throw Lombok.sneakyThrow(e);
            }
        }).stream().map(JavaCore::create).collect(Collectors.toList());
    }

    public static List<IProject> listProjects(Predicate<IProject> predicate) {
        List<IProject> projectList = new ArrayList<>();
        try {
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = workspaceRoot.getProjects();
            for (IProject project : projects) {
                if (project.isOpen() && predicate.test(project)) {
                    projectList.add(project);
                }
            }
        } catch (Throwable e) {
            throw new AzureToolkitRuntimeException("Cannot list projects.", e);
        }
        return projectList;
    }

    public static void buildMavenProject(IJavaProject project) {
        // run `mvn compile` first to generate .class files which are required before generating function staging folder
        final MavenExecuteAction action = new MavenExecuteAction("compile");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            action.launch(MavenUtils.getPomFile(project.getProject()).getParent(), () -> {
                countDownLatch.countDown();
                return "ignore";
            });
            countDownLatch.await();
        } catch (CoreException | InterruptedException e) {
            throw new AzureToolkitRuntimeException("Cannot build maven project: " + project.getElementName(), e);
        }
    }

    public static Path getTempStagingFolder() {
        try {
            final Path path = Files.createTempDirectory(AZURE_FUNCTIONS);
            final File file = path.toFile();
            FileUtils.forceDeleteOnExit(file);
            return path;
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException("failed to get temp staging folder", e);
        }
    }

    @AzureOperation(
            name = "function.clean_staging_folder",
            params = {"stagingFolder.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static void cleanUpStagingFolder(File stagingFolder) {
        try {
            if (stagingFolder != null) {
                FileUtils.deleteDirectory(stagingFolder);
            }
        } catch (final IOException e) {
            // swallow exceptions while clean up
        }
    }

    @AzureOperation(
            name = "function.list_function_modules",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static IJavaProject[] listFunctionProjects() {
        return listJavaProjects().stream().filter(FunctionUtils::isFunctionProject).toArray(IJavaProject[]::new);
    }

    @AzureOperation(
            name = "common.validate_project",
            params = {"project.getName()"},
            type = AzureOperation.Type.TASK
    )
    public static boolean isFunctionProject(IJavaProject project) {
        if (project == null) {
            return false;
        }
        if (!MavenUtils.isMavenProject(project.getProject())) {
            return false;
        }
        try {
            CollectingSearchRequestor requestor = EclipseFunctionStagingContributor.searchFunctionNameAnnotation(project,
                    JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(project, false));
            return !requestor.getResults().isEmpty();
        } catch (CoreException e) {
            // ignore
        }
        return false;
    }

    public static String getFuncPath() throws IOException, InterruptedException {
        final AzureConfiguration config = Azure.az().config();
        if (StringUtils.isBlank(config.getFunctionCoreToolsPath())) {
            return FunctionCliResolver.resolveFunc();
        }
        return config.getFunctionCoreToolsPath();
    }
}
