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

package com.microsoft.intellij.helpers.springcloud;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpringCloudStreamingLogConsoleView extends ConsoleViewImpl {

    private AtomicBoolean enable;
    private ExecutorService executorService;

    private String resourceId;
    private InputStream logInputStream;

    public SpringCloudStreamingLogConsoleView(@NotNull Project project, String resourceId) {
        super(project, true);
        enable = new AtomicBoolean();
        this.resourceId = resourceId;
    }

    public boolean isEnable() {
        return enable.get();
    }

    public void startLog(InputStream inputStream) {
        enable.set(true);
        this.logInputStream = inputStream;

        this.print("Streaming Log Start.\n", ConsoleViewContentType.SYSTEM_OUTPUT);
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            final BufferedReader br = new BufferedReader(new InputStreamReader(logInputStream, StandardCharsets.UTF_8));
            while (true) {
                try {
                    final String log = br.readLine();
                    if (enable.get()) {
                        SpringCloudStreamingLogConsoleView.this.print(log + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
                    } else {
                        break;
                    }
                    Thread.sleep(50);
                } catch (IOException e) {
                    // swallow io exception
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    public void shutdown() {
        if (enable.get()) {
            enable.set(false);
            DefaultLoader.getIdeHelper().runInBackground(getProject(), "Closing Streaming Log", false, true, "Closing Streaming Log", () -> {
                if (logInputStream != null) {
                    try {
                        logInputStream.close();
                    } catch (IOException e) {
                        // swallow io exception when close
                    }
                }
                if (executorService != null) {
                    ThreadPoolUtils.stop(executorService, 100, TimeUnit.MICROSECONDS);
                }
                this.print("Streaming Log Stop.\n", ConsoleViewContentType.SYSTEM_OUTPUT);
            });
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        shutdown();
        SpringCloudStreamingLogManager.getInstance().removeConsoleView(resourceId);
    }
}
