/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.future.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.future.FutureHelper;

public class SimpleFutureHelper implements FutureHelper {
    private static final FutureHelper INSTANCE = new SimpleFutureHelper();
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFutureHelper.class);

    @Override
    public <T> T getResult(Future<T> future) {
        if (future == null) {
            throw new PreconditionException("Required parameter is null");
        }
        try {
            while (!future.isDone() && !future.isCancelled()) {
                Thread.sleep(10);
            }
            if (!future.isCancelled()) {
                return future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public static FutureHelper getInstance() {
        return INSTANCE;
    }
}
