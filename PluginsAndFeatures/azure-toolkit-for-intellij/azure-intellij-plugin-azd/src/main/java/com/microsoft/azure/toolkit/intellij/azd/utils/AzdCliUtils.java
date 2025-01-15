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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class AzdCliUtils {

    private static final Logger logger = Logger.getInstance(AzdCliUtils.class);

    private static final long CACHE_LIFETIME = 15 * 60 * 1000; // 15 minutes

    private static long lastCheckTime = 0;

    private static AzdVersion cachedAzdVersion = null;

    @Nullable
    public static AzdVersion getAzdVersion() {
        // Check if the cached result is still valid
        long currentTime = System.currentTimeMillis();
        if (cachedAzdVersion != null && (currentTime - lastCheckTime) < CACHE_LIFETIME) {
            return cachedAzdVersion;
        }

        GeneralCommandLine commandLine = new GeneralCommandLine()
                .withExePath("azd")
                .withParameters("version", "--output", "json");

        try {
            ProcessOutput output = runCommand(commandLine);

            if (output.getExitCode() == 0) {
                String stdout = output.getStdout();
                Gson gson = new Gson();
                AzdVersion azdVersion = gson.fromJson(stdout, AzdVersion.class);
                // Cache the result
                cachedAzdVersion = azdVersion;
                lastCheckTime = currentTime;

                return azdVersion;
            } else {
                logger.warn("Failed to check azd login status. Exit code: " + output.getExitCode());
            }
        } catch (Exception e) {
            logger.warn("Unexpected error while checking azd login status", e);
        }

        return null;
    }

    public static class AzdVersion {

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

    public static boolean installAzdCli(@NotNull Project project) {
        try {
            GeneralCommandLine commandLine = getInstallationCommandLine();

            ProcessOutput output = runCommand(commandLine);

            if (output.getExitCode() == 0) {
                Messages.showInfoMessage(project, "Azure Developer CLI (azd) installed successfully!", "Installation Complete");
                return true;
            } else {
                Messages.showErrorDialog(project, "Failed to install Azure Developer CLI (azd). Error: " + output.getStderr(), "Installation Failed");
                return false;
            }
        } catch (Exception e) {
            Messages.showErrorDialog(project, "An error occurred while installing Azure Developer CLI (azd): " + e.getMessage(), "Installation Error");
        }
        return false;
    }

    private static ProcessOutput runCommand(GeneralCommandLine commandLine) throws ExecutionException {
        OSProcessHandler processHandler = new OSProcessHandler(commandLine);
        ProcessOutput output = new ProcessOutput();

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

    private static GeneralCommandLine getInstallationCommandLine() {
        GeneralCommandLine commandLine = new GeneralCommandLine();

        // See https://aka.ms/azd-install
        if (SystemInfo.isWindows) {
            commandLine.withExePath("powershell")
                    .withParameters("-ex", "AllSigned")
                    .withParameters("-c", "Invoke-RestMethod 'https://aka.ms/install-azd.ps1' | Invoke-Expression");
        } else if (SystemInfo.isLinux || SystemInfo.isMac) {
            commandLine.withExePath("curl")
                    .withParameters("-fsSL", "https://aka.ms/install-azd.sh")
                    .withParameters("|", "bash");
        } else {
            String osName = System.getProperty("os.name");
            logger.error("Unsupported platform: " + osName);
            throw new UnsupportedOperationException("Unsupported platform: " + osName);
        }

        return commandLine;
    }
}
