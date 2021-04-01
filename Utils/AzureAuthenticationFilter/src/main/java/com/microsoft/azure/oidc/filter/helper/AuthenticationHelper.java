/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.helper;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.azure.oidc.token.Token;

public interface AuthenticationHelper {

    void doUnauthenticatedAction(FilterChain chain, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            Token token, final Boolean isError) throws IOException, ServletException;

    void doAuthenticateAction(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Token token, Boolean isError)
            throws IOException;

    void doInvalidTokenAction(HttpServletResponse httpResponse) throws IOException;

    void doActiveTokenAction(FilterChain chain, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            Token token) throws ServletException, IOException;

    void doExceptionAction(final HttpServletResponse httpResponse, RuntimeException e) throws IOException;

    String getTokenString(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final String tokenName);

    String getErrorString(final HttpServletRequest httpRequest, final String tokenName);

    Token getToken(String tokenString);

    Boolean isValidToken(Token token);

    Boolean isActiveToken(Token token);

    Boolean isAuthenticationError(String errorString);
}
