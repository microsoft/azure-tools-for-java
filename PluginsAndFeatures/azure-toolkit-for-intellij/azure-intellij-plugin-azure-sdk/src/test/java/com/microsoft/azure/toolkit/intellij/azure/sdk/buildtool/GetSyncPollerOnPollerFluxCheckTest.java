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


    private JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        GetSyncPollerOnPollerFluxVisitor visitor = new GetSyncPollerOnPollerFluxVisitor(mockHolder, isOnTheFly);
        return visitor;
    }


    /**
     * Test to verify if the visitor is able to detect the use of getSyncPoller() on a PollerFlux
     * and register a problem with the suggestion message.
     */
    @Test
    public void testGetSyncPollerOnPollerFluxCheck() {

        assertVisitor();

        String methodName = "getSyncPoller";
        String packageName = "com.azure.core.util.polling.PollerFlux";
        int numberOfInvocations = 1;
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }


    @Test
    public void testGetSyncPollerOnPollerFluxCheckWithDifferentMethodName() {

        assertVisitor();

        String methodName = "getAnotherMethod";
        String packageName = "com.azure.core.util.polling.PollerFlux";
        int numberOfInvocations = 0;
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }


    @Test
    public void testGetSyncPollerOnPollerFluxCheckWithDifferentPackageName() {

        assertVisitor();

        String methodName = "getSyncPoller";
        String packageName = "com.azure.core.util.polling.DifferentPackageName";
        int numberOfInvocations = 0;
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }


    @Test
    public void testGetSyncPollerOnPollerFluxCheckWithNullPackageName() {

        assertVisitor();

        String methodName = "getSyncPoller";
        String packageName = null;
        int numberOfInvocations = 0;
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }



    private void assertVisitor() {
        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    private void verifyRegisterProblem(String methodName, String packageName, int numberOfInvocations) {

        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiExpression expression = mock(PsiExpression.class);
        PsiType type = mock(PsiType.class);
        PsiClass containingClass = mock(PsiClass.class);
        PsiTreeUtil mockTreeUtil = mock(PsiTreeUtil.class);

        when(mockElement.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getReferenceName()).thenReturn(methodName);

        when(referenceExpression.getQualifierExpression()).thenReturn(expression);
        when(expression.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn(packageName);


        when(mockTreeUtil.getParentOfType(mockElement, PsiClass.class)).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        // Act
        mockVisitor.visitMethodCallExpression(mockElement);

        // Verify registerProblem is not called
        verify(mockHolder, times(numberOfInvocations)).registerProblem(Mockito.eq(mockElement), Mockito.contains("Use of getSyncPoller() on a PollerFlux detected. Directly use SyncPoller to handle synchronous polling tasks"));
    }
}
