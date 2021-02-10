/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import java.io.IOException;

/**
 * Created by vlashch on 4/7/2017.
 */
public class AuthCanceledException extends IOException {
    private static final long serialVersionUID = 1L;

    public AuthCanceledException() {

    }
    public AuthCanceledException(String desc) {
        super(desc );
    }

}
