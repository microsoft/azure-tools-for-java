package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.ServiceBusReceiveModeCheck.ServiceBusReceiveModeVisitor;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServiceBusReceiveModeCheckTest {

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


    @Test
    public void testProblemRegisteredWithReceiveModeAndHighPeekLock() {

        String packageName = "servicebus";
        String methodFoundOne = "receiveMode";
        String methodFoundTwo = "prefetchCount";
        int numOfInvocations = 1;
        String prefetchCountValue = "100";

        verifyProblemRegistered(packageName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    @Test
    public void testProblemRegisteredWithoutReceiveModeAndHighPeekLock() {

        String packageName = "servicebus";
        String methodFoundOne = "notreceiveMode";
        String methodFoundTwo = "prefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "100";

        verifyProblemRegistered(packageName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    @Test
    public void testProblemRegisteredWithReceiveModeAndNoPrefetchCount() {

        String packageName = "servicebus";
        String methodFoundOne = "receiveMode";
        String methodFoundTwo = "noprefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "100";

        verifyProblemRegistered(packageName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    @Test
    public void testProblemRegisteredWithoutReceiveModeAndNoPrefetchCount() {

        String packageName = "servicebus";
        String methodFoundOne = "notreceiveMode";
        String methodFoundTwo = "noprefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "100";

        verifyProblemRegistered(packageName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }




    /**
     * Create a visitor by calling the buildVisitor method of the ServiceBusReceiveModeCheck class.
     *
     * @return The visitor created
     */
    private JavaElementVisitor createVisitor() {
        ServiceBusReceiveModeVisitor mockVisitor = new ServiceBusReceiveModeVisitor(mockHolder, true);
        return mockVisitor;
    }



    private void verifyProblemRegistered(String packageName, String methodFoundOne, String methodFoundTwo, int numOfInvocations, String prefetchCountValue) {

        PsiVariable declaredElement = mock(PsiVariable.class);
        PsiElement[] declaredElements = new PsiElement[]{declaredElement};

        // processVariableDeclaration method
        PsiType clientType = mock(PsiType.class);
        PsiMethodCallExpression initializer = mock(PsiMethodCallExpression.class);

        // determineReceiveMode method
        // First level qualifier method call
        PsiReferenceExpression expressionOne = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression qualifierOne = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpressionOne = mock(PsiReferenceExpression.class);

        // receiveModePeekLockCheck
        PsiExpression receiveModePeekLockParameter = mock(PsiExpression.class);
        PsiReferenceExpression prefetchCountParameter = mock(PsiReferenceExpression.class);
        PsiExpressionList methodArgumentList = mock(PsiExpressionList.class);
        PsiExpression[] methodArguments = new PsiExpression[]{prefetchCountParameter, receiveModePeekLockParameter};

        PsiMethodCallExpression transitionMethodCall = mock(PsiMethodCallExpression.class);

        // Second level qualifier method call
        PsiReferenceExpression expressionTwo = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression qualifierTwo = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpressionTwo = mock(PsiReferenceExpression.class);


        when(mockDeclarationStatement.getDeclaredElements()).thenReturn(declaredElements);

        // processVariableDeclaration method
        when(declaredElement.getType()).thenReturn(clientType);
        when(declaredElement.getInitializer()).thenReturn(initializer);
        when(clientType.getCanonicalText()).thenReturn(packageName);

        // determineReceiveMode method
        // First level qualifier method call
        when(initializer.getMethodExpression()).thenReturn(expressionOne);
        when(expressionOne.getQualifierExpression()).thenReturn(qualifierOne);
        when(qualifierOne.getMethodExpression()).thenReturn(methodExpressionOne);
        when(methodExpressionOne.getReferenceName()).thenReturn(methodFoundOne);

        // receiveModePeekLockCheck
        when(qualifierOne.getArgumentList()).thenReturn(methodArgumentList);
        when(methodArgumentList.getExpressions()).thenReturn(methodArguments);
        when(receiveModePeekLockParameter.getText()).thenReturn("PEEK_LOCK");

        // Second level qualifier method call
        when(methodExpressionOne.getQualifierExpression()).thenReturn(qualifierTwo);
        when(qualifierTwo.getMethodExpression()).thenReturn(methodExpressionTwo);
        when(methodExpressionTwo.getReferenceName()).thenReturn(methodFoundTwo);

        // getPrefetchCount method
        when(qualifierTwo.getArgumentList()).thenReturn(methodArgumentList);
        when(methodArgumentList.getExpressions()).thenReturn(methodArguments);
        when(prefetchCountParameter.getText()).thenReturn(prefetchCountValue);

        if (methodFoundOne != "receiveMode" || methodFoundTwo != "prefetchCount") {
            when(methodExpressionTwo.getQualifierExpression()).thenReturn(null);
        }

        mockVisitor.visitDeclarationStatement(mockDeclarationStatement);
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(qualifierTwo),
                contains("A high prefetch value in PEEK_LOCK detected. We recommend a prefetch value of 0 or 1 for efficient message retrieval."));
    }
}