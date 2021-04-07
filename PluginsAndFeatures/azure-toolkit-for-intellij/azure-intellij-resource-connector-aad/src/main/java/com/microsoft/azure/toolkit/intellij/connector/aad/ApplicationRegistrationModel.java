/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import java.util.Objects;

class ApplicationRegistrationModel {
    private String displayName;
    private String clientId;
    private String domain;
    private String callbackUrl;
    private boolean isMultiTenant;
    private boolean allowOverwrite;

    String getDisplayName() {
        return displayName;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    String getClientId() {
        return clientId;
    }

    void setClientId(String clientId) {
        this.clientId = clientId;
    }

    String getDomain() {
        return domain;
    }

    void setDomain(String domain) {
        this.domain = domain;
    }

    String getCallbackUrl() {
        return callbackUrl;
    }

    void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    boolean isMultiTenant() {
        return isMultiTenant;
    }

    void setMultiTenant(boolean multiTenant) {
        isMultiTenant = multiTenant;
    }

    boolean getAllowOverwrite() {
        return allowOverwrite;
    }

    void setAllowOverwrite(boolean allowOverwrite) {
        this.allowOverwrite = allowOverwrite;
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, clientId, domain, callbackUrl, isMultiTenant, allowOverwrite);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationRegistrationModel model = (ApplicationRegistrationModel) o;
        return isMultiTenant == model.isMultiTenant &&
                Objects.equals(displayName, model.displayName) &&
                Objects.equals(clientId, model.clientId) &&
                Objects.equals(domain, model.domain) &&
                Objects.equals(callbackUrl, model.callbackUrl) &&
                Objects.equals(allowOverwrite, model.allowOverwrite);
    }

    @Override
    public String toString() {
        return "ApplicationRegistrationModel{" +
                "displayName='" + displayName + '\'' +
                ", clientId='" + clientId + '\'' +
                ", domain='" + domain + '\'' +
                ", callbackUrl='" + callbackUrl + '\'' +
                ", isMultiTenant=" + isMultiTenant +
                ", allowOverwrite=" + allowOverwrite +
                '}';
    }
}
