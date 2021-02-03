/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

public abstract class CommonRunnable<T,E extends Exception> implements Runnable {
    private final T parameter;

    public CommonRunnable(T parameter) {
        this.parameter = parameter;
    }

    public abstract void runSpecificParameter(T parameter) throws E, AzureCmdException;

    public abstract void exceptionHandle(Exception e);

    public void run(){
        try {
            runSpecificParameter(this.parameter);
        }
        catch (Exception e) {
            exceptionHandle(e);
        }
    }
}
