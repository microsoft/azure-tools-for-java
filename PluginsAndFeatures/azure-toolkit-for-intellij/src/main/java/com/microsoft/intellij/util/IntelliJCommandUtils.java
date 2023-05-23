package com.microsoft.intellij.util;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.ICommandUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class IntelliJCommandUtils implements ICommandUtils {
    @Override
    public String executeCommandAndGetOutput(CommandLine commandLine, File directory, Map<String, String> env, boolean mergeErrorStream) throws IOException {
        final GeneralCommandLine command = new GeneralCommandLine();
        command.withExePath(commandLine.getExecutable());
        command.withParameters(commandLine.getArguments());
        command.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);
        command.withEnvironment(env);
//        command.withEnvironment(EnvironmentUtil.getEnvironmentMap());
        command.withWorkDirectory(directory);
        command.setRedirectErrorStream(mergeErrorStream);
        final ProcessOutput processOutput;
        try {
            processOutput = ExecUtil.execAndGetOutput(command);
            final String out = processOutput.getStdout();
            final String err = processOutput.getStderr();
            if (!mergeErrorStream && StringUtils.isNotBlank(err) && StringUtils.isBlank(out)) {
                throw new AzureToolkitRuntimeException(StringUtils.trim(err));
            }
            return out;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
