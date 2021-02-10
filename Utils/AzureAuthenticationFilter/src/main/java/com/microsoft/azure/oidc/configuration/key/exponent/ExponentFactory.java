/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.key.exponent;

public interface ExponentFactory {

    Exponent createKeyExponent(String value);

}
