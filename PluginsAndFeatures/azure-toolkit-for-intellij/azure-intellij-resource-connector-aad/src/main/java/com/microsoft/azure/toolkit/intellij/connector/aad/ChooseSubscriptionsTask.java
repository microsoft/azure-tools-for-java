/*
 * Copyright (c) 2018-2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

class ChooseSubscriptionsTask implements Runnable {
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.aad");

    @NotNull
    private final Project project;
    @NotNull
    private final List<Subscription> subscriptions;
    @NotNull
    private final Consumer<Subscription> callback;

    public ChooseSubscriptionsTask(@NotNull Project project,
                                   @NotNull List<Subscription> subscriptions,
                                   @NotNull Consumer<Subscription> callback) {
        this.project = project;
        this.subscriptions = subscriptions;
        this.callback = callback;
    }

    @Override
    public void run() {
        if (project.isDisposed()) {
            return;
        }

        var step = new BaseListPopupStep<>(MessageBundle.message("selectSubscriptionPopup.title"), subscriptions) {
            @Override
            @Nullable
            public PopupStep<?> onChosen(Subscription selectedValue, boolean finalChoice) {
                doFinalStep(() -> callback.accept(selectedValue));
                return FINAL_CHOICE;
            }

            @Override
            @NotNull
            public String getTextFor(Subscription value) {
                if (value != null) {
                    return String.format("%s (%s)", value.getName(), value.getId());
                }
                return "";
            }
        };

        LOG.debug("Showing popup to select Azure AD subscription");
        JBPopupFactory.getInstance().createListPopup(step).showCenteredInCurrentWindow(project);
    }
}
