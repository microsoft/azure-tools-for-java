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

package com.microsoft.azuretools.azureexplorer.messages;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import com.microsoft.azuretools.core.Activator;


public class MessageHandler {

    private static Activator LOG = Activator.getDefault();
    private static ResourceBundle commonResourceBundle = ResourceBundle.getBundle("com.microsoft.azuretools.azureexplorer.messages.common");

    private static final String BUNDLE_PACKAGE_NAME = "com.microsoft.azuretools.azureexplorer.messages.%s";
    private static final String CANNOT_FIND_BUNDLE_ERROR = "Cannot find package: %s";
    private static final String CANNOT_FIND_KEY_ERROR = "Cannot find key: %s";
    private static final String BUNDLE_OR_KEY_IS_NULL = "Bundle or key is null";

    public static ResourceBundle getResourceBundle(String module) {
        String fullBundleName = String.format(BUNDLE_PACKAGE_NAME, module);
        try {
            return ResourceBundle.getBundle(fullBundleName);
        } catch (MissingResourceException ex){
            //TODO : Exceoption handler
            LOG.log(String.format(CANNOT_FIND_BUNDLE_ERROR, fullBundleName), ex);
        }
        return null;
    }

    public static String getResourceString(ResourceBundle bundle, String key) {
        if (bundle == null || key == null) {
            LOG.log(BUNDLE_OR_KEY_IS_NULL);
            return "";
        }
        try {
            return bundle.getString(key);
        } catch (Exception ex) {
            LOG.log(String.format(CANNOT_FIND_KEY_ERROR, key), ex);
            return key;
        }
    }

    public static String getCommonStr(String key) {
        return getResourceString(commonResourceBundle, key);
    }
}
