package com.microsoft.azure.toolkit.intellij.dbtools;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.database.dataSource.DatabaseConnectionInterceptor;
import com.intellij.database.dataSource.DatabaseConnectionPoint;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.*;

@SuppressWarnings("UnstableApiUsage")
public abstract class TelemetryConnectionInterceptor implements DatabaseConnectionInterceptor {
    @Nonnull
    private final String resourceKey;
    @Nonnull
    private final String serviceName;
    @Nonnull
    private final String opName;

    protected TelemetryConnectionInterceptor(@Nonnull String resourceKey, @Nonnull String serviceName, @Nonnull String opName) {
        this.resourceKey = resourceKey;
        this.serviceName = serviceName;
        this.opName = opName;
    }

    @Nullable
    public CompletionStage<DatabaseConnectionInterceptor.ProtoConnection> intercept(@Nonnull DatabaseConnectionInterceptor.ProtoConnection proto, boolean silent) {
        final DatabaseConnectionPoint point = proto.getConnectionPoint();
        final String resourceId = point.getAdditionalProperty(resourceKey);
        if (StringUtils.isNotBlank(resourceId) && !StringUtils.equalsIgnoreCase(resourceId, AzureParamsEditorBase.NONE)) {
            final Map<String, String> properties = new HashMap<>();
            final ResourceId id = ResourceId.fromString(resourceId);
            properties.put("subscriptionId", id.subscriptionId());
            properties.put(SERVICE_NAME, serviceName);
            properties.put(OPERATION_NAME, "connect_jdbc_from_dbtools");
            properties.put(OP_NAME, opName);
            properties.put(OP_TYPE, Operation.Type.USER);
            AzureTelemeter.log(AzureTelemetry.Type.OP_END, properties);
        }
        return null;
    }
}
