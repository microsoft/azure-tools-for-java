package com.microsoft.azure.toolkit.ide.guidance.phase;

import com.microsoft.azure.toolkit.ide.guidance.Course;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.config.PhaseConfig;
import com.microsoft.azure.toolkit.ide.guidance.view.components.PhasePanel;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;

public class DefaultPhaseProvider implements GuidancePhaseProvider {
    @Override
    public Phase createPhase(@Nonnull PhaseConfig config, @Nonnull Course course) {
        return StringUtils.isEmpty(config.getType()) ? new Phase(config, course) : null;
    }

    @Override
    public JPanel createPhasePanel(@Nonnull Phase phase) {
        return StringUtils.isEmpty(phase.getType()) ? new PhasePanel(phase) : null;
    }
}
