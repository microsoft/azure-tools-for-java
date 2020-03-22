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
package com.microsoft.azuretools.azureexplorer.actions;

import java.io.IOException;

import com.microsoft.azuretools.azureexplorer.forms.createrediscache.CreateRedisCacheForm;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;


@Name("Create Redis Cache")
public class CreateRedisCacheAction extends NodeActionListener {

    private RedisCacheModule redisCacheModule;

    public CreateRedisCacheAction(RedisCacheModule redisCacheModule) {
        this.redisCacheModule = redisCacheModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        if (!SignInCommandHandler.doSignIn(PluginUtil.getParentShell())) return;
        try {
            CreateRedisCacheForm createRedisCacheForm = new CreateRedisCacheForm(PluginUtil.getParentShell());
            createRedisCacheForm.create();
            createRedisCacheForm.setOnCreate(new Runnable() {
                @Override
                public void run() {
                    if (redisCacheModule != null) {
                        redisCacheModule.load(false);
                    }
                }
            });
            createRedisCacheForm.open();
        } catch (IOException ex) {
             ex.printStackTrace();
        }
    }
}
