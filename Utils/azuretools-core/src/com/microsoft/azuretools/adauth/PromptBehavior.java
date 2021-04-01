/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

// TODO: remove later
public enum PromptBehavior {
    /// <summary>
    /// Acquire token will prompt the user for credentials only when necessary.  If a token
    /// that meets the requirements is already cached then the user will not be prompted.
    /// </summary>
    Auto,

    /// <summary>
    /// The user will be prompted for credentials even if there is a token that meets the requirements
    /// already in the cache.
    /// </summary>
    Always,

    /// <summary>
    /// The user will not be prompted for credentials.  If prompting is necessary then the AcquireToken request
    /// will fail.
    /// </summary>
    Never,

    /// <summary>
    /// Re-authorizes (through displaying webview) the resource usage, making sure that the resulting access
    /// token contains updated claims. If user logon cookies are available, the user will not be asked for
    /// credentials again and the logon dialog will dismiss automatically.
    /// </summary>
    RefreshSession
}
