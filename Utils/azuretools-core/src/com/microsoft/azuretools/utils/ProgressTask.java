/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

/**
 * Created by vlashch on 1/23/17.
 */
public class ProgressTask implements IProgressTask {
    private IProgressTaskImpl impl;

    public ProgressTask(IProgressTaskImpl impl) {
        this.impl = impl;
    }

    @Override
    public void work(IWorker worker) {
        impl.doWork(worker);
    }
}
