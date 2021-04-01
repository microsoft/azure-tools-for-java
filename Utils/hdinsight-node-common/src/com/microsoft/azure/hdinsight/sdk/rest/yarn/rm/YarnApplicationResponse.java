/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

import java.util.List;
import java.util.Optional;

public class YarnApplicationResponse implements IConvertible {
    private YarnApplications apps;

    public YarnApplications getApps() {
        return apps;
    }

    public void setApps(YarnApplications apps) {
        this.apps = apps;
    }

    public static YarnApplicationResponse EMPTY = new YarnApplicationResponse();

    public Optional<List<App>> getAllApplication() {
        if(apps != null) {
            return Optional.ofNullable(apps.getApps());
        }
        return Optional.empty();
    }
}
