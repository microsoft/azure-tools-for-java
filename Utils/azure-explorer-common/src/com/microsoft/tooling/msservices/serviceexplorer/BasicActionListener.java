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

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.base.Preconditions;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import lombok.Getter;

import javax.swing.*;
import java.util.Objects;

public final class BasicActionListener extends NodeActionListener {

    @Getter
    private final NodeActionListener delegate;
    @Getter
    private final AzureActionEnum actionEnum;

    public BasicActionListener(NodeActionListener delegate, AzureActionEnum actionEnum) {
        Preconditions.checkNotNull(delegate);
        Preconditions.checkNotNull(actionEnum);
        this.delegate = delegate;
        this.actionEnum = actionEnum;
    }

    @Override
    public int getPriority() {
        if (Objects.nonNull(actionEnum.getPriority())) {
            return delegate.getPriority() + actionEnum.getPriority();
        } else {
            return delegate.getPriority();
        }
    }

    @Override
    public int getGroup() {
        if (Objects.nonNull(actionEnum.getGroup())) {
            return delegate.getGroup() + actionEnum.getGroup();
        } else {
            return delegate.getGroup();
        }
    }

    @Override
    public Icon getIcon() {
        return DefaultLoader.getUIHelper().loadIconByAction(actionEnum);
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
        delegate.actionPerformed(e);
    }
}
