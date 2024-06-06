/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.keyvalue;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class KeyValueConnectorPanel implements AzureFormJPanel<Resource<KeyValueData>> {
    private JLabel lblConnectionName;
    private AzureTextInput txtConnectionName;
    private JLabel lblConnectionString;
    private AzureTextInput txtConnectionString;
    private JPanel pnlRoot;
    private String id = UUID.randomUUID().toString();

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(Resource<KeyValueData> target) {
        this.id = target.getDataId();
        Optional.ofNullable(target.getData()).ifPresent(data -> {
            this.txtConnectionName.setValue(data.getKey());
            this.txtConnectionString.setValue(data.getValue());
        });
    }

    @Override
    public Resource<KeyValueData> getValue() {
        final String id = StringUtils.isEmpty(this.id) ? UUID.randomUUID().toString() : this.id;
        final String connectionName = txtConnectionName.getValue();
        final String connectionString = txtConnectionString.getValue();
        final KeyValueData target = KeyValueData.builder().id(id).value(connectionString).key(connectionName).build();
        return KeyValueResource.Definition.INSTANCE.define(target);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtConnectionName, txtConnectionString);
    }
}
