/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.request.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.microsoft.azure.oidc.filter.session.impl.SandboxSessionWrapper;
import com.microsoft.azure.oidc.graph.GraphCache;
import com.microsoft.azure.oidc.token.Token;

public final class SandboxRequestWrapper extends AuthenticationRequestWrapper {
    private HttpSession session;

    public SandboxRequestWrapper(final HttpServletRequest request, final Token token, final GraphCache graphCache) {
        super(request, token, graphCache);
        final HttpSession newSession = request.getSession(false);
        if (newSession == null) {
            this.session = null;
        } else {
            this.session = new SandboxSessionWrapper(newSession);
        }
    }

    @Override
    public HttpSession getSession(final boolean create) {
        if (session == null) {
            final HttpSession newSession = super.getSession(create);
            if (newSession == null) {
                return session;
            }
            session = new SandboxSessionWrapper(newSession);
        }
        return session;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

}
