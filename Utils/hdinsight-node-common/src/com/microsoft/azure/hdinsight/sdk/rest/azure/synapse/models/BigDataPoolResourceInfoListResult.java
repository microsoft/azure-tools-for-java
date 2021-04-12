/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BigDataPoolResourceInfoListResult extends PageImpl<BigDataPoolResourceInfo> implements IConvertible {
}
