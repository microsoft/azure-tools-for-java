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

package com.microsoft.azuretools.adauth;

import com.microsoft.aad.adal4j.AdalErrorCode;
import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.DeviceCode;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.codec.binary.Base64;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthContext {
    private static final Logger log = Logger.getLogger(AuthContext.class.getName());
    // TODO: move the cast later after package change
    private final IWebUi webUi;
    
    private final String clientId;
    private final String authority;
    private UUID correlationId;
    private final String redirectUrl;
    private final AuthenticationAuthority authenticationAuthority;
    private final boolean validateAuthority;
    private CacheDriver driver;

    public static void cleanTokenCache() {
        AdTokenCache.getInstance().clear();
    }

    /**
     * AuthContext to acquire token.
     * @param authority String not null.
     * @param clientId String not null.
     * @param redirectUrl String not null.
     * @param webUi IWebUi ui to get auth token interactively.
     * @param validateAuthority boolean whether to validate the authority url.
     * @param correlationId UUID request correlation id.
     * @throws MalformedURLException exception thrown when parse url from authority.
     */
    // todo: webUi should not be a property of AuthContext, it is only used for interactive login
    // todo: make it nullable as first step, then remove it and get it as parameter when acquire token by auth code
    public AuthContext(@NotNull String authority, @NotNull String clientId, @NotNull String redirectUrl,
                       IWebUi webUi, final boolean validateAuthority, UUID correlationId)
            throws MalformedURLException {
        this.authority = this.canonicalizeUri(authority);
        this.clientId = clientId;
        this.correlationId = (correlationId != null) ? correlationId : UUID.randomUUID();
        this.redirectUrl = redirectUrl;
        this.validateAuthority = validateAuthority;
        authenticationAuthority = new AuthenticationAuthority(new URL(this.authority), this.validateAuthority);
        this.webUi = webUi;
        initDriver();
    }
    
    public String getAuthority() {
        return authority;
    }
    
    public String getClientId() {
        return clientId;
    }

    /**
     * Get token from last authentication result
     *
     * @param lastResult last authentication result
     * @return authentication result with updated tokens
     * @throws AuthException exception during getting token
     */
    public AuthResult acquireToken(@NotNull AuthResult lastResult) throws AuthException {
        driver.createAddEntry(lastResult, null);

        return acquireToken(lastResult.getResource(), false, lastResult.getUserId(), lastResult.isMultipleResourceRefreshToken());
    }

    /**
     * Get token for resource and user.
     * @param resource String resource url.
     * @param newAuthCode String need to get new auth code.
     * @param userId String userId.
     * @param isDisplayable boolean whether the userId is displayable id.
     * @return AuthResult auth result with the tokens.
     * @throws AuthException exception during get token.
     */
    public AuthResult acquireToken(@NotNull String resource, boolean newAuthCode,
                                   String userId, boolean isDisplayable) throws AuthException {
        String userDisplayableId = null;
        if (null != userId && isDisplayable) {
            userDisplayableId = userId;
        }
        if (newAuthCode) {
            String code = acquireAuthCode(resource, userDisplayableId);
            return getTokenWithAuthCode(code, resource);
        } else {
            AuthResult result = null;
            result = acquireTokenFromCache(resource, userId);
            if (result != null) {
                return result;
            } else {
                throw new AuthException(AuthError.UnknownUser);
            }
        }
    }

    /**
     * Acquire token for resource by device code.
     */
    public AuthResult acquireToken(@NotNull final String resource, final boolean newDeviceCode, final String uid,
                                   final AuthenticationCallback<AuthenticationResult> callback) throws AuthException {
        try {
            if (!newDeviceCode) {
                final AuthResult result = acquireTokenFromCache(resource, uid);
                if (result != null) {
                    return result;
                } else {
                    throw new AuthException(AuthError.UnknownUser);
                }
            }
            final ExecutorService service = Executors.newFixedThreadPool(5);
            final AuthenticationContext ctx = new AuthenticationContext(authority, true, service);
            final IDeviceLoginUI deviceLoginUI = CommonSettings.getUiFactory().getDeviceLoginUI();
            final DeviceCode deviceCode = ctx.acquireDeviceCode(clientId, resource, null).get();
            deviceLoginUI.showDeviceLoginMessage(deviceCode.getMessage());
            final AuthResult result = acquireTokenByDeviceCode(ctx, resource, deviceCode, deviceLoginUI, callback);
            deviceLoginUI.close();
            return result;
        } catch (Exception e) {
            throw new AuthException(e.getMessage(), e);
        }
    }

    /**
     * Acquire the token by device code.
     */
    private AuthResult acquireTokenByDeviceCode(@NotNull final AuthenticationContext ctx,
                                                @NotNull final String resource,
                                                @NotNull final DeviceCode deviceCode,
                                                @NotNull final IDeviceLoginUI deviceLoginUI,
                                                final AuthenticationCallback<AuthenticationResult> callback)
        throws ExecutionException, InterruptedException, AuthException {

        final long interval = deviceCode.getInterval();
        long remaining = deviceCode.getExpiresIn();
        AuthenticationResult result = null;
        while (result == null && remaining > 0 && !deviceLoginUI.isCancelled()) {
            try {
                result = ctx.acquireTokenByDeviceCode(deviceCode, callback).get();
                remaining -= interval;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof AuthenticationException &&
                    ((AuthenticationException) e.getCause()).getErrorCode() == AdalErrorCode.AUTHORIZATION_PENDING) {
                    // swallow the pending exception
                    Thread.sleep(interval * 1000);
                } else {
                    throw e;
                }
            }
        }
        if (result == null) {
            throw new AuthException(deviceLoginUI.isCancelled() ? "The device login was cancelled." :
                "Unknown User.");
        }

        final AuthResult finalResult = new AuthResult(result.getAccessTokenType(), result.getAccessToken(),
            result.getRefreshToken(), result.getExpiresAfter(),
            UserInfo.createFromAdAlUserInfo(result.getUserInfo()), resource);
        driver.createAddEntry(finalResult, resource);
        return finalResult;
    }


    private String acquireAuthCode(@NotNull final String resource, String userDisplayableId) throws AuthException {
        AuthCode code = null;
        try {
            AuthCodeInteractiveHandler handler = new AuthCodeInteractiveHandler(this.authenticationAuthority,
                    this.clientId, this.webUi, this.redirectUrl, resource, userDisplayableId);
            String response = handler.acquireAuthCode(this.correlationId);
            code = parseAuthorizeResponse(response, this.correlationId);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new AuthException(e.getMessage(), e);
        }

        log.log(Level.FINEST, "==> authorization code: " + code.getCode());
            
        if (code.getStatus() == AuthorizationStatus.Success) {
            return code.getCode();
        } else {
            if (code.getErrorSubcode().compareToIgnoreCase("cancel") == 0) {
                throw new AuthException(AuthError.AuthenticationCanceled);
            }

            String message = code.getError()
                    + (code.getErrorDescription() == null ? "" : ": ")
                    + code.getErrorDescription();
            log.log(Level.SEVERE, message);
            throw new AuthException(code.getError(), code.getErrorDescription());
        }
    }
    
    private AuthResult acquireTokenFromCache(@NotNull String resource, String userId) throws AuthException {
        AuthResult result =  driver.find(resource, userId);
        if (result == null) {
            log.log(Level.SEVERE, "cannot get token");
        }
        return result;
    }
 
    private AuthResult getTokenWithAuthCode(@NotNull final String code,
                                            @NotNull final String resource) throws AuthException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(OAuthParameter.Resource, resource);
        requestParameters.put(OAuthParameter.ClientId, clientId);
        requestParameters.put(OAuthParameter.GrantType, OAuthGrantType.AuthorizationCode);
        requestParameters.put(OAuthParameter.Code, code);
        requestParameters.put(OAuthParameter.RedirectUri, redirectUrl);
        AuthResult result = sendRequest(requestParameters);
        driver.createAddEntry(result, resource);
        return result;
    }
    
    private void initDriver() {
        driver = new CacheDriver(this.authority, this.clientId) {
            protected AuthResult getTokenWithRefreshToken(@NotNull final String refreshToken,
                                                          final String resource) throws AuthException {
                Map<String, String> requestParameters = new HashMap<>();
                if (!StringUtils.isNullOrEmpty(resource)) {
                    requestParameters.put(OAuthParameter.Resource, resource);
                }
                requestParameters.put(OAuthParameter.ClientId, clientId);
                requestParameters.put(OAuthParameter.GrantType, OAuthGrantType.RefreshToken);
                requestParameters.put(OAuthParameter.RefreshToken, refreshToken);
                return sendRequest(requestParameters);
            }
        };
    }

    private AuthResult sendRequest(final Map<String, String> requestParameters) throws AuthException {
        CallState callState = new CallState(correlationId);
        TokenResponse tokenResponse = null;
        try {
            authenticationAuthority.doInstanceDiscovery();
            tokenResponse =
                    HttpHelper.sendPostRequestAndDeserializeJsonResponse(authenticationAuthority.getTokenUri(),
                            requestParameters, callState, TokenResponse.class);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new AuthException(e.getMessage(), e);
        }

        if (tokenResponse != null && tokenResponse.accessToken != null) {
            IdToken idToken = parseIdToken(tokenResponse.idToken);
            UserInfo userInfo = null;
            if (null != idToken) {
                userInfo = UserInfo.createFromIdTokens(idToken);
            }
            return new AuthResult(tokenResponse.tokenType,
                    tokenResponse.accessToken, tokenResponse.refreshToken,
                    tokenResponse.expiresIn, userInfo, tokenResponse.resource);
        } else if (tokenResponse != null && tokenResponse.error != null) {
            String message = tokenResponse.error + tokenResponse.errorDescription;
            log.log(Level.SEVERE, message);
            throw new AuthException(tokenResponse.error, tokenResponse.errorDescription);
        } else {
            String message = AuthError.Unknown + AuthErrorMessage.Unknown;
            log.log(Level.SEVERE, message);
            throw new AuthException(AuthError.Unknown, AuthErrorMessage.Unknown);
        }
    }
    
    private String canonicalizeUri(String authority) {
        if (!authority.endsWith("/")) {
            authority += "/";
        }
        return authority;
    }
 
    private static AuthCode parseAuthorizeResponse(@NotNull String webAuthenticationResult,
                                                   UUID reqCorrelationId)
            throws URISyntaxException, UnsupportedEncodingException {
        AuthCode result = null;

        URI resultUri = new URI(webAuthenticationResult);
        // NOTE: The Fragment property actually contains the leading '#' character and that must be dropped
        String resultData = resultUri.getQuery();
        if (!StringUtils.isNullOrEmpty(resultData)) {
            // Remove the leading '?' first
            Map<String, String> map = UriUtils.formQueryStirng(resultData);

            if (map.containsKey(OAuthHeader.CorrelationId)) {
                String correlationIdHeader = (map.get(OAuthHeader.CorrelationId)).trim();
                try {
                    UUID correlationId = UUID.fromString(correlationIdHeader);
                    if (!correlationId.equals(reqCorrelationId)) {
                        log.log(Level.WARNING, "Returned correlation id '"
                                + correlationId + "' does not match the sent correlation id '"
                                + reqCorrelationId + "'");
                    }
                } catch (IllegalArgumentException ex) {
                    log.log(Level.WARNING, "Returned correlation id '"
                            + correlationIdHeader + "' is not in GUID format.");
                }
            }
            if (map.containsKey(OAuthReservedClaim.Code)) {
                result = new AuthCode(map.get(OAuthReservedClaim.Code));
            } else if (map.containsKey(OAuthReservedClaim.Error)) {
                result = new AuthCode(map.get(OAuthReservedClaim.Error),
                        map.get(OAuthReservedClaim.ErrorDescription), map.get(OAuthReservedClaim.ErrorSubcode));
            } else {
                result = new AuthCode(AuthError.AuthenticationFailed,
                        AuthErrorMessage.AuthorizationServerInvalidResponse);
            }
        }

        if (result == null) {
            throw new UnsupportedEncodingException("Input is invalid");
        }
        return result;
    }

    // TODO: use JWTParser.parse(idToken).getJWTClaimsSet()
    private static IdToken parseIdToken(String idToken) {
        IdToken idTokenBody = null;
        try {
            if (!StringUtils.isNullOrWhiteSpace(idToken)) {
                log.log(Level.FINEST, "idToken: " + idToken);
                String[] idTokenSegments = idToken.split("\\.");

                // If Id token format is invalid, we silently ignore the id token
                if (idTokenSegments.length == 2) {
                    byte[] decoded = Base64.decodeBase64(idTokenSegments[1]);
                    log.log(Level.FINEST, "==> decoded idToken: " + new String(decoded));
                    idTokenBody = JsonHelper.deserialize(IdToken.class, new String(decoded));
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return idTokenBody;
    }

}
