/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.filter.configuration.algorithm;

import java.util.Map;

public interface AlgorithmConfiguration {

    Map<String, String> getAlgorithmMap();

    Map<String, String> getAlgorithmClassMap();

}
