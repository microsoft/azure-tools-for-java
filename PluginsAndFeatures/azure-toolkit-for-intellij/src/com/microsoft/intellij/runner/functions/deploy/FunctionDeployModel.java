package com.microsoft.intellij.runner.functions.deploy;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.runner.functions.IntelliJFunctionContext;

public class FunctionDeployModel extends IntelliJFunctionContext {

    private String functionId;

    public FunctionDeployModel(Project project) {
        super(project);
    }

    public String getFunctionId() {
        return functionId;
    }

    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }
}
