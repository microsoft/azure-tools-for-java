/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.launch.local;

import com.microsoft.azure.toolkit.eclipse.common.launch.AzureLaunchConfigurationTabGroup;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionLocalRunConfiguration;
import org.eclipse.swt.SWT;

public class AzureFunctionLocalRunTabGroup extends AzureLaunchConfigurationTabGroup {
    public AzureFunctionLocalRunTabGroup() {
        super("Run Azure Function", (parent) -> new FunctionLocalRunPanel(parent, SWT.NONE), FunctionLocalRunConfiguration.class);
    }
}