/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import java.net.URI;

public interface IWebUi {
    String authenticate(URI requestUri, URI redirectUri);
    //Future<String> authenticateAsync(URI requestUri, URI redirectUri)throws IOException;
}
