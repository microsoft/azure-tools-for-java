/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.base;

public class SchedulerProviderFactory {

    private SchedulerProvider schedulerProvider;

    private SchedulerProviderFactory() {
    }

    private static final class SchedulerProviderFactoryHolder {
        private static final SchedulerProviderFactory INSTANCE = new SchedulerProviderFactory();
    }

    public static SchedulerProviderFactory getInstance() {
        return SchedulerProviderFactoryHolder.INSTANCE;
    }

    public void init(SchedulerProvider schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
    }

    public SchedulerProvider getSchedulerProvider() {
        return this.schedulerProvider;
    }
}
