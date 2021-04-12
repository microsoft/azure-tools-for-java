/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration;

import java.util.List;
import java.util.Map;

import com.microsoft.azure.oidc.common.algorithm.Algorithm;
import com.microsoft.azure.oidc.common.issuer.Issuer;
import com.microsoft.azure.oidc.common.name.Name;
import com.microsoft.azure.oidc.configuration.endpoint.EndPoint;
import com.microsoft.azure.oidc.configuration.key.Key;

public interface ConfigurationFactory {

    Configuration createConfiguration(List<Algorithm> algorithms, Map<Name, Key> keys, Issuer issuer,
            EndPoint authenticationEndPoint, EndPoint logoutEndPoint);

}
