/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class UserInfo {
    private String uniqueId;
    private String displayableId;
    private String givenName;
    private String familyName;
    private String identityProvider;
    private String passwordChangeUrl;
    private Date passwordExpiresOn;
    private String tenantId;

    private UserInfo() {
    }

    public String getDisplayableId() {
        return displayableId;
    }

    /**
     * Get user id
     *
     * @return String value
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Get given name
     *
     * @return String value
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * Get family name
     *
     * @return String value
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Get identity provider
     *
     * @return String value
     */
    public String getIdentityProvider() {
        return identityProvider;
    }

    public String getPasswordChangeUrl() {
        return passwordChangeUrl;
    }

    public Date getPasswordExpiresOn() {
        if (passwordExpiresOn != null) {
            return (Date)passwordExpiresOn.clone();
        } else {
            return null;
        }
    }

    static UserInfo createFromAdAlUserInfo(final com.microsoft.aad.adal4j.UserInfo adalUserInfo) {
        if (adalUserInfo == null) {
            return null;
        }
        final UserInfo userInfo = new UserInfo();
        userInfo.uniqueId = adalUserInfo.getUniqueId();
        userInfo.displayableId = adalUserInfo.getDisplayableId();
        userInfo.givenName = adalUserInfo.getGivenName();
        userInfo.familyName = adalUserInfo.getFamilyName();
        userInfo.identityProvider = adalUserInfo.getIdentityProvider();
        userInfo.passwordExpiresOn = adalUserInfo.getPasswordExpiresOn();
        userInfo.passwordChangeUrl = adalUserInfo.getPasswordChangeUrl();
        return userInfo;
    }

    static UserInfo createFromIdTokens(final IdToken tokens){
        if (null == tokens) {
            return null;
        }

  //      String tenantId = tokens.tenantId;
        String uniqueId = null;
        String displayableId = null;
        if (!StringUtils.isNullOrWhiteSpace(tokens.objectId)) {
            uniqueId = tokens.objectId;
        }
        else if (!StringUtils.isNullOrWhiteSpace(tokens.subject)) {
            uniqueId = tokens.subject;
        }
        if (!StringUtils.isNullOrWhiteSpace(tokens.upn)) {
            displayableId = tokens.upn;
        }
        else if (!StringUtils.isNullOrWhiteSpace(tokens.email)) {
            displayableId = tokens.email;
        }

        final UserInfo userInfo = new UserInfo();
        userInfo.uniqueId = uniqueId;
        userInfo.tenantId = tokens.tenantId;
        userInfo.displayableId = displayableId;
        userInfo.givenName = tokens.givenName;
        userInfo.familyName = tokens.familyName;
        userInfo.identityProvider = (tokens.identityProvider == null)
                ? tokens.issuer
               : tokens.identityProvider;
        if (!StringUtils.isNullOrEmpty(tokens.passwordExpiration)) {
            int expiry = Integer.parseInt(tokens.passwordExpiration);
            Calendar expires = new GregorianCalendar();
            expires.add(Calendar.SECOND, expiry);
            userInfo.passwordExpiresOn = expires.getTime();
        }

        if (!StringUtils.isNullOrEmpty(tokens.passwordChangeUrl)) {
            userInfo.passwordChangeUrl = tokens.passwordChangeUrl;
        }

        return userInfo;
    }

    public String getTenantId() {
        return tenantId;
    }
}
