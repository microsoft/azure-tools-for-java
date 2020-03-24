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

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class ApplicationKey {
    private final IClusterDetail clusterDetail;
    private final String appId;

    public ApplicationKey(@NotNull IClusterDetail clusterDetail, @NotNull String appId) {
        this.clusterDetail = clusterDetail;
        this.appId = appId;
    }

    public IClusterDetail getClusterDetails() {
        return clusterDetail;
    }

    public String getClusterConnString() {
        return getClusterDetails().getConnectionUrl();
    }

    public String getAppId() {
        return appId;
    }

    @Override
    public int hashCode() {
        return getClusterConnString().toLowerCase().hashCode() + getAppId().toLowerCase().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof ApplicationKey) {
            ApplicationKey that = (ApplicationKey)obj;
            return getClusterConnString().equalsIgnoreCase(that.getClusterConnString()) &&
                    getAppId().equalsIgnoreCase(that.getClusterConnString());
        }
        return false;
    }
}
