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
