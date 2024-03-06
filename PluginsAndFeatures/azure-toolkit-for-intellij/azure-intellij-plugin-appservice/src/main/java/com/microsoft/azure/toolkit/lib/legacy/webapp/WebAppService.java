/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.webapp;

import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;

import java.util.HashMap;
import java.util.Map;

public class WebAppService {
	private static final WebAppService instance = new WebAppService();

	public static WebAppService getInstance() {
		return WebAppService.instance;
	}

	public WebAppBase<?, ?, ?> createWebApp(final AppServiceConfig config) {
		final Map<String, String> properties = new HashMap<>();
		final Operation operation = TelemetryManager.createOperation("webapp", "create-webapp");
		try {
			operation.start();
			operation.trackProperties(properties);
			final CreateOrUpdateWebAppTask task = new CreateOrUpdateWebAppTask(config);
			final WebAppBase<?, ?, ?> result = task.execute();
			if (result instanceof AzResource.Draft<?, ?> draft) {
				draft.reset(); // todo: reset draft after create web app
			}
			return result;
		} catch (final RuntimeException e) {
			EventUtil.logError(operation, ErrorType.userError, e, properties, null);
			throw e;
		} finally {
			operation.complete();
		}
	}
}
