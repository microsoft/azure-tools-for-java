/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui.views;

import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TableItem;

class TableRowDescriptor{
    private final ProgressBar progressBar;
    private final TableItem item;
    private final Link link;

    public TableRowDescriptor(TableItem item, ProgressBar progressBar, Link link){
        this.item = item;
        this.progressBar = progressBar;
        this.link = link;
    }

    /**
     * @return the progressBar
     */
    public ProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * @return the item
     */
    public TableItem getItem() {
        return item;
    }

    public Link getLink() {
        return link;
    }
}
