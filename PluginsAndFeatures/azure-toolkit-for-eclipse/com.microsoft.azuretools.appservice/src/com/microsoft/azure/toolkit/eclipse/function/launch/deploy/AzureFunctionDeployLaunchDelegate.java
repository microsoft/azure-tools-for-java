/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.launch.deploy;

import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public class AzureFunctionDeployLaunchDelegate extends LaunchConfigurationDelegate {
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        //TODO(andxu) start the deploy process
        AzureMessager.getMessager().info("Deploy is not supported by now");
    }
}
