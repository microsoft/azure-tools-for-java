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

package com.microsoft.intellij.runner.springcloud.deploy;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentInstance;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResourceStatus;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.UserSourceInfo;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.maven.SpringCloudDependencyManager;
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.util.MavenUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.SpringCloudUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.DefaultAzureResourceTracker;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class SpringCloudDeploymentState extends AzureRunProfileState<AppResourceInner> {

    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String[] SPRING_ARTIFACTS = {
        "spring-boot-starter-actuator",
        "spring-cloud-config-client",
        "spring-cloud-starter-netflix-eureka-client",
        "spring-cloud-starter-zipkin",
        "spring-cloud-starter-sleuth"
    };
    private static final List<DeploymentResourceStatus> DEPLOYMENT_PROCESSING_STATUS =
            Arrays.asList(DeploymentResourceStatus.COMPILING,
                          DeploymentResourceStatus.ALLOCATING,
                          DeploymentResourceStatus.UPGRADING);
    private static final String JAR = ".jar";
    private static final String MAIN_CLASS = "Main-Class";
    private static final String SPRING_BOOT_LIB = "Spring-Boot-Lib";
    private static final String SPRING_BOOT_AUTOCONFIGURE = "spring-boot-autoconfigure";
    private static final String NOT_SPRING_BOOT_PROJECT = "Project %s is not a spring-boot project.";
    private static final String FAILED_TO_LOAD_DEPENDENCIES = "Failed to load spring cloud app dependencies";
    private static final String DEPENDENCIES_IS_NOT_UPDATED = "Azure Spring Cloud dependencies is not updated.";
    private static final String MAIN_CLASS_NOT_FOUND =
            "Main class was not found in current artifact, which is required for spring cloud app.";
    private static final String DEPENDENCY_WARNING = "Dependency %s is missing or out of date, you "
            + "may update the dependencies with `Add Azure Spring Cloud dependency`.";

    private final SpringCloudDeployConfiguration springCloudDeployConfiguration;

    /**
     * Place to execute the Web App deployment task.
     */
    public SpringCloudDeploymentState(Project project, SpringCloudDeployConfiguration springCloudDeployConfiguration) {
        super(project);
        this.springCloudDeployConfiguration = springCloudDeployConfiguration;
    }

    @Nullable
    @Override
    public AppResourceInner executeSteps(@NotNull RunProcessHandler processHandler
            , @NotNull Map<String, String> telemetryMap) throws Exception {
        // prepare the jar to be deployed
        updateTelemetryMap(telemetryMap);
        if (StringUtils.isEmpty(springCloudDeployConfiguration.getProjectName())) {
            throw new AzureExecutionException("You must specify a maven project.");
        }
        List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
        MavenProject targetProject = mavenProjects
                .stream()
                .filter(t -> StringUtils.equals(t.getName(), springCloudDeployConfiguration.getProjectName()))
                .findFirst()
                .orElse(null);
        if (targetProject == null) {
            throw new AzureExecutionException(String.format("Project '%s' cannot be found.",
                                                            springCloudDeployConfiguration.getProjectName()));
        }
        String finalJarName = MavenUtils.getSpringBootFinalJarPath(project, targetProject);
        if (!Files.exists(Paths.get(finalJarName))) {
            throw new AzureExecutionException(String.format("File '%s' cannot be found.",
                                                            finalJarName));
        }
        validateSpringBootApp(finalJarName);
        // get or create spring cloud app
        setText(processHandler, "Creating/Updating spring cloud app...");
        final AppResourceInner appResourceInner = SpringCloudUtils.createOrUpdateSpringCloudApp(springCloudDeployConfiguration);
        DefaultAzureResourceTracker.getInstance().handleDataChanges(appResourceInner.id(), appResourceInner, null);
        DefaultAzureResourceTracker.getInstance().handleDataChanges(springCloudDeployConfiguration.getClusterId(), appResourceInner
                , null);
        setText(processHandler, "Create/Update spring cloud app succeed.");
        // upload artifact to correspond storage
        setText(processHandler, "Uploading artifact to storage...");
        final UserSourceInfo userSourceInfo = SpringCloudUtils.deployArtifact(springCloudDeployConfiguration, finalJarName);
        setText(processHandler, "Upload artifact succeed.");
        // get or create active deployment
        setText(processHandler, "Creating/Updating deployment...");
        final DeploymentResourceInner deploymentResourceInner = SpringCloudUtils.createOrUpdateDeployment(
                springCloudDeployConfiguration,
                userSourceInfo);
        setText(processHandler, "Create/Update deployment succeed.");
        DefaultAzureResourceTracker.getInstance().handleDataChanges(appResourceInner.id(), appResourceInner, deploymentResourceInner);
        DefaultAzureResourceTracker.getInstance().handleDataChanges(springCloudDeployConfiguration.getClusterId(), appResourceInner
                , deploymentResourceInner);
        // update spring cloud properties (enable public access)
        setText(processHandler, "Activating deployment...");
        AppResourceInner newApps = SpringCloudUtils.activeDeployment(appResourceInner,
                                                                     deploymentResourceInner,
                                                                     springCloudDeployConfiguration);
        DefaultAzureResourceTracker.getInstance().handleDataChanges(newApps.id(), newApps, deploymentResourceInner);
        DefaultAzureResourceTracker.getInstance().handleDataChanges(springCloudDeployConfiguration.getClusterId(), newApps, deploymentResourceInner);
        AzureSpringCloudMvpModel.startApp(newApps.id(), newApps.properties().activeDeploymentName()).await();
        // Waiting until instances start
        DeploymentResourceInner newDeploymentResourceInner = getDeploymentStatus(newApps.id(), processHandler);

        DefaultAzureResourceTracker.getInstance().handleDataChanges(newApps.id(), newApps, newDeploymentResourceInner);
        DefaultAzureResourceTracker.getInstance().handleDataChanges(springCloudDeployConfiguration.getClusterId(), newApps, newDeploymentResourceInner);

        if (newApps.properties().publicProperty()) {
            getUrl(newApps.id(), processHandler);
        }
        setText(processHandler, "Deployment done.");
        return newApps;
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.SPRING_CLOUD,
                                                TelemetryConstants.CREATE_SPRING_CLOUD_APP);
    }

    @Override
    protected void onSuccess(AppResourceInner result, @NotNull RunProcessHandler processHandler) {
        setText(processHandler, "Deploy succeed");
        processHandler.notifyComplete();
    }

    @Override
    protected void onFail(Throwable throwable, @NotNull RunProcessHandler processHandler) {
        DefaultLoader.getUIHelper().showException(throwable.getMessage(), throwable, "Deployed failed", false, true);
        try {
            processHandler.println(throwable.getMessage(), ProcessOutputTypes.STDERR);
        } catch (Exception ex) {
            // should not propagate error infinitely
        }
        processHandler.notifyComplete();
    }

    @Override
    protected String getDeployTarget() {
        return "SPRING_CLOUD";
    }

    @Override
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
        telemetryMap.putAll(springCloudDeployConfiguration.getModel().getTelemetryProperties());
    }

    private void validateSpringBootApp(String finalJar) throws AzureExecutionException, IOException {
        final JarFile jarFile = new JarFile(finalJar);
        final Attributes maniFestAttributes = jarFile.getManifest().getMainAttributes();
        final String mainClass = maniFestAttributes.getValue(MAIN_CLASS);
        final String library = maniFestAttributes.getValue(SPRING_BOOT_LIB);
        if (StringUtils.isEmpty(mainClass)) {
            throw new AzureExecutionException(MAIN_CLASS_NOT_FOUND);
        }
        if (StringUtils.isEmpty(library)) {
            throw new AzureExecutionException(FAILED_TO_LOAD_DEPENDENCIES);
        }
        final Map<String, String> dependencies = getSpringAppDependencies(jarFile.entries(), library);
        if (!dependencies.containsKey(SPRING_BOOT_AUTOCONFIGURE)) {
            throw new AzureExecutionException(NOT_SPRING_BOOT_PROJECT);
        }
        final String springVersion = dependencies.get(SPRING_BOOT_AUTOCONFIGURE);
        for (String artifact : SPRING_ARTIFACTS) {
            if (!SpringCloudDependencyManager.isCompatibleVersion(dependencies.get(artifact),
                                                                  springVersion)) {
                PluginUtil.showInfoNotificationProject(project, DEPENDENCIES_IS_NOT_UPDATED,
                                                       String.format(DEPENDENCY_WARNING, artifact));
                return;
            }
        }
    }

    private Map<String, String> getSpringAppDependencies(Enumeration<JarEntry> jarEntryEnumeration,
                                                         String libraryPath) {
        final List<JarEntry> jarEntries = Collections.list(jarEntryEnumeration);
        return jarEntries.stream()
                         .filter(jarEntry -> StringUtils.startsWith(jarEntry.getName(), libraryPath)
                                 && StringUtils.endsWith(jarEntry.getName(), JAR))
                         .map(jarEntry -> {
                             String fileName = StringUtils.strip(jarEntry.getName(), libraryPath);
                             fileName = StringUtils.stripEnd(fileName, JAR);
                             final int i = StringUtils.lastIndexOf(fileName, "-");
                             return (i > 0 && i < fileName.length() - 1) ?
                                    new String[]{
                                            StringUtils.substring(fileName, 0, i),
                                            StringUtils.substring(fileName, i + 1)
                                    } :
                                    new String[]{fileName, ""};
                         })
                         .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
    }

    private void getUrl(String appId, RunProcessHandler processHandler) {
        try {
            String url = getResourceWithTimeout(() -> AzureSpringCloudMvpModel.getAppById(appId).properties().url(),
                                                StringUtils::isNotEmpty, GET_URL_TIMEOUT, TimeUnit.SECONDS);
            setText(processHandler, "URL: " + url);
        } catch (Exception e) {
            setText(processHandler, "Failed to get the public url, you may get the data in portal later.");
        }
    }

    private DeploymentResourceInner getDeploymentStatus(String appId, RunProcessHandler processHandler) {
        try {
            DeploymentResourceInner deploymentResourceInner =
                    getResourceWithTimeout(() -> AzureSpringCloudMvpModel.getActiveDeploymentForApp(appId),
                                           this::isDeploymentDone, GET_STATUS_TIMEOUT, TimeUnit.SECONDS);
            setText(processHandler,
                    "Deployment done with status " + deploymentResourceInner.properties().status().toString());
            return deploymentResourceInner;
        } catch (Exception e) {
            setText(processHandler, "Failed to get the deployment status, you may get the status in portal later.");
            return null;
        }
    }

    @FunctionalInterface
    private interface SupplierWithIOException<T> {
        T get() throws IOException;
    }

    private static <T> T getResourceWithTimeout(SupplierWithIOException<T> consumer, Predicate<T> predicate,
                                         int timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<T> future = executor.submit(() -> {
            try {
                T result = null;
                do {
                    result = consumer.get();
                } while (!predicate.test(result));
                return result;
            } catch (IOException e) {
                return null;
            }

        });
        return future.get(timeout, timeUnit);
    }

    // todo: move this logic copied from maven plugin to tools-common
    private boolean isDeploymentDone(DeploymentResourceInner deploymentResource) {
        final DeploymentResourceStatus deploymentResourceStatus = deploymentResource.properties().status();
        if (DEPLOYMENT_PROCESSING_STATUS.contains(deploymentResourceStatus)) {
            return false;
        }
        final String finalDiscoverStatus = BooleanUtils.isTrue(deploymentResource.properties().active()) ?
                                           "UP" : "OUT_OF_SERVICE";
        final List<DeploymentInstance> instanceList = deploymentResource.properties().instances();
        final boolean isInstanceDeployed = !instanceList.stream()
                .anyMatch(instance -> StringUtils.equalsIgnoreCase(instance.status(), "waiting") ||
                        StringUtils.equalsIgnoreCase(instance.status(), "pending"));
        final boolean isInstanceDiscovered =
                instanceList.stream()
                            .allMatch(instance -> StringUtils.equalsIgnoreCase(
                                    instance.discoveryStatus(), finalDiscoverStatus));
        return isInstanceDeployed && isInstanceDiscovered;
    }
}
