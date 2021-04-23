/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.Objects;

public class JdbcUrl {
    private final URIBuilder uri;

    public JdbcUrl(String url) {
        Preconditions.checkArgument(StringUtils.startsWith(url, "jdbc:"), "invalid jdbc url.");
        try {
            this.uri = new URIBuilder(url.substring(5));
        } catch (final URISyntaxException e) {
            throw new AzureToolkitRuntimeException("invalid jdbc url: %s", url);
        }
    }

    public static JdbcUrl from(String connectionString) {
        return new JdbcUrl(connectionString);
    }

    public static JdbcUrl mysql(String serverHost, String database) {
        return new JdbcUrl(String.format("jdbc:mysql://%s:3306/%s?serverTimezone=UTC&useSSL=true&requireSSL=false", serverHost, database));
    }

    public int getPort() {
        if (this.uri.getScheme().toLowerCase().startsWith("mysql")) {
            return this.uri.getPort() < 1 ? 3306 : this.uri.getPort();
        }
        throw new AzureToolkitRuntimeException("unknown jdbc url scheme: %s", this.uri.getScheme());
    }

    public String getHost() {
        return this.uri.getHost();
    }

    public String getServer() {
        return this.getHost();
    }

    public String getDatabase() {
        final String path = this.uri.getPath();
        return StringUtils.startsWith(path, "/") ? path.substring(1) : path;
    }

    public JdbcUrl setHost(String server) {
        this.uri.setHost(server);
        return this;
    }

    public JdbcUrl setServer(String server) {
        return this.setHost(server);
    }

    public JdbcUrl setDatabase(String database) {
        this.uri.setPath("/" + database);
        return this;
    }

    public String toString() {
        return "jdbc:" + this.uri.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JdbcUrl jdbcUrl = (JdbcUrl) o;
        return Objects.equals(uri.toString(), jdbcUrl.uri.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri.toString());
    }
}
