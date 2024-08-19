package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.FieldSize;
import com.intellij.openapi.actionSystem.AnAction;
import com.microsoft.azure.toolkit.intellij.dbtools.AzureParamsEditorBase;
import com.microsoft.azure.toolkit.intellij.dbtools.NoResourceTipLabel;
import com.microsoft.azure.toolkit.intellij.dbtools.NotSignedInTipLabel;
import com.microsoft.azure.toolkit.redis.RedisCache;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class RedisCacheParamEditor extends AzureParamsEditorBase<RedisCacheComboBox, RedisCache> {
    public static final String KEY_REDIS_CACHE = "AZURE_REDIS_CACHE";

    public RedisCacheParamEditor(
            @NotNull DataInterchange interchange,
            @NotNull FieldSize fieldSize,
            @Nullable String caption,
            AnAction @NotNull ... actions) {
        super(new RedisCacheComboBox(), interchange, fieldSize, caption, KEY_REDIS_CACHE, actions);
        final LocalDataSource dataSource = getDataSourceConfigurable().getDataSource();

        final RedisCacheComboBox comboBox = getEditorComponent();
        final String cacheId = interchange.getProperty(KEY_REDIS_CACHE);

        if (StringUtils.isNotBlank(cacheId)) {
            comboBox.setByResourceId(cacheId);
        }
    }

    @Override
    protected NotSignedInTipLabel createNotSignedInTipLabel() {
        return new RedisNotSignedInLabel();
    }

    @Override
    protected NoResourceTipLabel createNoResourceTipLabel() {
        return new NoCachesTipLabel();
    }

    @Override
    protected void setResource(
            @Nonnull DataInterchange interchange,
            @Nullable Object fromBackground,
            @Nullable RedisCache value,
            @Nullable String oldResourceId,
            @Nullable String newResourceId) {
        interchange.putProperty(KEY_REDIS_CACHE, Optional.ofNullable(newResourceId).orElse(NONE));
        if (Objects.isNull(value) || Objects.equals(oldResourceId, newResourceId)) {
            return;
        }

        setJdbcUrl(RedisJdbcUrl.from(value));
    }

    @Override
    protected String getHostFromValue(RedisCache value) {
        return value.getHostName();
    }
}



