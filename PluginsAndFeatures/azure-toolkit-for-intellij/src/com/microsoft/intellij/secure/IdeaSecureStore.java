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

package com.microsoft.intellij.secure;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.securestore.SecureStore;

public class IdeaSecureStore implements SecureStore {
    private IdeaSecureStore() { }

    // Leverage IntelliJ PasswordSafe component
    private PasswordSafe passwordSafe = PasswordSafe.getInstance();

    private static class LazyHolder {
        static final IdeaSecureStore INSTANCE = new IdeaSecureStore();
    }

    public static IdeaSecureStore getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public void savePassword(@NotNull String serviceName, @NotNull String userName, @Nullable String password) {
        passwordSafe.setPassword(new CredentialAttributes(serviceName, userName), password);
    }

    @Override
    @Nullable
    public String loadPassword(@NotNull String serviceName, @NotNull String userName) {
        return passwordSafe.getPassword(new CredentialAttributes(serviceName, userName));
    }
}
