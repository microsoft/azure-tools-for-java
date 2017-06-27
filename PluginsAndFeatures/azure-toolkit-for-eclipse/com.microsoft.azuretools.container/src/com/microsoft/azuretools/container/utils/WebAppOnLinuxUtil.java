package com.microsoft.azuretools.container.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.CanceledByUserException;

public class WebAppOnLinuxUtil {
	private static Map<String, String> defaultAppSettings = new HashMap<>();
	private static Region defaultRegion = Region.US_WEST;
	private static PricingTier defaultPricingTier = PricingTier.STANDARD_S1;

	public static WebApp deploy(String subscriptionId, String resourceGroup, String appName, boolean createNewRg)
			throws IOException {
		PrivateRegistry pr = new PrivateRegistry(DockerRuntime.getInstance().getRegistryUrl(),
				DockerRuntime.getInstance().getRegistryUsername(), DockerRuntime.getInstance().getRegistryPassword());
		String imageName = DockerRuntime.getInstance().getLatestImageName();
		return deploy(subscriptionId, resourceGroup, appName, pr, imageName, createNewRg);
	}

	public static WebApp deploy(String subscriptionId, String resourceGroup, String appName, PrivateRegistry pr,
			String imageName, boolean createNewRg) throws IOException {
		AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
		// not signed in
		if (azureManager == null) {
			return null;
		}
		Azure azure = azureManager.getAzure(subscriptionId);

		WebApp app = null;
		/**
		 * java.util.NoSuchElementException: Sequence contains no elements
		 * occurs when no available WebApp
		 */
		try {
			app = azure.webApps().getByResourceGroup(resourceGroup, appName);
		} catch (NoSuchElementException e) {
			// DO NOTHING
		}

		if (app == null) {
			// create new app
			if (createNewRg) {
				app = createAppWithNewResourceGroup(azure, appName, resourceGroup, pr, imageName);
			} else {
				app = createAppWithExisitingResourceGroup(azure, appName, resourceGroup, pr, imageName);
			}
		} else {
			// update existing app
			app.update().withPrivateRegistryImage(String.format("%s/%s", pr.getUrl(), imageName), pr.getUrl())
					.withCredentials(pr.getUsername(), pr.getPassword()).withStartUpCommand(pr.getStartupFile())
					.withAppSettings(defaultAppSettings).apply();
		}
		return app;
	}

	public static List<SiteInner> listAllWebAppOnLinux() {
		List<SiteInner> wal = new ArrayList<SiteInner>();
		try {
			AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
			// not signed in
			if (azureManager == null) {
				return wal;
			}
			AzureModelController.updateSubscriptionMaps(null);

			for (Subscription sb : azureManager.getSubscriptions()) {
				Azure azure = azureManager.getAzure(sb.subscriptionId());
				for (ResourceGroup rg : azure.resourceGroups().list()) {
					for (SiteInner si : azure.webApps().inner().listByResourceGroup(rg.name())) {
						if (si.kind().equals("app;linux")) {
							wal.add(si);
						}
					}
				}
			}
		} catch (IOException | CanceledByUserException e) {
			e.printStackTrace();
		}
		return wal;
	}

	private static WebApp createAppWithExisitingResourceGroup(Azure azure, String appName, String resourceGroup,
			PrivateRegistry pr, String imageName) {
		return azure.webApps().define(appName).withRegion(defaultRegion).withExistingResourceGroup(resourceGroup)
				.withNewLinuxPlan(defaultPricingTier)
				.withPrivateRegistryImage(String.format("%s/%s", pr.getUrl(), imageName), pr.getUrl())
				.withCredentials(pr.getUsername(), pr.getPassword()).withStartUpCommand(pr.getStartupFile())
				.withAppSettings(defaultAppSettings).create();

	}

	private static WebApp createAppWithNewResourceGroup(Azure azure, String appName, String resourceGroup,
			PrivateRegistry pr, String imageName) {
		return azure.webApps().define(appName).withRegion(defaultRegion).withNewResourceGroup(resourceGroup)
				.withNewLinuxPlan(defaultPricingTier)
				.withPrivateRegistryImage(String.format("%s/%s", pr.getUrl(), imageName), pr.getUrl())
				.withCredentials(pr.getUsername(), pr.getPassword()).withStartUpCommand(pr.getStartupFile())
				.withAppSettings(defaultAppSettings).create();
	}

}
