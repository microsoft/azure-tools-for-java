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

package com.microsoft.azure.oidc.filter.configuration.authentication.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationLoader;

public final class SimpleAuthenticationConfigurationLoader implements AuthenticationConfigurationLoader {
    private static final AuthenticationConfigurationLoader INSTANCE = new SimpleAuthenticationConfigurationLoader();

    @Override
    public JsonNode load(final FilterConfig filterConfig, final String parameterName) throws ServletException {
        final String authenticationConfigurationFileName = filterConfig.getInitParameter(parameterName);
        final InputStream is = filterConfig.getServletContext()
                .getResourceAsStream(authenticationConfigurationFileName);
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
            final StringBuilder builder = new StringBuilder();
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                builder.append(line);
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(builder.toString(), JsonNode.class);
        } catch (final IOException e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    public static AuthenticationConfigurationLoader getInstance() {
        return INSTANCE;
    }
}
