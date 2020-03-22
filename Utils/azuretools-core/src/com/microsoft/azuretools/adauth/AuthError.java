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

public class AuthError {
    /// <summary>
    /// Unknown error.
    /// </summary>
    public static final String Unknown = "unknown_error";

    /// <summary>
    /// Invalid argument.
    /// </summary>
    public static final String InvalidArgument = "invalid_argument";

    /// <summary>
    /// Authentication failed.
    /// </summary>
    public static final String AuthenticationFailed = "authentication_failed";

    /// <summary>
    /// Authentication canceled.
    /// </summary>
    public static final String AuthenticationCanceled = "authentication_canceled";

    /// <summary>
    /// Unauthorized response expected from resource server.
    /// </summary>
    public static final String UnauthorizedResponseExpected = "unauthorized_response_expected";

    /// <summary>
    /// 'authority' is not in the list of valid addresses.
    /// </summary>
    public static final String AuthorityNotInValidList = "authority_not_in_valid_list";

    /// <summary>
    /// Authority validation failed.
    /// </summary>
    public static final String AuthorityValidationFailed = "authority_validation_failed";

    /// <summary>
    /// Loading required assembly failed.
    /// </summary>
    public static final String AssemblyLoadFailed = "assembly_load_failed";

    /// <summary>
    /// Loading required assembly failed.
    /// </summary>
    public static final String InvalidOwnerWindowType = "invalid_owner_window_type";

    /// <summary>
    /// MultipleTokensMatched were matched.
    /// </summary>
    public static final String MultipleTokensMatched = "multiple_matching_tokens_detected";

    /// <summary>
    /// Invalid authority type.
    /// </summary>
    public static final String InvalidAuthorityType = "invalid_authority_type";

    /// <summary>
    /// Invalid credential type.
    /// </summary>
    public static final String InvalidCredentialType = "invalid_credential_type";

    /// <summary>
    /// Invalid service URL.
    /// </summary>
    public static final String InvalidServiceUrl = "invalid_service_url";

    /// <summary>
    /// failed_to_acquire_token_silently.
    /// </summary>
    public static final String FailedToAcquireTokenSilently = "failed_to_acquire_token_silently";

    /// <summary>
    /// Certificate key size too small.
    /// </summary>
    public static final String CertificateKeySizeTooSmall = "certificate_key_size_too_small";

    /// <summary>
    /// Identity protocol login URL Null.
    /// </summary>
    public static final String IdentityProtocolLoginUrlNull = "identity_protocol_login_url_null";

    /// <summary>
    /// Identity protocol mismatch.
    /// </summary>
    public static final String IdentityProtocolMismatch = "identity_protocol_mismatch";

    /// <summary>
    /// Email address suffix mismatch.
    /// </summary>
    public static final String EmailAddressSuffixMismatch = "email_address_suffix_mismatch";

    /// <summary>
    /// Identity provider request failed.
    /// </summary>
    public static final String IdentityProviderRequestFailed = "identity_provider_request_failed";

    /// <summary>
    /// STS token request failed.
    /// </summary>
    public static final String StsTokenRequestFailed = "sts_token_request_failed";

    /// <summary>
    /// Encoded token too long.
    /// </summary>
    public static final String EncodedTokenTooLong = "encoded_token_too_long";

    /// <summary>
    /// Service unavailable.
    /// </summary>
    public static final String ServiceUnavailable = "service_unavailable";

    /// <summary>
    /// Service returned error.
    /// </summary>
    public static final String ServiceReturnedError = "service_returned_error";

    /// <summary>
    /// Federated service returned error.
    /// </summary>
    public static final String FederatedServiceReturnedError = "federated_service_returned_error";

    /// <summary>
    /// STS metadata request failed.
    /// </summary>
    public static final String StsMetadataRequestFailed = "sts_metadata_request_failed";

    /// <summary>
    /// No data from STS.
    /// </summary>
    public static final String NoDataFromSts = "no_data_from_sts";

    /// <summary>
    /// User Mismatch.
    /// </summary>
    public static final String UserMismatch = "user_mismatch";

    /// <summary>
    /// Unknown User Type.
    /// </summary>
    public static final String UnknownUserType = "unknown_user_type";

    /// <summary>
    /// Unknown User.
    /// </summary>
    public static final String UnknownUser = "unknown_user";

    /// <summary>
    /// User Realm Discovery Failed.
    /// </summary>
    public static final String UserRealmDiscoveryFailed = "user_realm_discovery_failed";

    /// <summary>
    /// Accessing WS Metadata Exchange Failed.
    /// </summary>
    public static final String AccessingWsMetadataExchangeFailed = "accessing_ws_metadata_exchange_failed";

    /// <summary>
    /// Parsing WS Metadata Exchange Failed.
    /// </summary>
    public static final String ParsingWsMetadataExchangeFailed = "parsing_ws_metadata_exchange_failed";

    /// <summary>
    /// WS-Trust Endpoint Not Found in Metadata Document.
    /// </summary>
    public static final String WsTrustEndpointNotFoundInMetadataDocument = "wstrust_endpoint_not_found";

    /// <summary>
    /// Parsing WS-Trust Response Failed.
    /// </summary>
    public static final String ParsingWsTrustResponseFailed = "parsing_wstrust_response_failed";

    /// <summary>
    /// The request could not be preformed because the network is down.
    /// </summary>
    public static final String NetworkNotAvailable = "network_not_available";

    /// <summary>
    /// The request could not be preformed because of an unknown failure in the UI flow.
    /// </summary>
    public static final String AuthenticationUiFailed = "authentication_ui_failed";

    /// <summary>
    /// One of two conditions was encountered.
    /// 1. The PromptBehavior.Never flag was passed and but the static finalraint could not be honored
    ///    because user interaction was required.
    /// 2. An error occurred during a silent web authentication that prevented the authentication
    ///    flow from completing in a short enough time frame.
    /// </summary>
    public static final String UserInteractionRequired = "user_interaction_required";

    /// <summary>
    /// Password is required for managed user.
    /// </summary>
    public static final String PasswordRequiredForManagedUserError = "password_required_for_managed_user";

    /// <summary>
    /// Failed to get user name.
    /// </summary>
    public static final String GetUserNameFailed = "get_user_name_failed";

    /// <summary>
    /// Federation Metadata Url is missing for federated user.
    /// </summary>
    public static final String MissingFederationMetadataUrl = "missing_federation_metadata_url";

    /// <summary>
    /// Failed to refresh token.
    /// </summary>
    public static final String FailedToRefreshToken = "failed_to_refresh_token";

    /// <summary>
    /// Integrated authentication failed. You may try an alternative authentication method.
    /// </summary>
    public static final String IntegratedAuthFailed = "integrated_authentication_failed";

    /// <summary>
    /// Duplicate query parameter in extraQueryParameters
    /// </summary>
    public static final String DuplicateQueryParameter = "duplicate_query_parameter";

    public static final String InvalidGrant = "invalid_grant";

    public static final String InteractionRequired = "interaction_required";
}
