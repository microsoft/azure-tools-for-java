package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Mock;

import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReferenceExpression;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * This class is used to test the StorageUploadWithoutLengthCheck class.
 * It tests the visitor method to check if the upload methods are being called without a 'length' parameter of type 'long'.
 *
 * These are examples of situations where the visitor method should register a problem:
 * 1. @ServiceMethod(returns = ReturnType.SINGLE)
 *     public void upload(InputStream data) {
 *         uploadWithResponse(new BlobParallelUploadOptions(data), null, null);
 *     }
 *
 * 2. uploadWithResponse(new BlobParallelUploadOptions(data).setRequestConditions(blobRequestConditions), null, Context.NONE);
 *
 * 3. upload(data, false);
 */

public class StorageUploadWithoutLengthCheckTest {

    // Declare as instance variables
    @Mock
    private ProblemsHolder mockHolder;
    private PsiElementVisitor mockVisitor;
    private PsiElement mockElement;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockElement = mock(PsiElement.class);
        mockVisitor = createVisitor();
    }

    /**
     * assertVisitor(mockVisitor) asserts that it the visitor not null and is an instance of JavaRecursiveElementWalkingVisitor.
     * verifyRegisterProblem(mockVisitor, METHOD_TO_CHECK, NUMBER_OF_INVOCATIONS) verifies that the registerProblem method is called
     * when the visitor visits a method call expression with the method name METHOD_TO_CHECK.
     */
    @Test
    public void testStorageUploadWithoutLengthCheck() {
        assertVisitor(mockVisitor);

        String METHOD_TO_CHECK = "upload";
        int NUMBER_OF_INVOCATIONS = 1;  // Number of times registerProblem should be called
        verifyRegisterProblem(mockVisitor, METHOD_TO_CHECK, NUMBER_OF_INVOCATIONS);

    }

    // Create a visitor for the class under test
    private PsiElementVisitor createVisitor() {
        boolean isOnTheFly = false;

        // Create an instance of the class under test
        StorageUploadWithoutLengthCheck check = new StorageUploadWithoutLengthCheck();
        return check.buildVisitor(mockHolder, isOnTheFly);
    }

    // Assert that the visitor is not null and is an instance of JavaRecursiveElementWalkingVisitor
    void assertVisitor(PsiElementVisitor visitor) {
        assertNotNull(visitor);
        assertTrue(visitor instanceof JavaRecursiveElementWalkingVisitor);
    }

    // Verify that the registerProblem method is called when the visitor visits a method call expression with the method name METHOD_TO_CHECK.
    private void verifyRegisterProblem(PsiElementVisitor visitor, String METHOD_TO_CHECK, int NUMBER_OF_INVOCATIONS) {

        // Arrange
        PsiMethodCallExpression mockExpression = mock(PsiMethodCallExpression.class);
        PsiExpressionList mockArgList = mock(PsiExpressionList.class);

        PsiExpression mockArgExpression = mock(PsiExpression.class);

        PsiMethodCallExpression mockArgMethodCall = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression mockMethodExpression = mock(PsiReferenceExpression.class);
        PsiExpression mockQualifier = mock(PsiExpression.class);
        PsiNewExpression nextMockQualifier = mock(PsiNewExpression.class);
        PsiType mockType = mock(PsiType.class);
        PsiMethodCallExpression mockQualifierMethodCall = mock(PsiMethodCallExpression.class);


        // Act
        when(mockExpression.getMethodExpression()).thenReturn(mockMethodExpression);
        when(mockMethodExpression.getReferenceName()).thenReturn(METHOD_TO_CHECK);

        when(mockExpression.getArgumentList()).thenReturn(mockArgList);
        when(mockArgList.getExpressions()).thenReturn(new PsiExpression[0]);

        when(mockArgExpression.getType()).thenReturn(mockType);
        when(mockType.toString()).thenReturn("long");

        when(mockArgMethodCall.getMethodExpression()).thenReturn(mockMethodExpression);
        when(mockMethodExpression.getQualifierExpression()).thenReturn(mockQualifier);

        when(mockQualifierMethodCall.getMethodExpression()).thenReturn(mockMethodExpression);
        when(mockMethodExpression.getQualifierExpression()).thenReturn(nextMockQualifier);

        when(nextMockQualifier.getArgumentList()).thenReturn(mockArgList);
        when(mockArgList.getExpressions()).thenReturn(new PsiExpression[0]);

        when(nextMockQualifier.getType()).thenReturn(mockType);
        when(mockType.toString()).thenReturn("long");

        ((JavaRecursiveElementWalkingVisitor) visitor).visitMethodCallExpression(mockExpression);

        // Verify registerProblem is called
        verify(mockHolder, times(NUMBER_OF_INVOCATIONS)).registerProblem(Mockito.eq(mockExpression), Mockito.contains("Azure Storage upload API without length parameter detected"));
    }

    /**
     * Test the visitor method when the method call expression is not in the list of methods to check.
     * This should not call registerProblem.
     */
    @Test
    public void testMethodCallNotInList() {
        String METHOD_TO_CHECK = "notInList";
        int NUMBER_OF_INVOCATIONS = 0;  // Number of times registerProblem should be called
        verifyRegisterProblem(mockVisitor, METHOD_TO_CHECK, NUMBER_OF_INVOCATIONS);
    }
}

