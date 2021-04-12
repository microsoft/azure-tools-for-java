/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.exception;

import com.microsoft.azuretools.enums.ErrorEnum;

/**
 * RuntinmeException for azure tools.
 */
public class AzureRuntimeException extends RuntimeException {

    private int code;

    public AzureRuntimeException(ErrorEnum errorEnum) {
        super(errorEnum.getErrorMessage());
        this.code = errorEnum.getErrorCode();
    }

    public AzureRuntimeException(int code) {
        this.code = code;
    }

    public AzureRuntimeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AzureRuntimeException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
