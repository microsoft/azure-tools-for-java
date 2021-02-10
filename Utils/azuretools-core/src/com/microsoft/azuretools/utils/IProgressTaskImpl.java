/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

/**
 * Created by vlashch on 1/23/17.
 */
public interface IProgressTaskImpl {
    void doWork(IWorker worker);
}
