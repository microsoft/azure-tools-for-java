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


import java.net.URL;
import java.util.Arrays;

import com.microsoft.azuretools.adauth.AuthErrorMessage;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.ProvidedEnvironment;

enum AuthorityType {
    AAD,
    ADFS
}

class AuthenticationAuthority {
    private final static String[] TRUSTED_HOST_LIST = { "login.windows.net",
            "login.chinacloudapi.cn", "login-us.microsoftonline.com", "login.microsoftonline.de",
            "login.microsoftonline.com", "login.microsoftonline.us" };
    private final static String TENANTLESS_TENANT_NAME = "common";
    private final static String AUTHORIZE_ENDPOINT_TEMPLATE = "https://{host}/{tenant}/oauth2/authorize";
    private final static String DISCOVERY_ENDPOINT = "common/discovery/instance";
    private final static String TOKEN_ENDPOINT = "/oauth2/token";
    private final static String USER_REALM_ENDPOINT = "common/userrealm";
    private final static String AUTHORIZE_ENDPOINT = "/oauth2/authorize";

    private String host;
    private String issuer;
    private final String instanceDiscoveryEndpointFormat = "https://%s/"
            + DISCOVERY_ENDPOINT;
    private final String userRealmEndpointFormat = "https://%s/"
            + USER_REALM_ENDPOINT + "/%s?api-version=1.0";
    private final String tokenEndpointFormat = "https://%s/{tenant}"
            + TOKEN_ENDPOINT;
    private final String authorizeEndpointFormat = "https://%s/{tenant}"
            + AUTHORIZE_ENDPOINT;
    private String authority = "https://%s/%s/";
    private String instanceDiscoveryEndpoint;
    private String tokenEndpoint;
    private String authorizeEndpoint;

    private final AuthorityType authorityType;
    private boolean isTenantless;
    private String tokenUri;
    private String selfSignedJwtAudience;
    private boolean instanceDiscoveryCompleted;

    private final URL authorityUrl;
    private final boolean validateAuthority;

    AuthenticationAuthority(final URL authorityUrl,
            final boolean validateAuthority) {

        this.authorityUrl = authorityUrl;
        this.authorityType = detectAuthorityType();
        this.validateAuthority = validateAuthority;
        validateAuthorityUrl();
        setupAuthorityProperties();
    }

    String getHost() {
        return host;
    }

    String getIssuer() {
        return issuer;
    }

    String getAuthority() {
        return authority;
    }

    String getTokenEndpoint() {
        return tokenEndpoint;
    }

    String getUserRealmEndpoint(String username) {
        return String.format(userRealmEndpointFormat, host, username);
    }

    AuthorityType getAuthorityType() {
        return authorityType;
    }

    boolean isTenantless() {
        return isTenantless;
    }

    String getTokenUri() {
        return tokenUri;
    }

    String getAuthorizationEndpoint() {
        return authorizeEndpoint;
    }

    String getSelfSignedJwtAudience() {
        return selfSignedJwtAudience;
    }

    void setSelfSignedJwtAudience(final String selfSignedJwtAudience) {
        this.selfSignedJwtAudience = selfSignedJwtAudience;
    }

//    void doInstanceDiscovery(final Map<String, String> headers,
//            final Proxy proxy, final SSLSocketFactory sslSocketFactory)
//            throws Exception {
//
//        // instance discovery should be executed only once per context instance.
//        if (!instanceDiscoveryCompleted) {
//            // matching against static list failed
//            if (!doStaticInstanceDiscovery()) {
//                // if authority must be validated and dynamic discovery request
//                // as a fall back is success
//                if (validateAuthority
//                        && !doDynamicInstanceDiscovery(headers, proxy,
//                                sslSocketFactory)) {
//                    throw new AuthenticationException(
//                            AuthenticationErrorMessage.AUTHORITY_NOT_IN_VALID_LIST);
//                }
//            }
//            log.info(LogHelper.createMessage(
//                    "Instance discovery was successful",
//                    headers.get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME)));
//            instanceDiscoveryCompleted = true;
//        }
//    }
//
//    boolean doDynamicInstanceDiscovery(final Map<String, String> headers,
//            final Proxy proxy, final SSLSocketFactory sslSocketFactory)
//            throws Exception {
//        final String json = HttpHelper.executeHttpGet(log,
//                instanceDiscoveryEndpoint, headers, proxy, sslSocketFactory);
//        final InstanceDiscoveryResponse discoveryResponse = JsonHelper
//                .convertJsonToObject(json, InstanceDiscoveryResponse.class);
//        return !StringHelper.isBlank(discoveryResponse
//                .getTenantDiscoveryEndpoint());
//    }
//
    void doInstanceDiscovery() throws AuthException{
        if (!instanceDiscoveryCompleted) {
            if (!doStaticInstanceDiscovery()) {
                throw new AuthException(AuthErrorMessage.AuthorityValidationFailed);
            }
        }
        instanceDiscoveryCompleted = true;
    }

    boolean doStaticInstanceDiscovery() {
        // Exception for provided environment
        if (CommonSettings.getEnvironment() instanceof ProvidedEnvironment) {
            return true;
        }

        if (validateAuthority) {
            return Arrays.asList(TRUSTED_HOST_LIST).contains(this.host);
        }
        return true;
    }

    void setupAuthorityProperties() {

        final String host = this.authorityUrl.getAuthority().toLowerCase();
        final String path = this.authorityUrl.getPath().substring(1)
                .toLowerCase();
        final String tenant = path.substring(0, path.indexOf("/"))
                .toLowerCase();

        this.host = host;
        this.authority = String.format(this.authority, host, tenant);
        this.instanceDiscoveryEndpoint = String.format(
                this.instanceDiscoveryEndpointFormat, host);
        this.tokenEndpoint = String.format(this.tokenEndpointFormat, host);
        this.tokenEndpoint = this.tokenEndpoint.replace("{tenant}", tenant);
        this.tokenUri = this.tokenEndpoint;
        this.authorizeEndpoint = String.format(this.authorizeEndpointFormat, host);
        this.authorizeEndpoint = this.authorizeEndpoint.replace("{tenant}", tenant);
        this.issuer = this.tokenUri;

        this.isTenantless = TENANTLESS_TENANT_NAME.equalsIgnoreCase(tenant);
        this.setSelfSignedJwtAudience(this.getIssuer());
        this.createInstanceDiscoveryEndpoint(tenant);
    }

    AuthorityType detectAuthorityType() {
        if (authorityUrl == null) {
            throw new NullPointerException("authority");
        }

        final String path = authorityUrl.getPath().substring(1);
        if (StringUtils.isNullOrEmpty(path)) {
            throw new IllegalArgumentException(AuthErrorMessage.AuthorityUriInvalidPath);
        }

        final String firstPath = path.substring(0, path.indexOf("/"));
        final AuthorityType authorityType = isAdfsAuthority(firstPath) ? AuthorityType.ADFS
                : AuthorityType.AAD;

        return authorityType;
    }

    void validateAuthorityUrl() {

        if (authorityType != AuthorityType.AAD && validateAuthority) {
            throw new IllegalArgumentException(
                    AuthErrorMessage.UnsupportedAuthorityValidation);
        }

        if (!this.authorityUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException(
                    AuthErrorMessage.AuthorityUriInsecure);
        }

        if (this.authorityUrl.toString().contains("#")) {
            throw new IllegalArgumentException(
                    "authority is invalid format (contains fragment)");
        }

        if (!StringUtils.isNullOrEmpty(this.authorityUrl.getQuery())) {
            throw new IllegalArgumentException(
                    "authority cannot contain query parameters");
        }
    }

    void createInstanceDiscoveryEndpoint(final String tenant) {
        this.instanceDiscoveryEndpoint += "?api-version=1.0&authorization_endpoint="
                + AUTHORIZE_ENDPOINT_TEMPLATE;
        this.instanceDiscoveryEndpoint = this.instanceDiscoveryEndpoint
                .replace("{host}", host);
        this.instanceDiscoveryEndpoint = this.instanceDiscoveryEndpoint
                .replace("{tenant}", tenant);
    }

    static boolean isAdfsAuthority(final String firstPath) {
        return firstPath.compareToIgnoreCase("adfs") == 0;
    }

}
