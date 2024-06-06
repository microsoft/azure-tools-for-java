package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;

import java.awt.*;

public class SignInHyperLinkLabel extends HyperlinkLabel {
    private static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select existing Azure resource.</html>";
    public static final String SIMPLE_NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> now.</html>";

    public SignInHyperLinkLabel() {
        super();
        this.setHtmlText(NOT_SIGNIN_TIPS);
        this.setIcon(AllIcons.General.Information);
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setVisible(!Azure.az(AzureAccount.class).isLoggedIn());
        AzureEventBus.on("account.logged_in.account", new AzureEventBus.EventListener((e) -> this.setVisible(!Azure.az(AzureAccount.class).isLoggedIn())));
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener((e) -> this.setVisible(!Azure.az(AzureAccount.class).isLoggedIn())));
        this.addHyperlinkListener(e -> {
            final DataContext context = DataManager.getInstance().getDataContext(this);
            final AnActionEvent event = AnActionEvent.createFromInputEvent(e.getInputEvent(), "ConnectorDialog", new Presentation(), context);
            AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH)
                .handle((a) -> this.setVisible(!Azure.az(AzureAccount.class).isLoggedIn()), event);
        });
    }

    public SignInHyperLinkLabel(@NlsContexts.LinkLabel final String text) {
        this();
        this.setHtmlText(text);
    }
}
