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

package com.microsoft.intellij.components;

import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.Sortable;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.Comparator;

public class SortableTreeNode extends DefaultMutableTreeNode implements Sortable {

    private Node node;

    public SortableTreeNode() {
        super();
    }

    public SortableTreeNode(Node userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
        this.node = userObject;
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

    @Override
    public int getPriority() {
        return node == null ? DEFAULT_PRIORITY : node.getPriority();
    }

    private static final Comparator nodeComparator =
        (Object first, Object second) -> (first instanceof Sortable && second instanceof Sortable) ?
                                         Comparator.comparing(Sortable::getPriority).thenComparing(Object::toString)
                                                   .compare((Sortable) first, (Sortable) second) :
                                         StringUtils.compare(first.toString(), second.toString());
}
