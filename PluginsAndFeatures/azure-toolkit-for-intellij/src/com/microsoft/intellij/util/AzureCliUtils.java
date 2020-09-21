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

package com.microsoft.intellij.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azuretools.utils.CommandUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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
    public static boolean isCliInstalled() {
        return checkCliCommandExecutedStatus(new String[]{AzureCliUtils.CLI_COMMAND_VERSION});
    }

    /**
     * check these status of local cli login.
     * @return
     */
    public static boolean isCliLogined() {
        return checkCliCommandExecutedStatus(new String[]{CLI_SUBGROUP_ACCOUNT, CLI_SUBGROUP_ACCOUNT_COMMAND_SHOW});
    }

    private static boolean checkCliCommandExecutedStatus(String[] parameters) {
        try {
            DefaultExecuteResultHandler resultHandler = CommandUtils.executeCommandAndGetResultHandler(CLI_GROUP_AZ, parameters);
            resultHandler.waitFor(CMD_EXEC_TIMEOUT);
            int exitValue = resultHandler.getExitValue();
            logger.info("exitCode: " + exitValue);
            if (exitValue == CMD_EXEC_EXIT_CODE_SUCCESS) {
                return true;
            }
            return false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * check contains a specific subscription or not.
     */
    public static boolean containSubscription(String subscriptionId) {
        if (StringUtils.isBlank(subscriptionId)) {
            return false;
        }
        String subscriptionsAsJson = executeCliCommandAndGetOutputIfSuccess(new String[]{CLI_SUBGROUP_ACCOUNT, CLI_SUBGROUP_ACCOUNT_COMMAND_LIST});
        if (StringUtils.isBlank(subscriptionsAsJson)) {
            return false;
        }
        Gson gson = new Gson();
        List<AzureCliSubscription> subscriptions = gson.fromJson(subscriptionsAsJson, new TypeToken<List<AzureCliSubscription>>(){}.getType());
        if (CollectionUtils.isEmpty(subscriptions)) {
            return false;
        }
        return subscriptions.stream().filter(e -> subscriptionId.equals(e.getId())).count() > 0;
    }

    private static String executeCliCommandAndGetOutputIfSuccess(String[] parameters) {
        try {
            CommandUtils.CommandExecutionOutput executionOutput =
                    CommandUtils.executeCommandAndGetExecution(CLI_GROUP_AZ, parameters);
            executionOutput.getResultHandler().waitFor(CMD_EXEC_TIMEOUT);
            int exitValue = executionOutput.getResultHandler().getExitValue();
            logger.info("exitCode: " + exitValue);
            if (exitValue == CMD_EXEC_EXIT_CODE_SUCCESS) {
                return executionOutput.getOutputStream().toString();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * execute CLI command with detecting the output message with a success-complete and a failed-completed key words.
     * @param parameters
     * @param sucessKeyWords
     * @param failedKeyWords
     * @return
     */
    public static CommandUtils.CommendExecOutput executeCommandAndGetOutputWithCompleteKeyWord(
            final String[] parameters, final String[] sucessKeyWords, final String[] failedKeyWords) throws IOException {
        ByteArrayOutputStream outputStream = CommandUtils.executeCommandAndGetOutputStream(CLI_GROUP_AZ, parameters);
        CommandUtils.CommendExecOutput commendExecOutput = new CommandUtils.CommendExecOutput();
        if ((sucessKeyWords == null || sucessKeyWords.length == 0) && (failedKeyWords == null || failedKeyWords.length == 0)) {
            commendExecOutput.setSuccess(true);
            commendExecOutput.setOutputMessage(outputStream.toString());
            return commendExecOutput;
        }
        int interval = 100;
        int maxCount = CMD_EXEC_CONNECT_TIMEOUT / interval;
        int count = 0;
        try {
            while (count++ <= maxCount) {
                String currentOutputMessage = outputStream.toString();
                if (sucessKeyWords != null && sucessKeyWords.length > 0 && checkCommendExecComplete(currentOutputMessage, sucessKeyWords)) {
                    commendExecOutput.setOutputMessage(currentOutputMessage);
                    commendExecOutput.setSuccess(true);
                    break;
                }
                if (failedKeyWords != null && failedKeyWords.length > 0 && checkCommendExecComplete(currentOutputMessage, failedKeyWords)) {
                    commendExecOutput.setOutputMessage(currentOutputMessage);
                    commendExecOutput.setSuccess(false);
                    break;
                }
                Thread.sleep(interval);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return commendExecOutput;
    }

    private static boolean checkCommendExecComplete(String outputMessage, String[] completeKeyWords) {
        if (completeKeyWords == null || completeKeyWords.length == 0) {
            return true;
        }
        if (StringUtils.isBlank(outputMessage)) {
            return false;
        }
        for (String completeKeyWord : completeKeyWords) {
            if (outputMessage.contains(completeKeyWord)) {
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
