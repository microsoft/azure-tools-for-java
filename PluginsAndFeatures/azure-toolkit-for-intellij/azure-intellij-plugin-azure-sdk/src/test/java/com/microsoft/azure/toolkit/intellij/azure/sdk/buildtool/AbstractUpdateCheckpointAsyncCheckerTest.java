package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.UpdateCheckpointAsyncBlockChecker.BlockVisitor;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.UpdateCheckpointAsyncSubscribeChecker.SubscribeVisitor;
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
 * This class is used to test the AbstractUpdateCheckpointChecker class.
 * The test methods test the visitMethodCallExpression method of the classes that extend the AbstractUpdateCheckpointChecker class.
 * These are the UpdateCheckpointBlockChecker and UpdateCheckpointSubscribeChecker classes.
 */
public class AbstractUpdateCheckpointAsyncCheckerTest {

    // Create a mock ProblemsHolder to be used in the test
    @Mock
    private ProblemsHolder mockHolder;

    // Create a mock PsiMethodCallExpression.
    @Mock
    private PsiMethodCallExpression mockMethodCallExpression;


    @BeforeEach
    public void setUp() {
        // Set up the test
        mockHolder = mock(ProblemsHolder.class);
        mockMethodCallExpression = mock(PsiMethodCallExpression.class);
    }

    /**
     * This test method tests the visitMethodCallExpression method of the UpdateCheckpointAsyncSubscribeVisitor class.
     * The test checks if the problem is registered when the following method is subscribe.
     */
    @Test
    public void testWithSubscribe() {

        SubscribeVisitor mockSubscribeVisitor = new SubscribeVisitor(mockHolder, true);

        String packageName = "com.azure";
        String followingMethod = "subscribe";
        int numOfInvocations = 1;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(mockSubscribeVisitor, packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }

    /**
     * This test method tests the visitMethodCallExpression method of the UpdateCheckpointAsyncBlockVisitor class.
     * The test checks if the problem is registered when the following method is block.
     * A problem should NOT be registered in this case.
     */
    @Test
    public void testWithBlock() {

        BlockVisitor mockBlockVisitor = new BlockVisitor(mockHolder, true);

        String packageName = "com.azure";
        String followingMethod = "block";
        int numOfInvocations = 0;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(mockBlockVisitor, packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }

    /**
     * This test method tests the visitMethodCallExpression method of the UpdateCheckpointAsyncBlockVisitor class.
     * The test checks if the problem is registered when the following method is null.
     * A problem should be registered in this case -- there is no 'block' or 'block(timeout)' method following the 'updateCheckpointAsync' method.
     */
    @Test
    public void testWithNullFollowingMethod() {

        BlockVisitor mockBlockVisitor = new BlockVisitor(mockHolder, true);

        String packageName = "com.azure";
        String followingMethod = null;
        int numOfInvocations = 1;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(mockBlockVisitor, packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }

    /**
     * This test method tests the visitMethodCallExpression method of the UpdateCheckpointAsyncSubscribeVisitor class.
     * The test checks if the problem is registered when the following method is null.
     * A problem should NOT be registered in this case because the 'subscribe' method is not called
     */
    @Test
    public void testSubscribeCheckerWithNullFollowingMethod() {

        SubscribeVisitor mockSubscribeVisitor = new SubscribeVisitor(mockHolder, true);

        String packageName = "com.azure";
        String followingMethod = null;
        int numOfInvocations = 0;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(mockSubscribeVisitor, packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }

    /**
     * This test method tests the visitMethodCallExpression method of the UpdateCheckpointAsyncBlockVisitor class.
     * The test checks if the problem is registered when the package name is not com.azure....
     * A problem should NOT be registered in this case.
     */
    @Test
    public void testWithWrongPackage() {

        BlockVisitor mockBlockVisitor = new BlockVisitor(mockHolder, true);

        String packageName = "com.microsoft.azure";
        String followingMethod = "block";
        int numOfInvocations = 0;
        String mainMethodFound = "updateCheckpointAsync";
        String objectType = "EventBatchContext";

        verifyProblemRegistered(mockBlockVisitor, packageName, mainMethodFound, numOfInvocations, followingMethod, objectType);
    }

    /**
     * This method verifies if the problem is registered with the ProblemsHolder.
     */
    private void verifyProblemRegistered(JavaElementVisitor mockVisitor, String packageName, String mainMethodFound, int numOfInvocations, String followingMethod, String objectType) {

        PsiReferenceExpression mockReferenceExpression = mock(PsiReferenceExpression.class);

        // getFollowingMethodName mocking
        PsiReferenceExpression parentReferenceExpression = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression grandParentMethodCalLExpression = mock(PsiMethodCallExpression.class);

        // isCalledOnEventBatchContext
        PsiReferenceExpression mockQualifier = mock(PsiReferenceExpression.class);
        PsiParameter mockParameter = mock(PsiParameter.class);
        PsiClassType parameterType = mock(PsiClassType.class);
        PsiClass psiClass = mock(PsiClass.class);
        PsiExpression duration = mock(PsiExpression.class);
        PsiExpressionList arguments = mock(PsiExpressionList.class);
        PsiExpression[] expressions = new PsiExpression[]{duration};
        PsiType durationType = mock(PsiType.class);

        when(mockMethodCallExpression.getMethodExpression()).thenReturn(mockReferenceExpression);
        when(mockReferenceExpression.getReferenceName()).thenReturn(mainMethodFound);

        // getFollowingMethodName mocking
        when(mockMethodCallExpression.getParent()).thenReturn(mockReferenceExpression);
        when(mockReferenceExpression.getParent()).thenReturn(grandParentMethodCalLExpression);
        when(grandParentMethodCalLExpression.getMethodExpression()).thenReturn(parentReferenceExpression);
        when(parentReferenceExpression.getReferenceName()).thenReturn(followingMethod);
        when(grandParentMethodCalLExpression.getArgumentList()).thenReturn(arguments);
        when(arguments.getExpressions()).thenReturn(expressions);
        when(expressions[0].getType()).thenReturn(durationType);
        when(durationType.getPresentableText()).thenReturn("java.time.Duration");

        // isCalledOnEventBatchContext
        when(mockReferenceExpression.getQualifierExpression()).thenReturn(mockQualifier);
        when(mockQualifier.resolve()).thenReturn(mockParameter);
        when(mockParameter.getType()).thenReturn(parameterType);
        when(parameterType.getPresentableText()).thenReturn(objectType);
        when(parameterType.resolve()).thenReturn(psiClass);
        when(psiClass.getQualifiedName()).thenReturn(packageName);

        mockVisitor.visitMethodCallExpression(mockMethodCallExpression);

        if (followingMethod != null && followingMethod.equals("subscribe")) {
            verify(mockHolder, times(numOfInvocations)).registerProblem(eq(mockMethodCallExpression), contains("Instead of `subscribe()`, call `block()` or `block()` with timeout, or use the synchronous version `updateCheckpoint()`"));
        } else {
            verify(mockHolder, times(numOfInvocations)).registerProblem(eq(mockMethodCallExpression), contains("Calling updateCheckpointAsync() without block() will not do anything, use `block()` or `block` operator with a timeout, or consider using the synchronous version `updateCheckpoint()."));
        }
    }
}
