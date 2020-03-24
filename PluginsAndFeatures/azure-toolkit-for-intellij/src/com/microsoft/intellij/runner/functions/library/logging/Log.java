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

package com.microsoft.intellij.runner.functions.library.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


public final class Log {
    private static Logger logger = LoggerFactory.getLogger(com.microsoft.azure.common.logging.Log.class);

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
