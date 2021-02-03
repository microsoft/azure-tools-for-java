/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.state;

public interface State {

    public String getUserID();

    public String getSessionName();

    public String getRequestURI();

    boolean equals(Object object);

    int hashCode();

}
