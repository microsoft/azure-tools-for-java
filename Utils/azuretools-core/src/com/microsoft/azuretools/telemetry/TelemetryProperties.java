/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetry;

import java.util.Map;

/*
Implement this interface in case you want to put something in telemetry as properties.
Sub classes of AzureDialogWrapper, AzureAnAction, Node, WizardModel don't need to do extra work except the implementation of this interface.
While for other classes, you have to call the toProperties method and add the returned Map to telemetry manually even after you implement this interface.
 */
public interface TelemetryProperties {
    Map<String, String> toProperties();
}
