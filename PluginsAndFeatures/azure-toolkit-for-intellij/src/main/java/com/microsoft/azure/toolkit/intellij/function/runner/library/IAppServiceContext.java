/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.library;

import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.toolkit.lib.common.IProject;

import java.util.Map;

public interface IAppServiceContext {
    String getDeploymentStagingDirectoryPath();

    String getSubscription();

    String getAppName();

    String getResourceGroup();

    RuntimeConfiguration getRuntime();

    String getRegion();

    String getPricingTier();

    String getAppServicePlanResourceGroup();

    String getAppServicePlanName();

    Map getAppSettings();

    String getDeploymentType();

    Azure getAzureClient();

    IProject getProject();
}
