/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SpringCloudAppComboBox extends AzureComboBox<SpringCloudApp> {
    private SpringCloudCluster cluster;
    private List<SpringCloudApp> localItems = new ArrayList<>();

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final SpringCloudApp app = (SpringCloudApp) item;
        if (!app.exists()) {
            return "(New) " + app.name();
        }
        return app.name();
    }

    public void setCluster(SpringCloudCluster cluster) {
        if (Objects.equals(cluster, this.cluster)) {
            return;
        }
        this.cluster = cluster;
        if (cluster == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "springcloud|app.list.cluster",
        params = {"@cluster.name()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends SpringCloudApp> loadItems() throws Exception {
        final List<SpringCloudApp> apps = new ArrayList<>();
        if (Objects.nonNull(this.cluster)) {
            if (CollectionUtils.isNotEmpty(this.localItems)) {
                apps.addAll(this.localItems.stream()
                    .filter(i -> Objects.equals(this.cluster.name(), i.cluster().name()))
                    .collect(Collectors.toList()));
            }
            apps.addAll(cluster.apps());
        }
        return apps;
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
            AllIcons.General.Add, message("springCloud.app.create.tooltip"), this::showAppCreationPopup);
    }

    private void showAppCreationPopup() {
        final SpringCloudAppCreationDialog dialog = new SpringCloudAppCreationDialog(this.cluster);
        dialog.setOkActionListener((entity) -> {
            final SpringCloudApp app = cluster.app(entity);
            this.localItems.add(0, app);
            dialog.close();
            final List<SpringCloudApp> items = this.getItems();
            items.add(0, app);
            this.setItems(items);
            this.setValue(app);
        });
        dialog.show();
    }
}
