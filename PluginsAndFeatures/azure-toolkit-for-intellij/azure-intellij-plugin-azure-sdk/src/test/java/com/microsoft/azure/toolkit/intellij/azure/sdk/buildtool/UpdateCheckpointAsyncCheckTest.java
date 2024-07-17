package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.UpdateCheckpointAsyncCheck.UpdateCheckpointAsyncVisitor;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateCheckpointAsyncCheckTest {

    // Create a mock ProblemsHolder to be used in the test
    @Mock
    private ProblemsHolder mockHolder;

    // Create a mock JavaElementVisitor for visiting the PsiMethodCallExpression
    @Mock
    private JavaElementVisitor mockVisitor;

    // Create a mock PsiMethodCallExpression.
    @Mock
    private PsiMethodCallExpression mockMethodCallExpression;


    @BeforeEach
    public void setUp() {
        // Set up the test
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockMethodCallExpression = mock(PsiMethodCallExpression.class);
    }

    @Test
    public void testWithSubscribe() {
        String packageName = "com.azure";
        String followingMethod = "subscribe";
        int numOfInvocations = 1;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }

    @Test
    public void testWithBlock() {
        String packageName = "com.azure";
        String followingMethod = "block";
        int numOfInvocations = 0;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }

    @Test
    public void testWithNullFollowingMethod() {
        String packageName = "com.azure";
        String followingMethod = null;
        int numOfInvocations = 1;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }


    private JavaElementVisitor createVisitor() {
        UpdateCheckpointAsyncVisitor mockVisitor = new UpdateCheckpointAsyncVisitor(mockHolder, true);
        return mockVisitor;
    }


    private void verifyProblemRegistered(String packageName, String mainMethodFound, int numOfInvocations, String followingMethod, String objectType) {

        PsiReferenceExpression mockReferenceExpression = mock(PsiReferenceExpression.class);

        // getFollowingMethodName mocking
        PsiReferenceExpression parentReferenceExpression = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression grandParentMethodCalLExpression = mock(PsiMethodCallExpression.class);

        // isCalledOnEventBatchContext
        PsiReferenceExpression mockQualifier = mock(PsiReferenceExpression.class);
        PsiParameter mockParameter = mock(PsiParameter.class);
        PsiClassType parameterType = mock(PsiClassType.class);
        PsiClass psiClass = mock(PsiClass.class);

        when(mockMethodCallExpression.getMethodExpression()).thenReturn(mockReferenceExpression);
        when(mockReferenceExpression.getReferenceName()).thenReturn(mainMethodFound);

        // getFollowingMethodName mocking
        when(mockMethodCallExpression.getParent()).thenReturn(mockReferenceExpression);
        when(mockReferenceExpression.getParent()).thenReturn(grandParentMethodCalLExpression);
        when(grandParentMethodCalLExpression.getMethodExpression()).thenReturn(parentReferenceExpression);
        when(parentReferenceExpression.getReferenceName()).thenReturn(followingMethod);

        // isCalledOnEventBatchContext
        when(mockReferenceExpression.getQualifierExpression()).thenReturn(mockQualifier);
        when(mockQualifier.resolve()).thenReturn(mockParameter);
        when(mockParameter.getType()).thenReturn(parameterType);
        when(parameterType.getPresentableText()).thenReturn(objectType);
        when(parameterType.resolve()).thenReturn(psiClass);
        when(psiClass.getQualifiedName()).thenReturn(packageName);

        mockVisitor.visitMethodCallExpression(mockMethodCallExpression);

        if (followingMethod != null && followingMethod.equals("subscribe")) {
            verify(mockHolder, times(numOfInvocations)).registerProblem(eq(mockMethodCallExpression), contains("Instead of subscribe(), use block() or block() with timeout or use the synchronous version updateCheckpoint()."));
        }
        else {
            verify(mockHolder, times(numOfInvocations)).registerProblem(eq(mockMethodCallExpression), contains("The updateCheckpointAsync() without block() will not do anything, use block() operator with a timeout or consider using the synchronous version updateCheckpoint()"));
        }
    }
}
