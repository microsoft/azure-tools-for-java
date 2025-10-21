package com.microsoft.azuretools.azuremcp;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.ui.extensions.IMcpRegistrationProvider;

public class GithubCopilotAzureMcpInitializer implements IMcpRegistrationProvider {
	
	private AzureMcpPackageManager azureMcpPackageManager;

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
			String mcpConfig = "{ \"servers\": { \"azuremcp\": { \"command\": \"" + azureMcpExe.getAbsolutePath().replace("\\", "\\\\") + "\", \"args\": [server, start], \"description\": \"Azure MCP Server\" } } }";
			return mcpConfig;
		} else {
			return null;
		}
	}

}
