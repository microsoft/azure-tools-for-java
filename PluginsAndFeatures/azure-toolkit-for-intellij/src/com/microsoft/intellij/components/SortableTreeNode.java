/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.components;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.Comparator;

public class SortableTreeNode extends DefaultMutableTreeNode {

    public SortableTreeNode() {
        super();
    }

    public SortableTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public SortableTreeNode(Object userObject, boolean allowsChildren, @Nullable Comparator<Node> comparator) {
        super(userObject, allowsChildren);
        if (comparator != null) {
            // Cast Comparator<Node> to Comparator<SortableTreeNode> instance to be able to sort presentation nodes.
            this.nodeComparator =
                    (Comparator<SortableTreeNode>) (node1, node2) -> comparator.compare((Node)node1.userObject, (Node)node2.userObject);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(MutableTreeNode newChild) {
        super.add(newChild);
        this.children.sort(nodeComparator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void insert(MutableTreeNode newChild, int childIndex) {
        super.insert(newChild, childIndex);
        this.children.sort(nodeComparator);
    }

    private Comparator nodeComparator =
            (Comparator<SortableTreeNode>) (node1, node2) -> node1.toString().compareToIgnoreCase(node2.toString());
}
