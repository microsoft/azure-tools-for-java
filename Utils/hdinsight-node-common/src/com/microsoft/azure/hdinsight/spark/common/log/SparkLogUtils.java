/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common.log;

import org.apache.log4j.Level;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Error;
import static com.microsoft.azure.hdinsight.common.MessageInfoType.*;

public class SparkLogUtils {
    public static final List<Level> log4jAllLevels = Arrays.asList(
            Level.FATAL,
            Level.ERROR,
            Level.WARN,
            Level.INFO,
            Level.DEBUG,
            Level.TRACE);

    public static final Pattern log4jLevelRegex = Pattern.compile(
            "\\b(?<level>"
                    + log4jAllLevels.stream().map(Level::toString).collect(Collectors.joining("|")) + ")\\b");

    public static SparkLogLine mapTypedMessageByLog4jLevels(
            final SparkLogLine previous,
            final SparkLogLine current) {
        if (current.getMessageInfoType() == Log) {
            final String msg = current.getRawLog();
            final Matcher matcher = log4jLevelRegex.matcher(msg);

            if (matcher.find()) {
                final Level level = Level.toLevel(matcher.group("level"));
                if (level.isGreaterOrEqual(Level.ERROR)) {
                    return new SparkLogLine(current.getLogSource(), Error, msg);
                }

                if (level == Level.WARN) {
                    return new SparkLogLine(current.getLogSource(), Warning, msg);
                }

                if (level == Level.INFO) {
                    return new SparkLogLine(current.getLogSource(), Info, msg);
                }

                // Keep the current level
                return current;
            }

            // No level keyword found, use the previous's level
            return new SparkLogLine(current.getLogSource(), previous.getMessageInfoType(), msg);
        }

        return current;
    }
}
