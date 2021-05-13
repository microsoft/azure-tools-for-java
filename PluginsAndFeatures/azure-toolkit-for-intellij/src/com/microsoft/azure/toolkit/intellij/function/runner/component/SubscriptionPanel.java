/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.List;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SubscriptionPanel extends JPanel {
    private JComboBox<Object> cbSubscription;
    private JPanel pnlSubscription;

    private Window window;

    public SubscriptionPanel(Window window) {
        this();
        this.window = window;
    }

    public SubscriptionPanel() {
        cbSubscription.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof Subscription) {
                    setText(((Subscription) object).getName());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        // Set label for screen reader for accessibility issue
        final JLabel label = new JLabel("Subscription");
        label.setLabelFor(cbSubscription);
    }

    @Nullable
    public String getSubscriptionId() {
        final Object selectedObject = cbSubscription.getSelectedItem();
        return selectedObject instanceof Subscription ? ((Subscription) selectedObject).getId() : null;
    }

    @AzureOperation(
        name = "load selected subscriptions",
        type = AzureOperation.Type.SERVICE
    )
    public void loadSubscription() {
        beforeLoadSubscription();
        Observable.fromCallable(() -> az(AzureAccount.class).account().getSelectedSubscriptions())
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::fillSubscription);
    }

    public JComponent getComboComponent() {
        return cbSubscription;
    }

    private void beforeLoadSubscription() {
        cbSubscription.removeAllItems();
        cbSubscription.setEnabled(false);
        cbSubscription.addItem("Loading...");
    }

    private void fillSubscription(List<Subscription> subscriptions) {
        cbSubscription.removeAllItems();
        cbSubscription.setEnabled(true);
        subscriptions.forEach(subscription -> cbSubscription.addItem(subscription));
        cbSubscription.setSelectedItem(cbSubscription.getItemAt(0));
        if (window != null) {
            window.pack();
        }
    }

    public void addActionListener(ActionListener actionListener) {
        cbSubscription.addActionListener(actionListener);
    }

    public void addItemListener(ItemListener actionListener) {
        cbSubscription.addItemListener(actionListener);
    }
}
