package com.microsoft.azure.toolkit.intellij.connector.database;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Database {
    private final ResourceId serverId;
    private final String name;

    private JdbcUrl jdbcUrl;
    private String username;
    private Password password;

    public Database(String serverId, String name) {
        this.serverId = ResourceId.fromString(serverId);
        this.name = name;
    }

    public Database(String id) {
        final ResourceId dbId = ResourceId.fromString(id);
        this.serverId = dbId.parent();
        this.name = dbId.name();
    }

    public String getFullName() {
        return this.getServerName() + "/" + this.getName();
    }

    public String getServerName() {
        return this.serverId.name();
    }

    @EqualsAndHashCode.Include
    public String getId() {
        return this.serverId.id() + "/databases/" + name;
    }
}
