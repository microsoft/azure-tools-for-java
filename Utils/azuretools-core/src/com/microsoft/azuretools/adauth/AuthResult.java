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
