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

package com.microsoft.intellij.helpers.containerregistry;

import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.ui.containerregistry.ContainerRegistryProperty;
import com.microsoft.intellij.helpers.base.BaseEditor;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryPropertyViewPresenter;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ContainerRegistryPropertyView extends BaseEditor implements ContainerRegistryPropertyMvpView {

    public static final String ID = ContainerRegistryPropertyView.class.getName();
    private static final Color COLOR_CHOSEN = new Color(191, 238, 251);
    private static final Color COLOR_UNCHOSEN = null;

    private final ContainerRegistryPropertyViewPresenter<ContainerRegistryPropertyView> containerPropertyPresenter;

    boolean isAdminEnabled;
    private String registryId = "";
    private String subscriptionId = "";
    private String password = "";
    private String password2 = "";

    private JPanel pnlMain;
    private JTextField txtName;
    private JTextField txtResGrp;
    private JTextField txtSubscription;
    private JTextField txtRegion;
    private JTextField txtServerUrl;
    private JTextField txtUserName;
    private JButton btnPrimaryPassword;
    private JButton btnSecondaryPassword;
    private JTextField txtType;
    private JLabel lblSecondaryPwd;
    private JLabel lblPrimaryPwd;
    private JLabel lblUserName;
    private JButton btnEnable;
    private JButton btnDisable;
    private JPanel pnlAdminUserBtn;

    /**
     * Constructor of ACR property view.
     */
    public ContainerRegistryPropertyView() {
        this.containerPropertyPresenter = new ContainerRegistryPropertyViewPresenter<>();
        this.containerPropertyPresenter.onAttachView(this);

        disableTxtBoard();
        makeTxtOpaque();

        btnPrimaryPassword.addActionListener(event -> {
            try {
                Utils.copyToSystemClipboard(password);
            } catch (Exception e) {
                onError(e.getMessage());
            }
        });

        btnSecondaryPassword.addActionListener(event -> {
            try {
                Utils.copyToSystemClipboard(password2);
            } catch (Exception e) {
                onError(e.getMessage());
            }
        });

        btnEnable.addActionListener(this::onAdminUserBtnClick);
        btnDisable.addActionListener(this::onAdminUserBtnClick);
    }

    private void onAdminUserBtnClick(ActionEvent actionEvent) {
        btnEnable.setEnabled(false);
        btnDisable.setEnabled(false);
        this.containerPropertyPresenter.onEnableAdminUser(subscriptionId, registryId, !isAdminEnabled);
    }

    private void updateAdminUserBtn(boolean isAdminEnabled) {
        btnEnable.setEnabled(!isAdminEnabled);
        btnDisable.setEnabled(isAdminEnabled);
        btnEnable.setBackground(isAdminEnabled ? COLOR_CHOSEN : COLOR_UNCHOSEN);
        btnDisable.setBackground(!isAdminEnabled ? COLOR_CHOSEN : COLOR_UNCHOSEN);
    }


    @NotNull
    @Override
    public JComponent getComponent() {
        return pnlMain;
    }

    @NotNull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        containerPropertyPresenter.onAttachView(this);
    }

    @Override
    public void onReadProperty(String sid, String id) {
        containerPropertyPresenter.onGetRegistryProperty(sid, id);
    }


    @Override
    public void showProperty(ContainerRegistryProperty property) {
        registryId = property.getId();
        subscriptionId = property.getSubscriptionId();
        isAdminEnabled = property.isAdminEnabled();

        txtName.setText(property.getName());
        txtType.setText(property.getType());
        txtResGrp.setText(property.getGroupName());
        txtSubscription.setText(subscriptionId);
        txtRegion.setText(property.getRegionName());
        txtServerUrl.setText(property.getLoginServerUrl());

        lblUserName.setVisible(isAdminEnabled);
        txtUserName.setVisible(isAdminEnabled);
        lblPrimaryPwd.setVisible(isAdminEnabled);
        btnPrimaryPassword.setVisible(isAdminEnabled);
        lblSecondaryPwd.setVisible(isAdminEnabled);
        btnSecondaryPassword.setVisible(isAdminEnabled);
        if (isAdminEnabled) {
            txtUserName.setText(property.getUserName());
            password = property.getPassword();
            password2 = property.getPassword2();
        }
        updateAdminUserBtn(isAdminEnabled);
    }

    private void disableTxtBoard() {
        txtName.setBorder(BorderFactory.createEmptyBorder());
        txtType.setBorder(BorderFactory.createEmptyBorder());
        txtResGrp.setBorder(BorderFactory.createEmptyBorder());
        txtSubscription.setBorder(BorderFactory.createEmptyBorder());
        txtRegion.setBorder(BorderFactory.createEmptyBorder());
        txtServerUrl.setBorder(BorderFactory.createEmptyBorder());
        txtUserName.setBorder(BorderFactory.createEmptyBorder());
    }

    private void makeTxtOpaque() {
        txtName.setBackground(null);
        txtType.setBackground(null);
        txtResGrp.setBackground(null);
        txtSubscription.setBackground(null);
        txtRegion.setBackground(null);
        txtServerUrl.setBackground(null);
        txtUserName.setBackground(null);
    }
}
