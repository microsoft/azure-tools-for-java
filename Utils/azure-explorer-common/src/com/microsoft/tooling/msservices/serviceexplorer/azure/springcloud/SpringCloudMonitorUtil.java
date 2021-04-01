/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.common.utils.SneakyThrowUtils;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpringCloudMonitorUtil {
    private static final Logger LOGGER = Logger.getLogger(SpringCloudMonitorUtil.class.getName());

    public static void awaitAndMonitoringStatus(String appId, DeploymentResourceStatus originalStatus) {
        String clusterId = getParentSegment(appId);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(() -> {
            DeploymentResourceStatus status = null;
            do {
                try {
                    AppResourceInner app = AzureSpringCloudMvpModel.getAppById(appId);
                    if (app == null) {
                        SpringCloudStateManager.INSTANCE.notifySpringAppDelete(clusterId, appId);
                        return;
                    }
                    DeploymentResourceInner deployment = AzureSpringCloudMvpModel.getActiveDeploymentForApp(appId);
                    SpringCloudStateManager.INSTANCE.notifySpringAppUpdate(clusterId, app, deployment);
                    if (deployment == null) {
                        return;
                    }

                    status = deployment.properties().status();
                    SpringCloudStateManager.INSTANCE.notifySpringAppUpdate(clusterId, app, deployment);
                    Thread.sleep(1000 * 5);
                } catch (InterruptedException e) {
                    SneakyThrowUtils.sneakyThrow(e);
                }

            } while (originalStatus == status);
        });
        try {
            future.get(180, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to get the spring cloud app status for app: %s.", SpringCloudIdHelper
                    .getAppName(appId)), e);
        }
    }

    private static String getParentSegment(String id) {
        if (StringUtils.isEmpty(id)) {
            return id;
        }
        final String[] attributes = id.split("/");
        return StringUtils.join(ArrayUtils.subarray(attributes, 0, attributes.length - 2), "/");
    }
}
