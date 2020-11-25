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

package com.microsoft.intellij.runner;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.handler.IntelliJAzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;

public abstract class AzureRunProfileState<T> implements RunProfileState {
    protected final Project project;

    public AzureRunProfileState(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) throws ExecutionException {
        final RunProcessHandler processHandler = new RunProcessHandler();
        processHandler.addDefaultListener();
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);
        Map<String, String> telemetryMap = new HashMap<>();
        final Operation operation = createOperation();
        Observable.fromCallable(
            () -> {
                try {
                    if (operation != null) {
                        operation.start();
                        EventUtil.logEvent(EventType.info, operation, telemetryMap);
                    }
                    return this.executeSteps(processHandler, telemetryMap);
                } finally {
                    // Once the operation done, whether success or not, `setText` should not throw new exception
                    processHandler.setProcessTerminatedHandler(RunProcessHandler.DO_NOTHING);
                }
            }).subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io()).subscribe(
                (res) -> {
                    if (operation != null) {
                        operation.complete();
                    }
                    this.sendTelemetry(telemetryMap, true, null);
                    this.onSuccess(res, processHandler);
                },
                (err) -> {
                    err.printStackTrace();
                    if (operation != null) {
                        EventUtil.logError(operation, ErrorType.userError, new Exception(err.getMessage(), err),
                                           telemetryMap, null);
                        operation.complete();
                    }
                    this.onFail(err, processHandler);
                    this.sendTelemetry(telemetryMap, false, err.getMessage());
                });
        return new DefaultExecutionResult(consoleView, processHandler);
    }

    protected Operation createOperation() {
        return null;
    }

    protected void onFail(@NotNull Throwable error, @NotNull RunProcessHandler processHandler) {
        onFail(error.getMessage(), processHandler);
        IntelliJAzureExceptionHandler.getInstance().handleException(project, new AzureToolkitRuntimeException("execute run configuration", error), true);
    }

    protected void onFail(@NotNull String errMsg, @NotNull RunProcessHandler processHandler) {

    }

    protected void setText(RunProcessHandler runProcessHandler, String text) {
        if (runProcessHandler.isProcessRunning()) {
            runProcessHandler.setText(text);
        }
    }

    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
    }

    private void sendTelemetry(@NotNull Map<String, String> telemetryMap, boolean success, @Nullable String errorMsg) {
        updateTelemetryMap(telemetryMap);
        telemetryMap.put("Success", String.valueOf(success));
        if (!success) {
            telemetryMap.put("ErrorMsg", errorMsg);
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Action
                , getDeployTarget(), "Deploy", telemetryMap);
    }

    protected abstract String getDeployTarget();

    protected abstract T executeSteps(@NotNull RunProcessHandler processHandler
            , @NotNull Map<String, String> telemetryMap) throws Exception;

    protected abstract void onSuccess(T result, @NotNull RunProcessHandler processHandler);
}
