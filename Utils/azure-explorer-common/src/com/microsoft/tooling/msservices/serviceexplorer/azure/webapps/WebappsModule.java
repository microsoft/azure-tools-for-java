/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.serviceexplorer.azure.webapps;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.azuretools.utils.WebAppUtils.WebAppDetails;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;
import java.util.Map;

public class WebappsModule extends AzureRefreshableNode {
	private static final String WEBAPPS_MODULE_ID = WebappsModule.class.getName();
	private static final String WEB_RUN_ICON = "WebAppRunning_16.png";
	private static final String WEB_STOP_ICON = "WebAppStopped_16.png";
	private static final String WEB_APP_ICON = "WebApp_16.png";
	private static final String BASE_MODULE_NAME = "Web Apps (Deprecated)";
	private static final String RUN_STATUS = "Running";

	public WebappsModule(Node parent) {
		super(WEBAPPS_MODULE_ID, BASE_MODULE_NAME, parent, WEB_APP_ICON);
		createListener();
	}

	@Override
	protected void refreshItems() throws AzureCmdException {
		if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
			try {
				AzureModelController.updateResourceGroupMaps(null);
			} catch (Exception ex) {
				DefaultLoader.getUIHelper().logError("Error updating webapps cache", ex);
			}
			DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
				@Override
				public void run() {
					fillWebappsNodes();
				}
			});
		} else {
			fillWebappsNodes();
		}
	}

	@Override
	protected void refreshFromAzure() throws Exception {
		AzureModelController.updateResourceGroupMaps(null);
	}

	private void fillWebappsNodes() {
		Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
		Map<ResourceGroup, List<WebApp>> rgwaMap = AzureModel.getInstance().getResourceGroupToWebAppMap();
		if (srgMap != null) {
			for (SubscriptionDetail sd : srgMap.keySet()) {
				if (!sd.isSelected()) continue;

				for (ResourceGroup rg : srgMap.get(sd)) {
					for (WebApp webApp : rgwaMap.get(rg)) {
						addChildNode(new WebappNode(this, webApp, rg,
								RUN_STATUS.equalsIgnoreCase(webApp.inner().state()) ? WEB_RUN_ICON : WEB_STOP_ICON));
					}
				}
			}
		}
	}

	private void createListener() {
		String id = "WebappsModule";
		AzureUIRefreshListener listener = new AzureUIRefreshListener() {
			@Override
			public void run() {
				if (event.opsType == AzureUIRefreshEvent.EventType.SIGNIN || event.opsType == AzureUIRefreshEvent.EventType.SIGNOUT) {
					removeAllChildNodes();
				} else if (event.object == null &&
						(event.opsType == AzureUIRefreshEvent.EventType.UPDATE || event.opsType == AzureUIRefreshEvent.EventType.REMOVE)) {
					if (hasChildNodes()) {
						load(true);
					}
				} else if (event.object != null && event.object.getClass().toString().equals(WebAppDetails.class.toString())) {
					WebAppDetails webAppDetails = (WebAppDetails) event.object;
					switch (event.opsType) {
						case ADD:
							DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
								@Override
								public void run() {
									try {
										addChildNode(new WebappNode(WebappsModule.this, webAppDetails.webApp, webAppDetails.resourceGroup,
												RUN_STATUS.equalsIgnoreCase(webAppDetails.webApp.inner().state()) ? WEB_RUN_ICON : WEB_STOP_ICON));
									} catch (Exception ex) {
										DefaultLoader.getUIHelper().logError("WebappsModule::createListener ADD", ex);
										ex.printStackTrace();
									}
								}
							});
							break;
						case UPDATE:
							break;
						case REMOVE:
							break;
						case REFRESH:
						    load(true);
						    break;
						default:
							break;
					}
				}
			}
		};
		AzureUIRefreshCore.addListener(id, listener);
	}

}