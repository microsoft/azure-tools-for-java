/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.mysql.cj.jdbc.ConnectionImpl;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

public class MySQLConnectionUtils {

    public static boolean connect(JdbcUrl url, String username, String password) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            DriverManager.getConnection(url.toString(), username, password);
            return true;
        } catch (final ClassNotFoundException | SQLException ignored) {
        }
        return false;
    }

    public static ConnectResult connectWithPing(JdbcUrl url, String username, String password) {
        boolean connected = false;
        String errorMessage = null;
        Long pingCost = null;
        String serverVersion = null;
        // refresh property
        try {
            Class.forName("com.mysql.jdbc.Driver");
            final long start = System.currentTimeMillis();
            final Connection connection = DriverManager.getConnection(url.toString(), username, password);
            connected = true;
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("select 'hi'");
            if (resultSet.next()) {
                final String result = resultSet.getString(1);
                connected = "hi".equals(result);
            }
            pingCost = System.currentTimeMillis() - start;
            serverVersion = ((ConnectionImpl) connection).getServerVersion().toString();
        } catch (final ClassNotFoundException | SQLException exception) {
            errorMessage = exception.getMessage();
        }
        EventUtil.logEvent(EventType.info, ActionConstants.parse(ActionConstants.MySQL.TEST_CONNECTION).getServiceName(),
                ActionConstants.parse(ActionConstants.MySQL.TEST_CONNECTION).getOperationName(),
                Collections.singletonMap("result", String.valueOf(connected)));
        return new ConnectResult(connected, errorMessage, pingCost, serverVersion);
    }

    @Getter
    @AllArgsConstructor
    public static class ConnectResult {
        private final boolean connected;
        private final String message;
        private final Long pingCost;
        private final String serverVersion;
    }
}
