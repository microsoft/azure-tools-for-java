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

package com.microsoft.azure.oidc.application.settings.impl;

import com.microsoft.azure.oidc.application.settings.ApplicationSettings;
import com.microsoft.azure.oidc.application.settings.Policy;
import com.microsoft.azure.oidc.application.settings.RedirectURL;
import com.microsoft.azure.oidc.application.settings.Secret;
import com.microsoft.azure.oidc.application.settings.Tenant;
import com.microsoft.azure.oidc.common.id.ID;
import com.microsoft.azure.oidc.common.id.IDFactory;
import com.microsoft.azure.oidc.common.id.impl.SimpleIDFactory;

final class SimpleApplicationSettings implements ApplicationSettings {
    private static final String TENANT = "AAD_TENANT";
    private static final String APPLICATION_ID = "AAD_APPLICATION_ID";
    private static final String APPLICATION_SECRET = "AAD_APPLICATION_SECRET";
    private static final String PRINCIPAL_ID = "AAD_PRINCIPAL_ID";
    private static final String PRINCIPAL_SECRET = "AAD_PRINCIPAL_SECRET";
    private static final String REDIRECT_URL = "AAD_REDIRECT_URL";
    private static final String OIDC_POLICY = "AAD_OIDC_POLICY";

    private final Tenant tenant = new SimpleTenant(System.getenv(SimpleApplicationSettings.TENANT));
    private volatile ID applicationId;
    private final Secret applicationSecret = new SimpleSecret(
            System.getenv(SimpleApplicationSettings.APPLICATION_SECRET) == null ? ""
                    : System.getenv(SimpleApplicationSettings.APPLICATION_SECRET));
    private volatile ID principalId;
    private final Secret principalSecret = new SimpleSecret(
            System.getenv(SimpleApplicationSettings.PRINCIPAL_SECRET) == null ? ""
                    : System.getenv(SimpleApplicationSettings.PRINCIPAL_SECRET));
    private final RedirectURL redirectURL = new SimpleRedirectURL(
            System.getenv(SimpleApplicationSettings.REDIRECT_URL));
    private final Policy oIDCPolicy = new SimplePolicy(System.getenv(SimpleApplicationSettings.OIDC_POLICY));

    private final Object Lock = new Object();

    private final IDFactory iDFactory = SimpleIDFactory.getInstance();

    @Override
    public Tenant getTenant() {
        return tenant;
    }

    @Override
    public ID getApplicationId() {
        if (applicationId == null) {
            synchronized (Lock) {
                if (applicationId == null) {
                    applicationId = iDFactory.createID(System.getenv(SimpleApplicationSettings.APPLICATION_ID));
                }
            }
        }
        return applicationId;
    }

    @Override
    public Secret getApplicationSecret() {
        return applicationSecret;
    }

    @Override
    public ID getPrincipalId() {
        if (principalId == null) {
            synchronized (Lock) {
                if (principalId == null) {
                    principalId = iDFactory.createID(System.getenv(SimpleApplicationSettings.PRINCIPAL_ID));
                }
            }
        }
        return principalId;
    }

    @Override
    public Secret getPrincipalSecret() {
        return principalSecret;
    }

    @Override
    public RedirectURL getRedirectURL() {
        return redirectURL;
    }

    @Override
    public Policy getOIDCPolicy() {
        return oIDCPolicy;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((applicationId == null) ? 0 : applicationId.hashCode());
        result = prime * result + ((applicationSecret == null) ? 0 : applicationSecret.hashCode());
        result = prime * result + ((oIDCPolicy == null) ? 0 : oIDCPolicy.hashCode());
        result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
        result = prime * result + ((principalSecret == null) ? 0 : principalSecret.hashCode());
        result = prime * result + ((redirectURL == null) ? 0 : redirectURL.hashCode());
        result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleApplicationSettings other = (SimpleApplicationSettings) obj;
        if (applicationId == null) {
            if (other.applicationId != null)
                return false;
        } else if (!applicationId.equals(other.applicationId))
            return false;
        if (applicationSecret == null) {
            if (other.applicationSecret != null)
                return false;
        } else if (!applicationSecret.equals(other.applicationSecret))
            return false;
        if (oIDCPolicy == null) {
            if (other.oIDCPolicy != null)
                return false;
        } else if (!oIDCPolicy.equals(other.oIDCPolicy))
            return false;
        if (principalId == null) {
            if (other.principalId != null)
                return false;
        } else if (!principalId.equals(other.principalId))
            return false;
        if (principalSecret == null) {
            if (other.principalSecret != null)
                return false;
        } else if (!principalSecret.equals(other.principalSecret))
            return false;
        if (redirectURL == null) {
            if (other.redirectURL != null)
                return false;
        } else if (!redirectURL.equals(other.redirectURL))
            return false;
        if (tenant == null) {
            if (other.tenant != null)
                return false;
        } else if (!tenant.equals(other.tenant))
            return false;
        return true;
    }
}
