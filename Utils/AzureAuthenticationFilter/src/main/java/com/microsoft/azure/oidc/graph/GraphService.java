/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.graph;

import java.util.concurrent.Future;

public interface GraphService {

    Future<Boolean> isUserInRoleAsync(String userID, String role);

}
