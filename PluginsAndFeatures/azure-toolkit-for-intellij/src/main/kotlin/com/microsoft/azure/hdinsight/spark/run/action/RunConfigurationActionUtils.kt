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

package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.execution.ExecutionException
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.impl.RunDialog
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.YES
import com.intellij.openapi.ui.Messages.showYesNoDialog
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.configuration.RunProfileStatePrepare
import com.microsoft.azuretools.telemetrywrapper.ErrorType.userError
import com.microsoft.azuretools.telemetrywrapper.EventUtil.logErrorClassNameOnly
import com.microsoft.azuretools.telemetrywrapper.EventUtil.logErrorClassNameOnlyWithComplete
import com.microsoft.intellij.rxjava.IdeaSchedulers
import com.microsoft.intellij.telemetry.TelemetryKeys
import com.microsoft.intellij.ui.util.UIUtils.assertInDispatchThread
import rx.Observable
import rx.Observable.empty
import rx.Observable.fromCallable

object RunConfigurationActionUtils: ILogger {
    fun runEnvironmentProfileWithCheckSettings(environment: ExecutionEnvironment,
                                               title: String = "Edit configuration") {
        val runner = ProgramRunner.getRunner(environment.executor.id, environment.runProfile) ?: return
        val setting = environment.runnerAndConfigurationSettings ?: return
        val asyncOperation = environment.getUserData(TelemetryKeys.OPERATION)

        if (setting.isEditBeforeRun && !RunDialog.editConfiguration(environment, title)) {
            logErrorClassNameOnlyWithComplete(asyncOperation, userError, ExecutionException("run config dialog closed"), null, null)
            return
        }

        val ideaSchedulers = IdeaSchedulers(environment.project)

        fromCallable { checkRunnerSettings(environment.runProfile as RunConfigurationBase<*>, runner) }
                .subscribeOn(ideaSchedulers.dispatchUIThread()) // Check Runner Settings in EDT
                .flatMap { checkAndPrepareRunProfileState(it, runner, ideaSchedulers) }
                .retryWhen { errOb -> errOb.observeOn(ideaSchedulers.dispatchUIThread() )
                        .doOnNext { logErrorClassNameOnly(asyncOperation, userError, ExecutionException(it), null, null) }
                        .takeWhile { configError -> // Check when can retry
                            showFixOrNotDialogForError(environment.project, configError.message ?: "Unknown").apply {
                                if (!this) {
                                    // User clicks `Cancel Submit` button
                                    throw ExecutionException(configError);
                                }
                            } && RunDialog.editConfiguration(environment, "Edit configuration").apply {
                                if (!this) {
                                    // User clicks `Cancel` button in Run Configuration Editor dialog
                                    throw ProcessCanceledException(configError)
                                }
                            }
                        }
                } .subscribe({ }, { err ->
                    if (err is ProcessCanceledException) {
                        // User cancelled edit configuration dialog
                        logErrorClassNameOnlyWithComplete(asyncOperation, userError, ExecutionException("run config dialog closed"), null, null)

                        return@subscribe
                    }

                    logErrorClassNameOnlyWithComplete(asyncOperation, userError, err, null, null)
                    ProgramRunnerUtil.handleExecutionError(environment.project, environment, err, setting.configuration)
                }, {
                    environment.assignNewExecutionId()

                    try {
                        runner.execute(environment)
                    } catch (err: ExecutionException) {
                        ProgramRunnerUtil.handleExecutionError(environment.project, environment, err, setting.configuration)
                    }
                })
    }

    private fun showFixOrNotDialogForError(project: Project, configError: String): Boolean = showYesNoDialog(
                            project,
                            "Configuration is incorrect: $configError. Do you want to edit it?",
                            "Change Configuration Settings",
                            "Edit",
                            "Cancel Submit",
                            Messages.getErrorIcon()) == YES

    private fun <T : Any?> checkRunnerSettings(runProfile: RunConfigurationBase<T>, runner: ProgramRunner<RunnerSettings>)
            : RunConfigurationBase<T> {
        assertInDispatchThread()

        runProfile.checkRunnerSettings(runner, null, null)

        return runProfile
    }

    private fun <T : Any?> checkAndPrepareRunProfileState(runProfile: RunConfigurationBase<T>,
                                               runner: ProgramRunner<RunnerSettings>,
                                               ideaSchedulers: IdeaSchedulers): Observable<out Any?> {
        if (runProfile !is RunProfileStatePrepare<*>) {
            return empty()
        }

        return runProfile.prepare(runner)
                .subscribeOn(ideaSchedulers.backgroundableTask("Checking Spark Job configuration settings"))
    }
}