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

import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.PopupMenuEvent;
import java.awt.Window;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class ResourceGroupPanel extends JPanel {
    public static final String CREATE_RESOURCE_GROUP = "Create resource group";
    private JComboBox cbResourceGroup;
    private JPanel panel1;

    private Window window;
    private String subscriptionId;
    private ResourceGroupWrapper selectedResourceGroup;

    public ResourceGroupPanel(Window window) {
        this();
        this.window = window;
    }

    public ResourceGroupPanel() {
        cbResourceGroup.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof ResourceGroupWrapper) {
                    setText(((ResourceGroupWrapper) object).getDisplayName());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        // Set label for screen reader for accessibility issue
        final JLabel label = new JLabel("Resource Group");
        label.setLabelFor(cbResourceGroup);

        cbResourceGroup.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onSelectResourceGroup();
            }
        });
    }

    public String getResourceGroupName() {
        final Object selectedObject = cbResourceGroup.getSelectedItem();
        return selectedObject instanceof ResourceGroupWrapper ? ((ResourceGroupWrapper) selectedObject).resourceGroup : null;
    }

    public boolean isNewResourceGroup() {
        final Object selectedObject = cbResourceGroup.getSelectedItem();
        return selectedObject instanceof ResourceGroupWrapper ? ((ResourceGroupWrapper) selectedObject).isCreateNewResourceGroup() : false;
    }

    public void loadResourceGroup(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        beforeLoadSubscription();
        Observable.fromCallable(() -> AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(subscriptionId))
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::fillResourceGroup);
    }

    public void addItemListener(ItemListener actionListener) {
        cbResourceGroup.addItemListener(actionListener);
    }

    private void createResourceGroup() {
        cbResourceGroup.setSelectedItem(null);
        final NewResourceGroupDialog dialog = new NewResourceGroupDialog();
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                super.windowClosed(windowEvent);
                final ResourceGroupWrapper newResourceGroup = dialog.getResourceGroup();
                if (newResourceGroup != null) {
                    cbResourceGroup.addItem(newResourceGroup);
                    cbResourceGroup.setSelectedItem(newResourceGroup);
                } else {
                    cbResourceGroup.setSelectedItem(selectedResourceGroup);
                }
            }
        });
        dialog.setVisible(true);
    }

    private void onSelectResourceGroup() {
        final Object selectedObject = cbResourceGroup.getSelectedItem();
        if (CREATE_RESOURCE_GROUP.equals(selectedObject)) {
            createResourceGroup();
        } else if (selectedObject instanceof ResourceGroupWrapper) {
            selectedResourceGroup = (ResourceGroupWrapper) selectedObject;
        }
    }

    private void beforeLoadSubscription() {
        cbResourceGroup.removeAllItems();
        cbResourceGroup.setEnabled(false);
        cbResourceGroup.addItem("Loading...");
    }

    private void fillResourceGroup(List<ResourceGroup> resourceGroups) {
        cbResourceGroup.removeAllItems();
        cbResourceGroup.setEnabled(true);
        cbResourceGroup.addItem(CREATE_RESOURCE_GROUP);
        if (resourceGroups.size() == 0) {
            cbResourceGroup.setSelectedItem(null);
        } else {
            resourceGroups.stream().forEach(resourceGroup -> cbResourceGroup.addItem(new ResourceGroupWrapper(resourceGroup)));
            // Choose the first resource group by default
            selectedResourceGroup = (ResourceGroupWrapper) cbResourceGroup.getItemAt(1);
            cbResourceGroup.setSelectedItem(selectedResourceGroup);
        }
        if (window != null) {
            window.pack();
        }
    }

    static class ResourceGroupWrapper {
        private String resourceGroup;
        private boolean isCreateNewResourceGroup;

        public ResourceGroupWrapper(String resourceGroup) {
            this.isCreateNewResourceGroup = true;
            this.resourceGroup = resourceGroup;
        }

        public ResourceGroupWrapper(ResourceGroup resourceGroup) {
            this.isCreateNewResourceGroup = false;
            this.resourceGroup = resourceGroup.name();
        }

        public String getResourceGroup() {
            return resourceGroup;
        }

        public boolean isCreateNewResourceGroup() {
            return isCreateNewResourceGroup;
        }

        public String getDisplayName() {
            return isCreateNewResourceGroup ? String.format("%s (New created)", resourceGroup) : resourceGroup;
        }
    }
}
