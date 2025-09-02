package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class McpServer {
    @JsonProperty("command")
    private String command;
    @JsonProperty("args")
    private List<String> args;
    @JsonProperty("env")
    private Map<String, String> env;
    @JsonProperty("description")
    private String description;
}
