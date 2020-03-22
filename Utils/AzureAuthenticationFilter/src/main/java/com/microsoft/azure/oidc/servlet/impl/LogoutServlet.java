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
package com.microsoft.azure.oidc.servlet.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.oidc.application.settings.ApplicationSettings;
import com.microsoft.azure.oidc.application.settings.ApplicationSettingsLoader;
import com.microsoft.azure.oidc.application.settings.impl.SimpleApplicationSettingsLoader;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.configuration.ConfigurationCache;
import com.microsoft.azure.oidc.configuration.impl.SimpleConfigurationCache;
import com.microsoft.azure.oidc.exception.GeneralException;
import com.microsoft.azure.oidc.exception.PreconditionException;

@WebServlet(name = "logout", urlPatterns = "/logout")
public final class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutServlet.class);

    private final ConfigurationCache configurationCache = SimpleConfigurationCache.getInstance();

    private final ApplicationSettingsLoader applicationSettingsLoader = SimpleApplicationSettingsLoader.getInstance();

    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        try {
            final Configuration configuration = configurationCache.load();
            final ApplicationSettings applicationSettings = applicationSettingsLoader.load();

            // First stage. Ask Azure AD B2C to log out out and return here with
            // the finishLogout parameter set
            if (request.getParameter("finishLogout") == null) {
                String tokenString = null;
                final Cookie[] cookies = request.getCookies();
                for (final Cookie cookie : cookies) {
                    if (cookie.getName().equals("id_token")) {
                        tokenString = cookie.getValue();
                        break;
                    }
                }
                final String redirectURL = String.format("%s%spost_logout_redirect_uri=%s%s%s",
                        configuration.getLogoutEndPoint(),
                        configuration.getLogoutEndPoint().getName().contains("?") ? "&" : "?",
                        URLEncoder.encode(applicationSettings.getRedirectURL().getValue(), "UTF-8"),
                        URLEncoder.encode(request.getRequestURI(), "UTF-8"),
                        URLEncoder.encode("?finishLogout=true", "UTF-8"));
                response.setHeader("Authorization", String.format("Bearer %s", tokenString));
                response.sendRedirect(redirectURL);
                return;
            }

            // setup clearing the cookies and invalidate the session
            for (final Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("id_token")) {
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                }
                if (cookie.getName().equals("JSESSIONID") || cookie.getName().equals("SESSON")) {
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                }
            }
            final HttpServletRequest newRequest = new HttpServletRequestWrapper(request) {
                @Override
                public Cookie[] getCookies() {
                    final List<Cookie> cookieList = new ArrayList<Cookie>();
                    for (Cookie cookie : request.getCookies()) {
                        if (!cookie.getName().equals("SESSION") && !cookie.getName().equals("JSESSIONID")) {
                            cookieList.add(cookie);
                        }
                    }
                    final Cookie[] cookieArray = new Cookie[cookieList.size()];
                    cookieList.toArray(cookieArray);
                    return cookieArray;
                }
            };

            // Second stage. Forward the request so the cookies are cleared
            if (request.getAttribute("logout") == null) {
                request.setAttribute("logout", Boolean.TRUE);
                request.getRequestDispatcher(request.getRequestURI() + "?finishLogout=true").forward(newRequest,
                        response);
                return;
            }

            // Final stage. Return to the application landing page
            response.sendRedirect(applicationSettings.getRedirectURL().getValue());
            return;
        } catch (IOException | GeneralException | PreconditionException e) {
            LOGGER.warn(e.getMessage(), e);
            final ApplicationSettings applicationSettings = applicationSettingsLoader.load();
            response.sendRedirect(applicationSettings.getRedirectURL().getValue());
        }
    }
}
