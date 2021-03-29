/*
 * Copyright (c) 2021 JetBrains s.r.o.
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

package com.microsoft.azuretools.authmanage;

import com.microsoft.azuretools.adauth.AuthError;
import com.microsoft.azuretools.adauth.AuthException;
import org.apache.commons.lang3.exception.ExceptionUtils;

@SuppressWarnings("unused")
public final class AuthExceptionUtil {

    /**
     * When a Throwable is an AuthException that requires the user to re-authenticate,
     * such as the one described in https://github.com/JetBrains/azure-tools-for-intellij/issues/438,
     * returns the AuthException.
     *
     * @param throwable Throwable to check.
     * @return When a Throwable is an AuthException that requires the user to re-authenticate, returns the AuthException. Returns null otherwise.
     */
    public static AuthException getUserRetryableAuthException(Throwable throwable) {

        final Throwable rootCause = ExceptionUtils.getRootCause(throwable);
        if (rootCause instanceof AuthException) {
            final AuthException authException = (AuthException) rootCause;

            // See com/microsoft/azuretools/authmanage/AdAuthManager.java:74
            // and com/microsoft/azuretools/authmanage/DCAuthManager.java:97
            // for similar logic.
            if (AuthError.InvalidGrant.equalsIgnoreCase(authException.getError()) ||
                    AuthError.InteractionRequired.equalsIgnoreCase(authException.getError())) {

                return authException;
            }
        }

        return null;
    }

}
