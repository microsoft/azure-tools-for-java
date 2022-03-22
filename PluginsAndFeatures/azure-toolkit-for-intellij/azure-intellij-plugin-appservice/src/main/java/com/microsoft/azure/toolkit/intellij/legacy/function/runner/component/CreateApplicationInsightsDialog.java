/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component;

import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class CreateApplicationInsightsDialog extends AzureDialogWrapper {
    private static final String APP_INSIGHTS_NAME_INVALID_CHARACTERS = "[*;/?:@&=+$,<>#%\\\"\\{}|^'`\\\\\\[\\]]";

    private JPanel contentPane;
    private JTextField txtInsightsName;
    private JButton buttonOK;
    private String applicationInsightsName;

    public CreateApplicationInsightsDialog() {
        super(false);
        setModal(true);
        setTitle("Create new Application Insights");

        getRootPane().setDefaultButton(buttonOK);
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> res = new ArrayList<>();
        final String insightsName = txtInsightsName.getText();
        try {
            validateApplicationInsightsName(insightsName);
        } catch (IllegalArgumentException iae) {
            res.add(new ValidationInfo(iae.getMessage(), txtInsightsName));
        }
        return res;
    }

    @Override
    protected void doOKAction() {
        applicationInsightsName = txtInsightsName.getText();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        applicationInsightsName = null;
        super.doCancelAction();
    }

    public String getApplicationInsightsName() {
        return applicationInsightsName;
    }

    public static void validateApplicationInsightsName(String applicationInsightsName) {
        if (StringUtils.isEmpty(applicationInsightsName)) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.empty"));
        }
        if (applicationInsightsName.length() > 255) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.length"));
        }
        if (applicationInsightsName.endsWith(".")) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.point"));
        }
        if (applicationInsightsName.endsWith(" ") || applicationInsightsName.startsWith(" ")) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.space"));
        }
        final Pattern pattern = Pattern.compile(APP_INSIGHTS_NAME_INVALID_CHARACTERS);
        final Matcher matcher = pattern.matcher(applicationInsightsName);
        final Set<String> invalidCharacters = new HashSet<>();
        while (matcher.find()) {
            invalidCharacters.add(matcher.group());
        }
        if (!invalidCharacters.isEmpty()) {
            throw new IllegalArgumentException(message("function.applicationInsights.validate.invalidChar", String.join(",", invalidCharacters)));
        }
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return this.txtInsightsName;
    }
}
