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

package com.microsoft.azuretools.ui.embeddedbrowser;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JxBrowserUtil {

    private static final Map<String, String> versionMap;
    static {
        Map<String, String> temp = new HashMap<>();
        temp.put("win32", "jxbrowser-win32-6.16");
        temp.put("win64", "jxbrowser-6.16");
        temp.put("mac", "jxbrowser-mac-6.16");
        temp.put("linux64", "jxbrowser-linux64-6.16");
        temp.put("linux32", "jxbrowser-linux64-6.16");
        versionMap = Collections.unmodifiableMap(temp);
    }

    public JComponent createBrowserViewAndLoadURL(String url) throws Exception {
        Object browserView = null;
        try {
            Class<?> browserClass = Class.forName("com.teamdev.jxbrowser.chromium.Browser");
            Object browser = browserClass.newInstance();
            Class<?> browserViewClass = Class.forName("com.teamdev.jxbrowser.chromium.swing.BrowserView");
            Constructor<?> browserViewConstructor = browserViewClass.getConstructor(browserClass);
            browserView = browserViewConstructor.newInstance(browser);

            if (browserView != null) {
                Method loadURLMethod = browserClass.getMethod("loadURL", String.class);
                loadURLMethod.invoke(url);
            }
        } catch (Exception e) {
            throw new Exception("Fail to load JxBrowser or load URL: " + e.getMessage());
        }

        return (JComponent)browserView;
    }
}