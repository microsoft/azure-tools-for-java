/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import rx.Subscription;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AppServiceComboBox<T extends AppServiceConfig> extends AzureComboBox<T> {

    protected Project project;
    protected Subscription subscription;

    @Setter
    protected T configModel;

    public AppServiceComboBox(final Project project) {
        super(false);
        this.project = project;
        this.setRenderer(new AppComboBoxRender());
    }

    @Nonnull
    @Override
    protected List<? extends T> loadItems() throws Exception {
        final List<T> items = loadAppServiceModels();
        final boolean isConfigResourceCreated = !isDraftResource(configModel) ||
                items.stream().anyMatch(item -> AppServiceConfig.isSameApp(item, configModel));
        if (isConfigResourceCreated) {
            this.configModel = null;
        } else {
            items.add(configModel);
        }
        return items;
    }

    protected T convertAppServiceToConfig(final Supplier<T> supplier, AppServiceAppBase<?, ?, ?> appService) {
        final T config = supplier.get();
        config.setResourceId(appService.getId());
        config.setName(appService.getName());
        config.setRuntime(null);
        config.setSubscription(new com.microsoft.azure.toolkit.lib.common.model.Subscription(appService.getSubscriptionId()));
        AzureTaskManager.getInstance()
            .runOnPooledThreadAsObservable(new AzureTask<>(() -> {
                config.setResourceGroup(ResourceGroupConfig.fromResource(appService.getResourceGroup()));
                config.setRuntime(appService.getRuntime());
                config.setRegion(appService.getRegion());
                config.setServicePlan(AppServicePlanConfig.fromResource(appService.getAppServicePlan()));
            })).subscribe();
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
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.Add, "Create", this::createResource);
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof AppServiceConfig) {
            final AppServiceConfig selectedItem = (AppServiceConfig) item;
            return isDraftResource(selectedItem) ? String.format("(New) %s", selectedItem.getName()) : selectedItem.getName();
        } else {
            return Objects.toString(item, StringUtils.EMPTY);
        }
    }

    protected abstract void createResource();

    public static class AppComboBoxRender extends SimpleListCellRenderer<AppServiceConfig> {

        @Override
        public void customize(JList<? extends AppServiceConfig> list, AppServiceConfig app, int index, boolean isSelected, boolean cellHasFocus) {
            if (app != null) {
                final boolean isJavaApp = Optional.ofNullable(app.getRuntime()).map(Runtime::getJavaVersion)
                        .map(javaVersion -> !Objects.equals(javaVersion, JavaVersion.OFF)).orElse(false);
                setEnabled(isJavaApp);
                setFocusable(isJavaApp);

                if (index >= 0) {
                    setText(getAppServiceLabel(app));
                } else {
                    setText(app.getName());
                }
                this.repaint();
            }
        }

        private String getAppServiceLabel(AppServiceConfig appServiceModel) {
            final String appServiceName = isDraftResource(appServiceModel) ?
                    String.format("(New) %s", appServiceModel.getName()) : appServiceModel.getName();
            final String runtime = appServiceModel.getRuntime() == null ?
                "Loading:" : WebAppService.getInstance().getRuntimeDisplayName(appServiceModel.getRuntime());
            final String resourceGroup = Optional.ofNullable(appServiceModel.getResourceGroupName()).orElse(StringUtils.EMPTY);
            return String.format("<html><div>%s</div></div><small>Runtime: %s | Resource Group: %s</small></html>",
                    appServiceName, runtime, resourceGroup);
        }
    }

    private static boolean isDraftResource(final AppServiceConfig config) {
        return config != null && StringUtils.isEmpty(config.getResourceId());
    }
}
