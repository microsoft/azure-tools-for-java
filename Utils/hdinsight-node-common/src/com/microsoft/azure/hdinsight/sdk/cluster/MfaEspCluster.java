/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;

public interface MfaEspCluster extends AzureAdAccountDetail, ILogger {
    // get path suffix user/<user_name>
    default String getUserPath() {
        if (IdeAzureAccount.getInstance().isLoggedIn()) {
            // FIXME!!! since this is only a workaround to get user folder name
            String loginUserEmail = Azure.az(AzureAccount.class).account().getUsername();
            String loginUser = loginUserEmail.substring(0, loginUserEmail.indexOf("@"));
            return String.format("%s/%s", "user", loginUser);
        }

        return "<unknown_user>";
    }
}
