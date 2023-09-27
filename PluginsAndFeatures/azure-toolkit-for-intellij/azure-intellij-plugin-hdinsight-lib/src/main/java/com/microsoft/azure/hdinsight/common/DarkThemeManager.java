/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.ui.UIUtil;

import java.awt.*;

public class DarkThemeManager {
    private DarkThemeManager() {
    }

    private static DarkThemeManager instance = null;

    private static final String Gold = "#FFA500";
    private static final String LightOrange = "#FFC66D";

    private static final String ColorRed = "red";
    private static final String Rose = "#FF5050";

    private static final String Black = "black";
    private static final String Gray = "#BBBBBB";

    private static final String Blue = "blue";
    private static final String LightBlue = "#5394EC";

    public static DarkThemeManager getInstance() {
        if (instance == null) {
            synchronized (DarkThemeManager.class) {
                if (instance == null) {
                    instance = new DarkThemeManager();
                }
            }
        }

        return instance;
    }

    public String getWarningColor() {
        if (UIUtil.isUnderDarcula()) {
            return LightOrange;
        }

        return Gold;
    }

    public Color getErrorTextBackgroundColor() {
        final EditorColorsScheme editorColorsScheme =
                EditorColorsManager.getInstance().getGlobalScheme();
        final Color consoleBackgroundColor =
                editorColorsScheme.getColor(ConsoleViewContentType.CONSOLE_BACKGROUND_KEY);
        return consoleBackgroundColor != null
                ? consoleBackgroundColor
                : editorColorsScheme.getDefaultBackground();
    }

    public Color getErrorMessageColor() {
        return UIUtil.getErrorForeground();
    }

    public Color getWarningMessageColor() {
        if (UIUtil.isUnderDarcula()) {
            return new Color(255, 198, 109);
        }

        return new Color(255, 165, 0);
    }

    public String getErrorColor() {
        if (UIUtil.isUnderDarcula()) {
            return Rose;
        }

        return ColorRed;
    }

    public String getInfoColor() {
        if (UIUtil.isUnderDarcula()) {
            return Gray;
        }

        return Black;
    }

    public String getHyperLinkColor() {
        if (UIUtil.isUnderDarcula()) {
            return LightBlue;
        }

        return Blue;
    }
}
