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
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.Subscription;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AppServicePlanPanel extends JPanel {
    private static final String CREATE_APP_SERVICE_PLAN = "Create app service plan";
    private JComboBox cbAppServicePlan;
    private JLabel lblLocation;
    private JLabel lblPricingTier;
    private JPanel pnlRoot;

    private Window window;
    private Subscription subscription;

    private String subscriptionId;
    private OperatingSystem operatingSystem;
    private AppServicePlanWrapper selectedAppServicePlan = null;
    private List<AppServicePlanWrapper> appServicePlanWrapperList = new ArrayList<>();

    public AppServicePlanPanel() {
        cbAppServicePlan.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof AppServicePlanWrapper || object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        cbAppServicePlan.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onSelectAppServicePlan();
            }
        });

        // Set label for screen reader for accessibility issue
        final JLabel label = new JLabel("App Service Plan");
        label.setLabelFor(cbAppServicePlan);
    }

    public AppServicePlanPanel(Window window) {
        this();
        this.window = window;
    }

    public void setOSType(OperatingSystem os) {
        this.operatingSystem = os;
        reloadAppServicePlan();
    }

    public boolean isNewAppServicePlan() {
        return selectedAppServicePlan == null ? false : selectedAppServicePlan.isNewCreate();
    }

    public String getAppServicePlanName() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.getName();
    }

    public Region getAppServicePlanRegion() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.getRegion();
    }

    public PricingTier getAppServicePlanPricingTier() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.getPricingTier();
    }

    public String getAppServicePlanResourceGroup() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.resourceGroup;
    }

    public void loadAppServicePlan(String subscriptionId, OperatingSystem operatingSystem) {
        this.subscriptionId = subscriptionId;
        this.operatingSystem = operatingSystem;
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
        beforeLoadAppServicePlan();
        subscription = Observable.fromCallable(() -> {
            return AzureFunctionMvpModel.getInstance()
                    .listAppServicePlanBySubscriptionId(subscriptionId).stream()
                    .sorted((first, second) -> StringUtils.compare(first.name(), second.name()))
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.newThread()).subscribe(this::fillAppServicePlan);
    }

    public void addItemListener(ItemListener actionListener) {
        cbAppServicePlan.addItemListener(actionListener);
    }

    private void onSelectAppServicePlan() {
        final Object selectedObject = cbAppServicePlan.getSelectedItem();
        if (selectedObject instanceof AppServicePlanWrapper) {
            selectedAppServicePlan = (AppServicePlanWrapper) selectedObject;
            showAppServicePlan(selectedAppServicePlan);
        } else if (CREATE_APP_SERVICE_PLAN.equals(selectedObject)) {
            createAppServicePlan();
        }
    }

    private void createAppServicePlan() {
        cbAppServicePlan.setSelectedItem(null);
        final NewAppServicePlanDialog dialog = new NewAppServicePlanDialog(subscriptionId);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                super.windowClosed(windowEvent);
                final AppServicePlanWrapper newCreateAppServicePlan = dialog.getAppServicePlan();
                if (newCreateAppServicePlan != null) {
                    selectedAppServicePlan = newCreateAppServicePlan;
                    appServicePlanWrapperList.add(selectedAppServicePlan);
                }
                reloadAppServicePlan();
            }
        });
        dialog.setVisible(true);
    }

    private void showAppServicePlan(AppServicePlanWrapper appServicePlanWrapper) {
        if (appServicePlanWrapper == null) {
            lblLocation.setText("N/A");
            lblPricingTier.setText("N/A");
        } else {
            lblLocation.setText(appServicePlanWrapper.getRegion().name());
            lblPricingTier.setText(appServicePlanWrapper.getPricingTier().toString());
        }
    }

    private void beforeLoadAppServicePlan() {
        showAppServicePlan(null);
        cbAppServicePlan.removeAllItems();
        cbAppServicePlan.setEnabled(false);
        cbAppServicePlan.addItem("Loading...");
    }

    private void fillAppServicePlan(List<AppServicePlan> appServicePlans) {
        appServicePlanWrapperList = appServicePlans.stream()
                .map(appServicePlan -> new AppServicePlanWrapper(appServicePlan))
                .collect(Collectors.toCollection(ArrayList::new));
        reloadAppServicePlan();
    }

    private void reloadAppServicePlan() {
        cbAppServicePlan.removeAllItems();
        cbAppServicePlan.setEnabled(true);
        cbAppServicePlan.addItem(CREATE_APP_SERVICE_PLAN);

        final List<AppServicePlanWrapper> items = appServicePlanWrapperList.stream()
                .filter(appServicePlanWrapper -> appServicePlanWrapper.isNewCreate() || operatingSystem.equals(appServicePlanWrapper.getOperatingSystem()))
                .collect(Collectors.toList());
        items.stream().forEach(appServicePlanWrapper -> cbAppServicePlan.addItem(appServicePlanWrapper));
        final AppServicePlanWrapper selectedItem = items.size() == 0 ? null :
                (selectedAppServicePlan == null || !items.contains(selectedAppServicePlan)) ? items.get(0) : selectedAppServicePlan;
        cbAppServicePlan.setSelectedItem(selectedItem);
        showAppServicePlan(selectedItem);

        if (window != null) {
            window.pack();
        }
    }

    static class AppServicePlanWrapper {

        private static final String NEW_CREATED_PATTERN = "%s (New Created)";

        private boolean isNewCreate;
        private String name;
        private String resourceGroup;
        private Region region;
        private PricingTier pricingTier;
        private OperatingSystem operatingSystem;

        public AppServicePlanWrapper(AppServicePlan appServicePlan) {
            this.isNewCreate = false;
            this.name = appServicePlan.name();
            this.region = appServicePlan.region();
            this.pricingTier = appServicePlan.pricingTier();
            this.operatingSystem = appServicePlan.operatingSystem();
            this.resourceGroup = appServicePlan.resourceGroupName();
        }

        public AppServicePlanWrapper(String name, Location location, PricingTier pricingTier) {
            this.isNewCreate = true;
            this.name = name;
            this.region = location.region();
            this.pricingTier = pricingTier;
        }

        public boolean isNewCreate() {
            return isNewCreate;
        }

        public String getName() {
            return name;
        }

        public Region getRegion() {
            return region;
        }

        public PricingTier getPricingTier() {
            return pricingTier;
        }

        public OperatingSystem getOperatingSystem() {
            return operatingSystem;
        }

        public String getResourceGroup() {
            return resourceGroup;
        }

        @Override
        public String toString() {
            return isNewCreate ? String.format(NEW_CREATED_PATTERN, name) : String.format("%s (Resource Group: %s)", name, resourceGroup);
        }
    }
}
