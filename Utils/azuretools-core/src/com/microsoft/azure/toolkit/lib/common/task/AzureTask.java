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

package com.microsoft.azure.toolkit.lib.common.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Supplier;

@Getter
public class AzureTask<T> {
    private final Modality modality;
    private final Supplier<T> supplier;
    private final Object project;
    private final boolean cancellable;
    @Builder.Default
    private final boolean backgroundable = true;
    private final String title;

    @Setter
    private boolean runningBackground = false;

    public AzureTask(Runnable runnable) {
        this(runnable, Modality.DEFAULT);
    }

    public AzureTask(String title, Runnable runnable) {
        this(title, runnable, Modality.DEFAULT);
    }

    public AzureTask(Supplier<T> supplier) {
        this(supplier, Modality.DEFAULT);
    }

    public AzureTask(String title, Supplier<T> supplier) {
        this(null, title, false, supplier, Modality.DEFAULT);
    }

    public AzureTask(Runnable runnable, Modality modality) {
        this(null, null, false, runnable, modality);
    }

    public AzureTask(String title, Runnable runnable, Modality modality) {
        this(null, title, false, runnable, modality);
    }

    public AzureTask(Supplier<T> supplier, Modality modality) {
        this(null, null, false, supplier, modality);
    }

    public AzureTask(String title, Supplier<T> supplier, Modality modality) {
        this(null, title, false, supplier, modality);
    }

    public AzureTask(Object project, String title, boolean cancellable, Runnable runnable) {
        this(project, title, cancellable, runnable, Modality.DEFAULT);
    }

    public AzureTask(Object project, String title, boolean cancellable, Supplier<T> supplier) {
        this(project, title, cancellable, supplier, Modality.DEFAULT);
    }

    public AzureTask(Object project, String title, boolean cancellable, Runnable runnable, Modality modality) {
        this(project, title, cancellable, () -> {
            runnable.run();
            return null;
        }, modality);
    }

    public AzureTask(Object project, String title, boolean cancellable, Supplier<T> supplier, Modality modality) {
        this.project = project;
        this.title = title;
        this.cancellable = cancellable;
        this.supplier = supplier;
        this.modality = modality;
    }

    public enum Modality {
        DEFAULT, ANY, NONE
    }
}
