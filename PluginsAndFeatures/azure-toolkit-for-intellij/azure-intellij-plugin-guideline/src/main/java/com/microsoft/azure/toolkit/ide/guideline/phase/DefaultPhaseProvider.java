package com.microsoft.azure.toolkit.ide.guideline.phase;

import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.config.PhaseConfig;
import com.microsoft.azure.toolkit.ide.guideline.view.components.PhasePanel;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;

public class DefaultPhaseProvider implements GuidancePhaseProvider {
    @Override
    public Phase createPhase(@Nonnull PhaseConfig config, @Nonnull Process process) {
        return StringUtils.isEmpty(config.getType()) ? new Phase(config, process) : null;
    }

    @Override
    public JPanel createPhasePanel(@Nonnull Phase phase) {
        return StringUtils.isEmpty(phase.getType()) ? new PhasePanel(phase) : null;
    }
}
