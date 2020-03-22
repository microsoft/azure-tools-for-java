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
package com.microsoft.azure.oidc.filter.helper.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.oidc.application.settings.ApplicationSettings;
import com.microsoft.azure.oidc.application.settings.ApplicationSettingsLoader;
import com.microsoft.azure.oidc.application.settings.impl.SimpleApplicationSettingsLoader;
import com.microsoft.azure.oidc.common.state.State;
import com.microsoft.azure.oidc.common.state.StateFactory;
import com.microsoft.azure.oidc.common.state.impl.SimpleStateFactory;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.configuration.ConfigurationCache;
import com.microsoft.azure.oidc.configuration.impl.SimpleConfigurationCache;
import com.microsoft.azure.oidc.exception.GeneralException;
import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.filter.configuration.authentication.AuthenticationConfigurationService;
import com.microsoft.azure.oidc.filter.configuration.authentication.impl.SimpleAuthenticationConfigurationService;
import com.microsoft.azure.oidc.filter.helper.AuthenticationHelper;
import com.microsoft.azure.oidc.filter.request.impl.AuthenticationRequestWrapper;
import com.microsoft.azure.oidc.filter.request.impl.SandboxRequestWrapper;
import com.microsoft.azure.oidc.graph.GraphCache;
import com.microsoft.azure.oidc.graph.impl.SimpleGraphCache;
import com.microsoft.azure.oidc.token.Token;
import com.microsoft.azure.oidc.token.TokenParser;
import com.microsoft.azure.oidc.token.TokenValidator;
import com.microsoft.azure.oidc.token.impl.SimpeTokenParser;
import com.microsoft.azure.oidc.token.impl.SimpleTokenValidator;

public final class SimpleAuthenticationHelper implements AuthenticationHelper {
    private static final AuthenticationHelper INSTANCE = new SimpleAuthenticationHelper();
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAuthenticationHelper.class);
    private static final State NO_STATE = null;
    private static final String[] ERROR_VALUES = { "access_denied", "interaction_required" };

    private final TokenParser tokenParser = SimpeTokenParser.getInstance();

    private final TokenValidator tokenValidator = SimpleTokenValidator.getInstance();

    private final GraphCache graphCache = SimpleGraphCache.getInstance();

    private final ApplicationSettingsLoader applicationSettingsLoader = SimpleApplicationSettingsLoader.getInstance();

    private final ConfigurationCache configurationCache = SimpleConfigurationCache.getInstance();

    private final StateFactory stateFactory = SimpleStateFactory.getInstance();

    private final AuthenticationConfigurationService authenticationConfigurationService = SimpleAuthenticationConfigurationService
            .getInstance();

    @Override
    public void doUnauthenticatedAction(final FilterChain chain, final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final Token token, final Boolean isError)
            throws IOException, ServletException {
        final Boolean isExcluded = isExcluded(httpRequest);
        if (isExcluded && !isError) {
            doExcludedAction(chain, httpRequest, httpResponse, token);
            return;
        }
        doAuthenticateAction(httpRequest, httpResponse, token, isError);
    }

    @Override
    public void doAuthenticateAction(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final Token token, final Boolean isError) throws IOException {
        httpResponse.sendRedirect(getAuthenticationEndPoint(httpRequest, token, isError));
    }

    @Override
    public void doInvalidTokenAction(final HttpServletResponse httpResponse) throws IOException {
        LOGGER.error("Token Failed Validation");
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Token Failed Validation");
    }

    @Override
    public void doActiveTokenAction(final FilterChain chain, final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final Token token) throws ServletException, IOException {
        final State state = getState(httpRequest);
        final Boolean isRedirectedFromAzureB2C = state != NO_STATE;
        if (isRedirectedFromAzureB2C) {
            final Boolean isForwardRequestRequired = !state.getRequestURI().equals(getFullRequestURI(httpRequest));
            if (isForwardRequestRequired) {
                doForwardRequestAction(httpRequest, httpResponse, token, state);
                return;
            }
            doRedirectRequestAction(httpRequest, httpResponse, state);
            return;
        }
        final Boolean isUnauthorised = !isAuthorised(httpRequest, token);
        if (isUnauthorised) {
            doUnauthorisedAction(httpResponse);
            return;
        }
        doAuthenticatedAction(chain, httpRequest, httpResponse, token);
        return;
    }

    @Override
    public void doExceptionAction(final HttpServletResponse httpResponse, final RuntimeException e) throws IOException {
        LOGGER.error("Error in Authentication", e);
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Error in Authentication");
    }

    @Override
    public String getTokenString(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final String tokenName) {
        if (httpRequest == null || httpResponse == null || tokenName == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final String tokenString = httpRequest.getParameter(tokenName);
        if (tokenString != null) {
            return addCookie(httpRequest, httpResponse, tokenName, tokenString);
        }

        final Cookie cookieToken = getCookie(httpRequest, tokenName);
        if (cookieToken != null) {
            return addCookie(httpRequest, httpResponse, tokenName, cookieToken.getValue());
        }
        return null;
    }

    @Override
    public String getErrorString(final HttpServletRequest httpRequest, final String errorName) {
        if (httpRequest == null || errorName == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final String errorString = httpRequest.getParameter(errorName);
        return errorString;
    }

    @Override
    public Token getToken(final String tokenString) {
        return tokenParser.getToken(tokenString);
    }

    @Override
    public Boolean isValidToken(final Token token) {
        return tokenValidator.validateCommon(token);
    }

    @Override
    public Boolean isActiveToken(final Token token) {
        return tokenValidator.validateExpiration(token);
    }

    @Override
    public Boolean isAuthenticationError(final String errorString) {
        for (final String errorValue : ERROR_VALUES) {
            if (errorValue.equals(errorString)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private void doExcludedAction(final FilterChain chain, final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final Token token) throws IOException, ServletException {
        chain.doFilter(getSandboxWrapper(httpRequest, token), httpResponse);
    }

    private void doForwardRequestAction(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final Token token, final State state) throws ServletException, IOException {
        final HttpServletRequest cookieRequestWrapper = clearSessionCoookie(httpRequest, httpResponse, token, state);
        httpRequest.getRequestDispatcher(state.getRequestURI()).forward(cookieRequestWrapper, httpResponse);
    }

    private void doRedirectRequestAction(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final State state) throws IOException {
        httpResponse.sendRedirect(getStateRedirectURL(httpRequest, state));
    }

    private void doAuthenticatedAction(final FilterChain chain, final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final Token token) throws IOException, ServletException {
        chain.doFilter(getAuthenticationWrapper(httpRequest, token), httpResponse);
    }

    private void doUnauthorisedAction(final HttpServletResponse httpResponse) throws IOException {
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Not authorised to access this resource");
    }

    private Boolean isExcluded(final HttpServletRequest httpRequest) {
        String uriString = null;
        final Boolean isRootContext = "".equals(httpRequest.getContextPath());
        if (isRootContext) {
            uriString = httpRequest.getRequestURI();
        } else {
            final int length = httpRequest.getRequestURI().length();
            uriString = httpRequest.getRequestURI().substring(length);
        }
        for (final Pattern pattern : authenticationConfigurationService.get().getExclusionRegexPatternList()) {
            final Matcher matcher = pattern.matcher(uriString);
            final Boolean isMatchFound = matcher.matches();
            if (isMatchFound) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private Boolean isAuthorised(final HttpServletRequest httpRequest, final Token token) {
        String uriString = null;
        final Boolean isRootContext = "".equals(httpRequest.getContextPath());
        if (isRootContext) {
            uriString = httpRequest.getRequestURI();
        } else {
            final int length = httpRequest.getRequestURI().length();
            uriString = httpRequest.getRequestURI().substring(length);
        }
        int index = 0;
        for (final String urlPattern : authenticationConfigurationService.get().getAuthorisationUriPatternList()) {
            final Pattern pattern = authenticationConfigurationService.get().getAuthorisationRegexPatternList()
                    .get(index++);
            final Matcher matcher = pattern.matcher(uriString);
            final Boolean isMatchFound = matcher.matches();
            if (isMatchFound) {
                final HttpServletRequest authRequest = getAuthenticationWrapper(httpRequest, token);
                for (final String roleName : authenticationConfigurationService.get().getAuthorisationRoleMap()
                        .get(urlPattern)) {
                    final Boolean isUserInRole = authRequest.isUserInRole(roleName);
                    if (isUserInRole) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    private String addCookie(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
            final String cookieName, final String cookieValue) {
        if (httpRequest == null || httpResponse == null || cookieName == null || cookieValue == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final Cookie cookie = new Cookie(cookieName, "");
        cookie.setValue(cookieValue);
        cookie.setMaxAge(-1);
        cookie.setSecure(true);
        cookie.setDomain(httpRequest.getServerName());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        httpResponse.addCookie(cookie);
        return cookie.getValue();
    }

    private HttpServletRequest getSandboxWrapper(final HttpServletRequest httpRequest, final Token token) {
        return new SandboxRequestWrapper(httpRequest, token, graphCache);
    }

    private HttpServletRequest getAuthenticationWrapper(final HttpServletRequest httpRequest, final Token token) {
        if (httpRequest == null) {
            throw new PreconditionException("Required parameter is null");
        }
        return new AuthenticationRequestWrapper(httpRequest, token, graphCache);
    }

    private String getAuthenticationEndPoint(final HttpServletRequest httpRequest, final Token token,
            final Boolean isError) {
        if (httpRequest == null) {
            throw new PreconditionException("Required parameter is null");
        }
        try {
            final String requestURI = httpRequest.getRequestURI();
            final String queryString = httpRequest.getQueryString();
            final ApplicationSettings applicationSettings = applicationSettingsLoader.load();
            final Configuration configuration = configurationCache.load();
            if (configuration == null) {
                throw new GeneralException("Error loading configuration");
            }
            final HttpSession session = httpRequest.getSession(false);
            final String sessionName = session == null ? "" : session.getId();
            final StringBuilder uriStringBuilder = new StringBuilder();
            Base64 encoder = new Base64();

            if (isError) {
                final State previousState = getState(httpRequest);
                uriStringBuilder.append(previousState.getRequestURI());
            } else {
                uriStringBuilder.append(requestURI);
                if (queryString != null && !"".equals(queryString.trim())) {
                    uriStringBuilder.append("?");
                    uriStringBuilder.append(queryString);
                }
            }

            final String userID = token == null ? "" : token.getUserID().getValue();
            final State state = stateFactory.createState(userID, sessionName, uriStringBuilder.toString());
            final ObjectMapper mapper = new ObjectMapper();
            final String stateString = mapper.writeValueAsString(state);
            final String urlString = String.format(
                    "%s%sclient_Id=%s&state=%s&nonce=defaultNonce&redirect_uri=%s&scope=openid%%20offline_access&response_type=code+id_token&prompt=%s&response_mode=form_post",
                    configuration.getAuthenticationEndPoint(),
                    configuration.getAuthenticationEndPoint().getName().contains("?") ? "&" : "?",
                    applicationSettings.getApplicationId(),
                    new String(encoder.encode(stateString.getBytes()), "UTF-8"),
                    URLEncoder.encode(applicationSettings.getRedirectURL().getValue(), "UTF-8"),
                    token == null ? "login" : "none");
            return urlString;
        } catch (IOException e) {
            throw new GeneralException("IO Exception", e);
        }
    }

    private Cookie getCookie(final HttpServletRequest httpRequest, final String cookieName) {
        if (httpRequest == null || cookieName == null) {
            throw new PreconditionException("Required parameter is null");
        }
        if (httpRequest.getCookies() == null) {
            return null;
        }
        if (httpRequest.getCookies() != null) {
            for (final Cookie cookie : httpRequest.getCookies()) {
                if (cookie == null || cookie.getName() == null) {
                    continue;
                }
                if (cookie.getName().equals(cookieName)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private State getState(final HttpServletRequest request) {
        if (request == null) {
            throw new PreconditionException("Required parameter is null");
        }
        try {
            final Base64 decoder = new Base64();
            final String stateString = request.getParameter("state") == null ? null
                    : new String(decoder.decode(request.getParameter("state").getBytes()), "UTF-8");
            if (stateString == null || stateString.equals("")) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode stateNode = mapper.readValue(stateString, JsonNode.class);
            final State state = stateFactory.createState(stateNode.get("userID").asText(""),
                    stateNode.get("sessionName").asText(""), stateNode.get("requestURI").asText());
            return state;
        } catch (IOException e) {
            throw new GeneralException("IO Exception", e);
        }
    }

    private String getFullRequestURI(final HttpServletRequest request) {
        if (request == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final String requestURI = request.getRequestURI();
        final String queryString = request.getQueryString();
        final StringBuilder builder = new StringBuilder();
        builder.append(requestURI);
        if (queryString != null && !"".equals(queryString.trim())) {
            builder.append("?");
            builder.append(queryString);
        }
        return builder.toString();
    }

    private String getStateRedirectURL(final HttpServletRequest httpRequest, final State state) {
        if (httpRequest == null || state == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final String urlString = String.format("https://%s%s", httpRequest.getServerName(), state.getRequestURI());
        return urlString;
    }

    // this needs refactoring.
    private HttpServletRequest clearSessionCoookie(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final Token token, final State state) {
        if (httpRequest == null || httpResponse == null || token == null || state == null) {
            throw new PreconditionException("Required parameter is null");
        }
        final Cookie redisSessionCookie = getCookie(httpRequest, "SESSION");
        final Cookie javaSessionCookie = getCookie(httpRequest, "JSESSIONID");
        if (redisSessionCookie != null || javaSessionCookie != null) {
            if (token.getUserID().toString().equals(state.getUserID())) {
                if (redisSessionCookie != null && redisSessionCookie.getValue().equals(state.getSessionName())) {
                    return httpRequest;
                }
                if (javaSessionCookie != null && javaSessionCookie.getValue().equals(state.getSessionName())) {
                    return httpRequest;
                }
            }
            if (redisSessionCookie != null) {
                redisSessionCookie.setMaxAge(0);
                httpResponse.addCookie(redisSessionCookie);
                HttpSession session = httpRequest.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
            if (javaSessionCookie != null) {
                javaSessionCookie.setMaxAge(0);
                httpResponse.addCookie(javaSessionCookie);
                HttpSession session = httpRequest.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
            return new HttpServletRequestWrapper(httpRequest) {
                @Override
                public Cookie[] getCookies() {
                    final List<Cookie> cookieList = new ArrayList<Cookie>();
                    for (Cookie cookie : httpRequest.getCookies()) {
                        if (!cookie.getName().equals("SESSION") && !cookie.getName().equals("JSESSIONID")) {
                            cookieList.add(cookie);
                        }
                    }
                    final Cookie[] cookieArray = new Cookie[cookieList.size()];
                    cookieList.toArray(cookieArray);
                    return cookieArray;
                }
            };
        }
        return httpRequest;
    }

    public static AuthenticationHelper getInstance() {
        return INSTANCE;
    }
}
