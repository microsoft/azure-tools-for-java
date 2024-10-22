package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.microsoft.azure.toolkit.intellij.dbtools.TelemetryConnectionInterceptor;

public class DatabaseServerConnectionInterceptor extends TelemetryConnectionInterceptor {
    protected DatabaseServerConnectionInterceptor() {
        super(DatabaseServerParamEditor.KEY_DB_SERVER_ID, "database", "database.connect_jdbc_from_dbtools");
    }
}
