/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

public enum AuthTypeOptions {
    HDICluster(new AuthType[]{
            AuthType.BasicAuth,
            AuthType.AADAuth
    }),

    LivyCluster(new AuthType[]{
            AuthType.BasicAuth,
            AuthType.NoAuth

    });

    private AuthType[] optionTypes;

    AuthTypeOptions(AuthType[] optionTypes) {
        this.optionTypes = optionTypes;
    }

    public AuthType[] getOptionTypes() {
        return this.optionTypes;
    }
}
