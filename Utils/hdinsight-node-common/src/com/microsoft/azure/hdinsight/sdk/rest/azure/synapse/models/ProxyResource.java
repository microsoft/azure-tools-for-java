/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The resource model definition for a ARM proxy resource. It will have everything other than required location and
 * tags.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyResource extends com.microsoft.azure.ProxyResource {
}
