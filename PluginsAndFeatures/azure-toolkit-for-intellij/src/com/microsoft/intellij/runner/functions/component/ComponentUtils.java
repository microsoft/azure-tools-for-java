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

package com.microsoft.intellij.runner.functions.component;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.InterruptedIOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ComponentUtils {

    public static <T> Disposable loadResourcesAsync(Callable<T> callable,
                                                    Consumer<T> resourceHandler,
                                                    Consumer<? super Throwable> errorHandler) {
        return Observable
                .fromCallable(() -> {
                    try {
                        return callable.call();
                    } catch (RuntimeException ex) {
                        if (ex.getCause() instanceof InterruptedIOException) {
                            // swallow InterruptedException caused by Disposable.dispose
                            return null;
                        } else {
                            throw ex;
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                    resource -> {
                        DefaultLoader.getIdeHelper().invokeLater(() -> resourceHandler.accept(resource));
                    },
                    exception -> {
                        DefaultLoader.getIdeHelper().invokeLater(() -> {
                            errorHandler.accept(exception);
                        });
                    });
    }

    private ComponentUtils(){

    }
}
