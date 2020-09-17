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

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utils of Azure CLI
 */
public class AzureCliUtils {

    public final static String CLI_GROUP_AZ = "az";

    private final static String CLI_SUBGROUP_WEBAPP = "webapp";
    private final static String CLI_COMMAND_REMOTE_CONNECTION = "create-remote-connection";

    public final static String CLI_COMMAND_VERSION = "version";
    private final static String CLI_ARGUMENTS_WEBAPP_NAME = "-n";
    private final static String CLI_ARGUMENTS_RESOURCE_GROUP = "-g";
    private final static String CLI_ARGUMENTS_SUBSCRIPTION = "--subscription";

    private final static String CLI_COMMAND_VERSION_EXEC_SUCCESS_KEY_WORD = "\"azure-cli\":";
    private final static String CLI_COMMAND_AZ_NOT_FOUND_KEY_WORD_WINDOWS = "is not recognized as an internal or external command";
    private final static String CLI_COMMAND_AZ_NOT_FOUND_KEY_WORD_LINUX = "command not found";
    private final static String CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORD = "Ctrl + C to close";
    private final static String CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORD = "SSH is not enabled for this app.";

    public final static String[] CLI_COMMAND_VERSION_EXEC_SUCCESS_KEY_WORDS = new String[]{CLI_COMMAND_VERSION_EXEC_SUCCESS_KEY_WORD};
    public final static String[] CLI_COMMAND_VERSION_EXEC_FAILED_KEY_WORDS = new String[]{CLI_COMMAND_AZ_NOT_FOUND_KEY_WORD_WINDOWS, CLI_COMMAND_AZ_NOT_FOUND_KEY_WORD_LINUX};
    public final static String[] CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORDS = new String[]{CLI_COMMAND_REMOTE_CONNECTION_EXEC_SUCCESS_KEY_WORD};
    public final static String[] CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORDS = new String[]{CLI_COMMAND_REMOTE_CONNECTION_EXEC_FAILED_KEY_WORD};

    private final static int CMD_EXEC_TIMEOUT = 15 * 1000;

    public static String[] formatCreateWebAppRemoteConnectionParameters(final String subscrption, final String resourceGroup, final String webapp) {
        String[] parameters = new String[]{
                CLI_SUBGROUP_WEBAPP, CLI_COMMAND_REMOTE_CONNECTION,
                CLI_ARGUMENTS_SUBSCRIPTION, subscrption,
                CLI_ARGUMENTS_RESOURCE_GROUP, resourceGroup,
                CLI_ARGUMENTS_WEBAPP_NAME, webapp//,
//                "-p", "9876"
        };
        return parameters;
    }

    public static CommandUtils.CommendExecOutput executeCommandAndGetOutputWithCompleteKeyWord(final String command, final String[] parameters, final String[] sucessKeyWords, final String[] failedKeyWords) throws IOException {
        ByteArrayOutputStream outputStream = CommandUtils.executeCommandAndGetOutputStream(command, parameters);
        CommandUtils.CommendExecOutput commendExecOutput = new CommandUtils.CommendExecOutput();
        if ((sucessKeyWords == null || sucessKeyWords.length ==0) && (failedKeyWords == null || failedKeyWords.length ==0)) {
            commendExecOutput.setSuccess(true);
            commendExecOutput.setOutputMessage(outputStream.toString());
            return commendExecOutput;
        }
        int interval = 100;
        int maxCount = CMD_EXEC_TIMEOUT / interval;
        int count = 0;
        try {
            while (count++ <= maxCount) {
                String currentOutputMessage = outputStream.toString();
                if (sucessKeyWords !=null && sucessKeyWords.length > 0 && checkCommendExecComplete(currentOutputMessage, sucessKeyWords)) {
                    commendExecOutput.setOutputMessage(currentOutputMessage);
                    commendExecOutput.setSuccess(true);
                    break;
                }
                if (failedKeyWords !=null && failedKeyWords.length > 0 && checkCommendExecComplete(currentOutputMessage, failedKeyWords)) {
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

}
