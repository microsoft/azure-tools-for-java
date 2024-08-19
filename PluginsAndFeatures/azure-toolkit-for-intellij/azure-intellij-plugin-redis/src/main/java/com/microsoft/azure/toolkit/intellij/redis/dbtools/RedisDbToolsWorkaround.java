package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.intellij.dbtools.DbToolsWorkarounds;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

public class RedisDbToolsWorkaround implements ProjectActivity, DumbAware {

    @Nullable
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                loadRedisTemplates();
                makeCacheShowAtTop();
            } catch (final Throwable t) {
                // swallow exception for preloading workarounds
                AzureTelemeter.log(AzureTelemetry.Type.ERROR, new HashMap<>(), t);
            }
        });
        return null;
    }

    private static void makeCacheShowAtTop() {
        DbToolsWorkarounds.makeParameterShowAtTop(RedisAccountTypeFactory.PARAM_NAME);
    }

    private static void loadRedisTemplates() {
        DbToolsWorkarounds.loadDriverTemplate(
                "redis",
                RedisAccountTypeFactory.TYPE_NAME,
                "Azure",
                "jdbc:redis://[[{user}:]{password}@]{host::localhost}[:{port::6379}][/{database:database/[^?]+:0}?][/{cache:redis_cache}?][\\?<&,{:identifier}={:param}>]");
    }
}
