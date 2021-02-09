/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.request.impl;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.microsoft.azure.oidc.exception.GeneralException;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.graph.GraphCache;
import com.microsoft.azure.oidc.token.Token;

public class AuthenticationRequestWrapper extends HttpServletRequestWrapper {
    private final Token token;
    private final GraphCache graphCache;

    public AuthenticationRequestWrapper(final HttpServletRequest request, final Token token,
            final GraphCache graphCache) {
        super(request);
        if (request == null || graphCache == null) {
            throw new PreconditionException("Required parameter is null");
        }
        this.token = token;
        this.graphCache = graphCache;
    }

    @Override
    public final String getRemoteUser() {
        if (token == null) {
            return null;
        }
        if (token.getUserEmails().isEmpty()) {
            return token.getUserID().getValue();
        }
        return token.getUserEmails().get(0).getValue();
    }

    @Override
    public final Principal getUserPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return token == null ? null : token.getUserID().getValue();
            }
        };
    }

    @Override
    public final boolean isUserInRole(final String role) {
        if (token == null) {
            return Boolean.FALSE;
        }
        final Boolean result = graphCache.isUserInRole(token.getUserID().getValue(), role);
        if (result == null) {
            throw new GeneralException("Authorization Error");
        }
        return result;
    }
}
