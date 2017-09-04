/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.adauth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AcquireTokenInteractiveHandler extends AcquireTokenHandlerBase {
    final static Logger log = Logger.getLogger(AcquireTokenInteractiveHandler.class.getName());
    private AuthorizationResult authorizationResult;
    private URI redirectUri;
    private String redirectUriRequestParameter;
    private PromptBehavior promptBehavior;
    private final IWebUi webUi;
    private final UserIdentifier userId;

    AcquireTokenInteractiveHandler(Authenticator authenticator, TokenCache tokenCache, String resource,
                                   String clientId, String redirectUri, PromptBehavior promptBehavior, UserIdentifier userId, IWebUi webUi) throws IOException {
        super(authenticator, tokenCache, resource, new ClientKey(clientId), TokenSubjectType.User);
        if (redirectUri == null) {
            throw new IllegalArgumentException("redirectUri");
        }
        try {
            this.redirectUri = new URI(redirectUri);
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
            log.log(Level.SEVERE, "AcquireTokenInteractiveHandler@AcquireTokenInteractiveHandler", ex);
        }
        if (this.redirectUri.getFragment() != null && !this.redirectUri.getFragment().isEmpty()) {
            throw new IllegalArgumentException("redirectUri: " + AuthErrorMessage.RedirectUriContainsFragment);
        }
        this.setRedirectUriRequestParameter();
        if (userId == null) {
            throw new IllegalArgumentException("userId: " +  AuthErrorMessage.SpecifyAnyUser);
        }
        this.userId = userId;
        this.promptBehavior = promptBehavior;
        this.webUi = webUi;
        this.uniqueId = userId.uniqueId();
        this.displayableId = userId.displayableId();
        this.userIdentifierType = userId.type;
        this.loadFromCache = (tokenCache != null
                && this.promptBehavior != PromptBehavior.Always
                && this.promptBehavior != PromptBehavior.RefreshSession);
        this.supportADFS = true;
        log.log(Level.FINEST, String.format(
                "\n=== AcquireTokenInteractiveHandler params:\n\tpromptBehavior: %s\n\twebUi: %s\n\tuniqueId: %s\n\tdisplayableId: %s"
                , promptBehavior.toString()
                , webUi.getClass().getName()
                , uniqueId
                , displayableId));
    }

    private void setRedirectUriRequestParameter() {
        this.redirectUriRequestParameter = redirectUri.toString();
    }

    @Override
    protected void preTokenRequest() throws IOException {
        acquireAuthorization();
    }

    private void acquireAuthorization() throws IOException {
        try {
            log.log(Level.FINEST, "acquireAuthorization...");

            URI authorizationUri = this.createAuthorizationUri(false);
            log.log(Level.FINEST, "Starting web ui...");
            String resultUri = webUi.authenticate(authorizationUri, redirectUri);
            if(resultUri == null) {
                String message = "Interactive sign in is unsuccessful or canceled.";
                log.log(Level.SEVERE, message);
                throw new AuthException(message);
            }
            authorizationResult = ResponseUtils.parseAuthorizeResponse(resultUri, this.callState);
            log.log(Level.FINEST, "==> authorization code: " + authorizationResult.getCode());
            verifyAuthorizationResult();

        } catch (UnsupportedEncodingException | URISyntaxException ex) {
            log.log(Level.SEVERE, "acquireAuthorization@AcquireTokenInteractiveHandler: " + ex);
            throw new IOException(ex);
        }
   }

    private URI createAuthorizationUri(boolean includeFormsAuthParam) throws UnsupportedEncodingException, URISyntaxException {
        String loginHint = null;
        if (!userId.isAnyUser()
            && (userId.type == UserIdentifierType.OptionalDisplayableId
                || userId.type == UserIdentifierType.RequiredDisplayableId)) {
            loginHint = userId.displayableId();
        }
        Map<String, String> requestParameters = this.createAuthorizationRequest(loginHint, includeFormsAuthParam);
        return new URI(this.authenticator.authorizationUri + "?" + UriUtils.toQueryString(requestParameters));
    }

    private Map<String, String> createAuthorizationRequest(String loginHint, boolean includeFormsAuthParam) {
        Map<String, String> authorizationRequestParameters = new HashMap<>();
        authorizationRequestParameters.put(OAuthParameter.Resource, this.resource);
        authorizationRequestParameters.put(OAuthParameter.ClientId, this.clientKey.clientId);
        authorizationRequestParameters.put(OAuthParameter.ResponseType, OAuthResponseType.Code);
        authorizationRequestParameters.put(OAuthParameter.RedirectUri, this.redirectUriRequestParameter);
        if (loginHint != null
                && !loginHint.isEmpty()) {
            authorizationRequestParameters.put(OAuthParameter.LoginHint, loginHint);
        }
        if (this.callState != null
                && this.callState.correlationId != null) {
            authorizationRequestParameters.put(OAuthParameter.CorrelationId, this.callState.correlationId.toString());
            authorizationRequestParameters.put(OAuthHeader.RequestCorrelationIdInResponse, "true");
        }
        // ADFS currently ignores the parameter for now.
        if (promptBehavior == PromptBehavior.Always) {
            authorizationRequestParameters.put(OAuthParameter.Prompt, PromptValue.Login);
        } else if (promptBehavior == PromptBehavior.RefreshSession) {
            authorizationRequestParameters.put(OAuthParameter.Prompt, PromptValue.RefreshSession);
        } else if (promptBehavior == PromptBehavior.Never) {
            authorizationRequestParameters.put(OAuthParameter.Prompt, PromptValue.AttemptNone);
        }
        if (includeFormsAuthParam) {
            authorizationRequestParameters.put(OAuthParameter.FormsAuth, OAuthValue.FormsAuth);
        }
        return authorizationRequestParameters;
    }

    private void verifyAuthorizationResult() throws IOException {
        if (this.promptBehavior == PromptBehavior.Never
                && authorizationResult.getError().equals(OAuthError.LoginRequired)) {
            String message = AuthError.UserInteractionRequired;
            log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
        if (authorizationResult.getStatus() != AuthorizationStatus.Success) {
            if (authorizationResult.getErrorSubcode().compareToIgnoreCase("cancel") == 0) {
                throw new AuthCanceledException("Canceled by user");
            }
            String message = authorizationResult.getError() +
                    authorizationResult.getErrorDescription() == null
                ? ""
                : ": " + authorizationResult.getErrorDescription();
            log.log(Level.SEVERE, message);
            throw new AuthException(message);
        }
    }

    @Override
    protected void addAdditionalRequestParameters(Map<String, String> requestParameters) {
        requestParameters.put(OAuthParameter.GrantType, OAuthGrantType.AuthorizationCode);
        requestParameters.put(OAuthParameter.Code, authorizationResult.getCode());
        requestParameters.put(OAuthParameter.RedirectUri, this.redirectUriRequestParameter);
    }
}
