/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.step;

import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shch on 8/20/2016.
 */
public class StepManager {

    private List<IStep> steps = new ArrayList<>();
    private Map<String, Object> params = new HashMap<>();

    public Map<String, Object> getParamMap() {
        return params;
    }

    public void add(IStep step) {
        steps.add(step);
    }

    private int curStep = 0;
    private Reporter<String> reporter;

    public void execute() {
        IStep step = null;
        try {
            reporter = CommonParams.getReporter();
            for (; curStep<steps.size(); ++curStep) {
                step =  steps.get(curStep);
                reporter.report(">> Executing step : " + step.getName() + "...");
                step.execute(getParamMap());
            }
            reporter.report(">> Done.");
        } catch (AzureException ex) {
            String errorDetails = ex.getCode() + " - " + ex.getDescription();
            reporter.report(">> Executing step FAILED: " + errorDetails);
            CommonParams.getStatusReporter().report(new Status(
                    step.getName(),
                    Status.Result.FAILED,
                    errorDetails
            ));
            reporter.report("!! Rolling back...");

            rollback();
        } catch (IOException | InterruptedException e) {
            //e.printStackTrace();
            reporter.report(">> Executing step FAILED: " + e.getMessage());
            reporter.report("!! Rolling back...");

            rollback();
        }
    }

    public void rollback(){
        boolean isOk = true;
        for (; curStep>=0; --curStep) {
            try {
                IStep step =  steps.get(curStep);
                reporter.report("<< Rolling back step: " + step.getName() + "...");
                step.rollback(getParamMap());
            } catch (IOException e) {
                isOk = false;
                reporter.report("<< Roll back FAILED: " + e.getMessage());
                //e.printStackTrace();
                reporter.report("!! Please remove created artifacts manually");
            }
        }

        if (isOk)
            reporter.report("<< Roll back SUCCESSFUL");
    }
}
