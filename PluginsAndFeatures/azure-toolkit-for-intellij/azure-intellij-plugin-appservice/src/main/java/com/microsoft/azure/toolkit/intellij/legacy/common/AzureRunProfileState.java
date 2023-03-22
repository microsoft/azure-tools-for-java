/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.common.runconfig.RunConfigurationUtils;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

public abstract class AzureRunProfileState<T> implements RunProfileState {
    protected final Project project;
    @Getter
    protected RunProcessHandlerMessenger processHandlerMessenger;
    public AzureRunProfileState(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner programRunner) {
        final RunProcessHandler processHandler = new RunProcessHandler();
        processHandler.addDefaultListener();
        this.processHandlerMessenger = new RunProcessHandlerMessenger(processHandler);
        final ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(this.project).getConsole();
        processHandler.startNotify();
        consoleView.attachToProcess(processHandler);

        final Operation operation = createOperation();
        final Disposable subscribe = Mono.fromCallable(() -> {
            operation.start();
            return this.executeSteps(processHandler, operation);
        }).subscribeOn(Schedulers.boundedElastic()).subscribe(
            (res) -> {
                processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, true);
                this.sendTelemetry(operation, null);
                this.onSuccess(res, processHandler);
            },
            (err) -> {
                processHandlerMessenger.error(err, null, (Object[]) getErrorActions(executor, programRunner, err));
                this.sendTelemetry(operation, err);
                this.onFail(err, processHandler);
            });

        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                subscribe.dispose();
            }
        });

        return new DefaultExecutionResult(consoleView, processHandler);
    }

    protected void setText(RunProcessHandler runProcessHandler, String text) {
        if (runProcessHandler.isProcessRunning()) {
            runProcessHandler.setText(text);
        }
    }

    private void sendTelemetry(Operation operation, Throwable exception) {
        operation.trackProperties(getTelemetryMap());
        if (exception != null) {
            EventUtil.logError(operation, ErrorType.userError, new Exception(exception.getMessage(), exception), null, null);
        }
        operation.complete();
    }

    protected Action<Void>[] getErrorActions(final Executor executor, @NotNull ProgramRunner programRunner, final Throwable throwable) {
        return new Action[]{Action.retryFromFailure(() -> this.execute(executor, programRunner))};
    }

    protected abstract T executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception;

    @NotNull
    protected abstract Operation createOperation();

    protected abstract Map<String, String> getTelemetryMap();

    protected abstract void onSuccess(T result, @NotNull RunProcessHandler processHandler);

    protected void onFail(@NotNull Throwable error, @NotNull RunProcessHandler processHandler) {
        processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_RESULT, false);
        processHandler.putUserData(RunConfigurationUtils.AZURE_RUN_STATE_EXCEPTION, error);
        processHandler.notifyProcessTerminated(-1);
    }
}
