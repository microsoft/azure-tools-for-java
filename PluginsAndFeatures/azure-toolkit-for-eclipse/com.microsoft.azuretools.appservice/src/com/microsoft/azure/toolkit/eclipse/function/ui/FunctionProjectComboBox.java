package com.microsoft.azure.toolkit.eclipse.function.ui;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.widgets.Composite;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionProjectComboBox extends AzureComboBox<IJavaProject> {
    public FunctionProjectComboBox(Composite parent) {
        super(parent, false);
    }
    protected List<IJavaProject> loadItems() throws Exception {
        return Arrays.stream(FunctionUtils.listFunctionProjects()).collect(Collectors.toList());
    }
    protected String getItemText(Object item) {
        if (item instanceof IJavaProject) {
            return ((IJavaProject) item).getElementName();
        } else {
            return StringUtils.EMPTY;
        }
    }
}
