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
package com.microsoft.intellij.util;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.List;

public class AzureUtils {

    public static final String NEED_SIGN_IN = "Please sign in with your Azure account.";
    public static final String NO_SUBSCRIPTION = "No subscription in current account, you may get a free one from "
            + "https://azure.microsoft.com/en-us/free/";
    public static final String MUST_SELECT_SUBSCRIPTION =
            "Please select at least one subscription first (Tools -> Azure -> Select Subscriptions)";

    public static void checkAzurePreconditions() throws AzureExecutionException {
        if (!AuthMethodManager.getInstance().isSignedIn()) {
            throw new AzureExecutionException(NEED_SIGN_IN);
        }
        try {
            final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            final List<SubscriptionDetail> subscriptions =
                    azureManager.getSubscriptionManager().getSubscriptionDetails();
            if (CollectionUtils.isEmpty(subscriptions)) {
                throw new AzureExecutionException(NO_SUBSCRIPTION);
            }
            final List<SubscriptionDetail> selectedSubscriptions =
                    azureManager.getSubscriptionManager().getSelectedSubscriptionDetails();
            if (CollectionUtils.isEmpty(selectedSubscriptions)) {
                throw new AzureExecutionException(MUST_SELECT_SUBSCRIPTION);
            }
        } catch (IOException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        }
    }

    public static boolean checkAzurePreconditionsAndPrompt(String dialogTitle) {
        try {
            AzureUtils.checkAzurePreconditions();
            return true;
        } catch (AzureExecutionException e) {
            PluginUtil.displayErrorDialog(dialogTitle, e.getMessage());
            return false;
        }
    }
}
