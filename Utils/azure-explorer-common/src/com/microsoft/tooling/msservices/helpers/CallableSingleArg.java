/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers;


import java.util.concurrent.Callable;

public abstract class CallableSingleArg<T, TArg> implements Callable<T> {
    @Override
    public final T call() throws Exception {
        return call(null);
    }

    public abstract T call(TArg argument) throws Exception;
}
