/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.vfs.VirtualFileSystem;
import com.microsoft.azure.hdinsight.common.logger.ILogger;

public abstract class AzureStorageVirtualFileSystem extends VirtualFileSystem implements ILogger {
    public enum VFSSupportStorageType {
        ADLSGen2
    }
}
