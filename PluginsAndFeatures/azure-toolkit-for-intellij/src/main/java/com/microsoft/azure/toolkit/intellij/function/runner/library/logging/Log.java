/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.library.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


public final class Log {
    private static Logger logger = LoggerFactory.getLogger(com.microsoft.azure.toolkit.lib.common.logging.Log.class);

    public static void error(String message) {
        logger.error(message);
    }

    public static void error(Exception error) {
        logger.error(getErrorDetail(error));
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void info(Exception error) {
        logger.info(getErrorDetail(error));
    }

    public static void debug(String message) {
        logger.debug(message);
    }

    public static void debug(Exception error) {
        logger.debug(getErrorDetail(error));
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void warn(Exception error) {
        logger.warn(getErrorDetail(error));
    }

    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public static void prompt(String message) {
        if (logger.isInfoEnabled()) {
            logger.info(message);
            System.out.println(message);
        } else {
            System.out.println(message);
        }
    }

    private static String getErrorDetail(Exception error) {
        final StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        final String exceptionDetails = sw.toString();
        try {
            sw.close();
        } catch (IOException e) {
            // swallow error to avoid deadlock
        }
        return exceptionDetails;
    }
}
