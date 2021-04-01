/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
