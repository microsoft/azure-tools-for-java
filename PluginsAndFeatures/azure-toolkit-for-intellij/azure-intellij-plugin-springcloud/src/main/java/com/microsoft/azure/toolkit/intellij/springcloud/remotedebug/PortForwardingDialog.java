/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstanceComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PortForwardingDialog extends DialogWrapper {
    private JPanel contentPanel;
    private SpringCloudAppComboBox appComboBox;
    private JLabel appLabel;
    private JLabel appInstanceLabel;
    private SpringCloudAppInstanceComboBox appInstanceComboBox;
    private SubscriptionComboBox subscriptionComboBox;
    private SpringCloudClusterComboBox appServiceComboBox;

    protected PortForwardingDialog(@Nullable Project project) {
        super(project);
        init();
        initListeners();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPanel;
    }

    public void setSelectedAppInstance(SpringCloudAppInstance appInstance) {
        subscriptionComboBox.setValue(appInstance.getSubscription());
        appServiceComboBox.setValue(appInstance.getParent().getParent().getParent());
        appComboBox.setValue(appInstance.getParent().getParent());
        appInstanceComboBox.setValue(appInstance);
    }

    public SpringCloudAppInstance getSelectedAppInstance() {
        return appInstanceComboBox.getValue();
    }

    private void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.appServiceComboBox.addItemListener(this::onClusterChanged);
        this.appComboBox.addItemListener(this::onAppChanged);
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = this.subscriptionComboBox.getValue();
            this.appServiceComboBox.setSubscription(subscription);
        }
    }

    private void onClusterChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudCluster cluster = this.appServiceComboBox.getValue();
            this.appComboBox.setCluster(cluster);
        }
    }

    private void onAppChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudApp app = this.appComboBox.getValue();
            this.appInstanceComboBox.setApp(app);
        }
    }

    private void createUIComponents() {
        this.appComboBox = new SpringCloudAppComboBox() {
            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                final ArrayList<ExtendableTextComponent.Extension> list = new ArrayList<>();
                final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK);
                final String tooltip = String.format("Refresh (%s)", KeymapUtil.getKeystrokeText(keyStroke));
                final ExtendableTextComponent.Extension refreshEx = ExtendableTextComponent.Extension.create(AllIcons.Actions.Refresh, tooltip, this::refreshItems);
                this.registerShortcut(keyStroke, refreshEx);
                list.add(refreshEx);
                return list;
            }

            @Override
            protected @NotNull List<? extends SpringCloudApp> loadItems() {
                final SpringCloudCluster cluster = appServiceComboBox.getValue();
                final List<SpringCloudApp> apps = new ArrayList<>();
                Optional.ofNullable(cluster).ifPresent(springCloudCluster ->
                        apps.addAll(cluster.apps().list().stream().filter(SpringCloudApp::isRemoteDebuggingEnabled).collect(Collectors.toList())));
                return apps;
            }
        };
    }
}
