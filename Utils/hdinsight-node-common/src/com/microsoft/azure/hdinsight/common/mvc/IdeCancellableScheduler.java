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
package com.microsoft.azure.hdinsight.common.mvc;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IdeCancellableScheduler extends Scheduler {
    @NotNull
    private IdeCancellableTask task;

    class IdeCancellableWorker extends Worker {
        final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

        private boolean isCancelled = false;

        @Nullable
        private Timer delayTimer;

        @Override
        public Subscription schedule(Action0 action) {
            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }

            task.execute(action::call);

            return this;
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (delayTime <= 0) {
                return schedule(action);
            }

            if (isUnsubscribed()) {
                return Subscriptions.unsubscribed();
            }

            delayTimer = new Timer("Ide cancellable worker delay timer");

            delayTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isUnsubscribed()) {
                        schedule(action);
                    }
                }
            }, unit.toMillis(delayTime));

            return this;
        }

        @Override
        public void unsubscribe() {
            rwl.writeLock().lock();

            try {
                if (isCancelled) {
                    return;
                }

                isCancelled = true;
            } finally {
                rwl.writeLock().unlock();
            }

            if (delayTimer != null) {
                delayTimer.cancel();
            }

            task.cancel();
        }

        @Override
        public boolean isUnsubscribed() {
            rwl.readLock().lock();
            try {
                return isCancelled;
            } finally {
                rwl.readLock().unlock();
            }
        }
    }

    public IdeCancellableScheduler(@NotNull IdeCancellableTask task) {
        this.task = task;
    }

    @Override
    public Worker createWorker() {
        return new IdeCancellableWorker();
    }

    public void cancel() {

    }
}
