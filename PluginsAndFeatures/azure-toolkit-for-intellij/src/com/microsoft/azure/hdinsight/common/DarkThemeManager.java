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
package com.microsoft.azure.hdinsight.common;

import com.intellij.util.ui.UIUtil;

import java.awt.*;

public class DarkThemeManager {
    private DarkThemeManager(){}

    private static DarkThemeManager instance = null;

    private static final String Gold = "#FFA500";
    private static final String LightOrange = "#FFC66D";

    private static final String ColorRed = "red";
    private static final String Rose = "#FF5050";

    private static final String Black = "black";
    private static final String Gray = "#BBBBBB";

    private static final String Blue = "blue";
    private static final String LightBlue = "#5394EC";

    public static DarkThemeManager getInstance(){
        if(instance == null){
            synchronized (DarkThemeManager.class){
                if(instance == null){
                    instance = new DarkThemeManager();
                }
            }
        }

        return instance;
    }

    public String getWarningColor(){
        if(UIUtil.isUnderDarcula()){
            return LightOrange;
        }

        return Gold;
    }

    public Color getErrorMessageColor() {
        if (UIUtil.isUnderDarcula()) {
            return new Color(255, 80, 80);
        }

        return Color.red;
    }

    public Color getWarningMessageColor() {
        if (UIUtil.isUnderDarcula()) {
            return new Color(255, 198, 109);
        }

        return new Color(255, 165, 0);
    }

    public String getErrorColor(){
        if(UIUtil.isUnderDarcula()){
            return Rose;
        }

        return ColorRed;
    }

    public String getInfoColor(){
        if(UIUtil.isUnderDarcula()){
            return Gray;
        }

        return Black;
    }

    public String getHyperLinkColor(){
        if (UIUtil.isUnderDarcula()) {
            return LightBlue;
        }

        return Blue;
    }
}
