/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.state;

public interface StateFactory {

    State createState(String usereID, String sessionName, String requestURI);

}
