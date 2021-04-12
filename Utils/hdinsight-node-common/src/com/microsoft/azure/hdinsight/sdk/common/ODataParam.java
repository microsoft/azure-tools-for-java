/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.message.BasicNameValuePair;

public class ODataParam extends BasicNameValuePair {
    public static ODataParam filter(@NotNull String value) {
        return new ODataParam("$filter", value);
    }

    public static ODataParam orderby(String value) {
        return new ODataParam("$orderby", value);
    }

    private ODataParam(@NotNull String name, @NotNull String value) {
        super(name, value);
    }
}
