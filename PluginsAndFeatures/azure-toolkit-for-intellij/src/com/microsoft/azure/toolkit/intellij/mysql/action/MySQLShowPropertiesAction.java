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

package com.microsoft.azure.toolkit.intellij.mysql.action;

import com.microsoft.intellij.helpers.AzureAllIcons;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;

import javax.swing.*;

@Name(MySQLShowPropertiesAction.ACTION_NAME)
public class MySQLShowPropertiesAction extends NodeActionListener {

    public static final String ACTION_NAME = "Show Properties";

    private final MySQLNode node;

    public MySQLShowPropertiesAction(MySQLNode node) {
        super();
        this.node = node;
    }

    @Override
    public int getGroup() {
        return MySQLNode.OPERATE_GROUP;
    }

    @Override
    public int getPriority() {
        return MySQLNode.SHOW_PROPERTIES_PRIORITY;
    }

    @Override
    public Icon getIcon() {
        return AzureAllIcons.Common.SHOW_PROPERTIES;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        DefaultLoader.getUIHelper().openMySQLPropertyView(node);
    }

}
