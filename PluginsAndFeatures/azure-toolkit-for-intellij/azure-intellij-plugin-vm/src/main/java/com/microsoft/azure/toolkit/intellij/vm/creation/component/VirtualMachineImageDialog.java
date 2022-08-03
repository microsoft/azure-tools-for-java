/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImage;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImageOffer;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImagePublisher;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImageSku;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;

public class VirtualMachineImageDialog extends AzureDialog<VmImage> implements AzureForm<VmImage> {
    private JLabel lblPublisher;
    private JLabel lblOffer;
    private JLabel lblSku;
    private JLabel lblImage;
    private JPanel pnlRoot;
    private ImagePublisherComboBox cbPublisher;
    private ImageOfferComboBox cbOffer;
    private ImageSkuComboBox cbSku;
    private ImageComboBox cbImage;

    private Region region;
    private Subscription subscription;

    public VirtualMachineImageDialog(final Subscription subscription, final Region region) {
        super();
        this.subscription = subscription;
        this.region = region;
        $$$setupUI$$$();
        init();
    }

    @Override
    public AzureForm<VmImage> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Select Virtual Machine Image";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public VmImage getValue() {
        return cbImage.getValue();
    }

    @Override
    public void setValue(VmImage data) {

    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbImage, cbPublisher, cbOffer, cbSku);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        cbPublisher = new ImagePublisherComboBox(subscription, region);
        cbOffer = new ImageOfferComboBox();
        cbSku = new ImageSkuComboBox();
        cbImage = new ImageComboBox();

        cbPublisher.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof VmImagePublisher) {
                final VmImagePublisher publisher = (VmImagePublisher) e.getItem();
                this.cbOffer.setPublisher(publisher);
            }
        });

        cbOffer.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof VmImageOffer) {
                final VmImageOffer offer = (VmImageOffer) e.getItem();
                this.cbSku.setOffer(offer);
            }
        });

        cbSku.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof VmImageSku) {
                final VmImageSku sku = (VmImageSku) e.getItem();
                this.cbImage.setImageSku(sku);
            }
        });

        cbPublisher.reloadItems();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
