/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.state.impl;

import com.microsoft.azure.oidc.common.state.State;
import com.microsoft.azure.oidc.exception.PreconditionException;

public final class SimpleState implements State {
    private String userID;
    private String sessionName;
    private String requestURI;

    public SimpleState(final String userID, final String sessionName, final String requestURI) {
        if(userID == null || sessionName == null || requestURI == null) {
            throw new PreconditionException("Required parameter is null");
        }
        this.userID = userID;
        this.sessionName = sessionName;
        this.requestURI = requestURI;
    }

    @Override
    public String getUserID() {
        return userID;
    }

    @Override
    public String getSessionName() {
        return sessionName;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((requestURI == null) ? 0 : requestURI.hashCode());
        result = prime * result + ((sessionName == null) ? 0 : sessionName.hashCode());
        result = prime * result + ((userID == null) ? 0 : userID.hashCode());
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
        SimpleState other = (SimpleState) obj;
        if (requestURI == null) {
            if (other.requestURI != null)
                return false;
        } else if (!requestURI.equals(other.requestURI))
            return false;
        if (sessionName == null) {
            if (other.sessionName != null)
                return false;
        } else if (!sessionName.equals(other.sessionName))
            return false;
        if (userID == null) {
            if (other.userID != null)
                return false;
        } else if (!userID.equals(other.userID))
            return false;
        return true;
    }
}
