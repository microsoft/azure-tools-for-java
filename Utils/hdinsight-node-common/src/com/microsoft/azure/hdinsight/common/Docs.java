/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
