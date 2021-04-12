/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import java.util.List;

public class AppAttemptsResponse {
    public class AppAttempts {
        public List<AppAttempt> appAttempt;
    }

    private AppAttempts appAttempts;

    public AppAttempts getAppAttempts() {
        return appAttempts;
    }

    public void setAppAttempts(AppAttempts appAttempts) {
        this.appAttempts = appAttempts;
    }
}
