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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.ServiceBusReceiveModeCheck.ServiceBusReceiveModeVisitor;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class contains the tests for the ServiceBusReceiveModeCheck class.
 * It tests the visitDeclarationStatement method of the ServiceBusReceiveModeVisitor class.
 * It tests the method with different combinations of the receiveMode and prefetchCount methods.
 */
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


    /**
     * This tests the visitor with receiveMode as PEEK_LOCK and prefetchCount as 100.
     * The test verifies that a problem is registered with the ProblemsHolder.
     */
    @ParameterizedTest
    @ValueSource(strings = {"ServiceBusReceiverClient", "ServiceBusReceiverAsyncClient", "ServiceBusProcessorClient"})
    public void testWithPeekLockAndHighPrefetchCount(String clientName) {

        String methodFoundOne = "receiveMode";
        String methodFoundTwo = "prefetchCount";
        int numOfInvocations = 1;
        String prefetchCountValue = "100";

        verifyProblemRegistered(clientName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    /**
     * This tests the visitor with receiveMode as PEEK_LOCK and prefetchCount as 1.
     * The test verifies that a problem is not registered with the ProblemsHolder.
     */
    @Test
    public void testWithPeekLockAndLowPrefetchCount() {

        String clientName = "ServiceBusReceiverClient";
        String methodFoundOne = "receiveMode";
        String methodFoundTwo = "prefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "1";

        verifyProblemRegistered(clientName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    /**
     * This tests the visitor without a receiveMode.
     * The test verifies that a problem is not registered with the ProblemsHolder.
     */
    @Test
    public void testWithoutPeekLockAndHighPrefetchCount() {

        String clientName = "ServiceBusReceiverClient";
        String methodFoundOne = "notreceiveMode";
        String methodFoundTwo = "prefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "100";

        verifyProblemRegistered(clientName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    /**
     * This tests the visitor without a receiveMode and with a prefetchCount of 100.
     * The test verifies that a problem is not registered with the ProblemsHolder.
     */
    @Test
    public void testWithPeekLockAndNoPrefetchCount() {

        String clientName = "ServiceBusReceiverClient";
        String methodFoundOne = "receiveMode";
        String methodFoundTwo = "noprefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "100";

        verifyProblemRegistered(clientName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    /**
     * This tests the visitor without a receiveMode and without a prefetchCount.
     * The test verifies that a problem is not registered with the ProblemsHolder.
     */
    @Test
    public void testWithoutPeekLockAndNoPrefetchCount() {

        String clientName = "servicebus";
        String methodFoundOne = "notreceiveMode";
        String methodFoundTwo = "noprefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "100";

        verifyProblemRegistered(clientName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
    }

    /**
     * This tests the visitor with a different Azure service.
     * The test verifies that a problem is not registered with the ProblemsHolder.
     */
    @Test
    public void testDifferentAzureService() {

        String clientName = "notservicebus";
        String methodFoundOne = "notreceiveMode";
        String methodFoundTwo = "noprefetchCount";
        int numOfInvocations = 0;
        String prefetchCountValue = "100";

        verifyProblemRegistered(clientName, methodFoundOne, methodFoundTwo, numOfInvocations, prefetchCountValue);
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


    /**
     * This method verifies that a problem is registered with the ProblemsHolder.
     *
     * @param clientName         The name of the client eg. ServiceBusReceiverClient
     * @param methodFoundOne     The first method found, receiveMode
     * @param methodFoundTwo     The second method found, prefetchCount
     * @param numOfInvocations   The number of times the registerProblem method is called
     * @param prefetchCountValue The value of the prefetchCount
     */
    private void verifyProblemRegistered(String clientName, String methodFoundOne, String methodFoundTwo, int numOfInvocations, String prefetchCountValue) {

        String azurePackageName = "com.azure";

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

        // Second level qualifier method call
        PsiMethodCallExpression qualifierTwo = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpressionTwo = mock(PsiReferenceExpression.class);

        PsiElement prefetchCountMethod = mock(PsiElement.class);


        when(mockDeclarationStatement.getDeclaredElements()).thenReturn(declaredElements);

        // processVariableDeclaration method
        when(declaredElement.getType()).thenReturn(clientType);
        when(declaredElement.getInitializer()).thenReturn(initializer);
        when(clientType.getCanonicalText()).thenReturn(azurePackageName);
        when(clientType.getPresentableText()).thenReturn(clientName);

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

        when(methodExpressionTwo.getReferenceNameElement()).thenReturn(prefetchCountMethod);

        if (methodFoundOne != "receiveMode" || methodFoundTwo != "prefetchCount") {
            when(methodExpressionTwo.getQualifierExpression()).thenReturn(null);
        }

        mockVisitor.visitDeclarationStatement(mockDeclarationStatement);
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(prefetchCountMethod), contains("A high prefetch value in PEEK_LOCK detected. We recommend a prefetch value of 0 or 1 for efficient message retrieval."));
    }
}
