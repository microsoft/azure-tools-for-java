/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.net.URI;
import java.net.URISyntaxException;

public class UriUtil {
    public static URI normalizeWithSlashEnding(final @NotNull URI src) {
        try {
            return src.getPath().endsWith("/")
                    ? src
                    : new URI(
                            src.getScheme(),
                            src.getAuthority(),
                            src.getPath() + "/",
                            src.getQuery(),
                            src.getFragment());
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }
}
