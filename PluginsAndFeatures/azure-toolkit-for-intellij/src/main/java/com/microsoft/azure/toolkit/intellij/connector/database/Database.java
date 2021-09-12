package com.microsoft.azure.toolkit.intellij.connector.database;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class Database {
    private final ResourceId serverId;
    private final String name;

    private JdbcUrl jdbcUrl;
    private String username;
    private Password password;
    private String envPrefix;

    public Database(String serverId, String name) {
        this.serverId = ResourceId.fromString(serverId);
        this.name = name;
    }

    public Database(String id) {
        final ResourceId dbId = ResourceId.fromString(id);
        this.serverId = dbId.parent();
        this.name = dbId.name();
    }

    public static Database fromId(String id) {
        final ResourceId dbId = ResourceId.fromString(id);
        return new Database(dbId.parent(), dbId.name());
    }

    public String getServerName() {
        return this.serverId.name();
    }

    public String getServerId() {
        return this.serverId.toString();
    }

    public String getId() {
        return this.serverId.toString() + "/databases/" + name;
    }
}
