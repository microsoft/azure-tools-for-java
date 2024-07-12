package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.DisableAutoCompleteCheck.DisableAutoCompleteVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * This class is used to test the DisableAutoCompleteCheck class.
 * It tests the visitDeclarationStatement method of the DisableAutoCompleteVisitor class.
 * Use of AC refers to the auto-complete feature.
 */
public class DisableAutoCompleteCheckTest {

    // Create a mock ProblemsHolder to be used in the test
    @Mock
    private ProblemsHolder mockHolder;

    // Create a mock JavaElementVisitor for visiting the PsiDeclarationStatement
    @Mock
    private JavaElementVisitor mockVisitor;

    // Create a mock PsiDeclarationStatement.
    @Mock
    private PsiDeclarationStatement mockDeclarationStatement;


    @BeforeEach
    public void setUp() {
        // Set up the test
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockDeclarationStatement = mock(PsiDeclarationStatement.class);
    }

    /**
     * Test the visitDeclarationStatement method of the DisableAutoCompleteVisitor class.
     * This test checks if the auto-complete feature is disabled for the ServiceBusReceiverClient.
     * If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
     */
    @Test
    public void testACNotDisabledForReceiver() {

        String packageName = "com.azure";
        String clientName = "ServiceBusReceiverClient";
        int numOfInvocations = 1;
        String methodFound = "notDisableAutoComplete";


        // Assert
        verifyProblemRegistered(packageName, clientName, numOfInvocations, methodFound);
    }

    /**
     * Test the visitDeclarationStatement method of the DisableAutoCompleteVisitor class.
     * This test checks if the auto-complete feature is disabled for the ServiceBusProcessorClient.
     * If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
     */
    @Test
    public void testACNotDisabledForProcessor() {

        String packageName = "com.azure";
        String clientName = "ServiceBusProcessorClient";
        int numOfInvocations = 1;
        String methodFound = "notDisableAutoComplete";

        // Assert
        verifyProblemRegistered(packageName, clientName, numOfInvocations, methodFound);
    }

    /**
     * Test the visitDeclarationStatement method of the DisableAutoCompleteVisitor class.
     * This test checks if the auto-complete feature is disabled for the ServiceBusRuleManagerClient.
     * If the auto-complete feature is not disabled, a problem will NOT be registered with the ProblemsHolder
     * because the ServiceBusRuleManagerClient is not the correct client to check.
     */
    @Test
    public void testNoProblemRegisteredWithWrongClient() {

        String packageName = "com.azure";
        String clientName = "ServiceBusRuleManagerClient";
        int numOfInvocations = 0;
        String methodFound = "notDisableAutoComplete";

        // Assert
        verifyProblemRegistered(packageName, clientName, numOfInvocations, methodFound);
    }

    /**
     * Test the visitDeclarationStatement method of the DisableAutoCompleteVisitor class.
     * This test checks if the auto-complete feature is disabled for the ServiceBusReceiverClient.
     * A problem will NOT be registered with the ProblemsHolder
     * because the auto-complete feature is disabled.
     */
    @Test
    public void testACDisabledForReceiver() {

        String packageName = "com.azure";
        String clientName = "ServiceBusReceiverClient";
        int numOfInvocations = 0;
        String methodFound = "disableAutoComplete";


        // Assert
        verifyProblemRegistered(packageName, clientName, numOfInvocations, methodFound);
    }

    /**
     * Test the visitDeclarationStatement method of the DisableAutoCompleteVisitor class.
     * This test checks if the auto-complete feature is disabled for the ServiceBusReceiverClient.
     * A problem will NOT be registered with the ProblemsHolder
     * because the package name is different -- not an Azure SDK client.
     */
    @Test
    public void testNoproblemRegisteredDifferentPackage() {

        String packageName = "com.microsoft.azure";
        String clientName = "ServiceBusReceiverClient";
        int numOfInvocations = 0;
        String methodFound = "disableAutoComplete";


        // Assert
        verifyProblemRegistered(packageName, clientName, numOfInvocations, methodFound);
    }


    /**
     * Create a visitor by calling the buildVisitor method of the DisableAutoCompleteCheck class.
     *
     * @return The visitor created
     */
    private JavaElementVisitor createVisitor() {
        DisableAutoCompleteVisitor mockVisitor = new DisableAutoCompleteVisitor(mockHolder, true);
        return mockVisitor;
    }


    /**
     * Verify that a problem is registered with the ProblemsHolder.
     *
     * @param packageName      The package name of the client
     * @param clientName       The name of the client
     * @param numOfInvocations The number of times the registerProblem method should be called
     * @param methodFound      The method found in the initializer
     */
    private void verifyProblemRegistered(String packageName, String clientName, int numOfInvocations, String methodFound) {

        PsiVariable declaredElement = mock(PsiVariable.class);
        PsiElement[] declaredElements = new PsiElement[]{declaredElement};

        // processVariableDeclaration
        PsiType clientType = mock(PsiType.class);
        PsiMethodCallExpression initializer = mock(PsiMethodCallExpression.class);

        // isAutoCompleteDisabled method
        PsiReferenceExpression expression = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression qualifier = mock(PsiMethodCallExpression.class);
        PsiMethodCallExpression finalExpression = mock(PsiMethodCallExpression.class);

        when(mockDeclarationStatement.getDeclaredElements()).thenReturn(declaredElements);

        // processVariableDeclaration method
        when(declaredElement.getType()).thenReturn(clientType);
        when(declaredElement.getInitializer()).thenReturn(initializer);
        when(clientType.getCanonicalText()).thenReturn(packageName);
        when(clientType.getPresentableText()).thenReturn(clientName);

        // isAutoCompleteDisabled method
        when(initializer.getMethodExpression()).thenReturn(expression);
        when(expression.getQualifierExpression()).thenReturn(qualifier);

        // First level qualifier method call
        when(qualifier.getMethodExpression()).thenReturn(expression);
        when(expression.getQualifierExpression()).thenReturn(finalExpression);
        when(expression.getReferenceName()).thenReturn(methodFound);

        // Final expression should return null to break the loop if the method is not disableAutoComplete
        when(finalExpression.getMethodExpression()).thenReturn(expression);

        if (methodFound != "disableAutoComplete") {
            when(expression.getQualifierExpression()).thenReturn(null);
        }

        mockVisitor.visitDeclarationStatement(mockDeclarationStatement);
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(initializer), contains("Auto-complete enabled by default. Use the disableAutoComplete() API call to prevent automatic message completion."));
    }
}