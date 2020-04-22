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
                Level level = Level.toLevel(matcher.group("level"));
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
