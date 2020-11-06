/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.enums;

/**
 * Enums of backend errors for azure tools.
 */
public enum ErrorEnum {
    UNKNOWN_HOST_EXCEPTION(100000, "Encountered an unknown host exception.",
            "It seems you have an unstable network at the moment, please try again when network is available."),
    INVALID_AUTHENTICATION(100401, "Invalid authentication",
            "Authentication token invalid, sign in again or run \"az login\" if using Azure CLI credential"),
    SOCKET_TIMEOUT_EXCEPTION(100002, "Encountered a socket timeout exception.",
            "Timeout when accessing azure, please try your operation again."),
    FAILED_TO_GET_ACCESS_TOKEN(100003, "Failed to get access token by Azure CLI command.",
                               "Failed to get access token, please try to login Azure CLI using 'az login' and try again."),
    INVALID_SUBSCRIPTION_CACHE(100004, "Invalid subscription",
            "It seems local cache of subscription is expired, please try re-login"),
    ;

    private int errorCode;
    private String errorMessage;
    private String displayMessage;

    ErrorEnum(int errorCode, String errorMessage, String displayMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.displayMessage = displayMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public static String getDisplayMessageByCode(int code) {
        for (ErrorEnum e : ErrorEnum.values()) {
            if (e.getErrorCode() == code) {
                return e.getDisplayMessage();
            }
        }
        throw new IllegalArgumentException(String.format("Not found enum for code: %s", code));
    }
}
