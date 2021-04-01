/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import com.mysql.cj.jdbc.ConnectionImpl;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.*;

public class MySQLConnectionUtils {

    public static boolean connect(String url, String username, String password) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            DriverManager.getConnection(url, username, password);
            return true;
        } catch (ClassNotFoundException | SQLException exception) {
        }
        return false;
    }

    public static ConnectResult connectWithPing(String url, String username, String password) {
        boolean connected = false;
        String errorMessage = null;
        Long pingCost = null;
        String serverVersion = null;
        // refresh property
        try {
            Class.forName("com.mysql.jdbc.Driver");
            long start = System.currentTimeMillis();
            Connection connection = DriverManager.getConnection(url, username, password);
            connected = true;
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select 'hi'");
            if (resultSet.next()) {
                String result = resultSet.getString(1);
                connected = "hi".equals(result);
            }
            pingCost = System.currentTimeMillis() - start;
            serverVersion = ((ConnectionImpl) connection).getServerVersion().toString();
        } catch (ClassNotFoundException | SQLException exception) {
            errorMessage = exception.getMessage();
        }
        return new ConnectResult(connected, errorMessage, pingCost, serverVersion);
    }

    @Getter
    @AllArgsConstructor
    public static class ConnectResult {
        private boolean connected;
        private String message;
        private Long pingCost;
        private String serverVersion;
    }
}
