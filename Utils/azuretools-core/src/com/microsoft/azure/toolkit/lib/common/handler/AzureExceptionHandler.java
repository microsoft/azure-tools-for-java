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
package com.microsoft.azure.toolkit.lib.common.handler;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import lombok.extern.java.Log;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.InterruptedIOException;
import java.util.function.Consumer;
import java.util.logging.Level;

@Log
public abstract class AzureExceptionHandler {
    private static AzureExceptionHandler handler;

    public static synchronized void register(AzureExceptionHandler handler) {
        if (AzureExceptionHandler.handler == null) {
            AzureExceptionHandler.handler = handler;
        }
    }

    public static AzureExceptionHandler getInstance() {
        return AzureExceptionHandler.handler;
    }

    public static void onUncaughtException(final Throwable e) {
        AzureExceptionHandler.getInstance().handleException(e);
    }

    public static void notify(final Throwable e, @Nullable AzureExceptionAction... actions) {
        AzureExceptionHandler.getInstance().handleException(e, actions);
    }

    public static void notify(final Throwable e, boolean background, @Nullable AzureExceptionAction... actions) {
        AzureExceptionHandler.getInstance().handleException(e, background, actions);
    }

    public static void onRxException(final Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureExceptionHandler.getInstance().handleException(e);
    }

    public void handleException(Throwable throwable, @Nullable AzureExceptionAction... action) {
        log.log(Level.WARNING, "caught an error in AzureExceptionHandler", throwable);
        onHandleException(throwable, action);
    }

    public void handleException(Throwable throwable, boolean isBackGround, @Nullable AzureExceptionAction... action) {
        log.log(Level.WARNING, "caught an error in AzureExceptionHandler", throwable);
        this.onHandleException(throwable, isBackGround, action);
    }

    protected abstract void onHandleException(Throwable throwable, @Nullable AzureExceptionAction[] action);

    protected abstract void onHandleException(Throwable throwable, boolean isBackGround, @Nullable AzureExceptionAction[] action);

    public interface AzureExceptionAction {
        String name();

        void actionPerformed(Throwable throwable);

        static AzureExceptionAction simple(String name, Consumer<? super Throwable> consumer) {
            return new AzureExceptionAction() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public void actionPerformed(final Throwable throwable) {
                    consumer.accept(throwable);
                }
            };
        }
    }

}
