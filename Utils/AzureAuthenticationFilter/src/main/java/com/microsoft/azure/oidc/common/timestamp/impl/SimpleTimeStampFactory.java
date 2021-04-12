/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.timestamp.impl;

import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.common.timestamp.TimeStampFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleTimeStampFactory implements TimeStampFactory {
    private static final TimeStampFactory INSTANCE = new SimpleTimeStampFactory();

    @Override
    public TimeStamp createTimeStamp(final Long time) {
        if (time == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleTimeStamp(time);
    }

    public static TimeStampFactory getInstance() {
        return INSTANCE;
    }
}
