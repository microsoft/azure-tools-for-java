/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.wizard;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.ide.appservice.function.AzureFunctionsUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionTemplate;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NewFunctionClassWizardPage extends NewClassWizardPage {
    private Composite composite;
    private AzureComboBox<String> cbTriggers;

    @Override
    public void createControl(Composite parent) {

        this.setTitle("Create a sample Function class");
        this.setDescription("Create a sample Function class");
        initializeDialogUnits(parent);

        composite = new Composite(parent, SWT.NONE);
        int columns = 4;

        GridLayout gl_composite = new GridLayout();
        gl_composite.numColumns = columns;
        composite.setLayout(gl_composite);

        createContainerControls(composite, columns);
        createPackageControls(composite, columns);
        createTriggerTypes(composite, columns);
        createTypeNameControls(composite, columns);

        setControl(composite);
        Dialog.applyDialogFont(composite);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.microsoft.azuretools.appservice.create_function_class_dialog");
    }

    @Override
    protected void createTypeMembers(IType createdType, ImportsManager imports, IProgressMonitor monitor) throws CoreException {
        final String body;
        try {
            body = AzureFunctionsUtils.generateFunctionClassByTrigger(cbTriggers.getValue(), getPackageText(), getTypeName());
        } catch (IOException e) {
            // ignore
            return;
        }

        ASTParser parser = ASTParser.newParser(AST.JLS11);
        parser.setSource(body.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());

        List<String> methodBodies = new ArrayList<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(ImportDeclaration node) {
                String name = node.getName().getFullyQualifiedName();
                if (node.isOnDemand()) {
                    name = name + ".*";
                }
                if (node.isStatic()) {
                    name = "static " + name;
                }
                imports.addImport(name);

                return super.visit(node);
            }

            public boolean visit(MethodDeclaration node) {
                methodBodies.add(node.toString());
                return false;
            }
        });
        for (final String methodBody : methodBodies) {
            try {
                createdType.createMethod(methodBody, null, false, null);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    protected void createTriggerTypes(Composite container, int columns) {
        Label lblFunctionTrigger = new Label(container, SWT.NONE);
        lblFunctionTrigger.setText("Trigger type:");
        cbTriggers = new AzureComboBox<>(container, this::listTriggerTypes);

        cbTriggers.addValueChangedListener(value -> {
            final String key = "initial-value";
            String lastValue = (String) cbTriggers.getData(key);
            if (StringUtils.isNotBlank(value) && (StringUtils.isBlank(this.getTypeName())
                    || StringUtils.equalsIgnoreCase(lastValue, this.getTypeName()))
            ) {
                String suggestedFunctionName = value + "Function";
                this.setTypeName(suggestedFunctionName, true);
                cbTriggers.setData(key, suggestedFunctionName);
            }
        });
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = columns - 2;
        cbTriggers.setLayoutData(gd);
        new Label(container, SWT.NONE);
    }

    private List<String> listTriggerTypes() {
        try {
            final List<FunctionTemplate> functionTemplates = FunctionUtils.loadAllFunctionTemplates();
            return functionTemplates.stream().map(t -> t.getMetadata().getName()).collect(Collectors.toList());
        } catch (AzureExecutionException e) {
            return Collections.emptyList();
        }
    }

    protected String getTypeComment(ICompilationUnit parentCU, String lineDelimiter) {
        return String.format("/**\n" +
                " * Azure Functions with %s trigger.\n" +
                " */", cbTriggers.getValue());
    }
}
