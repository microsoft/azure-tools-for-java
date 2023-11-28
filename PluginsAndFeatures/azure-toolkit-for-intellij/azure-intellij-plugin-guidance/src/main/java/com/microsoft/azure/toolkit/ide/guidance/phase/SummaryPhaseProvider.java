package com.microsoft.azure.toolkit.ide.guidance.phase;

import com.microsoft.azure.toolkit.ide.guidance.Course;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Status;
import com.microsoft.azure.toolkit.ide.guidance.config.PhaseConfig;
import com.microsoft.azure.toolkit.ide.guidance.view.components.SummaryPanel;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;

public class SummaryPhaseProvider implements GuidancePhaseProvider {
    private static final String SUMMARY = "summary";

    @Override
    public Phase createPhase(@Nonnull PhaseConfig config, @Nonnull Course course) {
        return StringUtils.equals(config.getType(), SUMMARY) ? new SummaryPhase(config, course) : null;
    }

    @Override
    public JPanel createPhasePanel(@Nonnull Phase phase) {
        return StringUtils.equals(phase.getType(), SUMMARY) ? new SummaryPanel(phase) : null;
    }

    static class SummaryPhase extends Phase {
        public SummaryPhase(@Nonnull PhaseConfig config, @Nonnull Course parent) {
            super(config, parent);
        }

        @Override
        public void setStatus(Status status) {
            if (status == Status.READY || status == Status.RUNNING) {
                status = Status.SUCCEED;
            }
            super.setStatus(status);
        }
    }
}
