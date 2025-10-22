package com.microsoft.azuretools.azuremcp;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.ui.extensions.IMcpRegistrationProvider;

public class GithubCopilotAzureMcpInitializer implements IMcpRegistrationProvider {
	
	private AzureMcpPackageManager azureMcpPackageManager;
	private static final String MCP_CONFIG_TEMPLATE = "{ \"servers\": { \"azuremcp\": { \"command\": \"%s\", \"args\": [server, start], \"description\": \"Azure MCP Server\" } } }";

	public GithubCopilotAzureMcpInitializer() {
		this.azureMcpPackageManager = new AzureMcpPackageManager();
	}
	
	@Override
    public CompletableFuture<String> getMcpServerConfigurations() {
        return CompletableFuture.supplyAsync(() -> getAzureMcpConfig());
    }

	private String getAzureMcpConfig() {
		File azureMcpExe = azureMcpPackageManager.getAzureMcpExecutable();
		
		if(azureMcpExe != null) {
			String mcpConfig = String.format(MCP_CONFIG_TEMPLATE, azureMcpExe.getAbsolutePath().replace("\\", "\\\\"));
			azureMcpPackageManager.cleanup();
			return mcpConfig;
		} else {
			return null;
		}
	}

}