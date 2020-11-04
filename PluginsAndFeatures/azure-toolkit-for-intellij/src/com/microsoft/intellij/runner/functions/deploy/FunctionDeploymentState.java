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

package com.microsoft.intellij.runner.functions.deploy;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.runner.functions.library.function.CreateFunctionHandler;
import com.microsoft.intellij.runner.functions.library.function.DeployFunctionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class FunctionDeploymentState extends AzureRunProfileState<WebAppBase> {

    private static final String TARGET_FUNCTION_DOES_NOT_EXIST =
            "Target function does not exist, please select a valid function in function deployment run configuration.";

    private FunctionDeployConfiguration functionDeployConfiguration;
    private final FunctionDeployModel deployModel;
    private File stagingFolder;

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
        updateTelemetryMap(telemetryMap);
        // Update run time information by function app
        final FunctionApp functionApp;
        if (deployModel.isNewResource()) {
            functionApp = createFunctionApp(processHandler);
            functionDeployConfiguration.setFunctionId(functionApp.id());
        } else {
            functionApp = AzureFunctionMvpModel.getInstance()
                                               .getFunctionById(functionDeployConfiguration.getSubscriptionId(), functionDeployConfiguration.getFunctionId());
        }
        if (functionApp == null) {
            throw new AzureExecutionException(TARGET_FUNCTION_DOES_NOT_EXIST);
        }
        final AppServicePlan appServicePlan = AppServiceUtils.getAppServicePlanByAppService(functionApp);
        functionDeployConfiguration.setOs(appServicePlan.operatingSystem().name());
        functionDeployConfiguration.setPricingTier(appServicePlan.pricingTier().toSkuDescription().size());
        // Deploy function to Azure
        stagingFolder = FunctionUtils.getTempStagingFolder();
        deployModel.setDeploymentStagingDirectoryPath(stagingFolder.getPath());
        prepareStagingFolder(stagingFolder, processHandler);
        final DeployFunctionHandler deployFunctionHandler = new DeployFunctionHandler(deployModel, message -> {
            if (processHandler.isProcessRunning()) {
                processHandler.setText(message);
            }
        });
        return deployFunctionHandler.execute();
    }

    private FunctionApp createFunctionApp(RunProcessHandler processHandler) throws IOException, AzureExecutionException {
        FunctionApp functionApp =
                AzureFunctionMvpModel.getInstance().getFunctionByName(functionDeployConfiguration.getSubscriptionId(),
                                                                      functionDeployConfiguration.getResourceGroup(),
                                                                      functionDeployConfiguration.getAppName());
        if (functionApp != null) {
            functionDeployConfiguration.setNewResource(false);
            return functionApp;
        }
        processHandler.setText(String.format("Creating function app %s ...", functionDeployConfiguration.getAppName()));
        final CreateFunctionHandler createFunctionHandler = new CreateFunctionHandler(functionDeployConfiguration.getModel());
        functionApp = createFunctionHandler.execute();
        processHandler.setText(String.format("Function app %s created.", functionDeployConfiguration.getAppName()));
        return functionApp;
    }

    private void prepareStagingFolder(File stagingFolder, RunProcessHandler processHandler)
            throws AzureExecutionException {
        ReadAction.run(() -> {
            final Path hostJsonPath = FunctionUtils.getDefaultHostJson(project);
            final PsiMethod[] methods = FunctionUtils.findFunctionsByAnnotation(functionDeployConfiguration.getModule());
            try {
                FunctionUtils.prepareStagingFolder(stagingFolder.toPath(), hostJsonPath, functionDeployConfiguration.getModule(), methods);
            } catch (AzureExecutionException | IOException e) {
                throw new AzureExecutionException("Failed to prepare staging folder");
            }
        });
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.DEPLOY_FUNCTION_APP);
    }

    @Override
    protected void onSuccess(WebAppBase result, @NotNull RunProcessHandler processHandler) {
        processHandler.setText("Deploy succeed");
        processHandler.notifyComplete();
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
        if (functionDeployConfiguration.isNewResource() && AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, result));
        }
    }

    @Override
    protected void onFail(String errMsg, @NotNull RunProcessHandler processHandler) {
        processHandler.println(errMsg, ProcessOutputTypes.STDERR);
        processHandler.notifyComplete();
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    @Override
    protected String getDeployTarget() {
        return "FUNCTION";
    }

    @Override
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
        telemetryMap.putAll(functionDeployConfiguration.getModel().getTelemetryProperties(telemetryMap));
    }
}
