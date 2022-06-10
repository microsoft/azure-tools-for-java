package com.microsoft.azure.toolkit.ide.guideline.task;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TaskManager {

    private static final ExtensionPointName<GuidanceTaskProvider> exPoints =
            ExtensionPointName.create(" com.microsoft.azure.toolkit.ide.guideline.task.GuidanceTaskProvider");
    private static List<GuidanceTaskProvider> providers;

    public synchronized static List<GuidanceTaskProvider> getTaskProviders() {
        if (CollectionUtils.isEmpty(providers)) {
            providers = exPoints.extensions().collect(Collectors.toList());
        }
        return providers;
    }

    @Nonnull
    public static Task createTask(String id, Phase phase) {
        return getTaskProviders().stream()
                .map(provider -> provider.createTask(id, phase))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AzureToolkitRuntimeException("Unsupported task id"));
    }
}
