/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.connection;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBoxSimple;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class RedisResourcePanel implements AzureFormJPanel<RedisCache> {
    protected SubscriptionComboBox subscriptionComboBox;
    protected AzureComboBox<RedisCache> redisComboBox;
    @Getter
    protected JPanel contentPanel;

    public RedisResourcePanel() {
        this.init();
    }

    private void init() {
        this.subscriptionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.redisComboBox.refreshItems();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.redisComboBox.clear();
            }
        });
    }

    @Override
    public void setData(RedisCache account) {
        Optional.ofNullable(account).ifPresent((a -> {
            this.subscriptionComboBox.setValue(new ItemReference<>(a.subscriptionId(), Subscription::getId));
            this.redisComboBox.setValue(new ItemReference<>(a.name(), RedisCache::name));
        }));
    }

    @Override
    public RedisCache getData() {
        return this.redisComboBox.getValue();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
                this.subscriptionComboBox,
                this.redisComboBox
        );
    }

    protected void createUIComponents() {
        final Supplier<List<? extends RedisCache>> loader = () -> Optional
                .ofNullable(this.subscriptionComboBox)
                .map(AzureComboBox::getValue)
                .map(Subscription::getId)
                .map(id -> Azure.az(AzureRedis.class).list(id))
                .orElse(Collections.emptyList());
        this.redisComboBox = new AzureComboBoxSimple<>(loader) {
            @Override
            protected String getItemText(Object item) {
                return Optional.ofNullable(item).map(i -> ((RedisCache) i).name()).orElse(StringUtils.EMPTY);
            }
        };
    }
}
