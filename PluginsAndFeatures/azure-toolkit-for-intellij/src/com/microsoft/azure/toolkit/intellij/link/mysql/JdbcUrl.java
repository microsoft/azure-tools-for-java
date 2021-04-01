/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcUrl {

    private String hostname;
    private Integer port;
    private String database;

    public static JdbcUrl from(String jdbcUrl) {
        Preconditions.checkArgument(StringUtils.startsWith(jdbcUrl, "jdbc:"), "jdbcUrl is not valid.");
        URI uri = URI.create(jdbcUrl.substring(5));
        String hostname = uri.getHost();
        Integer port = uri.getPort();
        String path = uri.getPath();
        String database = StringUtils.startsWith(path, "/") ? path.substring(1) : path;
        return new JdbcUrl(hostname, port, database);
    }

}
