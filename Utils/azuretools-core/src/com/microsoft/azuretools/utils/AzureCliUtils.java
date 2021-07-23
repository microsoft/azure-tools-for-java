/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.toolkit.lib.Azure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utils of Azure CLI
 */
public class AzureCliUtils {

    private static final Logger logger = Logger.getLogger(AzureCliUtils.class.getName());
    private static final String CLI_GROUP_AZ = "az";
    private static final String CLI_SUBGROUP_WEBAPP = "webapp";
    private static final String CLI_SUBGROUP_WEBAPP_COMMAND_REMOTE_CONNECTION = "create-remote-connection";
    private static final String CLI_SUBGROUP_ACCOUNT = "account";
    private static final String CLI_SUBGROUP_ACCOUNT_COMMAND_SHOW = "show";
    private static final String CLI_SUBGROUP_ACCOUNT_COMMAND_LIST = "list";
    private static final String CLI_COMMAND_VERSION = "version";
    private static final String CLI_ARGUMENTS_WEBAPP_NAME = "-n";
    private static final String CLI_ARGUMENTS_RESOURCE_GROUP = "-g";
    private static final String CLI_ARGUMENTS_SUBSCRIPTION = "--subscription";
    private static final String CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORD = "Ctrl + C to close";
    private static final String CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORD = "SSH is not enabled for this app.";

    public static final String[] CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORDS = new String[]{CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORD};
    public static final String[] CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORDS = new String[]{CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORD};

    private static final int CMD_EXEC_TIMEOUT = 12 * 1000;
    private static final int CMD_EXEC_CONNECT_TIMEOUT = 30 * 1000;
    private static final int CMD_EXEC_EXIT_CODE_SUCCESS = 0;

    public static String[] formatCreateWebAppRemoteConnectionParameters(final String subscrption, final String resourceGroup, final String webapp) {
        String[] parameters = new String[] {CLI_SUBGROUP_WEBAPP, CLI_SUBGROUP_WEBAPP_COMMAND_REMOTE_CONNECTION
                , CLI_ARGUMENTS_SUBSCRIPTION, subscrption, CLI_ARGUMENTS_RESOURCE_GROUP, resourceGroup, CLI_ARGUMENTS_WEBAPP_NAME, webapp};
        return parameters;
    }

    /**
     * try to execute azure CLI command to detect it is installed or not.
     *
     * @return
     * true : azure installed.
     * false : azure not installed.
     */
    public static boolean isCliInstalled() throws IOException, InterruptedException {
        return isCliCommandExecutedStatus(new String[]{AzureCliUtils.CLI_COMMAND_VERSION});
    }

    /**
     * check these status of local cli login.
     * @return
     */
    public static boolean isCliLogined() throws IOException, InterruptedException {
        return isCliCommandExecutedStatus(new String[]{CLI_SUBGROUP_ACCOUNT, CLI_SUBGROUP_ACCOUNT_COMMAND_SHOW});
    }

    /**
     * check azure cli contains a specific subscription or not.
     */
    public static boolean containSubscription(String subscriptionId) throws IOException, InterruptedException {
        if (StringUtils.isBlank(subscriptionId)) {
            return false;
        }
        String subscriptionsAsJson = executeCliCommandAndGetOutputIfSuccess(new String[]{CLI_SUBGROUP_ACCOUNT, CLI_SUBGROUP_ACCOUNT_COMMAND_LIST});
        if (StringUtils.isBlank(subscriptionsAsJson)) {
            return false;
        }
        List<AzureCliSubscription> subscriptions = JsonUtils.fromJson(subscriptionsAsJson, new TypeToken<List<AzureCliSubscription>>(){}.getType());
        if (CollectionUtils.isEmpty(subscriptions)) {
            return false;
        }
        return subscriptions.stream().filter(e -> subscriptionId.equals(e.getId())).count() > 0;
    }

    /**
     * execute CLI command with detecting the output message with a success-complete and a failed-completed key words.
     * @param parameters
     * @param sucessKeyWords
     * @param failedKeyWords
     * @return
     */
    public static CommandUtils.CommandExecOutput executeCommandAndGetOutputWithCompleteKeyWord(final String[] parameters
            , final String[] sucessKeyWords, final String[] failedKeyWords) throws IOException, InterruptedException {
        CommandUtils.CommandExecutionOutput executionOutput = CommandUtils.executeCommandAndGetExecution(CLI_GROUP_AZ, parameters, getProxyEnvs());
        CommandUtils.CommandExecOutput commandExecOutput = new CommandUtils.CommandExecOutput();
        if (ArrayUtils.isEmpty(sucessKeyWords) && ArrayUtils.isEmpty(failedKeyWords)) {
            commandExecOutput.setSuccess(true);
            if (executionOutput.getOutputStream() != null) {
                commandExecOutput.setOutputMessage(executionOutput.getOutputStream().toString());
            }
            if (executionOutput.getErrorStream() != null) {
                commandExecOutput.setErrorMessage(executionOutput.getErrorStream().toString());
            }
            return commandExecOutput;
        }
        int interval = 100;
        int maxCount = CMD_EXEC_CONNECT_TIMEOUT / interval;
        int count = 0;
        while (count++ <= maxCount) {
            String currentOutputMessage = StreamUtils.toString(executionOutput.getOutputStream());
            String currentErrorMessage = StreamUtils.toString(executionOutput.getErrorStream());
            if (ArrayUtils.isNotEmpty(sucessKeyWords) && checkCommendExecComplete(currentOutputMessage, currentErrorMessage, sucessKeyWords)) {
                commandExecOutput.setOutputMessage(currentOutputMessage);
                commandExecOutput.setErrorMessage(currentErrorMessage);
                commandExecOutput.setSuccess(true);
                break;
            }
            if (ArrayUtils.isNotEmpty(failedKeyWords) && checkCommendExecComplete(currentOutputMessage, currentErrorMessage, failedKeyWords)) {
                commandExecOutput.setOutputMessage(currentOutputMessage);
                commandExecOutput.setErrorMessage(currentErrorMessage);
                commandExecOutput.setSuccess(false);
                break;
            }
            Thread.sleep(interval);
        }
        return commandExecOutput;
    }

    private static boolean isCliCommandExecutedStatus(String[] parameters) throws IOException, InterruptedException {
        DefaultExecuteResultHandler resultHandler = CommandUtils.executeCommandAndGetResultHandler(CLI_GROUP_AZ, parameters, getProxyEnvs());
        resultHandler.waitFor(CMD_EXEC_TIMEOUT);
        int exitValue = resultHandler.getExitValue();
        logger.info("exitCode: " + exitValue);
        if (exitValue == CMD_EXEC_EXIT_CODE_SUCCESS) {
            return true;
        }
        return false;
    }

    private static String executeCliCommandAndGetOutputIfSuccess(String[] parameters) throws IOException, InterruptedException {
        CommandUtils.CommandExecutionOutput executionOutput = CommandUtils.executeCommandAndGetExecution(CLI_GROUP_AZ, parameters, getProxyEnvs());
        executionOutput.getResultHandler().waitFor(CMD_EXEC_TIMEOUT);
        int exitValue = executionOutput.getResultHandler().getExitValue();
        logger.info("exitCode: " + exitValue);
        if (exitValue == CMD_EXEC_EXIT_CODE_SUCCESS) {
            return executionOutput.getOutputStream().toString();
        }
        return null;
    }

    private static Map<String, String> getProxyEnvs() {
        final InetSocketAddress proxy = Azure.az().config().getHttpProxy();
        Map<String, String> env = new HashMap<>();
        if (proxy != null) {
            String proxyStr = String.format("http://%s:%s", proxy.getHostString(), proxy.getPort());
            env.put("HTTPS_PROXY", proxyStr);
            env.put("HTTP_PROXY", proxyStr);
        }
        return env;
    }

    private static boolean checkCommendExecComplete(String outputMessage, String errorMessage, String[] completeKeyWords) {
        if (completeKeyWords == null || completeKeyWords.length == 0) {
            return true;
        }
        if (StringUtils.isBlank(outputMessage) && StringUtils.isBlank(errorMessage)) {
            return false;
        }
        for (String completeKeyWord : completeKeyWords) {
            if (StringUtils.isNotBlank(outputMessage) && outputMessage.contains(completeKeyWord)) {
                return true;
            }
            if (StringUtils.isNotBlank(errorMessage) && errorMessage.contains(completeKeyWord)) {
                return true;
            }
        }
        return false;
    }

    public static class AzureCliSubscription {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
