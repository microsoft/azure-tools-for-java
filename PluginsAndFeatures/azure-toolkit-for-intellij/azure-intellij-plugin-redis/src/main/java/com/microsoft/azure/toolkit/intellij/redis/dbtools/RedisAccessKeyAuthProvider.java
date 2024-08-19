/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.intellij.credentialStore.Credentials;
import com.intellij.database.Dbms;
import com.intellij.database.dataSource.DatabaseAuthProvider;
import com.intellij.database.dataSource.DatabaseConnectionPoint;
import com.intellij.database.dataSource.DatabaseCredentialsAuthProvider;
import com.microsoft.azure.toolkit.intellij.dbtools.AzureParamsEditorBase;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import com.microsoft.azure.toolkit.redis.RedisCacheModule;
import com.microsoft.azure.toolkit.redis.RedisServiceSubscription;
import kotlin.coroutines.Continuation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RedisAccessKeyAuthProvider implements DatabaseAuthProvider {
    public static final String RedisAccessKey = "redis_access_key";

    @Nullable
    @Override
    public Object interceptConnection(@Nonnull ProtoConnection proto, boolean silent, @Nonnull Continuation<? super Boolean> $completion) {
        final String cacheId = getCacheIdIfValid(proto);
        if (cacheId == null) return false;

        final AzureTaskManager manager = AzureTaskManager.getInstance();

        manager.runInBackground("Loading Redis Cache Primary Key", () -> {
            final String primaryKey = getPrimaryKeyForCache(cacheId);
            if (primaryKey == null) {
                $completion.resumeWith(false);
                return;
            }

            manager.runLater(() -> {
                setPrimaryKeyAsPassword(proto, primaryKey);
                $completion.resumeWith(true);
            });
        });

        return kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
    }

    @Nonnull
    @Override
    public ApplicabilityLevel.Result getApplicability(@Nonnull DatabaseConnectionPoint point, @Nonnull ApplicabilityLevel level) {
        final boolean isRedis = point.getDbms().in(Dbms.REDIS);
        final String cacheId = point.getAdditionalProperty(RedisCacheParamEditor.KEY_REDIS_CACHE);
        if (isRedis && isValidCacheId(cacheId))
            return ApplicabilityLevel.Result.PREFERRED;

        return isRedis ? ApplicabilityLevel.Result.DEFAULT : ApplicabilityLevel.Result.NOT_APPLICABLE;
    }

    @Nonnull
    @Override
    public String getId() {
        return RedisAccessKey;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Redis Access Key";
    }

    private void setPrimaryKeyAsPassword(@Nonnull ProtoConnection proto, @Nonnull String primaryKey) {
        final var credentials = new Credentials(null, primaryKey);
        DatabaseCredentialsAuthProvider.applyCredentials(proto, credentials, false);
    }

    @Nullable
    private String getPrimaryKeyForCache(@Nonnull String cacheId) {
        final RedisServiceSubscription subscription = Azure.az(AzureRedis.class).get(cacheId);
        if (subscription == null)  {
            return null;
        }

        final RedisCacheModule caches = subscription.caches();

        final RedisCache cache = caches.get(cacheId);
        if (cache == null) {
            return null;
        }

        return cache.getPrimaryKey();
    }

    @Nullable
    private String getCacheIdIfValid(@Nonnull ProtoConnection proto) {
        final String cacheId = proto.getConnectionPoint().getAdditionalProperty(RedisCacheParamEditor.KEY_REDIS_CACHE);
        return isValidCacheId(cacheId) ? cacheId : null;
    }

    private boolean isValidCacheId(@Nullable String cacheId) {
        return StringUtils.isNotBlank(cacheId) && !StringUtils.equalsIgnoreCase(cacheId, AzureParamsEditorBase.NONE);
    }
}
