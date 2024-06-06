/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AppServiceComboBox<T extends AppServiceConfig> extends AzureComboBox<T> {
    private List<T> draftItems = new LinkedList<>();

    protected Project project;

    @Setter
    protected T configModel;

    public AppServiceComboBox(final Project project) {
        super(false);
        this.project = project;
        this.setRenderer(new AppComboBoxRender(false));
    }

    @Override
    public void setValue(T val, Boolean fixed) {
        if (isDraftResource(val)) {
            this.draftItems.clear();
            this.draftItems.add(val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nonnull
    @Override
    protected List<? extends T> loadItems() throws Exception {
        final List<T> items = loadAppServiceModels();
        this.draftItems = this.draftItems.stream().filter(l -> !items.contains(l)).collect(Collectors.toList());
        items.addAll(this.draftItems);
        final boolean isConfigResourceCreated = !isDraftResource(configModel) ||
            items.stream().anyMatch(item -> isSameApp(item, configModel));
        if (isConfigResourceCreated) {
            this.configModel = null;
        } else {
            items.add(configModel);
        }
        return items;
    }

    protected T convertAppServiceToConfig(final Supplier<T> supplier, AppServiceAppBase<?, ?, ?> appService) {
        final T config = supplier.get();
        config.subscriptionId(appService.getSubscriptionId());
        config.resourceGroup(appService.getResourceGroupName());
        config.appName(appService.getName());
        config.runtime(null);
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            try {
                final AppServiceConfig fullConfig = AppServiceConfigUtils.fromAppService(appService);
                AppServiceConfigUtils.mergeAppServiceConfig(config, fullConfig);
                if (config.equals(this.getValue())) {
                    this.setValue((T) null);
                    this.setValue(config);
                }
            } catch (final Throwable ignored) {
                config.subscriptionId(null);
            }
        });
        return config;
    }

    @Override
    public T getValue() {
        if (value instanceof ItemReference && ((ItemReference<?>) value).is(configModel)) {
            return configModel;
        }
        return super.getValue();
    }

    protected abstract List<T> loadAppServiceModels() throws Exception;

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::createResource);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof AppServiceConfig) {
            final AppServiceConfig selectedItem = (AppServiceConfig) item;
            return isDraftResource(selectedItem) ? String.format("(New) %s", selectedItem.appName()) : selectedItem.appName();
        } else {
            return Objects.toString(item, StringUtils.EMPTY);
        }
    }

    protected abstract void createResource();

    public static boolean isSameApp(@Nullable final AppServiceConfig first, @Nullable final AppServiceConfig second) {
        if (Objects.isNull(first) || Objects.isNull(second)) {
            return first == second;
        }
        return (StringUtils.equalsIgnoreCase(first.appName(), second.appName()) &&
            StringUtils.equalsIgnoreCase(first.resourceGroup(), second.resourceGroup()) &&
            StringUtils.equalsIgnoreCase(first.subscriptionId(), second.subscriptionId()));
    }

    public static class AppComboBoxRender extends SimpleListCellRenderer<AppServiceConfig> {

        private final boolean enableDocker;

        public AppComboBoxRender(final boolean enableDocker) {
            super();
            this.enableDocker = enableDocker;
        }

        @Override
        public void customize(JList<? extends AppServiceConfig> list, AppServiceConfig app, int index, boolean isSelected, boolean cellHasFocus) {
            if (app != null) {
                final Runtime runtime = Optional.of(app)
                                                .filter(a -> Objects.nonNull(a.subscriptionId()))
                                                .filter(r -> Objects.nonNull(r.runtime()) &&
                                                        (r.runtime().os() == OperatingSystem.DOCKER || StringUtils.isNotBlank(r.runtime().getJavaVersion())))
                                                .map(c -> c instanceof FunctionAppConfig ?
                                                          RuntimeConfig.toFunctionAppRuntime(c.runtime()) : RuntimeConfig.toWebAppRuntime(c.runtime()))
                                                .orElse(null);
                final boolean isJavaApp = Optional.ofNullable(runtime)
                    .map(Runtime::getJavaVersion)
                    .map(javaVersion -> !Objects.equals(javaVersion, JavaVersion.OFF)).orElse(false);
                final boolean isDocker = Optional.ofNullable(runtime).map(Runtime::isDocker).orElse(false);
                setEnabled(isJavaApp || (isDocker && enableDocker));
                setFocusable(isJavaApp);

                if (index >= 0) {
                    setText(getAppServiceLabel(app, runtime));
                } else {
                    setText(app.appName());
                }

                getAccessibleContext().setAccessibleDescription(app.appName());

                this.repaint();
            }
        }

        private String getAppServiceLabel(@Nonnull AppServiceConfig appServiceModel, @Nullable Runtime runtime) {
            if (Objects.isNull(appServiceModel.subscriptionId())) {
                return String.format("<html><div>[UNKNOWN] %s</div></html>", appServiceModel.appName());
            }
            final String appServiceName = isDraftResource(appServiceModel) ?
                String.format("(New) %s", appServiceModel.appName()) : appServiceModel.appName();
            final String label = appServiceModel.getRuntime() == null ? "Loading:" :
                    Optional.ofNullable(runtime).map(Runtime::getDisplayName).orElse("UNKNOWN");
            final String resourceGroup = Optional.ofNullable(appServiceModel.getResourceGroup()).orElse(StringUtils.EMPTY);
            return String.format("<html><div>%s</div></div><small>Runtime: %s | Resource Group: %s</small></html>", appServiceName, label, resourceGroup);
        }
    }

    private static boolean isDraftResource(@Nullable final AppServiceConfig config) {
        if (Objects.isNull(config) || StringUtils.isBlank(config.subscriptionId()) || !Azure.az(AzureAccount.class).isLoggedIn()) {
            return false;
        }
        final AbstractAzResourceModule<?, ?, ?> module = config instanceof FunctionAppConfig ?
                                                         Azure.az(AzureFunctions.class).functionApps(config.subscriptionId()) :
                                                         Azure.az(AzureWebApp.class).webApps(config.subscriptionId());
        return !module.exists(config.appName(), config.resourceGroup());
    }
}
