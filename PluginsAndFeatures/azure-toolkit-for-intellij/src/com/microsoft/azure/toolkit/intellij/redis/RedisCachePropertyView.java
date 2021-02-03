/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis;

import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisCacheProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisPropertyMvpView;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisPropertyViewPresenter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class RedisCachePropertyView extends BaseEditor implements RedisPropertyMvpView {

    public static final String ID = "com.microsoft.intellij.helpers.rediscache.RedisCachePropertyView";

    private final RedisPropertyViewPresenter<RedisCachePropertyView> redisPropertyViewPresenter;

    private String primaryKey = "";
    private String secondaryKey = "";

    private static final String COPY_FAIL = "Cannot copy to system clipboard.";

    private JPanel pnlContent;
    private JTextField txtNameValue;
    private JTextField txtTypeValue;
    private JTextField txtResGrpValue;
    private JTextField txtSubscriptionValue;
    private JTextField txtRegionValue;
    private JTextField txtHostNameValue;
    private JTextField txtSslPortValue;
    private JTextField txtNonSslPortValue;
    private JTextField txtVersionValue;
    private JButton btnPrimaryKey;
    private JButton btnSecondaryKey;


    public RedisCachePropertyView() {
        this.redisPropertyViewPresenter = new RedisPropertyViewPresenter<>();
        this.redisPropertyViewPresenter.onAttachView(this);

        disableTxtBoard();
        makeTxtOpaque();

        btnPrimaryKey.addActionListener(event -> {
            try {
                Utils.copyToSystemClipboard(primaryKey);
            } catch (Exception e) {
                onError(e.getMessage());
            }
        });

        btnSecondaryKey.addActionListener(event -> {
            try {
                Utils.copyToSystemClipboard(secondaryKey);
            } catch (Exception e) {
                onError(e.getMessage());
            }
        });
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return pnlContent;
    }

    @NotNull
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public void dispose() {
        redisPropertyViewPresenter.onDetachView();
    }

    @Override
    public void onReadProperty(String sid, String id) {
        redisPropertyViewPresenter.onGetRedisProperty(sid, id);
    }

    @Override
    public void showProperty(RedisCacheProperty property) {
        primaryKey = property.getPrimaryKey();
        secondaryKey = property.getSecondaryKey();

        txtNameValue.setText(property.getName());
        txtTypeValue.setText(property.getType());
        txtResGrpValue.setText(property.getGroupName());
        txtSubscriptionValue.setText(property.getSubscriptionId());
        txtRegionValue.setText(property.getRegionName());
        txtHostNameValue.setText(property.getHostName());
        txtSslPortValue.setText(String.valueOf(property.getSslPort()));
        txtNonSslPortValue.setText(String.valueOf(property.isNonSslPort()));
        txtVersionValue.setText(property.getVersion());
        btnPrimaryKey.setEnabled(true);
        btnSecondaryKey.setEnabled(true);
    }

    private void disableTxtBoard() {
        txtNameValue.setBorder(BorderFactory.createEmptyBorder());
        txtTypeValue.setBorder(BorderFactory.createEmptyBorder());
        txtResGrpValue.setBorder(BorderFactory.createEmptyBorder());
        txtSubscriptionValue.setBorder(BorderFactory.createEmptyBorder());
        txtRegionValue.setBorder(BorderFactory.createEmptyBorder());
        txtHostNameValue.setBorder(BorderFactory.createEmptyBorder());
        txtSslPortValue.setBorder(BorderFactory.createEmptyBorder());
        txtNonSslPortValue.setBorder(BorderFactory.createEmptyBorder());
        txtVersionValue.setBorder(BorderFactory.createEmptyBorder());
    }

    private void makeTxtOpaque() {
        txtNameValue.setBackground(null);
        txtTypeValue.setBackground(null);
        txtResGrpValue.setBackground(null);
        txtSubscriptionValue.setBackground(null);
        txtRegionValue.setBackground(null);
        txtHostNameValue.setBackground(null);
        txtSslPortValue.setBackground(null);
        txtNonSslPortValue.setBackground(null);
        txtVersionValue.setBackground(null);
    }
}
