/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.utils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommandUtils {

    private static final Logger logger = Logger.getLogger(CommandUtils.class.getName());
    private static final String WINDOWS_STARTER = "cmd.exe";
    private static final String LINUX_MAC_STARTER = "/bin/sh";
    private static final String WINDOWS_SWITCHER = "/c";
    private static final String LINUX_MAC_SWITCHER = "-c";
    private static final String DEFAULT_WINDOWS_SYSTEM_ROOT = System.getenv("SystemRoot");
    private static final String DEFAULT_MAC_LINUX_PATH = "/bin/";
    public static final String COMMEND_SUFFIX_WINDOWS = ".cmd";

    public static List<File> resolvePathForCommandForCmdOnWindows(final String command) throws IOException, InterruptedException {
        return resolvePathForCommand(isWindows() ? (command + ".cmd") : command);
    }

    public static List<File> resolvePathForCommand(final String command)
            throws IOException, InterruptedException {
        return extractFileFromOutput(CommandUtils.executeMultipleLineOutput((CommandUtils.isWindows() ? "where " : "which ") + command, null));
    }

    public static String[] executeMultipleLineOutput(final String cmd, File cwd, Function<Process, InputStream> streamFunction)
            throws IOException, InterruptedException {
        final String[] cmds = isWindows() ? new String[]{"cmd.exe", "/c", cmd} : new String[]{"bash", "-c", cmd};
        final Process p = Runtime.getRuntime().exec(cmds, null, cwd);
        final int exitCode = p.waitFor();
        if (exitCode != 0) {
            return new String[0];
        }
        return StringUtils.split(IOUtils.toString(streamFunction.apply(p), StandardCharsets.UTF_8), "\n");
    }

    public static String exec(final String commandWithArgs) throws IOException {
        final String starter = isWindows() ? WINDOWS_STARTER : LINUX_MAC_STARTER;
        final String switcher = isWindows() ? WINDOWS_SWITCHER : LINUX_MAC_SWITCHER;
        final String workingDirectory = getSafeWorkingDirectory();
        if (StringUtils.isEmpty(workingDirectory)) {
            final IllegalStateException exception = new IllegalStateException("A Safe Working directory could not be found to execute command from.");
            logger.throwing(CommandUtils.class.getName(), "exec", exception);
            throw exception;
        }
        final String commandWithPath = isWindows() ? commandWithArgs : String.format("export PATH=$PATH:/usr/local/bin ; %s", commandWithArgs);
        return executeCommandAndGetOutput(starter, switcher, commandWithPath, new File(workingDirectory));
    }

    public static String executeCommandAndGetOutput(final String commandWithoutArgs, final String[] args, final File directory) throws IOException {
        return executeCommandAndGetOutput(commandWithoutArgs, args, directory, false);
    }

    public static String executeCommandAndGetOutput(final String commandWithoutArgs, final String[] args, final File directory,
                                                    final boolean mergeErrorStream) throws IOException {
        final CommandLine commandLine = new CommandLine(commandWithoutArgs);
        commandLine.addArguments(args);
        return executeCommandAndGetOutput(commandLine, directory, mergeErrorStream);
    }

    public static String executeCommandAndGetOutput(final String starter, final String switcher, final String commandWithArgs,
                                                    final File directory) throws IOException {
        final CommandLine commandLine = new CommandLine(starter);
        commandLine.addArgument(switcher, false);
        commandLine.addArgument(commandWithArgs, false);
        return executeCommandAndGetOutput(commandLine, directory);
    }

    public static String executeCommandAndGetOutput(final CommandLine commandLine, final File directory) throws IOException {
        return executeCommandAndGetOutput(commandLine, directory, false);
    }

    public static String executeCommandAndGetOutput(final CommandLine commandLine, final File directory, final boolean mergeErrorStream) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = mergeErrorStream ? out : new ByteArrayOutputStream();
        final PumpStreamHandler streamHandler = new PumpStreamHandler(out, err);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(directory);
        executor.setStreamHandler(streamHandler);
        executor.setExitValues(null);
        try {
            executor.execute(commandLine);
            if (!mergeErrorStream) {
                logger.log(Level.SEVERE, err.toString());
            }
            return out.toString();
        } catch (ExecuteException e) {
            // swallow execute exception and return empty
            return StringUtils.EMPTY;
        } finally {
            out.close();
            err.close();
        }
    }

    public static OutputStream executeCommandAndGetOutputStream(final String command, final String[] parameters) throws IOException {
        CommandExecutionOutput execution = executeCommandAndGetExecution(command, parameters);
        return execution.getOutputStream();
    }

    public static DefaultExecuteResultHandler executeCommandAndGetResultHandler(final String command, final String[] parameters) throws IOException {
        CommandExecutionOutput execution = executeCommandAndGetExecution(command, parameters);
        return execution.getResultHandler();
    }

    public static CommandExecutionOutput executeCommandAndGetExecution(final String command, final String[] parameters) throws IOException {
        final String starter = isWindows() ? WINDOWS_STARTER : LINUX_MAC_STARTER;
        final String switcher = isWindows() ? WINDOWS_SWITCHER : LINUX_MAC_SWITCHER;
        final CommandLine commandLine = new CommandLine(starter);
        commandLine.addArgument(switcher, false);
        commandLine.addArgument(command + StringUtils.SPACE + String.join(StringUtils.SPACE, parameters), false);
        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        final PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        final DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);
        executor.setExitValues(null);
        executor.execute(commandLine, resultHandler);
        CommandExecutionOutput execution = new CommandExecutionOutput();
        execution.setOutputStream(outputStream);
        execution.setErrorStream(errorStream);
        execution.setResultHandler(resultHandler);
        return execution;
    }

    public static String[] executeMultipleLineOutput(final String cmd, File cwd)
            throws IOException, InterruptedException {
        return executeMultipleLineOutput(cmd, cwd, Process::getInputStream);
    }

    public static List<File> extractFileFromOutput(final String[] outputStrings) {
        final List<File> list = new ArrayList<>();
        for (final String outputLine : outputStrings) {
            if (StringUtils.isBlank(outputLine)) {
                continue;
            }

            final File file = new File(outputLine.replaceAll("\\r|\\n", "").trim());
            if (!file.exists() || !file.isFile()) {
                continue;
            }

            list.add(file);
        }
        return list;
    }

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private static String getSafeWorkingDirectory() {
        if (isWindows()) {
            if (StringUtils.isEmpty(DEFAULT_WINDOWS_SYSTEM_ROOT)) {
                return null;
            }
            return DEFAULT_WINDOWS_SYSTEM_ROOT + "\\system32";
        } else {
            return DEFAULT_MAC_LINUX_PATH;
        }
    }

    public static class CommandExecOutput {
        private boolean success;
        private String outputMessage;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getOutputMessage() {
            return outputMessage;
        }

        public void setOutputMessage(String outputMessage) {
            this.outputMessage = outputMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public static class CommandExecutionOutput {

        private OutputStream outputStream;
        private OutputStream errorStream;
        private DefaultExecuteResultHandler resultHandler;

        public OutputStream getOutputStream() {
            return outputStream;
        }

        public void setOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        public OutputStream getErrorStream() {
            return errorStream;
        }

        public void setErrorStream(OutputStream errorStream) {
            this.errorStream = errorStream;
        }

        public DefaultExecuteResultHandler getResultHandler() {
            return resultHandler;
        }

        public void setResultHandler(DefaultExecuteResultHandler resultHandler) {
            this.resultHandler = resultHandler;
        }
    }
}
