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

package com.microsoft.azure.oidc.filter.impl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCacheService;
import com.microsoft.azure.oidc.concurrent.cache.impl.SimpleConcurrentCacheService;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.exception.GeneralException;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.filter.configuration.algorithm.AlgorithmConfigurationService;
import com.microsoft.azure.oidc.filter.configuration.algorithm.impl.SimpleAlgorithmConfigurationService;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationService;
import com.microsoft.azure.oidc.filter.configuration.authentication.impl.SimpleAuthenticationConfigurationService;
import com.microsoft.azure.oidc.filter.helper.AuthenticationHelper;
import com.microsoft.azure.oidc.filter.helper.impl.SimpleAuthenticationHelper;
import com.microsoft.azure.oidc.token.Token;

public final class SimpleAuthenticationFilter implements Filter {
    private static final String ALGORITHM_CONFIGURATION = "algorithmConfiguration";
    private static final String AUTHENTICATION_CONFIGURATION = "authenticationConfiguration";
    private static final String TOKEN_NAME = "id_token";
    private static final String ERROR_NAME = "error";
    private static final String NO_ERROR_STRING = null;
    private static final String NO_TOKEN_STRING = null;
    private static final Token NO_TOKEN = null;

    private final AuthenticationHelper authenticationHelper = SimpleAuthenticationHelper.getInstance();

    private final AuthenticationConfigurationService authenticationConfigurationService = SimpleAuthenticationConfigurationService
            .getInstance();

    private final AlgorithmConfigurationService algorithmConfigurationService = SimpleAlgorithmConfigurationService
            .getInstance();

    private final ConcurrentCacheService concurrentCacheService = SimpleConcurrentCacheService.getInstance();

    @Override
    public void destroy() {
        concurrentCacheService.shutdownNow();
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            final String tokenString = getHelper().getTokenString(httpRequest, httpResponse, TOKEN_NAME);
            final String errorString = getHelper().getErrorString(httpRequest, ERROR_NAME);
            final Boolean isAuthenticationError = getHelper().isAuthenticationError(errorString);
            final Boolean isUnauthenticated = tokenString == NO_TOKEN_STRING;
            if (isUnauthenticated || isAuthenticationError) {
                getHelper().doUnauthenticatedAction(chain, httpRequest, httpResponse, NO_TOKEN, isAuthenticationError);
                return;
            }
            final Token token = getHelper().getToken(tokenString);
            final Boolean isInvalidToken = !getHelper().isValidToken(token);
            if (isInvalidToken) {
                getHelper().doInvalidTokenAction(httpResponse);
                return;
            }
            final Boolean isActiveToken = getHelper().isActiveToken(token);
            if (isActiveToken) {
                getHelper().doActiveTokenAction(chain, httpRequest, httpResponse, token);
                return;
            }
            getHelper().doUnauthenticatedAction(chain, httpRequest, httpResponse, token, isAuthenticationError);
        } catch (GeneralException | PreconditionException e) {
            getHelper().doExceptionAction(httpResponse, e);
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        final String securityCacheSizeString = filterConfig.getInitParameter("securityCacheSize");
        if (securityCacheSizeString == null) {
            concurrentCacheService.createCache(Boolean.class, "roleCache", 30L, 1000L);
        } else {
            concurrentCacheService.createCache(Boolean.class, "roleCache", 30L,
                    Long.parseLong(securityCacheSizeString));
        }
        concurrentCacheService.createCache(Configuration.class, "configurationCache", 60L, 1L);
        authenticationConfigurationService.initialise(filterConfig, AUTHENTICATION_CONFIGURATION);
        algorithmConfigurationService.initialise(filterConfig, ALGORITHM_CONFIGURATION);
    }

    private AuthenticationHelper getHelper() {
        return authenticationHelper;
    }
}
