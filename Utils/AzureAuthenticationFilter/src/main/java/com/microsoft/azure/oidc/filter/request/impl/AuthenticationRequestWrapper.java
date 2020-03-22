/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
