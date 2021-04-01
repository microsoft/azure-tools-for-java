/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.state.impl;

import com.microsoft.azure.oidc.common.state.State;
import com.microsoft.azure.oidc.common.state.StateFactory;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleStateFactory implements StateFactory {
    private static final StateFactory INSTANCE = new SimpleStateFactory();

    @Override
    public State createState(final String userID, final String sessionName, final String requestURI) {
        if (userID == null || sessionName == null || requestURI == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new SimpleState(userID, sessionName, requestURI);
    }

    public static StateFactory getInstance() {
        return INSTANCE;
    }
}
