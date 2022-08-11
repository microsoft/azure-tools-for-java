/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.creation;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.model.PricingTier;
import com.microsoft.azure.toolkit.redis.model.RedisConfig;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RedisCreationDialog extends AzureDialog<RedisConfig> implements AzureForm<RedisConfig> {
    private static final String PRICING_LINK = "https://azure.microsoft.com/en-us/pricing/details/cache";
    private static final String DNS_NAME_REGEX = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$";
    private static final Integer REDIS_CACHE_MAX_NAME_LENGTH = 63;
    private JPanel panel;
    private SubscriptionComboBox subscriptionComboBox;
    private AzureComboBox<PricingTier> pricingComboBox;
    private ResourceGroupComboBox resourceGroupComboBox;
    private AzureTextInput redisNameTextField;
    private RegionComboBox regionComboBox;
    private JCheckBox enableNonSSLCheckBox;
    private JLabel lblPricing;
    private JLabel lblPricingHelp;

    public RedisCreationDialog(Project project) {
        super(project);
        this.init();
        initListeners();
    }

    @Override
    public AzureForm<RedisConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Cache for Redis";
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return panel;
    }

    protected void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        redisNameTextField.setValidator(() -> {
            try {
                validateRedisName(redisNameTextField.getValue());
            } catch (final Exception e) {
                final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
                return builder.input(redisNameTextField).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
            }
            return AzureValidationInfo.success(this);
        });
        lblPricingHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                BrowserUtil.browse(PRICING_LINK);
            }
        });
    }

    private void validateRedisName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException(AzureMessageBundle.message("redis.name.validate.empty").toString());
        }

        if (name.length() > REDIS_CACHE_MAX_NAME_LENGTH || !name.matches(DNS_NAME_REGEX) || StringUtils.contains(name, "--")) {
            throw new IllegalArgumentException(AzureMessageBundle.message("redis.name.validate.invalid").toString());
        }

        if (subscriptionComboBox.getValue() != null) {
            final Availability resultEntity =
                Azure.az(AzureRedis.class).forSubscription(subscriptionComboBox.getValue().getId()).checkNameAvailability(name);
            if (!resultEntity.isAvailable()) {
                final String message = resultEntity.getUnavailabilityReason();
                throw new AzureToolkitRuntimeException(message);
            }
        }

    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.resourceGroupComboBox.setSubscription(subscription);
            this.regionComboBox.setSubscription(subscription);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.resourceGroupComboBox.setSubscription(null);
            this.regionComboBox.setSubscription(null);
        }
    }

    private void createUIComponents() {
        this.subscriptionComboBox = new SubscriptionComboBox();
        this.resourceGroupComboBox = new ResourceGroupComboBox();
        this.regionComboBox = new RegionComboBox() {
            protected List<? extends Region> loadItems() {
                if (Objects.nonNull(this.subscription)) {
                    return Azure.az(AzureRedis.class).forSubscription(subscription.getId()).listSupportedRegions();
                }
                return Collections.emptyList();
            }
        };
        this.redisNameTextField = new AzureTextInput();
        this.pricingComboBox = new AzureComboBox<>(PricingTier::values) {
            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }
        };
        this.subscriptionComboBox.setRequired(true);
        this.resourceGroupComboBox.setRequired(true);
        this.regionComboBox.setRequired(true);
    }

    @Override
    public RedisConfig getValue() {
        final RedisConfig config = new RedisConfig();
        config.setSubscription(subscriptionComboBox.getValue());
        config.setResourceGroup(resourceGroupComboBox.getValue());
        config.setName(redisNameTextField.getText());
        config.setPricingTier(pricingComboBox.getValue());
        config.setRegion(regionComboBox.getValue());
        config.setEnableNonSslPort(enableNonSSLCheckBox.isSelected());
        return config;
    }

    @Override
    public void setValue(RedisConfig config) {
        Optional.ofNullable(config.getName()).ifPresent(e -> redisNameTextField.setText(e));
        Optional.ofNullable(config.getSubscription()).ifPresent(e -> subscriptionComboBox.setValue(e));
        Optional.ofNullable(config.getResourceGroup()).ifPresent(e -> resourceGroupComboBox.setValue(e));
        Optional.ofNullable(config.getPricingTier()).ifPresent(e -> pricingComboBox.setValue(e));
        Optional.ofNullable(config.getRegion()).ifPresent(e -> regionComboBox.setValue(e));
        enableNonSSLCheckBox.setSelected(config.isEnableNonSslPort());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.redisNameTextField,
            this.subscriptionComboBox,
            this.resourceGroupComboBox,
            this.pricingComboBox,
            this.regionComboBox
        };
        return Arrays.asList(inputs);
    }
}
