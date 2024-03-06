/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.servicesview;

import com.intellij.docker.registry.DockerRegistryConfiguration;
import com.intellij.docker.view.registry.DockerRegistryProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.AzureContainerRegistryComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.accessibility.AccessibleRelation;
import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class AzureContainerRegistryConfigurator implements DockerRegistryProvider.Configurator {
    public static final String ENABLE_ADMIN_USER_DOC_LINK = "https://docs.microsoft.com/en-us/azure/container-registry/container-registry-authentication#admin-account";
    public static final String ERROR_MESSAGE_PATTERN_ADMIN_DISABLED = "Cannot perform credential operations for %s as admin user is disabled. Kindly enable admin user as per docs: " + ENABLE_ADMIN_USER_DOC_LINK + " and then refresh.";
    public static final String NOT_SIGN_IN_TIPS = "<html><a href=''>Sign in</a> to select an existing Azure Container Registry.</html>";
    public static final String NO_REGISTRY_TIPS = "<html>No Azure Container Registry. You can <a href=''>create one in Azure Portal</a> first.</html>";
    public static final String ENABLE_ADMIN_TIPS_TEMPLATE = "<html><a href=''>Enable admin user</a> for \"%s\", <a href='" + ENABLE_ADMIN_USER_DOC_LINK + "'>learn more</a>.</html>";
    @Getter
    private JPanel contentPanel;
    private SubscriptionComboBox selectorSubscription;
    private AzureContainerRegistryComboBox selectorRegistry;
    private JLabel lblSubscription;
    private JLabel lblRegistry;
    private JBLabel notSignInLabel;
    private JBLabel noRegistryLabel;
    private JBLabel enableAdminLabel;

    @Getter
    private final String defaultAddress = null;

    public AzureContainerRegistryConfigurator() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to create components.
        this.init();
    }

    private void init() {
        this.selectorSubscription.setRequired(true);
        this.selectorRegistry.setRequired(true);

        final AzureAccount account = Azure.az(AzureAccount.class);
        this.notSignInLabel.setVisible(!account.isLoggedIn());
        this.notSignInLabel.setText(NOT_SIGN_IN_TIPS);
        this.noRegistryLabel.setText(NO_REGISTRY_TIPS);

        this.selectorSubscription.putClientProperty(AccessibleRelation.LABELED_BY, this.lblSubscription);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorRegistry.putClientProperty(AccessibleRelation.LABELED_BY, this.lblRegistry);
        this.selectorRegistry.addItemListener(e -> {
            final ContainerRegistry registry = this.selectorRegistry.getValue();
            if (Objects.nonNull(registry)) {
                this.enableAdminLabel.setText(String.format(ENABLE_ADMIN_TIPS_TEMPLATE, registry.getName()));
                this.enableAdminLabel.setVisible(!registry.isAdminUserEnabled());
            }
        });
    }

    private void onSubscriptionChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = this.selectorSubscription.getValue();
            this.selectorRegistry.setSubscription(subscription);
        }
    }

    @Override
    @SneakyThrows
    public void applyDataToRegistry(@Nonnull final DockerRegistryConfiguration registry) {
        final ContainerRegistry data = this.selectorRegistry.getValue();
        // ConfigurationException would be handled at `com.intellij.openapi.options.newEditor.ConfigurableEditor.apply(com.intellij.openapi.options.Configurable)`
        if (!this.contentPanel.isDisplayable()) {
            return;
        }
        if (Objects.isNull(data)) {
            throw new ConfigurationException("Please select a valid Azure container registry.");
        } else if (!data.isAdminUserEnabled()) {
            final Action<ContainerRegistry> enableAdminUser = AzureActionManager.getInstance().getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(data);
            final AzureString message = AzureString.format(ERROR_MESSAGE_PATTERN_ADMIN_DISABLED, data.getName());
            final ConfigurationException exception = new ConfigurationException(message.toString());
            exception.setQuickFix(dataContext -> enableAdminUser.handle(null, AnActionEvent.createFromDataContext("", null, dataContext)));
            throw exception;
        }
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, "user/acr.add_docker_registry_with_acr_instance_in_services_view");
        registry.setRegistryProviderId(AzureContainerRegistryProvider.ID);
        if (StringUtils.isBlank(registry.getName()) || StringUtils.equalsAnyIgnoreCase(registry.getName().trim(), "New Azure Container Registry", "New Docker Registry")) {
            registry.setName(data.getName());
        }
        registry.setAddress(data.getLoginServerUrl());
        registry.setUsername(data.getUserName());
        registry.setPasswordSafe(data.getPrimaryCredential());
    }

    @Override
    @AzureOperation("user/acr.select_acr_type_to_add_docker_registry_in_services_view")
    public void applyRegistryToData(@Nonnull final DockerRegistryConfiguration registry) {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            this.notSignInLabel.setVisible(true);
            return;
        }
        Azure.az(AzureAccount.class).account().getSelectedSubscriptions().stream()
            .map(s -> Azure.az(AzureContainerRegistry.class).registry(s.getId()))
            .flatMap(module -> module.list().stream())
            .filter(r -> StringUtils.equalsIgnoreCase(r.getLoginServerUrl(), registry.getAddress()))
            .findAny().ifPresent(r -> {
                this.selectorSubscription.setValue(r.getSubscription());
                this.selectorRegistry.setValue(r);
            });
    }

    @Nonnull
    @Override
    public JComponent createOptionsPanel() {
        return this.contentPanel;
    }

    private void createUIComponents() {
        this.selectorRegistry = new AzureContainerRegistryComboBox(false) {
            @Override
            protected List<? extends ContainerRegistry> loadItems() throws Exception {
                final List<? extends ContainerRegistry> items = super.loadItems();
                if (Azure.az(AzureAccount.class).isLoggedIn() && CollectionUtils.isEmpty(items)) {
                    noRegistryLabel.setVisible(true);
                }
                return items;
            }
        };
        this.selectorRegistry.addValidator(this::validateAdminUserEnableStatus);

        this.notSignInLabel = new JBLabel() {
            protected HyperlinkListener createHyperlinkListener() {
                return new HyperlinkAdapter() {
                    protected void hyperlinkActivated(@NotNull final HyperlinkEvent hyperlinkEvent) {
                        notSignInLabel.setIcon(AnimatedIcon.Default.INSTANCE);
                        AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) -> {
                            notSignInLabel.setVisible(Objects.isNull(a));
                            notSignInLabel.setIcon(AllIcons.General.Error);
                        });
                    }
                };
            }
        };
        this.notSignInLabel.setCopyable(true);
        this.notSignInLabel.setIcon(AllIcons.General.Error);

        this.noRegistryLabel = new JBLabel() {
            protected HyperlinkListener createHyperlinkListener() {
                return new HyperlinkAdapter() {
                    protected void hyperlinkActivated(@NotNull final HyperlinkEvent hyperlinkEvent) {
                        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.CREATE_IN_PORTAL).handle(Azure.az(AzureContainerRegistry.class));
                    }
                };
            }
        };
        this.noRegistryLabel.setCopyable(true);
        this.noRegistryLabel.setIcon(AllIcons.General.Error);

        this.enableAdminLabel = new JBLabel() {
            protected HyperlinkListener createHyperlinkListener() {
                return new HyperlinkAdapter() {
                    protected void hyperlinkActivated(@NotNull final HyperlinkEvent e) {
                        if (Objects.nonNull(e.getURL()) && StringUtils.isNotBlank(e.getURL().toString())) {
                            BrowserUtil.open(e.getURL().toString());
                        } else {
                            enableAdminLabel.setIcon(AnimatedIcon.Default.INSTANCE);
                            final ContainerRegistry registry = Objects.requireNonNull(selectorRegistry.getValue());
                            final Action<ContainerRegistry> enableAdminUser = AzureActionManager.getInstance()
                                .getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(registry);
                            final IView.Label view = enableAdminUser.getView(registry);
                            AzureTaskManager.getInstance().runInBackground(view.getLabel(), () -> {
                                enableAdminUser.handleSync(registry);
                                selectorRegistry.reloadItems();
                                selectorRegistry.setValidationInfo(validateAdminUserEnableStatus());
                                enableAdminLabel.setVisible(!registry.isAdminUserEnabled());
                                enableAdminLabel.setIcon(AllIcons.General.Error);
                            });
                        }
                    }
                };
            }
        };
        this.enableAdminLabel.setIcon(AllIcons.General.Error);
        this.enableAdminLabel.setCopyable(true);
    }

    private AzureValidationInfo validateAdminUserEnableStatus() {
        final ContainerRegistry value = selectorRegistry.getValue();
        if (Objects.isNull(value)) {
            return AzureValidationInfo.success(selectorRegistry);
        }
        return value.isAdminUserEnabled() ? AzureValidationInfo.success(selectorRegistry) :
            AzureValidationInfo.error(String.format("Admin user is not enabled for registry (%s)", value.getName()), selectorRegistry);
    }

    @Override
    public void dispose() {

    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
