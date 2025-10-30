package com.microsoft.azuretools.azuremcp;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.ILog;

import com.microsoft.copilot.eclipse.ui.extensions.IMcpRegistrationProvider;

public class GithubCopilotAzureMcpInitializer implements IMcpRegistrationProvider {
	
    private static final ILog log = ILog.of(GithubCopilotAzureMcpInitializer.class);

	private AzureMcpPackageManager azureMcpPackageManager;
	private static final String MCP_CONFIG_TEMPLATE = "{ \"servers\": { \"azuremcp\": { \"command\": \"%s\", \"args\": [server, start], \"description\": \"Azure MCP Server\" } } }";

	public GithubCopilotAzureMcpInitializer() {
		this.azureMcpPackageManager = new AzureMcpPackageManager();
		log.info("Azure MCP Initializer created");
	}
	
	@Override
    public CompletableFuture<String> getMcpServerConfigurations() {
		log.info("getMcpServerConfigurations invoked");
        return CompletableFuture.supplyAsync(() -> getAzureMcpConfig());
    }

	private String getAzureMcpConfig() {
		try {
			File azureMcpExe = azureMcpPackageManager.getAzureMcpExecutable();
			if(azureMcpExe != null) {
				log.info("Azure MCP executable available");
	
				String mcpConfig = String.format(MCP_CONFIG_TEMPLATE, azureMcpExe.getAbsolutePath().replace("\\", "\\\\"));
				azureMcpPackageManager.cleanup();
				return mcpConfig;
			} else {
				log.info("Azure MCP executable not available");
				return null;
			}
		} catch (Exception e) {
			log.info("Error while getting Azure MCP configuration", e);
			return null;
		}
	}

}