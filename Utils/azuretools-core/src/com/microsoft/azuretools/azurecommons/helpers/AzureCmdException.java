/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azurecommons.helpers;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AzureCmdException extends Exception {
    private String mErrorLog;

    public AzureCmdException(String message) {
        super(message);
        mErrorLog = "";
    }

    public AzureCmdException(String message, String errorLog) {
        super(message);
        mErrorLog = errorLog;
    }

    public AzureCmdException(String message, Throwable throwable) {
        super(message, throwable);
        if (throwable instanceof AzureCmdException) {
            mErrorLog = ((AzureCmdException) throwable).getErrorLog();
        } else {
            final StringWriter sw = new StringWriter();
            final PrintWriter writer = new PrintWriter(sw);
            try {
                throwable.printStackTrace(writer);
                writer.flush();
                mErrorLog = sw.toString();
            }
            finally {
                // closing the wrapping object
                writer.close();
            }
        }
    }

    public String getErrorLog() {
        return mErrorLog;
    }
}
