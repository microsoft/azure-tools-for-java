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

package com.microsoft.azure.sparkserverless;

import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sparkserverless.SparkServerlessClusterOps;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import rx.functions.Action1;

public class SparkServerlessClusterOpsCtrl {
    public SparkServerlessClusterOpsCtrl() {
        SparkServerlessClusterOps.getInstance().getDestroyAction().subscribe(new Action1<Triple<String, String, Node>>() {
            @Override
            public void call(Triple<String, String, Node> triplet) {
                System.out.println(String.format("Message received. AdlAccount: %s, clusterName: %s, parentNode: %s",
                        triplet.getLeft(), triplet.getMiddle(), triplet.getRight()));
                // TODO: pop up a destroy dialog
            }
        });

        SparkServerlessClusterOps.getInstance().getProvisionAction().subscribe(new Action1<Pair<String, Node>>() {
            @Override
            public void call(Pair<String, Node> pair) {
                System.out.println(String.format("Message received. AdlAccount: %s, node: %s",
                        pair.getLeft(), pair.getRight()));
                // TODO: pop up a provision dialog
            }
        });
    }
}
