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

package com.microsoft.azure.toolkit.lib.common.task;

import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

@Log
public abstract class AzureTaskContext {
    private static final ThreadLocal<AzureTaskContext.Node> context = new ThreadLocal<>();

    protected long threadId = -1;
    protected final Deque<AzureOperationRef> operations;

    private AzureTaskContext() {
        this.operations = new ArrayDeque<>();
    }

    private AzureTaskContext(final long threadId, final Deque<AzureOperationRef> operations) {
        this.operations = operations;
        this.threadId = threadId;
    }

    public Deque<AzureOperationRef> getOperations() {
        return new ArrayDeque<>(this.operations);
    }

    public static Deque<AzureOperationRef> getContextOperations(AzureTaskContext.Node node) {
        final Deque<AzureOperationRef> ops = new ArrayDeque<>();
        if (Objects.nonNull(node.parent)) {
            ops.addAll(node.parent.getOperations());
        }
        ops.addAll(node.operations);
        return ops;
    }

    public static AzureTaskContext.Node current() {
        Node ctxNode = AzureTaskContext.context.get();
        if (Objects.isNull(ctxNode)) {
            ctxNode = new Node(null);
            AzureTaskContext.context.set(ctxNode);
        }
        return ctxNode;
    }

    public static <T> void run(final Runnable runnable, AzureTaskContext.Node context) {
        try {
            context.setup();
            runnable.run();
        } catch (final Throwable throwable) {
            AzureExceptionHandler.onRxException(throwable);
        } finally {
            context.dispose();
        }
    }

    public static class Node extends AzureTaskContext {
        @Setter
        @Getter
        private Boolean backgrounded = null;
        @Getter
        private AzureTaskContext parent;
        protected boolean disposed;

        private Node(final AzureTaskContext parent) {
            super();
            this.parent = parent;
        }

        void pushOperation(final AzureOperationRef operation) {
            this.operations.push(operation);
        }

        @Nullable
        AzureOperationRef popOperation() {
            if (this.operations.size() > 0) {
                return this.operations.pop();
            }
            return null;
        }

        void clearOperations() {
            this.operations.clear();
        }

        Node derive() {
            final long threadId = Thread.currentThread().getId();
            final Node current = AzureTaskContext.current();
            assert this == current : String.format("[threadId:%s] deriving context from context[%s] in context[%s].", threadId, this, current);
            assert !this.disposed : String.format("[threadId:%s] deriving from a disposed context[%s]", threadId, this);
            this.threadId = this.threadId > 0 ? this.threadId : threadId;
            final Snapshot snapshot = new Snapshot(this);
            return new Node(snapshot);
        }

        private void setup() {
            final Node current = AzureTaskContext.current();
            final long threadId = Thread.currentThread().getId();
            assert this.threadId < 0 && !this.disposed : String.format("[threadId:%s] context[%s] already setup/disposed", threadId, this);
            this.threadId = threadId;
            if (this.threadId == current.threadId) {
                this.parent = current;
                log.info(String.format("[threadId:%s] setting up IN-THREAD context[%s]", threadId, this));
            } else {
                log.info(String.format("[threadId:%s] setting up THREAD context[%s]", threadId, this));
            }
            AzureTaskContext.context.set(this);
        }

        private void dispose() {
            final Node current = AzureTaskContext.current();
            final long threadId = Thread.currentThread().getId();
            assert this == current && this.threadId == threadId : String.format("[threadId:%s] disposing context[%s] in context[%s].", threadId, this, current);
            assert !this.disposed : String.format("[threadId:%s] disposing a disposed context[%s].", threadId, this);
            this.disposed = true;
            if (this.threadId == this.parent.threadId) {
                // log.info(String.format("[threadId:%s] disposing IN-THREAD context[%s]", threadId, this));
                assert !(this.parent instanceof Snapshot);
                AzureTaskContext.context.set((Node) this.parent);
            } else {
                // log.info(String.format("[threadId:%s] disposing THREAD context[%s]", threadId, this));
                assert this.parent instanceof Snapshot;
                AzureTaskContext.context.remove();
            }
        }

        public String toString() {
            final String hashcode = Integer.toHexString(System.identityHashCode(this));
            if (this.parent instanceof Snapshot) {
                final String orHashcode = Integer.toHexString(System.identityHashCode(((Snapshot) this.parent).origin));
                return String.format("{hashcode: %s, threadId:%s, snapshot@parent.origin:%s, disposed:%s}", hashcode, this.threadId, orHashcode, this.disposed);
            } else {
                final String prHashcode = Integer.toHexString(System.identityHashCode(this.parent));
                return String.format("{hashcode: %s, threadId:%s, parent:%s, disposed:%s}", hashcode, this.threadId, prHashcode, this.disposed);
            }
        }
    }

    private static class Snapshot extends AzureTaskContext {
        private final AzureTaskContext.Node origin; // snapshot refers original context

        private Snapshot(@NotNull final AzureTaskContext.Node origin) {
            super(origin.threadId, AzureTaskContext.getContextOperations(origin));
            this.origin = origin;
        }

        public String toString() {
            final String hashcode = Integer.toHexString(System.identityHashCode(this));
            final String orHashcode = Integer.toHexString(System.identityHashCode(this.origin));
            return String.format("{hashcode: %s, threadId:%s, origin:%s}", hashcode, this.threadId, orHashcode);
        }
    }
}
