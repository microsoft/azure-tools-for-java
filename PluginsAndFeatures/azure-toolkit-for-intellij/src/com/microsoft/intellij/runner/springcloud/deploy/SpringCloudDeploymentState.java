/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.springcloud.deploy;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.UserSourceInfo;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.springcloud.SpringCloudModel;
import com.microsoft.intellij.runner.springcloud.library.SpringCloudUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SpringCloudDeploymentState extends AzureRunProfileState<AppResourceInner> {

    private SpringCloudDeployConfiguration springCloudDeployConfiguration;
    private final SpringCloudModel springCloudModel;

    /**
     * Place to execute the Web App deployment task.
     */
    public SpringCloudDeploymentState(Project project, SpringCloudDeployConfiguration springCloudDeployConfiguration) {
        super(project);
//        this.springCloudDeployConfiguration = springCloudDeployConfiguration;
//        this.springCloudModel = springCloudDeployConfiguration.getModel();
        springCloudModel = new SpringCloudModel();
        springCloudModel.setAppName("hanli-test-2");
        springCloudModel.setClusterName("hanli-spring-cloud");
        springCloudModel.setResourceGroup("hanli-spring-cloud");
        springCloudModel.setCpu(1);
        springCloudModel.setMemoryInGB(1);
        springCloudModel.setInstanceCount(1);
        springCloudModel.setSubscriptionId("685ba005-af8d-4b04-8f16-a7bf38b2eb5a");
        springCloudModel.setPublic(true);
        springCloudModel.setEnablePersistentStorage(true);
        springCloudModel.setRuntimeVersion(RuntimeVersion.JAVA_8);
        springCloudModel.setArtifactPath("C:\\Demo\\PiggyMetrics\\gateway\\target\\gateway.jar");
    }

    @Nullable
    @Override
    public AppResourceInner executeSteps(@NotNull RunProcessHandler processHandler
            , @NotNull Map<String, String> telemetryMap) throws Exception {
        // get or create spring cloud app
        processHandler.setText("Creating/Updating spring cloud app...");
        final AppResourceInner appResourceInner = SpringCloudUtils.createOrUpdateSpringCloudApp(springCloudModel);
        processHandler.setText("Create/Update spring cloud app succeed.");
        // upload artifact to correspond storage
        processHandler.setText("Uploading artifact to storage...");
        final UserSourceInfo userSourceInfo = SpringCloudUtils.deployArtifact(springCloudModel);
        processHandler.setText("Upload artifact succeed.");
        // get or create active deployment
        processHandler.setText("Creating/Updating deployment...");
        final DeploymentResourceInner deploymentResourceInner = SpringCloudUtils.createOrUpdateDeployment(springCloudModel, userSourceInfo);
        processHandler.setText("Create/Update deployment succeed.");
        // update spring cloud properties (enable public access)
        processHandler.setText("Activating deployment...");
        return SpringCloudUtils.activeDeployment(appResourceInner, deploymentResourceInner, springCloudModel);
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.SPRING_CLOUD, TelemetryConstants.CREATE_SPRING_CLOUD_APP);
    }

    @Override
    protected void onSuccess(AppResourceInner result, @NotNull RunProcessHandler processHandler) {
        processHandler.setText("Deploy succeed");
        processHandler.notifyComplete();
    }

    @Override
    protected void onFail(String errMsg, @NotNull RunProcessHandler processHandler) {
        processHandler.println(errMsg, ProcessOutputTypes.STDERR);
        processHandler.notifyComplete();
    }

    @Override
    protected String getDeployTarget() {
        return "SPRING_CLOUD";
    }

    @Override
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
    }
}
