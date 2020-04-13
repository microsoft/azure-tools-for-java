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

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.process.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.util.io.BaseOutputReader;
import com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.subjects.PublishSubject;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

import static com.intellij.execution.process.ProcessOutputTypes.STDOUT;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogSource.SparkDriverStdErr;

public class SparkBatchJobRunProcessHandler extends BaseProcessHandler<SparkBatchJobProcessAdapter>
        implements SparkBatchJobProcessCtrlLogOut, AnsiEscapeDecoder.ColoredTextAcceptor {
    private final AnsiEscapeDecoder myAnsiEscapeDecoder = new AnsiEscapeDecoder();

    public SparkBatchJobRunProcessHandler(@NotNull SparkBatchJobRemoteProcess process, String commandLine, @Nullable Charset charset) {
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
                () -> notifyProcessDetached()
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
                                                           SparkDriverStdErr);

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
    public PublishSubject<SparkBatchJobLogLine> getCtrlSubject() {
        return getProcess().getSparkJobProcess().getCtrlSubject();
    }

    @NotNull
    @Override
    public PublishSubject<SparkBatchJobSubmissionEvent> getEventSubject() {
        return getProcess().getSparkJobProcess().getEventSubject();
    }
}

