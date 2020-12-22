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

package com.microsoft.azure.toolkit.lib.mysql;

import com.google.common.base.Preconditions;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerVersion;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.lib.appservice.DraftResourceGroup;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;
import java.util.List;

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
        List<Subscription> selectedSubscriptions = AzureMvpModel.getInstance().getSelectedSubscriptions();
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(selectedSubscriptions), "There is no subscription in your account.");
        Subscription subscription = selectedSubscriptions.get(0);
        config.setSubscription(subscription);
        DraftResourceGroup resourceGroup = DraftResourceGroup.builder().subscription(subscription).name("rs-" + defaultNameSuffix).build();
        config.setResourceGroup(resourceGroup);
        List<String> supportedVersions = MySQLMvpModel.listSupportedVersions();
        if (CollectionUtils.isNotEmpty(supportedVersions)) {
            config.setVersion(ServerVersion.fromString(supportedVersions.get(0)));
        }
        List<Region> supportedRegions = MySQLMvpModel.listSupportedRegions();
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
