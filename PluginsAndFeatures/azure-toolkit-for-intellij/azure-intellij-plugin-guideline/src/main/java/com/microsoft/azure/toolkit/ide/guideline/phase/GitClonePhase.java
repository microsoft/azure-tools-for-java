package com.microsoft.azure.toolkit.ide.guideline.phase;

import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.Status;
import com.microsoft.azure.toolkit.ide.guideline.config.PhaseConfig;
import com.microsoft.azure.toolkit.ide.guideline.config.StepConfig;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;

import static com.microsoft.azure.toolkit.ide.guideline.GuidanceConfigManager.GETTING_START_CONFIGURATION_NAME;

public class GitClonePhase extends Phase {

    public GitClonePhase(@Nonnull Process parent) {
        super(getClonePhaseConfig(), parent);
    }

    @Override
    public void prepareLaunch() {
        // Check whether project was clone to local
        final File file = new File(getProcess().getProject().getBasePath(), GETTING_START_CONFIGURATION_NAME);
        setStatus(file.exists() ? Status.SUCCEED : Status.READY);
    }

    private static PhaseConfig getClonePhaseConfig() {
        StepConfig stepConfig = new StepConfig();
        stepConfig.setDescription("Clone demo project to your local machine.");
        stepConfig.setName("Clone");
        stepConfig.setTitle("Clone");
        stepConfig.setTask("tasks.clone");

        PhaseConfig phaseConfig = new PhaseConfig();
        phaseConfig.setDescription("Clone demo project to your local machine.");
        phaseConfig.setName("Clone");
        phaseConfig.setTitle("Clone");
        phaseConfig.setSteps(Arrays.asList(stepConfig));
        return phaseConfig;
    }
}
