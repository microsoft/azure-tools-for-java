package com.microsoft.azuretools.azuremcp;

import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.ui.extensions.IMcpRegistrationProvider;

public class GithubCopilotAzureMcpInitializer implements IMcpRegistrationProvider {
	
	@Override
    public CompletableFuture<String> getMcpServerConfigurations() {
        return CompletableFuture.supplyAsync(() -> "{\"servers\": {\"memory\": {\"command\": \"npx\", \"args\": [\"-y\",\"@modelcontextprotocol/server-memory\"]}}}");
    }

}
