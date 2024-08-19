/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.dbtools;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.EventDispatcher;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.EventListener;

public class NotSignedInTipLabel extends HyperlinkLabel {
    private final EventDispatcher<SignedInListener> dispatcher = EventDispatcher.create(SignedInListener.class);

    public NotSignedInTipLabel(@Nonnull String text) {
        super();

        setHtmlText(text);
        setIcon(AllIcons.General.Information);
        addHyperlinkListener(e -> signIn());
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public void addSignedInListener(@Nonnull SignedInListener listener, @Nonnull Disposable parentDisposable) {
        dispatcher.addListener(listener, parentDisposable);
    }

    protected void signIn() {
        AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) -> {
            if (Azure.az(AzureAccount.class).isLoggedIn()) {
                dispatcher.getMulticaster().onSignedIn();
            }
        });
    }

    public interface SignedInListener extends EventListener {
        void onSignedIn();
    }
}

