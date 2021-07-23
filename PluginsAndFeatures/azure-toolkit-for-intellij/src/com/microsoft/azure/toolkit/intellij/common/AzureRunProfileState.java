/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.RunProcessHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

        final Operation operation = createOperation();
        final Disposable subscribe = Mono.fromCallable(() -> {
            try {
                operation.start();
                return this.executeSteps(processHandler, operation);
            } finally {
                // Once the operation done, whether success or not, `setText` should not throw new exception
                processHandler.setProcessTerminatedHandler(RunProcessHandler.DO_NOTHING);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe(
            (res) -> {
                this.sendTelemetry(operation, null);
                this.onSuccess(res, processHandler);
            },
            (err) -> {
                err.printStackTrace();
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

    protected abstract T executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception;

    @NotNull
    protected abstract Operation createOperation();

    protected abstract Map<String, String> getTelemetryMap();

    protected abstract void onSuccess(T result, @NotNull RunProcessHandler processHandler);

    protected void onFail(@NotNull Throwable error, @NotNull RunProcessHandler processHandler) {
        final String errorMessage = (error instanceof AzureToolkitRuntimeException || error instanceof AzureToolkitException) ?
            String.format("Failed to %s", error.getMessage()) : error.getMessage();
        processHandler.println(errorMessage, ProcessOutputTypes.STDERR);
        processHandler.notifyComplete();
        AzureMessager.getMessager().error(error);
    }
}
