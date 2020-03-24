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
