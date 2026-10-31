package com.microsoft.azure.toolkit.intellij.common.streaminglog;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import static com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT;
import static com.intellij.execution.ui.ConsoleViewContentType.SYSTEM_OUTPUT;
import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class StreamingLogsConsoleView extends ConsoleViewImpl {
    private static final String SEPARATOR = System.getProperty("line.separator");
    private boolean disposed;
    private Disposable subscription;

    public StreamingLogsConsoleView(@NotNull Project project) {
        super(project, true);
        this.setUpdateFoldingsEnabled(false);
    }

    public void startStreamingLog(Flux<String> logStreaming) {
        if (!isActive()) {
            printlnToConsole(message("app.logStreaming.hint.connect"), SYSTEM_OUTPUT);
            subscription = logStreaming.subscribeOn(Schedulers.boundedElastic())
                    .doAfterTerminate(() -> printlnToConsole(message("appService.logStreaming.hint.disconnected"), SYSTEM_OUTPUT))
                    .subscribe((log) -> printlnToConsole(log, NORMAL_OUTPUT));
        }
    }

    public void closeStreamingLog() {
        if (isActive()) {
            subscription.dispose();
            printlnToConsole(message("appService.logStreaming.hint.disconnected"), SYSTEM_OUTPUT);
        }
    }

    public boolean isActive() {
        return subscription != null && !subscription.isDisposed();
    }

    public boolean isConsoleDisposed() {
        return this.disposed;
    }

    private void printlnToConsole(String message, ConsoleViewContentType consoleViewContentType) {
        this.print(message + SEPARATOR, consoleViewContentType);
    }

    @Override
    public void dispose() {
        super.dispose();
        this.disposed = true;
        closeStreamingLog();
    }
}
