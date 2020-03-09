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

package com.microsoft.intellij.runner.functions.deploy;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.functions.IntelliJFunctionRuntimeConfiguration;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.runner.functions.library.function.DeployFunctionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class FunctionDeploymentState extends AzureRunProfileState<WebAppBase> {

    private FunctionDeployConfiguration functionDeployConfiguration;
    private final FunctionDeployModel deployModel;

    /**
     * Place to execute the Web App deployment task.
     */
    public FunctionDeploymentState(Project project, FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.functionDeployConfiguration = functionDeployConfiguration;
        this.deployModel = functionDeployConfiguration.getModel();
    }

    @Nullable
    @Override
    public WebAppBase executeSteps(@NotNull RunProcessHandler processHandler
            , @NotNull Map<String, String> telemetryMap) throws Exception {
        // Update run time information by function app
        final FunctionApp functionApp = AzureFunctionMvpModel.getInstance()
                .getFunctionById(functionDeployConfiguration.getSubscriptionId(), functionDeployConfiguration.getFunctionId());
        final AppServicePlan appServicePlan = AppServiceUtils.getAppServicePlanByAppService(functionApp);
        final IntelliJFunctionRuntimeConfiguration runtimeConfiguration = new IntelliJFunctionRuntimeConfiguration();
        runtimeConfiguration.setOs(appServicePlan.operatingSystem() == OperatingSystem.WINDOWS ? "windows" : "linux");
        functionDeployConfiguration.setRuntime(runtimeConfiguration);
        functionDeployConfiguration.setPricingTier(appServicePlan.pricingTier().toSkuDescription().size());
        // Deploy function to Azure
        final File stagingFolder = new File(functionDeployConfiguration.getDeploymentStagingDirectory());
        prepareStagingFolder(stagingFolder, processHandler);
        final DeployFunctionHandler deployFunctionHandler = new DeployFunctionHandler(deployModel, message -> {
            if (processHandler.isProcessRunning()) {
                processHandler.setText(message);
            }
        });
        return deployFunctionHandler.execute();
    }

    private void prepareStagingFolder(File stagingFolder, RunProcessHandler processHandler) {
        ReadAction.run(() -> {
            final Path hostJsonPath = FunctionUtils.getDefaultHostJson(project);
            final PsiMethod[] methods = FunctionUtils.findFunctionsByAnnotation(functionDeployConfiguration.getModule());
            try {
                FunctionUtils.prepareStagingFolder(stagingFolder.toPath(), hostJsonPath, functionDeployConfiguration.getModule(), methods);
            } catch (AzureExecutionException | IOException e) {
                processHandler.println(String.format("Failed to prepare staging folder, %s", e.getMessage()), ProcessOutputTypes.STDERR);
            }
        });
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.DEPLOY_WEBAPP);
    }

    @Override
    protected void onSuccess(WebAppBase result, @NotNull RunProcessHandler processHandler) {
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
        return "FUNCTION";
    }

    @Override
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
    }
}
