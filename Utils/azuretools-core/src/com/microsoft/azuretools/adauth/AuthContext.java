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

import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthContext {
    private static final Logger log = Logger.getLogger(AuthContext.class.getName());
    private final String clientId;
    private final String authority;
    private UUID correlationId;
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
     * @param validateAuthority boolean whether to validate the authority url.
     * @param correlationId UUID request correlation id.
     * @throws MalformedURLException exception thrown when parse url from authority.
     */
    public AuthContext(@NotNull final String authority, @NotNull final String clientId,
                       final boolean validateAuthority, final UUID correlationId) throws MalformedURLException {
        this.authority = this.canonicalizeUri(authority);
        this.clientId = clientId;
        this.correlationId = (correlationId != null) ? correlationId : UUID.randomUUID();
        this.validateAuthority = validateAuthority;
        authenticationAuthority = new AuthenticationAuthority(new URL(this.authority), this.validateAuthority);
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
    public AuthResult acquireToken(@NotNull final AuthResult lastResult) throws AuthException {
        driver.createAddEntry(lastResult, null);

        final AuthResult result = acquireTokenFromCache(lastResult.getResource(), lastResult.getUserId());
        if (result != null) {
            return result;
        } else {
            throw new AuthException(AuthError.UnknownUser);
        }
    }

    /**
     * Get token for resource and user.
     * @param resource String resource url.
     * @param newAuthCode String need to get new auth code.
     * @param userId String userId.
     * @param isDisplayable boolean whether the userId is displayable id.
     * @param webUi web ui
     * @param redirectUrl redirect url
     * @return AuthResult auth result with the tokens.
     * @throws AuthException exception during get token.
     */
    public AuthResult acquireToken(@NotNull final String resource, final boolean newAuthCode, final String userId,
                                   final boolean isDisplayable, final IWebUi webUi,
                                   final String redirectUrl) throws AuthException {
        String userDisplayableId = null;
        if (null != userId && isDisplayable) {
            userDisplayableId = userId;
        }
        if (newAuthCode) {
            String code = acquireAuthCode(resource, userDisplayableId, webUi, redirectUrl);
            return getTokenWithAuthCode(code, resource, redirectUrl);
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

        if (!newDeviceCode) {
            final AuthResult result = acquireTokenFromCache(resource, uid);
            if (result != null) {
                return result;
            } else {
                throw new AuthException(AuthError.UnknownUser);
            }
        }

        final AuthenticationResult result;
        final ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final AuthenticationContext ctx = new AuthenticationContext(authority, true, service);
            final DeviceCode deviceCode = ctx.acquireDeviceCode(clientId, resource, null).get();
            final IDeviceLoginUI deviceLoginUI = CommonSettings.getUiFactory().getDeviceLoginUI();
            result = deviceLoginUI.authenticate(ctx, deviceCode, callback);
        } catch (Exception e) {
            throw new AuthException(e.getMessage(), e);
        }

        if (result == null) {
            throw new AuthException("Device login failed or was cancelled.");
        }

        final AuthResult authResult = new AuthResult(result.getAccessTokenType(), result.getAccessToken(),
            result.getRefreshToken(), result.getExpiresAfter(),
            UserInfo.createFromAdAlUserInfo(result.getUserInfo()), resource);

        try {
            if (!service.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            // swallow interrupted exception
        } finally {
            service.shutdownNow();
        }
        driver.createAddEntry(authResult, resource);
        return authResult;
    }

    private String acquireAuthCode(@NotNull final String resource, final String userDisplayableId,
        final IWebUi webUi, final String redirectUrl) throws AuthException {
        AuthCode code = null;
        try {
            AuthCodeInteractiveHandler handler = new AuthCodeInteractiveHandler(this.authenticationAuthority,
                this.clientId, webUi, redirectUrl, resource, userDisplayableId);
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
                                            @NotNull final String resource,
                                            final String redirectUrl) throws AuthException {
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
