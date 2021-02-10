/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.base;

import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProvider;

import rx.Scheduler;
import rx.schedulers.TestScheduler;

public class TestSchedulerProvider implements SchedulerProvider {

    private TestScheduler testScheduler = new TestScheduler();

    @Override
    public Scheduler io() {
        return testScheduler;
    }

    @Override
    public Scheduler computation() {
        return testScheduler;
    }

    public void triggerActions() {
        testScheduler.triggerActions();
    }

}
