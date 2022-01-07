/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.property;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.BaseEditor;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.redis.RedisCache;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

public class RedisCachePropertiesEditor extends BaseEditor {

    public static final String ID = "com.microsoft.intellij.helpers.rediscache.RedisCachePropertyView";

    private String primaryKey = "";
    private String secondaryKey = "";

    private static final String COPY_FAIL = "Cannot copy to system clipboard.";

    private JPanel contentPanel;
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

    @Nonnull
    private final RedisCache redis;

    public RedisCachePropertiesEditor(@Nonnull Project project, @Nonnull RedisCache redis, @Nonnull final VirtualFile virtualFile) {
        super(virtualFile);
        this.redis = redis;
        this.rerender();
        this.initListeners();
    }

    private void rerender() {
        AzureTaskManager.getInstance().runLater(() -> {
            disableTxtBoard();
            makeTxtOpaque();
            primaryKey = this.redis.getPrimaryKey();
            secondaryKey = this.redis.getSecondaryKey();
            txtNameValue.setText(this.redis.getName());
            txtTypeValue.setText(this.redis.getType());
            txtResGrpValue.setText(this.redis.getResourceGroup());
            txtSubscriptionValue.setText(this.redis.getSubscriptionId());
            txtRegionValue.setText(Optional.ofNullable(this.redis.getRegion()).map(Region::getName).orElse(""));
            txtHostNameValue.setText(this.redis.getHostName());
            txtSslPortValue.setText(String.valueOf(this.redis.getSSLPort()));
            txtNonSslPortValue.setText(String.valueOf(this.redis.isNonSslPortEnabled()));
            txtVersionValue.setText(this.redis.getRedisVersion());
            btnPrimaryKey.setEnabled(true);
            btnSecondaryKey.setEnabled(true);
        });
    }

    private void initListeners() {
        btnPrimaryKey.addActionListener(event -> CopyPasteManager.getInstance().setContents(new StringSelection(primaryKey)));
        btnSecondaryKey.addActionListener(event -> CopyPasteManager.getInstance().setContents(new StringSelection(secondaryKey)));
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

    @Nonnull
    @Override
    public JComponent getComponent() {
        return contentPanel;
    }

    @Nonnull
    @Override
    public String getName() {
        return this.redis.getName();
    }

    @Override
    public void dispose() {
    }
}
