/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
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
            selectorSubscription.setValue(new ItemReference<>(this.cluster.subscriptionId(), Subscription::getId));
            selectorCluster.setValue(new ItemReference<>(this.cluster.name(), IAzureResource::name));
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
            final SpringCloudCluster c = (SpringCloudCluster) e.getItem();
            final String appName = StringUtils.firstNonBlank(this.getTextName().getName(), this.defaultAppName);
            final SpringCloudApp app = c.app(new SpringCloudAppEntity(appName, c.entity()));
            this.onAppChanged(app);
        }
    }

    protected void onAppChanged(SpringCloudApp app) {
        if (Objects.isNull(this.originalConfig)) {
            AzureTaskManager.getInstance().runOnPooledThread(() -> {
                this.originalConfig = SpringCloudAppConfig.fromApp(app);
                AzureTaskManager.getInstance().runLater(() -> this.setData(this.originalConfig));
            });
        }
    }

    protected SpringCloudAppConfig getData(SpringCloudAppConfig config) {
        config.setSubscriptionId(Optional.ofNullable(this.getSelectorSubscription().getValue()).map(Subscription::getId).orElse(null));
        config.setClusterName(Optional.ofNullable(this.getSelectorCluster().getValue()).map(IAzureResource::name).orElse(null));
        config.setAppName(this.getTextName().getValue());
        return config;
    }

    public SpringCloudAppConfig getData() {
        final SpringCloudAppConfig config = Optional.ofNullable(this.originalConfig)
                .orElse(SpringCloudAppConfig.builder().deployment(SpringCloudDeploymentConfig.builder().build()).build());
        return getData(config);
    }

    @Override
    public synchronized void setData(final SpringCloudAppConfig config) {
        final Integer count = config.getDeployment().getInstanceCount();
        config.getDeployment().setInstanceCount(Objects.isNull(count) || count == 0 ? 1 : count);
        this.originalConfig = config;
        this.getTextName().setValue(config.getAppName());
        if (Objects.nonNull(config.getClusterName())) {
            this.getSelectorCluster().setValue(new ItemReference<>(config.getClusterName(), IAzureResource::name));
        }
        if (Objects.nonNull(config.getSubscriptionId())) {
            this.getSelectorSubscription().setValue(new ItemReference<>(config.getSubscriptionId(), Subscription::getId));
        }
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
