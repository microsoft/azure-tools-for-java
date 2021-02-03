/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.process.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.util.io.BaseOutputReader;
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.subjects.PublishSubject;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

import static com.intellij.execution.process.ProcessOutputTypes.STDOUT;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine.SPARK_DRIVER_STDERR;

public class SparkBatchJobRunProcessHandler extends BaseProcessHandler<SparkBatchJobProcessAdapter>
        implements SparkBatchJobProcessCtrlLogOut, AnsiEscapeDecoder.ColoredTextAcceptor {
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public SparkBatchJobRunProcessHandler(@NotNull SparkBatchJobRemoteProcess process,
                                          String commandLine,
                                          @Nullable Charset charset) {
        super(new SparkBatchJobProcessAdapter(process), commandLine, charset);

        super.addProcessListener(new ProcessAdapter() {
            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                if (willBeDestroyed) {
                    // Kill the Spark Batch Job
                    process.destroy();
                } else {
                    // Just detach
                    process.disconnect();
                }
                super.processWillTerminate(event, willBeDestroyed);
            }
        });

        process.getCtrlSubject().subscribe(
                ignored -> {},
                err -> notifyProcessTerminated(-1),
                this::notifyProcessDetached
        );
    }

    @Override
    public void startNotify() {
        notifyTextAvailable(myCommandLine + '\n', ProcessOutputTypes.SYSTEM);

        addProcessListener(new ProcessAdapter() {
            @Override
            public void startNotified(@NotNull final ProcessEvent event) {
                try {
                    final BaseOutputReader stdoutReader =
                            new SparkSimpleLogStreamReader(SparkBatchJobRunProcessHandler.this,
                                                                myProcess.getInputStream(),
                                                                STDOUT);
                    final BaseOutputReader stderrReader =
                            new SparkDriverLogStreamReader(SparkBatchJobRunProcessHandler.this,
                                                           myProcess.getErrorStream(),
                                                           SPARK_DRIVER_STDERR);

                    myWaitFor.setTerminationCallback(exitCode -> {
                        try {
                            try {
                                stderrReader.waitFor();
                                stdoutReader.waitFor();
                            }
                            catch (final InterruptedException ignore) { }
                        }
                        finally {
                            onOSProcessTerminated(exitCode);
                        }
                    });
                }
                finally {
                    removeProcessListener(this);
                }
            }
        });

        super.startNotify();
    }

    @Override
    public final void notifyTextAvailable(@NotNull String text, @NotNull Key outputType) {
        myAnsiEscapeDecoder.escapeText(text, outputType, this);
    }

    @Override
    public void coloredTextAvailable(@NotNull final String text, @NotNull final Key attributes) {
        super.notifyTextAvailable(text, attributes);
    }

    @NotNull
    @Override
    public Future<?> executeTask(@NotNull final Runnable task) {
        return ApplicationManager.getApplication().executeOnPooledThread(task);
    }

    @NotNull
    @Override
    public PublishSubject<SparkLogLine> getCtrlSubject() {
        return getProcess().getSparkJobProcess().getCtrlSubject();
    }

    @NotNull
    @Override
    public PublishSubject<SparkBatchJobSubmissionEvent> getEventSubject() {
        return getProcess().getSparkJobProcess().getEventSubject();
    }
}

