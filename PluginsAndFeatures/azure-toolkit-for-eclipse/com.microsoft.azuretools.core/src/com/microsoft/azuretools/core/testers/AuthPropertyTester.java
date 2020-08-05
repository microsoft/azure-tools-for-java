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

package com.microsoft.azuretools.core.testers;

import org.eclipse.core.expressions.PropertyTester;

import com.microsoft.azuretools.authmanage.AuthMethodManager;

public class AuthPropertyTester extends PropertyTester {
    public static final String PROPERTY_NAMESPACE = "com.microsoft.azuretools.core.testers";
    public static final String PROPERTY_IS_SIGNED_IN = "isSignedIn";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        if (PROPERTY_IS_SIGNED_IN.equals(property)) {
            try {
                AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
                boolean isSignIn = authMethodManager.isSignedIn();
                return isSignIn;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }
}
