/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.intellij.ide.BrowserUtil;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.OpenInBrowserMessageAction;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class IntellijOpenInBrowserMessageAction extends OpenInBrowserMessageAction {
    public IntellijOpenInBrowserMessageAction(String name, String url) {
        super(name, url);
    }

    @Override
    public void actionPerformed(IAzureMessage message) {
        BrowserUtil.browse(this.getUrl());
    }
}
