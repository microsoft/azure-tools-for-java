/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureEntityManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.intellij.util.ValidationUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Getter(AccessLevel.PROTECTED)
public abstract class AbstractSpringCloudAppInfoPanel extends JPanel implements AzureFormPanel<SpringCloudAppConfig> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    @Nullable
    private final SpringCloudCluster cluster;
    private final String defaultAppName;
    private SpringCloudAppConfig originalConfig;

    public AbstractSpringCloudAppInfoPanel(@Nullable final SpringCloudCluster cluster) {
        super();
        this.cluster = cluster;
        this.defaultAppName = String.format("springcloud-app-%s", DATE_FORMAT.format(new Date()));
    }

    protected void init() {
        final SubscriptionComboBox selectorSubscription = this.getSelectorSubscription();
        final SpringCloudClusterComboBox selectorCluster = this.getSelectorCluster();
        final ValidationDebouncedTextInput textName = this.getTextName();
        selectorSubscription.setRequired(true);
        selectorSubscription.addItemListener(this::onSubscriptionChanged);
        selectorCluster.setRequired(true);
        selectorCluster.addItemListener(this::onClusterChanged);
        textName.setRequired(true);
        textName.setValue(this.defaultAppName);
        textName.setValidator(() -> {
            try {
                ValidationUtils.validateSpringCloudAppName(textName.getValue(), this.cluster);
            } catch (final IllegalArgumentException e) {
                final AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(textName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
            }
            return AzureValidationInfo.OK;
        });
        if (Objects.nonNull(this.cluster)) {
            selectorSubscription.setValue(new ItemReference<>(this.cluster.subscriptionId(), Subscription::getId), true);
            selectorCluster.setValue(new ItemReference<>(this.cluster.name(), IAzureEntityManager::name), true);
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.getSelectorCluster().setSubscription(subscription);
        }
    }

    private void onClusterChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudCluster cluster = (SpringCloudCluster) e.getItem();
            this.onAppChanged(cluster.app(StringUtils.firstNonBlank(this.getTextName().getName(), this.defaultAppName)));
        }
    }

    protected void onAppChanged(SpringCloudApp app) {
        if (Objects.isNull(this.originalConfig)) {
            this.originalConfig = SpringCloudAppConfig.fromApp(app);
            this.setData(this.originalConfig);
        }
    }

    protected SpringCloudAppConfig getData(SpringCloudAppConfig config) {
        config.setSubscriptionId(this.getSelectorSubscription().getValue().getId());
        config.setClusterName(this.getSelectorCluster().getValue().name());
        config.setAppName(this.getTextName().getValue());
        return config;
    }

    public SpringCloudAppConfig getData() {
        final SpringCloudAppConfig config = Optional.ofNullable(this.originalConfig)
                .orElse(SpringCloudAppConfig.builder().deployment(SpringCloudDeploymentConfig.builder().build()).build());
        return getData(config);
    }

    @Override
    public void setData(final SpringCloudAppConfig config) {
        this.originalConfig = config;
        this.getTextName().setValue(config.getAppName());
        this.getSelectorCluster().setValue(new ItemReference<>(config.getClusterName(), IAzureEntityManager::name));
        this.getSelectorSubscription().setValue(new ItemReference<>(config.getSubscriptionId(), Subscription::getId));
    }

    @Override
    public void setVisible(final boolean visible) {
        this.getContentPanel().setVisible(visible);
        super.setVisible(visible);
    }


    protected abstract SubscriptionComboBox getSelectorSubscription();

    protected abstract SpringCloudClusterComboBox getSelectorCluster();

    protected abstract ValidationDebouncedTextInput getTextName();

    protected abstract JPanel getContentPanel();
}
