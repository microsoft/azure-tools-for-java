/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.message.BasicNameValuePair;

public class ApiVersionParam extends BasicNameValuePair {
    public static final String NAME = "api-version";

    /**
     * Default Constructor taking a name and a value. The value may be null.
     *
     * @param value The value.
     */
    public ApiVersionParam(@NotNull String value) {
        super(NAME, value);
    }
}
