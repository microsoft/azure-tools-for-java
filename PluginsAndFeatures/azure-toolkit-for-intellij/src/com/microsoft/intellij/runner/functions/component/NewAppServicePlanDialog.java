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
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class NewAppServicePlanDialog extends JDialog {

    public static final String CONSUMPTION = "Consumption";
    public static final PricingTier CONSUMPTION_PRICING_TIER = new PricingTier("Consumption", "");

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel pnlNewAppServicePlan;
    private JComboBox cbPricing;
    private JComboBox cbLocation;
    private JTextField txtCreateAppServicePlan;

    private String subscriptionId;

    private boolean isNewCreatedAppServicePlan;
    private AppServicePlanPanel.AppServicePlanWrapper appServicePlan;

    public NewAppServicePlanDialog(String subscriptionId) {
        setContentPane(contentPane);
        setModal(true);
        setAlwaysOnTop(true);
        setTitle("Create App Service Plan");
        getRootPane().setDefaultButton(buttonOK);

        this.subscriptionId = subscriptionId;

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        cbLocation.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof Location) {
                    setText(((Location) object).name());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        cbPricing.setRenderer(new ListCellRendererWrapper() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof PricingTier) {
                    final PricingTier pricingTier = (PricingTier) object;
                    setText(pricingTier == CONSUMPTION_PRICING_TIER ? CONSUMPTION : pricingTier.toString());
                } else if (object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        init();
    }

    public AppServicePlanPanel.AppServicePlanWrapper getAppServicePlan() {
        return appServicePlan;
    }

    private void onOK() {
        appServicePlan = new AppServicePlanPanel.AppServicePlanWrapper(txtCreateAppServicePlan.getText(),
                (Location) cbLocation.getSelectedItem(), (PricingTier) cbPricing.getSelectedItem());
        dispose();
    }

    private void onCancel() {
        appServicePlan = null;
        dispose();
    }

    private void init() {
        onLoadPricingTier();
        onLoadLocation(subscriptionId);
    }

    public void onLoadLocation(String sid) {
        cbLocation.removeAllItems();
        Observable.fromCallable(() -> AzureMvpModel.getInstance().listLocationsBySubscriptionId(sid))
                .subscribeOn(Schedulers.newThread())
                .subscribe(locations -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    locations.stream().forEach(location -> cbLocation.addItem(location));
                }));
    }

    /**
     * Load pricing tier from model.
     */
    public void onLoadPricingTier() {
        try {
            cbPricing.removeAllItems();
            cbPricing.addItem(CONSUMPTION_PRICING_TIER);
            cbPricing.setSelectedItem(CONSUMPTION_PRICING_TIER);
            AzureFunctionMvpModel.getInstance().listFunctionPricingTier().stream()
                    .forEach(pricingTier -> cbPricing.addItem(pricingTier));
        } catch (IllegalAccessException e) {
            DefaultLoader.getUIHelper().logError("Failed to load pricing tier", e);
        }
    }
}
