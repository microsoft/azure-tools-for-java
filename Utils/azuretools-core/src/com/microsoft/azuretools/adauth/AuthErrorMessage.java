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

class AuthErrorMessage {
    public final static String AccessingMetadataDocumentFailed = "Accessing WS metadata exchange failed";
    public final static String AssemblyLoadFailedTemplate = "Loading an assembly required for interactive user authentication failed. Make sure assembly '{0}' exists";
    public final static String AuthenticationUiFailed = "The browser based authentication dialog failed to complete";
    public final static String AuthorityInvalidUriFormat = "'authority' should be in Uri format";
    public final static String AuthorityNotInValidList = "'authority' is not in the list of valid addresses";
    public final static String AuthorityValidationFailed = "Authority validation failed";
    public final static String AuthorityUriInsecure = "'authority' should use the 'https' scheme";
    public final static String AuthorityUriInvalidPath = "'authority' Uri should have at least one segment in the path (i.e. https://<host>/<path>/...)";
    public final static String AuthorizationServerInvalidResponse = "The authorization server returned an invalid response";
    public final static String CertificateKeySizeTooSmallTemplate = "The certificate used must have a key size of at least {0} bits";
    public final static String EmailAddressSuffixMismatch = "No identity provider email address suffix matches the provided address";
    public final static String EncodedTokenTooLong = "Encoded token size is beyond the upper limit";
    public final static String FailedToAcquireTokenSilently = "Failed to acquire token silently. Call method AcquireToken";
    public final static String FailedToRefreshToken = "Failed to refresh token";
    public final static String FederatedServiceReturnedErrorTemplate = "Federated service at {0} returned error: {1}";
    public final static String IdentityProtocolLoginUrlNull = "The LoginUrl property in identityProvider cannot be null";
    public final static String IdentityProtocolMismatch = "No identity provider matches the requested protocol";
    public final static String IdentityProviderRequestFailed = "Token request to identity provider failed. Check InnerException for more details";
    public final static String InvalidArgumentLength = "Parameter has invalid length";
    public final static String InvalidAuthenticateHeaderFormat = "Invalid authenticate header format";
    public final static String InvalidAuthorityTypeTemplate = "This method overload is not supported by '{0}'";
    public final static String InvalidCredentialType = "Invalid credential type";
    public final static String InvalidFormatParameterTemplate = "Parameter '{0}' has invalid format";
    public final static String InvalidTokenCacheKeyFormat = "Invalid token cache key format";
    public final static String MissingAuthenticateHeader = "WWW-Authenticate header was expected in the response";
    public final static String MultipleTokensMatched = "The cache contains multiple tokens satisfying the requirements. Call AcquireToken again providing more requirements (e.g. UserId)";
    public final static String NetworkIsNotAvailable = "The network is down so authentication cannot proceed";
    public final static String NoDataFromSTS = "No data received from security token service";
    public final static String NullParameterTemplate = "Parameter '{0}' cannot be null";
    public final static String ParsingMetadataDocumentFailed = "Parsing WS metadata exchange failed";
    public final static String ParsingWsTrustResponseFailed = "Parsing WS-Trust response failed";
    public final static String PasswordRequiredForManagedUserError = "Password is required for managed user";
    public final static String RedirectUriContainsFragment = "'redirectUri' must NOT include a fragment component";
    public final static String ServiceReturnedError = "Service returned error. Check InnerException for more details";
    public final static String StsMetadataRequestFailed = "Metadata request to Access Control service failed. Check InnerException for more details";
    public final static String StsTokenRequestFailed = "Token request to security token service failed.  Check InnerException for more details";
    public final static String UnauthorizedHttpStatusCodeExpected = "Unauthorized Http Status Code (401) was expected in the response";
    public final static String UnauthorizedResponseExpected = "Unauthorized http response (status code 401) was expected";
    public final static String UnexpectedAuthorityValidList = "Unexpected list of valid addresses";
    public final static String Unknown = "Unknown error";
    public final static String UnknownUser = "Could not identify logged in user";
    public final static String UnknownUserType = "Unknown User Type";
    public final static String UnsupportedAuthorityValidation = "Authority validation is not supported for this type of authority";
    public final static String UnsupportedMultiRefreshToken = "This authority does not support refresh token for multiple resources. Pass null as a resource";
    public final static String AuthenticationCanceled = "User canceled authentication";
    public final static String UserMismatch = "User '{0}' returned by service does not match user '{1}' in the request";
    public final static String UserCredentialAssertionTypeEmpty = "credential.AssertionType cannot be empty";
    public final static String UserInteractionRequired =
        "One of two conditions was encountered: "
        + "1. The PromptBehavior.Never flag was passed, but the finalraint could not be honored, because user interaction was required. "
        + "2. An error occurred during a silent web authentication that prevented the http authentication flow from completing in a short enough time frame";
    public final static String UserRealmDiscoveryFailed = "User realm discovery failed";
    public final static String WsTrustEndpointNotFoundInMetadataDocument = "WS-Trust endpoint not found in metadata document";
    public final static String GetUserNameFailed = "Failed to get user name";
    public final static String MissingFederationMetadataUrl = "Federation Metadata Url is missing for federated user. This user type is unsupported.";
    public final static String SpecifyAnyUser = "If you do not need access token for any specific user, pass userId=UserIdentifier.AnyUser instead of userId=null.";
    public final static String IntegratedAuthFailed = "Integrated authentication failed. You may try an alternative authentication method";
    public final static String DuplicateQueryParameterTemplate = "Duplicate query parameter '{0}' in extraQueryParameters";

}
