/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.intellij.util;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class AzureLoginHelper {

    private static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    private static final String NO_SUBSCRIPTION = "No subscription in current account, you may get a free one from "
            + "https://azure.microsoft.com/en-us/free/";
    public static final String MUST_SELECT_SUBSCRIPTION =
            "Please select at least one subscription first (Tools -> Azure -> Select Subscriptions)";

    public static void ensureAzureSubsAvailable() throws AzureExecutionException {
        if (!AuthMethodManager.getInstance().isSignedIn()) {
            throw new AzureExecutionException(NEED_SIGN_IN);
        }
        IdentityAzureManager azureManager = IdentityAzureManager.getInstance();
        final List<Subscription> subscriptions = azureManager.getSubscriptions();
        if (CollectionUtils.isEmpty(subscriptions)) {
            throw new AzureExecutionException(NO_SUBSCRIPTION);
        }
        final List<Subscription> selectedSubscriptions = azureManager.getSelectedSubscriptions();
        if (CollectionUtils.isEmpty(selectedSubscriptions)) {
            throw new AzureExecutionException(MUST_SELECT_SUBSCRIPTION);
        }
    }

    public static boolean isAzureSubsAvailableOrReportError(String dialogTitle) {
        try {
            AzureLoginHelper.ensureAzureSubsAvailable();
            return true;
        } catch (AzureExecutionException e) {
            PluginUtil.displayErrorDialog(dialogTitle, e.getMessage());
            return false;
        }
    }
}
