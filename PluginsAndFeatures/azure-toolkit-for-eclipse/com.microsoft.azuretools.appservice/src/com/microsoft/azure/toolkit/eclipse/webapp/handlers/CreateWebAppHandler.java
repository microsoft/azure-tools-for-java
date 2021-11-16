/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp.handlers;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.eclipse.appservice.CreateWebAppDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

public class CreateWebAppHandler extends AbstractHandler {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    private static final int RG_NAME_MAX_LENGTH = 90;
    private static final int SP_NAME_MAX_LENGTH = 40;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell();
        CreateWebAppDialog createDialog = new CreateWebAppDialog(shell, getDefaultAppServiceConfig());
        createDialog.setOkActionListener(config -> {
            createDialog.close();
            AzureTaskManager.getInstance().runInBackground(AzureString.format("Creating web app %s", config.appName()),
                    () -> new CreateOrUpdateWebAppTask(config).execute());
        });
        createDialog.open();
        return null;
    }

    private AppServiceConfig getDefaultAppServiceConfig() {
        final AppServiceConfig result = new AppServiceConfig();
        final String appName = String.format("app-%s", DATE_FORMAT.format(new Date()));
        final String resourceGroupName = StringUtils.substring(String.format("rg-%s", appName), 0, RG_NAME_MAX_LENGTH);
        final String planName = StringUtils.substring(String.format("sp-%s", appName), 0, SP_NAME_MAX_LENGTH);
        result.appName(appName);
        result.resourceGroup(resourceGroupName);
        result.runtime(new RuntimeConfig().os(OperatingSystem.LINUX).javaVersion(JavaVersion.JAVA_8)
                .webContainer(WebContainer.TOMCAT_9));
        result.region(getDefaultRegion());
        result.pricingTier(PricingTier.BASIC_B2);
        result.servicePlanName(planName);
        result.servicePlanResourceGroup(resourceGroupName);
        return result;
    }

    public static Region getDefaultRegion() {
        final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
        if (environment == AzureEnvironment.AZURE) {
            return Region.US_WEST;
        } else if (environment == AzureEnvironment.AZURE_CHINA) {
            return Region.CHINA_NORTH2;
        } else {
            return Azure.az(AzureAccount.class).listRegions().stream().findFirst().orElse(null);
        }
    }
}
