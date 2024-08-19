package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.microsoft.azure.toolkit.intellij.dbtools.DbUtilsComboBoxBase;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public class RedisCacheComboBox extends DbUtilsComboBoxBase<RedisCache> {

    @Nullable
    @Override
    protected RedisCache doGetDefaultValue() {
        return CacheManager.getUsageHistory(RedisCache.class).peek();
    }

    @Override
    protected List<RedisCache> load() {
        final Stream<RedisCache> allCaches = Azure.az(AzureRedis.class).list().stream().flatMap(x -> x.caches().list().stream());
        return filterDrafts(allCaches);
    }

    private List<RedisCache> filterDrafts(@Nonnull Stream<RedisCache> caches) {
        return caches.filter(m -> !m.isDraftForCreating()).collect(Collectors.toList());
    }
}
