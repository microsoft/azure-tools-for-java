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
package com.microsoft.azure.oidc.filter.session.impl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

@SuppressWarnings("deprecation")
public final class SandboxSessionWrapper implements HttpSession {
    private static final String SANDBOX = "com.microsoft.azure.oid.SANDBOX_SESSION";

    private final HttpSession session;

    public SandboxSessionWrapper(final HttpSession session) {
        this.session = session;
        session.setAttribute(SandboxSessionWrapper.SANDBOX, new HashMap<String, Object>());
    }

    @Override
    public long getCreationTime() {
        return session.getCreationTime();
    }

    @Override
    public String getId() {
        return session.getId();
    }

    @Override
    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }

    @Override
    public ServletContext getServletContext() {
        return session.getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(final int interval) {
        session.setMaxInactiveInterval(interval);
    }

    @Override
    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return session.getSessionContext();
    }

    @Override
    public Object getAttribute(final String name) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) session
                .getAttribute(SandboxSessionWrapper.SANDBOX);
        return map.get(name);
    }

    @Override
    public Object getValue(final String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) session
                .getAttribute(SandboxSessionWrapper.SANDBOX);
        return new Enumeration<String>() {
            final Iterator<String> iterator = map.keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    public String[] getValueNames() {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) session
                .getAttribute(SandboxSessionWrapper.SANDBOX);
        final String[] names = new String[map.size()];
        map.keySet().toArray(names);
        return names;
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) session
                .getAttribute(SandboxSessionWrapper.SANDBOX);
        map.put(name, value);

    }

    @Override
    public void putValue(final String name, final Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(final String name) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) session
                .getAttribute(SandboxSessionWrapper.SANDBOX);
        map.remove(name);
    }

    @Override
    public void removeValue(final String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        session.invalidate();
    }

    @Override
    public boolean isNew() {
        return session.isNew();
    }
}
