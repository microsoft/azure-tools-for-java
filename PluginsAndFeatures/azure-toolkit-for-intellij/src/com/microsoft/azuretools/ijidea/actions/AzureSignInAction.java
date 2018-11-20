/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.microsoft.azuretools.ijidea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.ijidea.ui.ErrorWindow;
import com.microsoft.azuretools.ijidea.ui.SignInWindow;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.SignInOutAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzureSignInAction extends AzureAnAction {
    private static final Logger LOGGER = Logger.getInstance(AzureSignInAction.class);
    private static final String SIGN_IN = "Azure Sign In...";
    private static final String SIGN_OUT = "Azure Sign Out...";

    public AzureSignInAction() throws Exception {
        super(AuthMethodManager.getInstance().isSignedIn() ? SIGN_OUT : SIGN_IN);
    }

    public AzureSignInAction(@Nullable String title) {
        super(title, title, UIHelperImpl.loadIcon(SignInOutAction.getIcon()));
    }

    @Override
    public void onActionPerformed(AnActionEvent e) {
        Project project = DataKeys.PROJECT.getData(e.getDataContext());
        onAzureSignIn(project);
    }

    @Override
    public  void update(AnActionEvent e) {
        try {
            boolean isSignIn = AuthMethodManager.getInstance().isSignedIn();
            if (isSignIn) {
                e.getPresentation().setText(SIGN_OUT);
                e.getPresentation().setDescription(SIGN_OUT);
            } else {
                e.getPresentation().setText(SIGN_IN);
                e.getPresentation().setDescription(SIGN_IN);
            }
            e.getPresentation().setIcon(UIHelperImpl.loadIcon(SignInOutAction.getIcon()));
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("update", ex);
        }
    }

    private static String getSignOutWarningMessage(@NotNull AuthMethodManager authMethodManager) {
        final AuthMethodDetails authMethodDetails = authMethodManager.getAuthMethodDetails();
        final AuthMethod authMethod = authMethodManager.getAuthMethod();
        final String warningMessage;
        switch (authMethod) {
            case SP:
                warningMessage = String.format("Signed in using file \"%s\"", authMethodDetails.getCredFilePath());
                break;
            case AD:
            case DC:
                warningMessage = String.format("Signed in as %s", authMethodDetails.getAccountEmail());
                break;
            default:
                warningMessage = "Signed in by unknown authentication method.";
                break;
        }
        return String.format("%s\n Do you really want to sign out?", warningMessage);
    }

    public static void onAzureSignIn(Project project) {
        JFrame frame = WindowManager.getInstance().getFrame(project);
        try {
            AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            boolean isSignIn = authMethodManager.isSignedIn();
            if (isSignIn) {
                int res = JOptionPane.showConfirmDialog(frame,
                    getSignOutWarningMessage(authMethodManager),
                    "Azure Sign Out",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    new ImageIcon("icons/azure.png"));
                if (res == JOptionPane.OK_OPTION) {
                    AdAuthManager adAuthManager = AdAuthManager.getInstance();
                    if (adAuthManager.isSignedIn())
                        adAuthManager.signOut();
                    authMethodManager.signOut();
                }
            } else {
                doSignIn(authMethodManager, project);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorWindow.show(project, ex.getMessage(), "AzureSignIn Action Error");
        }
    }
    
    public static boolean doSignIn(AuthMethodManager authMethodManager, Project project) throws Exception {
        boolean isSignIn = authMethodManager.isSignedIn();
        if (isSignIn) return true;
        SignInWindow w = SignInWindow.go(authMethodManager.getAuthMethodDetails(), project);
        if (w != null) {
            AuthMethodDetails authMethodDetailsUpdated = w.getAuthMethodDetails();
            authMethodManager.setAuthMethodDetails(authMethodDetailsUpdated);
            SelectSubscriptionsAction.onShowSubscriptions(project);
            authMethodManager.notifySignInEventListener();
        }
        return authMethodManager.isSignedIn();
    }
}
