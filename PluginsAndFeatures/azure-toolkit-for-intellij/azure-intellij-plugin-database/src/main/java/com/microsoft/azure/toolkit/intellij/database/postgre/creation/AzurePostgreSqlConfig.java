/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.creation;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.intellij.common.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;
import java.util.List;

import static com.microsoft.azure.toolkit.lib.Azure.az;

@Getter
@Setter
@ToString
public class AzurePostgreSqlConfig {

    private Subscription subscription;
    private ResourceGroup resourceGroup;

    private String serverName;
    private Region region;
    private String version;

    private String adminUsername;
    private char[] password;
    private char[] confirmPassword;

    private boolean allowAccessFromAzureServices;
    private boolean allowAccessFromLocalMachine;

    public static AzurePostgreSqlConfig getDefaultPostgreSQLConfig() {

        final String defaultNameSuffix = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        final AzurePostgreSqlConfig config = new AzurePostgreSqlConfig();
        final List<Subscription> selectedSubscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(selectedSubscriptions), "There are no subscriptions in your account.");
        final Subscription subscription = selectedSubscriptions.get(0);
        config.setSubscription(subscription);
        final DraftResourceGroup resourceGroup = new DraftResourceGroup(subscription, "rs-" + defaultNameSuffix);
        config.setResourceGroup(resourceGroup);
        config.setServerName("postgresql-" + defaultNameSuffix);
        config.setAdminUsername(StringUtils.EMPTY);
        config.setVersion("11"); // default to 11
        config.setPassword(StringUtils.EMPTY.toCharArray());
        config.setConfirmPassword(StringUtils.EMPTY.toCharArray());
        config.setRegion(Utils.selectFirstOptionIfCurrentInvalid("region",
                az(AzurePostgreSql.class).listSupportedRegions(subscription.getId()),
                Region.US_EAST));
        return config;
    }

}