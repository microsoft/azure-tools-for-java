/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public interface MfaEspCluster extends AzureAdAccountDetail, ILogger {
    // get path suffix user/<user_name>
    default String getUserPath() {
        if (AuthMethodManager.getInstance().isSignedIn()) {
            // FIXME!!! since this is only a workaround to get user folder name
            String loginUserEmail = AuthMethodManager.getInstance().getAuthMethodDetails().getAccountEmail();
            String loginUser = loginUserEmail.substring(0, loginUserEmail.indexOf("@"));
            return String.format("%s/%s", "user", loginUser);
        }

        return "<unknown_user>";
    }
}
