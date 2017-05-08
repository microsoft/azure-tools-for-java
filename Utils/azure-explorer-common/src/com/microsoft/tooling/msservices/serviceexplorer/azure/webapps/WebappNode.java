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
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebappNode extends Node implements TelemetryProperties {
	private static final String ACTION_START = "Start";
	private static final String ACTION_STOP = "Stop";
	private static final String ACTION_RESTART = "Restart";
	private static final String WEB_RUN_ICON = "WebAppRunning_16.png";
	private static final String WEB_STOP_ICON = "WebAppStopped_16.png";
	private static final String RUN_STATUS = "Running";
	private WebApp webApp;
	private ResourceGroup resourceGroup;
	private final String subscriptionId;

	public WebappNode(WebappsModule parent, WebApp webApp, ResourceGroup resourceGroup, String icon) {
		super(webApp.name(), webApp.name(), parent, icon, true);
		this.subscriptionId = ResourceId.fromString(webApp.id()).subscriptionId();
		this.webApp = webApp;
		this.resourceGroup = resourceGroup;

		loadActions();
	}

	public WebApp getWebApp() {
		return webApp;
	}

	@Override
	public List<NodeAction> getNodeActions() {
		boolean running = RUN_STATUS.equals(webApp.inner().state());
		getNodeActionByName(ACTION_START).setEnabled(!running);
		getNodeActionByName(ACTION_STOP).setEnabled(running);
		getNodeActionByName(ACTION_RESTART).setEnabled(running);

		return super.getNodeActions();
	}

	@Override
	protected void loadActions() {
		addAction(ACTION_STOP, WEB_STOP_ICON, new NodeActionListener() {
			@Override
			public void actionPerformed(NodeActionEvent e) {
				DefaultLoader.getIdeHelper().runInBackground(null, "Stopping Web App", false, true, "Stopping Web App...", new Runnable() {
					@Override
					public void run() {
						webApp.stop();
						setIconPath(WEB_STOP_ICON);
					}
				});
			}
		});
		addAction("Start", new NodeActionListener() {
			@Override
			public void actionPerformed(NodeActionEvent e) {
				DefaultLoader.getIdeHelper().runInBackground(null, "Starting Web App", false, true, "Starting Web App...", new Runnable() {
					@Override
					public void run() {
						webApp.start();
						setIconPath(WEB_RUN_ICON);
					}
				});
			}
		});
		addAction(ACTION_RESTART, new NodeActionListener() {
			@Override
			public void actionPerformed(NodeActionEvent e) {
				DefaultLoader.getIdeHelper().runInBackground(null, "Restarting Web App", false, true, "Restarting Web App...", new Runnable() {
					@Override
					public void run() {
						webApp.restart();
						setIconPath(WEB_RUN_ICON);
					}
				});
			}
		});
		addAction("Delete",new DeleteWebAppAction());
		super.loadActions();
	}

	@Override
	public Map<String, String> toProperties() {
		final Map<String, String> properties = new HashMap<>();
		properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
		properties.put(AppInsightsConstants.Region, this.webApp.regionName());
		return properties;
	}

	private class DeleteWebAppAction extends AzureNodeActionPromptListener {
		DeleteWebAppAction() {
			super(WebappNode.this,
					String.format("This operation will delete Web App %s.\nAre you sure you want to continue?", "", webApp.name()),
					"Deleting Web App");
		}

		@Override
		protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
			try {
				AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
				// not signed in
				if (azureManager == null) {
					return;
				}
				azureManager.getAzure(subscriptionId).webApps().deleteByResourceGroup(webApp.inner().resourceGroup(), webApp.name());
				AzureModelController.removeWebAppFromResourceGroup(resourceGroup, webApp);

				DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
					@Override
					public void run() {
						// instruct parent node to remove this node
						getParent().removeDirectChildNode(WebappNode.this);
					}
				});
			} catch (Exception ex) {
				DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete the Web App", ex,
						"Azure Services Explorer - Error Deleting Web App", false, true);
			}
		}

		@Override
		protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
		}
	}
}