package com.microsoft.azure.toolkit.intellij.sjad.utils;

import com.google.gson.Gson;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AzdCliUtils {

    private static final Logger logger = Logger.getInstance(AzdCliUtils.class);

    private static final long CACHE_LIFETIME = 15 * 60 * 1000; // 15 minutes

    private static long lastCheckTime = 0;

    private static AzdVersion cachedAzdVersion = null;

    // Indicate if installed azd cli in this session.
    // If yes, consider azd already installed in current session.
    public static boolean azdCliInstallAttempted = false;

    // Indicate if set up azd env in this session.
    // If yes, always setup in current session.
    public static boolean azdEnvSetupNeeded = false;

    // Indicate if enable azd compose.
    // Ensure it executes only once in current session.
    public static boolean azdComposeEnabled = false;

    private static final String AZURE_DEV_CLI_PATH = "AZURE_DEV_CLI_PATH";

    public static CompletableFuture<Boolean> azdCliInstalledAsync() {
        if (azdCliInstallAttempted) {
            return CompletableFuture.completedFuture(true);
        }
        return getAzdCliVersionAsync().thenApply(azdVersion -> azdVersion != null);
    }

    private static CompletableFuture<AzdVersion> getAzdCliVersionAsync() {
        // Check if the cached result is still valid
        final long currentTime = System.currentTimeMillis();
        if (cachedAzdVersion != null && (currentTime - lastCheckTime) < CACHE_LIFETIME) {
            return CompletableFuture.completedFuture(cachedAzdVersion);
        }

        final GeneralCommandLine commandLine = new GeneralCommandLine()
                .withExePath("azd")
                .withParameters("version", "--output", "json");

        return runCommandAsync(commandLine)
                .thenApply(output -> {
                    if (output.getExitCode() == 0) {
                        final String stdout = output.getStdout();
                        final Gson gson = new Gson();
                        final AzdVersion azdVersion = gson.fromJson(stdout, AzdVersion.class);
                        // Cache the result
                        cachedAzdVersion = azdVersion;
                        lastCheckTime = currentTime;
                        return azdVersion;
                    } else {
                        logger.warn("Failed to check azd version. Exit code: " + output.getExitCode());
                        return null;
                    }
                }).exceptionally(ex -> {
                    logger.warn("Unexpected error while checking azd version", ex);
                    return null;
                });
    }

    public static CompletableFuture<ProcessOutput> runCommandAsync(GeneralCommandLine commandLine) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return runCommand(commandLine);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static ProcessOutput runCommand(GeneralCommandLine commandLine) throws Exception {
        final OSProcessHandler processHandler = new OSProcessHandler(commandLine);
        final ProcessOutput output = new ProcessOutput();

        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                if (ProcessOutputTypes.STDOUT.equals(outputType)) {
                    output.appendStdout(event.getText());
                } else if (ProcessOutputTypes.STDERR.equals(outputType)) {
                    output.appendStderr(event.getText());
                }
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                output.setExitCode(event.getExitCode());
            }
        });

        processHandler.startNotify();
        processHandler.waitFor(TimeUnit.SECONDS.toMillis(3));
        return output;
    }

    private static class AzdVersion {

        private AzdVersionInfo azd;

        public AzdVersionInfo getAzd() {
            return azd;
        }

        public static class AzdVersionInfo {
            private String version;
            private String commit;

            public String getVersion() {
                return version;
            }

            public String getCommit() {
                return commit;
            }
        }
    }

    public static void installAndSetupAzdCli(@NotNull TerminalWidget terminal) {
        // Install or update
        final String installCommand = getInstallationCommandLine();
        terminal.sendCommandToExecute(installCommand);
        azdCliInstallAttempted = true;
        // Set up azd cli path env
        setupAzdEnvs(terminal);
        // enable azd compose
        enableAzdCompose(terminal);
    }

    public static void setupAzdCli(@NotNull TerminalWidget terminal) {
        // Set up azd cli path env if necessary
        if (azdEnvSetupNeeded) {
            setupAzdEnvs(terminal);
        }
        // Enable azd compose
        enableAzdCompose(terminal);
    }

    private static String getInstallationCommandLine() {
        // See https://aka.ms/azd-install
        final String commandLine;
        if (SystemInfo.isWindows) {
            if (TerminalInfo.isWindows()) {
                commandLine = "powershell -ex AllSigned -c \"Invoke-RestMethod 'https://aka.ms/install-azd.ps1' | Invoke-Expression\"";
            } else {
                // wsl or other bash like ones
                commandLine = "curl -fsSL https://aka.ms/install-azd.sh | bash";
            }
        } else if (SystemInfo.isLinux || SystemInfo.isMac) {
            commandLine = "curl -fsSL https://aka.ms/install-azd.sh | bash";
        } else {
            final String osName = System.getProperty("os.name");
            logger.error("Unsupported platform: " + osName);
            throw new UnsupportedOperationException("Unsupported platform: " + osName);
        }
        return commandLine;
    }

    /**
     * On Unix, the CLI is installed to /usr/local/bin, which is always going to be in the PATH.
     * On Windows, the install location is at %LOCALAPPDATA%\Programs\Azure Dev CLI when installed by default.
     * To avoid needing to restart IDE to get the updated PATH, we'll temporarily add the default install location,
     * as long as it's Windows, AZURE_DEV_CLI_PATH is unset, "Azure Dev CLI" isn't already in the PATH, and the user
     * did try to install within this session.
     */
    private static void setupAzdEnvs(@NotNull TerminalWidget terminal) {
        if (SystemInfo.isWindows
                && TerminalInfo.isWindows()
                && System.getenv(AZURE_DEV_CLI_PATH) == null
                && !getPathEnv().contains("/Azure Dev CLI/")) {
            final String envCommand = TerminalInfo.isCmd()
                    ? String.format("set Path=%s;%%Path%%", getDefaultAzdInstallLocation())
                    : String.format("$env:Path = '%s;' + $env:Path", getDefaultAzdInstallLocation());
            terminal.sendCommandToExecute(envCommand);
            azdEnvSetupNeeded = true;
        }
    }

    public static void enableAzdCompose(@NotNull TerminalWidget terminal) {
        if (!azdComposeEnabled) {
            final String command = "azd config set alpha.compose on";
            terminal.sendCommandToExecute(command);
            azdComposeEnabled = true;
        }
    }

    private static String getPathEnv() {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            pathEnv = "";
        }
        return pathEnv;
    }

    private static String getDefaultAzdInstallLocation() {
        final String localAppData = System.getenv("LOCALAPPDATA");
        return Paths.get(localAppData, "Programs", "Azure Dev CLI").toString();
    }

    public static String getAzdInvocation(String command) {
        final String azureDevCliPath = System.getenv(AZURE_DEV_CLI_PATH);
        if (azureDevCliPath == null) {
            return command;
        } else {
            // Extract arguments after "azd" and concat with azd cli path
            return azureDevCliPath + command.substring(3);
        }
    }

}
