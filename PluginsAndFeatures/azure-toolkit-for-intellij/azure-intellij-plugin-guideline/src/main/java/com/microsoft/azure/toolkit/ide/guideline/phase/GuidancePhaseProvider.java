package com.microsoft.azure.toolkit.ide.guideline.phase;

import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.config.PhaseConfig;

import javax.annotation.Nonnull;
import javax.swing.*;

public interface GuidancePhaseProvider {
    Phase createPhase(@Nonnull final PhaseConfig type, @Nonnull final Process process);

    JPanel createPhasePanel(@Nonnull Phase phase);
}
