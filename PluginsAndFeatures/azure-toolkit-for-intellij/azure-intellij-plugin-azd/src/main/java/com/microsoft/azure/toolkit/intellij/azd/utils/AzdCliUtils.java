package com.microsoft.azure.toolkit.intellij.azd.utils;

import com.google.gson.Gson;
import com.intellij.execution.ExecutionException;
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

import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class AzdCliUtils {

    private static final Logger logger = Logger.getInstance(AzdCliUtils.class);

    private static final long CACHE_LIFETIME = 15 * 60 * 1000; // 15 minutes

    private static long lastCheckTime = 0;

    private static AzdVersion cachedAzdVersion = null;

    public static boolean azdCliInstallAttempted = false;

    public static boolean checkAzdCliInstalled(TerminalWidget terminal) {
        if (azdCliInstallAttempted) {
            return true;
        } else {
            return getAzdCliVersion(terminal) != null;
        }
    }

    @Nullable
    private static AzdVersion getAzdCliVersion(TerminalWidget terminal) {
        // Check if the cached result is still valid
        final long currentTime = System.currentTimeMillis();
        if (cachedAzdVersion != null && (currentTime - lastCheckTime) < CACHE_LIFETIME) {
            return cachedAzdVersion;
        }

        try {
            final GeneralCommandLine commandLine = new GeneralCommandLine()
                    .withExePath("azd")
                    .withParameters("version", "--output", "json");

            final ProcessOutput output = runCommand(commandLine);
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
            }
        } catch (Exception ex) {
            logger.warn("Unexpected error while checking azd version", ex);
        }

        return null;
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

    public static void installAzdCli(@NotNull TerminalWidget terminal) {
        final String installCommand = getInstallationCommandLine();
        terminal.sendCommandToExecute(installCommand);
        setupAzdEnvs(terminal);
    }

    public static void setupAzdEnvs(@NotNull TerminalWidget terminal) {
        if (SystemInfo.isWindows && System.getenv("AZURE_DEV_CLI_PATH") == null && !getPathEnv().contains("/Azure Dev CLI/")) {
            terminal.sendCommandToExecute(String.format("$env:Path = '%s;' + $env:Path", getDefaultAzdInstallLocation()));
            azdCliInstallAttempted = true;
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
        final String azureDevCliPath = System.getenv("AZURE_DEV_CLI_PATH");
        if (azureDevCliPath == null) {
            return command;
        } else {
            return azureDevCliPath + command.substring(3);
        }
    }

    private static ProcessOutput runCommand(GeneralCommandLine commandLine) throws ExecutionException {
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
        processHandler.waitFor(TimeUnit.SECONDS.toMillis(30)); // Wait up to 30 seconds
        return output;
    }

    private static String getInstallationCommandLine() {
        // See https://aka.ms/azd-install
        final String commandLine;
        if (SystemInfo.isWindows) {
            commandLine = "powershell -ex AllSigned -c \"Invoke-RestMethod 'https://aka.ms/install-azd.ps1' | Invoke-Expression\"";
        } else if (SystemInfo.isLinux || SystemInfo.isMac) {
            commandLine = "curl -fsSL https://aka.ms/install-azd.sh | bash";
        } else {
            final String osName = System.getProperty("os.name");
            logger.error("Unsupported platform: " + osName);
            throw new UnsupportedOperationException("Unsupported platform: " + osName);
        }
        return commandLine;
    }
}
