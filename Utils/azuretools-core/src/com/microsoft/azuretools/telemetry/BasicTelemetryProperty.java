/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetry;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public interface BasicTelemetryProperty {
    @NotNull
    String getServiceName();
}
