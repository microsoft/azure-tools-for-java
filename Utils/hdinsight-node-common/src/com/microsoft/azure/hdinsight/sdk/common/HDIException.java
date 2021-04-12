/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import java.io.PrintWriter;
import java.io.StringWriter;

public class HDIException extends Exception {
    private String mErrorLog;
    private int errorCode;

    public HDIException(String message) {
        super(message);

        mErrorLog = "";
    }

    public HDIException(String message, int errorCode){
        super(message);
        this.errorCode = errorCode;
    }

    public HDIException(String message, String errorLog) {
        super(message);

        mErrorLog = errorLog;
    }

    public HDIException(String message, Throwable throwable) {
        super(message, throwable);

        if (throwable instanceof HDIException) {
            mErrorLog = ((HDIException) throwable).getErrorLog();
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);

            throwable.printStackTrace(writer);
            writer.flush();

            mErrorLog = sw.toString();
        }
    }

    public String getErrorLog() {
        return mErrorLog;
    }

    public int getErrorCode(){
        return errorCode;
    }
}
