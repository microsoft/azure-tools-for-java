/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest;

import java.util.Optional;

public interface IConvertible {
    // serialize an object to xml-format string
    default Optional<String> convertToXml() {
        return ObjectConvertUtils.convertObjectToJsonString(this);
    }

    // serialize an object to json-format string
    default Optional<String> convertToJson() {
        return ObjectConvertUtils.convertObjectToJsonString(this);
    }
}
