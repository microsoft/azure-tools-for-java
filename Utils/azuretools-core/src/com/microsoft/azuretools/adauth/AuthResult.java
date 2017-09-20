package com.microsoft.azuretools.adauth;

import java.util.Date;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class AuthResult {
    private final String accessTokenType;
    private final long expiresIn;
    private final Date expiresOn;
    private final String idToken;
    private final UserInfo userInfo;
    private final String userId;
    private final boolean isDisplaybaleUserId;
    private final String accessToken;
    private String refreshToken;
    private final boolean isMultipleResourceRefreshToken;
    private String resource;

    public AuthResult(final String accessTokenType,
            final String accessToken, final String refreshToken,
            final long expiresIn, final String idToken,
            final UserInfo userInfo,
            final String resource) {
        this.accessTokenType = accessTokenType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;

        Date now = new Date();
        now.setTime(now.getTime() + (expiresIn * 1000));
        this.expiresOn = now;

        this.idToken = idToken;
        this.userInfo = userInfo;
        if (userInfo != null) {
            if (null != userInfo.getDisplayableId()) {
                this.userId = userInfo.getDisplayableId();
                this.isDisplaybaleUserId = true;
            } else {
                this.userId = userInfo.getUniqueId() != null ? userInfo.getUniqueId() : "";
                this.isDisplaybaleUserId = false;
            }
        } else {
            this.userId = "";
            this.isDisplaybaleUserId = false;
        }
        this.resource = (resource != null ? resource : "");
        this.isMultipleResourceRefreshToken = !StringUtils.isNullOrEmpty(resource);
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

    public String getIdToken() {
        return idToken;
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
