/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.google.common.base.Preconditions;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerVersion;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
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
public class AzureMySQLConfig {

    private Subscription subscription;
    private ResourceGroup resourceGroup;

    private String serverName;
    private Region region;
    private ServerVersion version;

    private String adminUsername;
    private char[] password;
    private char[] confirmPassword;

    private boolean allowAccessFromAzureServices;
    private boolean allowAccessFromLocalMachine;

    public static AzureMySQLConfig getDefaultAzureMySQLConfig() {
        final String defaultNameSuffix = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");
        final AzureMySQLConfig config = new AzureMySQLConfig();
        List<Subscription> selectedSubscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(selectedSubscriptions), "There is no subscription in your account.");
        Subscription subscription = selectedSubscriptions.get(0);
        config.setSubscription(subscription);
        DraftResourceGroup resourceGroup = DraftResourceGroup.builder().subscription(subscription).name("rs-" + defaultNameSuffix).build();
        config.setResourceGroup(resourceGroup);
        List<String> supportedVersions = MySQLMvpModel.listSupportedVersions();
        if (CollectionUtils.isNotEmpty(supportedVersions)) {
            config.setVersion(ServerVersion.fromString(supportedVersions.get(0)));
        }
        List<Region> supportedRegions = MySQLMvpModel.listSupportedRegions(subscription);
        if (CollectionUtils.isNotEmpty(supportedRegions)) {
            config.setRegion(supportedRegions.get(0));
        }
        config.setServerName("mysql-" + defaultNameSuffix);
        config.setAdminUsername(StringUtils.EMPTY);
        config.setPassword(StringUtils.EMPTY.toCharArray());
        config.setConfirmPassword(StringUtils.EMPTY.toCharArray());

        return config;
    }

}
