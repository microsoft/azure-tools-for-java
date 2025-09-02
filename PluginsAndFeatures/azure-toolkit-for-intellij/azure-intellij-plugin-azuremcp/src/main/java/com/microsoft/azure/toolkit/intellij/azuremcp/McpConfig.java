package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class McpConfig {
    @JsonProperty("servers")
    private Map<String, McpServer> servers;
}
