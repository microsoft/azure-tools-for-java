package com.microsoft.azure.toolkit.ide.guideline.phase;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.config.PhaseConfig;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PhaseManager {
    private static final ExtensionPointName<GuidancePhaseProvider> exPoints =
            ExtensionPointName.create(" com.microsoft.azure.toolkit.ide.guideline.guideline.GuidancePhaseProvider");

    private static List<GuidancePhaseProvider> providers;

    public Phase createPhase(@Nonnull final PhaseConfig type, @Nonnull final Process process){
        return getTaskProviders().stream()
                .map(provider -> provider.createPhase(type, process))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AzureToolkitRuntimeException("Unsupported phase type"));
    }

    public JPanel createPhasePanel(@Nonnull Phase phase){
        return getTaskProviders().stream()
                .map(provider -> provider.createPhasePanel(phase))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AzureToolkitRuntimeException("Unsupported phase type"));
    }

    public synchronized static List<GuidancePhaseProvider> getTaskProviders() {
        if (CollectionUtils.isEmpty(providers)) {
            providers = exPoints.extensions().collect(Collectors.toList());
        }
        return providers;
    }
}
