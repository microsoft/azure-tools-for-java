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
package com.microsoft.intellij.common;


import com.microsoft.azure.ProxyResource;

public class AzureResourceWrapper {

    private static final String NEW_CREATED_PATTERN = "%s (New Created)";

    private final boolean isNewCreate;
    private final boolean fixedOption;
    private final String name;

    public AzureResourceWrapper(String name, boolean fixedOption, boolean isNewCreate) {
        this.fixedOption = fixedOption;
        this.isNewCreate = isNewCreate;
        this.name = name;
    }
    public AzureResourceWrapper(String name, boolean fixedOption) {
        this.fixedOption = fixedOption;
        this.isNewCreate = !fixedOption;
        this.name = name;
    }

    public AzureResourceWrapper(ProxyResource app) {
        this.fixedOption = false;
        this.isNewCreate = false;
        this.name = app.name();
    }

    public boolean isNewCreate() {
        return isNewCreate;
    }

    public boolean isFixedOption() {
        return fixedOption;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return isNewCreate ? String.format(NEW_CREATED_PATTERN, name) : name;
    }
}
