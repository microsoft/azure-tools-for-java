package com.microsoft.azure.toolkit.ide.guideline.view.components;

import com.microsoft.azure.toolkit.ide.guideline.Phase;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

public class PhasePanelFactory {
    public static JPanel createPhase(Phase phase) {
        final String type = phase.getType();
        if (StringUtils.equalsIgnoreCase(type, "summary")) {
            return new SummaryPhasePanel(phase);
        } else {
            return new PhasePanel(phase);
        }
    }
}
