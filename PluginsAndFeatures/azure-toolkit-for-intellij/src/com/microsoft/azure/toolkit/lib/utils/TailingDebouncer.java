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

package com.microsoft.azure.toolkit.lib.utils;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TailingDebouncer implements Debouncer {
    private final Runnable debounced;
    private final int delay;
    private Subscription timer;

    public TailingDebouncer(final Runnable debounced, final int delayInMillis) {
        this.debounced = debounced;
        this.delay = delayInMillis;
    }

    @Override
    public synchronized void debounce() {
        if (this.isPending()) {
            this.timer.unsubscribe();
        }
        this.timer = Observable.timer(this.delay, TimeUnit.MILLISECONDS)
                               .subscribeOn(Schedulers.io())
                               .subscribe(ignore -> {
                                   this.debounced.run();
                                   this.timer = null;
                               });
    }

    public synchronized boolean isPending() {
        return Objects.nonNull(this.timer) && !this.timer.isUnsubscribed();
    }
}
