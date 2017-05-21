/**
 * Copyright (c) Microsoft Corporation
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
package com.microsoft.azuretools.azurecommons.messagehandler;

import java.util.MissingResourceException;
import java.util.ResourceBundle;


public class MessageHandler {
    
    static ResourceBundle commonResourceBundle = ResourceBundle.getBundle("com.microsoft.azuretools.azurecommons.messagehandler.common");
    
    private static final String BUNDLE_PACKAGE_NAME = "com.microsoft.azuretools.azurecommons.messagehandler.%s";
    private static final String CANNOT_FIND_BUNDLE_ERROR = "Cannot find package: %s";
    
    public static ResourceBundle getResourceBundle(String module) {
        String fullBundleName = String.format(BUNDLE_PACKAGE_NAME, module);
        try {
            return ResourceBundle.getBundle(fullBundleName);
        } catch (MissingResourceException ex){
            //TODO : Exceoption handler
        }
        return null;
    }
    
    public static String getCommonStr(String key) {
        return commonResourceBundle.getString(key);
    }
}

