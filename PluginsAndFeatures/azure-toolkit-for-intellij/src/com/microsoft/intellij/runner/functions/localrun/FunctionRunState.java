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
 *
 */

package com.microsoft.intellij.runner.functions.localrun;

import com.intellij.execution.Executor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.intellij.runner.AzureRunProfileState;
import com.microsoft.intellij.runner.RunProcessHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class FunctionRunState extends AzureRunProfileState<FunctionApp> {

    private Process process;
    private Executor executor;
    private FunctionRunConfiguration functionRunConfiguration;

    public FunctionRunState(@NotNull Project project, FunctionRunConfiguration functionRunConfiguration, Executor executor) {
        super(project);
        this.executor = executor;
        this.functionRunConfiguration = functionRunConfiguration;
    }

    @Override
    protected String getDeployTarget() {
        return "null";
    }

    @Override
    protected FunctionApp executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Map<String, String> telemetryMap) throws Exception {
        // Todo: implement run function locally
        return null;
    }

    @Override
    protected void onSuccess(FunctionApp result, RunProcessHandler processHandler) {

    }

    @Override
    protected void onFail(String errMsg, RunProcessHandler processHandler) {

    }
}
