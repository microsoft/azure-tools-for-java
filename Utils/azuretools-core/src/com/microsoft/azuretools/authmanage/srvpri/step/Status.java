/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.step;

/**
 * Created by vlashch on 10/24/16.
 */
public class Status {
    public String getAction() {
        return action;
    }

    public Result getResult() {
        return result;
    }

    public String getDetails() {
        return details;
    }

    private String action;
    private Result result;
    private String details;

    public Status(String action, Result result, String details) {
        this.action = action;
        this.result = result;
        this.details = details;
    }
    public static enum Result {
        SUCCESSFUL,
        FAILED,
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s", action, result, details);
    }
}
