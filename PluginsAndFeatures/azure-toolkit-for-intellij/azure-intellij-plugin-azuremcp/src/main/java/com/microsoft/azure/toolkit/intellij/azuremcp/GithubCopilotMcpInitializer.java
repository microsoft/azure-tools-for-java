package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.toolkit.intellij.azuremcp.AzureMcpUtils.logErrorTelemetryEvent;
import static com.microsoft.azure.toolkit.intellij.azuremcp.AzureMcpUtils.logTelemetryEvent;

@Slf4j
public class GithubCopilotMcpInitializer implements ProjectActivity, DumbAware, ProjectManagerListener {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final ComparableVersion LOWEST_SUPPORTED_COPILOT_VERSION = new ComparableVersion("1.5.50");
    private static final String COPILOT_PLUGIN_ID = "com.github.copilot";
    private final AzureMcpPackageManager azureMcpPackageManager;

    public GithubCopilotMcpInitializer() {
        this.azureMcpPackageManager = new AzureMcpPackageManager();
    }

    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {

        if (Registry.is("azure.mcp.ghcp.autoconfigure.disabled", false)) {
            return null;
        }

        logTelemetryEvent("azmcp-copilot-initialization-started");
        log.info("Running GitHub Copilot MCP initializer");
        try {
            if (isCopilotMcpSupported()) {
                initializeAzureMcpServer();
            }
            log.info("GitHub Copilot MCP initializer completed.");
        } catch (final Exception ex) {
            log.error("Error initializing Azure MCP Server: " + ex.getMessage(), ex);
            logErrorTelemetryEvent("azmcp-copilot-initialization-failed", ex);
        }
        return null;
    }

    private boolean isCopilotMcpSupported() {
        // Get all installed plugins
        final IdeaPluginDescriptor[] installedPlugins = PluginManagerCore.getPlugins();
        final boolean copilotMcpSupported = Arrays.stream(installedPlugins)
                .anyMatch(plugin -> {
                    final boolean copilotPluginInstalled = COPILOT_PLUGIN_ID.equals(plugin.getPluginId().getIdString());
                    return copilotPluginInstalled && isMcpSupported(plugin.getVersion());
                });
        log.info("GitHub Copilot MCP supported: " + copilotMcpSupported);
        logTelemetryEvent("azmcp-copilot-supported-" + (copilotMcpSupported ? "true" : "false"));
        return copilotMcpSupported;
    }

    private void initializeAzureMcpServer() throws Exception {
        log.info("Initializing Azure MCP Server");
        final File azMcpExe = azureMcpPackageManager.getAzureMcpExecutable();
        if (azMcpExe != null) {
            log.info("Azure MCP executable found at: " + azMcpExe.getAbsolutePath());
            configureMcpServer(azMcpExe);
            azureMcpPackageManager.cleanup();
        }
    }

    private void configureMcpServer(File azMcpExe) throws Exception {
        log.info("Initializing Azure MCP Server");
        final String mcpConfigLocation = getConfigPath().getAbsolutePath() + "/mcp.json";
        final Path mcpConfigPath = Path.of(mcpConfigLocation);
        final McpConfig mcpConfig;
        if (mcpConfigPath.toFile().exists()) {
            log.info("MCP configuration file already exists at: " + mcpConfigLocation);
            final String mcpContents = new String(Files.readAllBytes(mcpConfigPath));
            mcpConfig = OBJECT_MAPPER.readValue(mcpContents, McpConfig.class);
        } else {
            // Generally, GitHub Copilot creates the mcp.json file. However, there is a possiblility that it's deleted.
            log.info("Creating MCP configuration directory: " + mcpConfigLocation);
            Files.createDirectories(mcpConfigPath.getParent());
            mcpConfig = new McpConfig();
        }
        Map<String, McpServer> servers = mcpConfig.getServers();
        if (servers == null) {
            servers = new HashMap<>();
            mcpConfig.setServers(servers);
        }

        final McpServer azureMcpServer = new McpServer();
        azureMcpServer.setCommand(azMcpExe.getAbsolutePath());
        azureMcpServer.setArgs(Arrays.asList("server", "start"));
        if (servers.containsKey("Azure MCP Server")) {
            servers.remove("Azure MCP Server");
        }
        servers.put("Azure MCP Server IntelliJ", azureMcpServer);
        Files.writeString(mcpConfigPath, OBJECT_MAPPER.writeValueAsString(mcpConfig));
        logTelemetryEvent("azmcp-copilot-initialization-success");
    }

    private boolean isMcpSupported(String version) {
        log.info("GitHub Copilot plugin version is: " + version);
        if (version == null) {
            return false;
        }
        final ComparableVersion installedVersion = new ComparableVersion(version);
        return LOWEST_SUPPORTED_COPILOT_VERSION.compareTo(installedVersion) <= 0;
    }

    /**
     * This is the same logic GitHub Copilot uses to determine the config path.
     *
     * @return the directory where the config file should be stored
     */
    private File getConfigPath() {
        final String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        final String userHome = System.getProperty("user.home");
        final File configPath;
        if (xdgConfigHome != null && (new File(xdgConfigHome)).isAbsolute()) {
            configPath = new File(xdgConfigHome, "github-copilot");
        } else if (SystemInfo.isWindows) {
            final String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                configPath = new File(localAppData, "github-copilot/intellij");
            } else {
                String userProfileLocation = System.getenv("USERPROFILE");
                if (userProfileLocation == null) {
                    userProfileLocation = userHome;
                }
                configPath = new File(userProfileLocation, "AppData/Local/github-copilot/intellij");
            }
        } else {
            configPath = new File(userHome, ".config/github-copilot/intellij");
        }
        return configPath;
    }
}
