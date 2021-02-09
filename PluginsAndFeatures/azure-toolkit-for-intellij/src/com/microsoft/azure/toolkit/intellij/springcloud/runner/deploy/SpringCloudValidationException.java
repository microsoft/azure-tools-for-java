/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.runner.deploy;

import com.microsoft.azure.common.exceptions.AzureExecutionException;

public class SpringCloudValidationException extends AzureExecutionException {
    public SpringCloudValidationException(final String errorMessage, final Throwable err) {
        super(errorMessage, err);
    }

    public SpringCloudValidationException(final String errorMessage) {
        super(errorMessage);
    }
}
