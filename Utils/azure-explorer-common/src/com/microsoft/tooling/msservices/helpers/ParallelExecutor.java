/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

final class ParallelThreadFactory implements ThreadFactory {
    private int counter = 0;
    private String prefix = "";
    public ParallelThreadFactory(String prefix) {
        this.prefix = prefix;
    }
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }
}

public final class ParallelExecutor {

    private static final int MAX_NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService forPool = Executors.newFixedThreadPool(MAX_NUM_THREADS * 2, new ParallelThreadFactory("Parallel.For"));

    public static <T> void For(final Iterable<T> elements, final Operation<T> operation) {
        try {
            // invokeAll blocks until all submitted tasks in the call complete
            forPool.invokeAll(createCallables(elements, operation));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> Collection<Callable<Void>> createCallables(final Iterable<T> elements, final Operation<T> operation) {
        List<Callable<Void>> callables = new LinkedList<Callable<Void>>();
        for (final T elem : elements) {
            callables.add(new Callable<Void>() {
                @Override
                public Void call() {
                    operation.perform(elem);
                    return null;
                }
            });
        }
        return callables;
    }

    public static interface Operation<T> {
        public void perform(T pParameter);
    }


}
