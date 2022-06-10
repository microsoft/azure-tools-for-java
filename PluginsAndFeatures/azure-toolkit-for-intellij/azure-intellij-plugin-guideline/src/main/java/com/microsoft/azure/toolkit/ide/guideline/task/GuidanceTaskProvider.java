package com.microsoft.azure.toolkit.ide.guideline.task;

import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Task;

import javax.annotation.Nonnull;

public interface GuidanceTaskProvider {
    Task createTask(@Nonnull final String taskId, @Nonnull final Phase phase);
}
