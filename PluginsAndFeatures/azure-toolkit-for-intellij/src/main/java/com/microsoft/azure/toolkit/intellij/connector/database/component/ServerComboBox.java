/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.mysql.service.MySqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.service.impl.SqlServer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Function;

public class ServerComboBox<T> extends AzureComboBox<T> {

    @Getter
    private Subscription subscription;
    @Setter
    private Function<T, String> itemTextFunc;

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return Objects.nonNull(itemTextFunc) && Objects.nonNull(item) ? itemTextFunc.apply((T) item) : super.getItemText(item);
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public AzureValidationInfo doValidate() {
        final AzureValidationInfo info = super.doValidate();
        if (!AzureValidationInfo.OK.equals(info)) {
            return info;
        }
        final Object value = this.getValue();
        /**
         * TODO (Qianjin) : refactor the if condition
         */
        if ((value instanceof MySqlServer && !StringUtils.equals("Ready", ((MySqlServer) value).entity().getState())) ||
                (value instanceof SqlServer && !StringUtils.equals("Ready", ((SqlServer) value).entity().getState()))) {
            return AzureValidationInfo.builder().input(this).message("This server is not ready. please start it first.")
                    .type(AzureValidationInfo.Type.ERROR).build();

        }
        return info;
    }
}
