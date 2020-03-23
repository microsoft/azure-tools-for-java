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

package com.microsoft.azure.hdinsight.common;

import java.util.Formatter;
import java.util.Locale;

public class Docs {
    private static final String DOC_URL_PATTERN = "https://docs.microsoft.com/%s-%s/azure/hdinsight/%s";

    static public final String TOPIC_CONNECT_HADOOP_LINUX_USING_SSH = "hdinsight-hadoop-linux-use-ssh-unix";

    private Locale locale;

    public Docs(Locale locale) {
        this.locale = locale;
    }

    public String getDocUrlByTopic(String topic) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, this.locale);

        return formatter.format(DOC_URL_PATTERN,
                                this.locale.getLanguage().toLowerCase(),
                                this.locale.getCountry().toLowerCase(),
                                topic)
                .toString();
    }
}
