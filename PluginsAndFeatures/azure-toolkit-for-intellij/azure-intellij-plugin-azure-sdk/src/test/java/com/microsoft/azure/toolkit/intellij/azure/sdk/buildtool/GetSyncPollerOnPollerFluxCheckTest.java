package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.GetSyncPollerOnPollerFluxCheck.GetSyncPollerOnPollerFluxVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the GetSyncPollerOnPollerFluxCheck inspection tool.
 * The test class will test the visitor's ability to detect the use of getSyncPoller() on a PollerFlux
 * and register a problem with the suggestion message.
 * <p>
 * This is an example of an anti-pattern that would be detected by the inspection tool.
 * public void exampleUsage() {
 * PollerFlux<String> pollerFlux = createPollerFlux();
 * <p>
 * // Anti-pattern: Using getSyncPoller() on PollerFlux
 * SyncPoller<String, Void> syncPoller = pollerFlux.getSyncPoller();
 * }
 */
public class GetSyncPollerOnPollerFluxCheckTest {

    // Declare as instance variables
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiMethodCallExpression mockElement;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockElement = mock(PsiMethodCallExpression.class);
    }

    /**
     * Test to verify if the visitor is able to detect the use of getSyncPoller() on a PollerFlux
     * and register a problem with the suggestion message.
     */
    @Test
    public void testGetSyncPollerOnPollerFluxCheck() {
        assertVisitor();

        String methodName = "getSyncPoller";
        String className = "com.azure.core.util.polling.PollerFlux";
        int numberOfInvocations = 1;
        verifyRegisterProblem(methodName, className, numberOfInvocations);
    }

    /**
     * Test to verify if the visitor is not flagged when a different method name is used.
     * The visitor should not register a problem in this case.
     */
    @Test
    public void testGetSyncPollerOnPollerFluxCheckWithDifferentMethodName() {

        String methodName = "getAnotherMethod";
        String className = "com.azure.core.util.polling.PollerFlux";
        int numberOfInvocations = 0;
        verifyRegisterProblem(methodName, className, numberOfInvocations);
    }

    /**
     * Test to verify if the visitor is not flagged when a different package name is used.
     * The visitor should not register a problem in this case.
     */
    @Test
    public void testGetSyncPollerOnPollerFluxCheckWithDifferentClassName() {

        String methodName = "getSyncPoller";
        String className = "com.azure.core.util.polling.DifferentClassName";
        int numberOfInvocations = 0;
        verifyRegisterProblem(methodName, className, numberOfInvocations);
    }

    /**
     * Test to verify if the visitor is not flagged when the package name is null.
     * The visitor should not register a problem in this case.
     */
    @Test
    public void testGetSyncPollerOnPollerFluxCheckWithNullClassName() {

        String methodName = "getSyncPoller";
        String className = null;
        int numberOfInvocations = 0;
        verifyRegisterProblem(methodName, className, numberOfInvocations);
    }

    /**
     * A helper method to create the visitor for the test.
     *
     * @return JavaElementVisitor
     */
    private JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        GetSyncPollerOnPollerFluxVisitor visitor = new GetSyncPollerOnPollerFluxVisitor(mockHolder, isOnTheFly);
        return visitor;
    }

    /**
     * A helper method to assert the visitor is not null and is an instance of JavaElementVisitor.
     */
    private void assertVisitor() {
        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    /**
     * A helper method to verify if the visitor is able to detect the use of getSyncPoller() on a PollerFlux
     * and register a problem with the suggestion message.
     *
     * @param methodName          The method name to be used in the test
     * @param className           The package name to be used in the test
     * @param numberOfInvocations The number of times registerProblem should be called
     */
    private void verifyRegisterProblem(String methodName, String className, int numberOfInvocations) {

        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiExpression expression = mock(PsiExpression.class);
        PsiType type = mock(PsiType.class);
        PsiClass containingClass = mock(PsiClass.class);
        PsiTreeUtil mockTreeUtil = mock(PsiTreeUtil.class);

        when(mockElement.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getReferenceName()).thenReturn(methodName);
        when(referenceExpression.getQualifierExpression()).thenReturn(expression);
        when(expression.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn(className);
        when(mockTreeUtil.getParentOfType(mockElement, PsiClass.class)).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(className);

        // Act
        mockVisitor.visitMethodCallExpression(mockElement);

        verify(mockHolder, times(numberOfInvocations)).registerProblem(Mockito.eq(mockElement), Mockito.contains("Use of getSyncPoller() on a PollerFlux detected. Directly use SyncPoller to handle synchronous polling tasks"));
    }
}
