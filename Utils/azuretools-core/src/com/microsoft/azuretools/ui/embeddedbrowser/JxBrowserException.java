/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.ui.embeddedbrowser;

/**
 * JxBrowser related functions caught exception and re-throw this one
 */
public class JxBrowserException extends Exception {
    public JxBrowserException(String message) {
        super(message);
    }
}
