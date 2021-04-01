/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.deploy;

import java.util.EventListener;

public interface UploadProgressEventListener extends EventListener {

    public abstract void onUploadProgress(UploadProgressEventArgs args);

}
