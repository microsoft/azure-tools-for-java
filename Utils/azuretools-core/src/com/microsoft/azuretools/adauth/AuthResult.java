/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

public class AuthResult {
    private final String accessTokenType;
    private final long expiresIn;
    @JsonProperty("expiresOnDate")
    private final Date expiresOn;
    private UserInfo userInfo;
    private String userId;
    @JsonProperty("userIdDisplayble")
    private boolean isDisplaybaleUserId;
    private final String accessToken;
    private String refreshToken;
    @JsonProperty("multipleResourceRefreshToken")
    private final boolean isMultipleResourceRefreshToken;
    private String resource;

    /**
     * AuthResult.
     * @param accessTokenType String token type.
     * @param accessToken String accessToken.
     * @param refreshToken String refreshToken.
     * @param expiresIn long expire in time.
     * @param userInfo UserInfo userInfo.
     * @param resource String resource.
     */
    public AuthResult(@JsonProperty("accessTokenType") final String accessTokenType,
                      @JsonProperty("accessToken") final String accessToken,
                      @JsonProperty("refreshToken") final String refreshToken,
                      @JsonProperty("expiresAfter") final long expiresIn,
                      @JsonProperty("userInfo") final UserInfo userInfo,
                      @JsonProperty("resource") final String resource) {
        this.accessTokenType = accessTokenType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;

        Date now = new Date();
        now.setTime(now.getTime() + (expiresIn * 1000));
        this.expiresOn = now;

        if (userInfo != null) {
            setUserInfo(userInfo);
        } else {
            this.userId = "";
            this.isDisplaybaleUserId = false;
        }
        this.resource = (resource != null ? resource : "");
        this.isMultipleResourceRefreshToken = !StringUtils.isNullOrEmpty(resource);
    }

    public void setUserInfo(UserInfo info) {
        if (null == info) {
            return;
        }
        this.userInfo = info;
        if (null != userInfo.getDisplayableId()) {
            this.userId = userInfo.getDisplayableId();
            this.isDisplaybaleUserId = true;
        } else {
            this.userId = userInfo.getUniqueId() != null ? userInfo.getUniqueId() : "";
            this.isDisplaybaleUserId = false;
        }
    }

    public void setRefreshToken(@NotNull String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAccessTokenType() {
        return accessTokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getResource() {
        return resource;
    }

    public long getExpiresAfter() {
        return expiresIn;
    }

    public Date getExpiresOnDate() {
        if (expiresOn != null) {
            return (Date)expiresOn.clone();
        } else {
            return null;
        }
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isUserIdDisplayble() {
        return this.isDisplaybaleUserId;
    }

    public boolean isMultipleResourceRefreshToken() {
        return isMultipleResourceRefreshToken;
    }
}
