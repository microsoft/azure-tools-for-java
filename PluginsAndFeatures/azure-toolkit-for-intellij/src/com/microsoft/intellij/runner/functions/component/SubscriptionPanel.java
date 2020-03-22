/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.runner.functions.component;

import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.List;

public class SubscriptionPanel extends JPanel {
    private JComboBox cbSubscription;
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
                    setText(((Subscription) object).displayName());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        // Set label for screen reader for accessibility issue
        final JLabel label = new JLabel("Subscription");
        label.setLabelFor(cbSubscription);
    }

    public String getSubscriptionId() {
        final Object selectedObject = cbSubscription.getSelectedItem();
        return selectedObject instanceof Subscription ? ((Subscription) selectedObject).subscriptionId() : null;
    }

    public void loadSubscription() {
        beforeLoadSubscription();
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getSelectedSubscriptions())
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::fillSubscription);
    }

    private void beforeLoadSubscription() {
        cbSubscription.removeAllItems();
        cbSubscription.setEnabled(false);
        cbSubscription.addItem("Loading...");
    }

    private void fillSubscription(List<Subscription> subscriptions) {
        cbSubscription.removeAllItems();
        cbSubscription.setEnabled(true);
        subscriptions.stream().forEach(subscription -> cbSubscription.addItem(subscription));
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
