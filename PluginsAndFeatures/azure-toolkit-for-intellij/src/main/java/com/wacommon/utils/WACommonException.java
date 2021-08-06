/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.wacommon.utils;

public class WACommonException extends Exception {

    private static final long serialVersionUID = 6280101034357272719L;

    public WACommonException(String message) {
        super(message);
    }

    public WACommonException() {
        super();
    }

    public WACommonException(String msg, Exception excep) {
        super(msg, excep);
    }
}
