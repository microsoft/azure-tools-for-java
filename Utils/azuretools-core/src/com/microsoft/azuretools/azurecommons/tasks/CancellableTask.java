/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.azuretools.azurecommons.tasks;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public interface CancellableTask {
    interface CancellationHandle {
        boolean isCancelled();
    }

    interface CancellableTaskHandle extends CancellationHandle {
        boolean isFinished();

        boolean isSuccessful();

        @Nullable
        Throwable getException();

        void cancel();
    }

    void run(CancellationHandle cancellationHandle) throws Throwable;

    void onCancel();

    void onSuccess();

    void onError(@NotNull Throwable exception);
}