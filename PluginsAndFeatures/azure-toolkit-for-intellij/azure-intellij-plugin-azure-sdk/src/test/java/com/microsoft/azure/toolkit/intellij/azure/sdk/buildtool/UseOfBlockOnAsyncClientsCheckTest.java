package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.UseOfBlockOnAsyncClientsCheck.UseOfBlockOnAsyncClientsVisitor;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
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
 * This class is used to test the UseOfBlockOnAsyncClientsCheck class.
 * The UseOfBlockOnAsyncClientsCheck class is an inspection tool that checks for the use of block() method on async clients in Azure SDK.
 * This inspection will check for the use of block() method on reactive types like Mono, Flux, etc.
 * 1. Flux<ByteBuffer> downloadFlux = blobAsyncClient.download()
 *                 .doOnComplete(() -> System.out.println("Download complete"))
 *                 .doOnError(error -> System.err.println("Error downloading file: " + error.getMessage()));
 *
 *             // This call should be flagged
 *             downloadFlux.blockFirst();
 *
 * 2. // Download a file from Azure Blob Storage using the async client
 *             Mono<Void> downloadMono = blobAsyncClient.downloadToFile("downloadedFilePath")
 *                 .doOnSuccess(response -> System.out.println("File downloaded successfully"))
 *                 .doOnError(error -> System.err.println("Error downloading file: " + error.getMessage()));
 *
 *             // This call should also be flagged by the inspection tool
 *             downloadMono.blockOptional();
 */
public class UseOfBlockOnAsyncClientsCheckTest {

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
     * This is the main test method that tests the KustoQueriesWithTimeIntervalInQueryStringCheck class.
     */
    @Test
    public void testUseOfBlockOnAsyncClientsVisitor() {

        // assert visitor
        assertVisitor();

        int numberOfInvocations = 1;
        String methodName = "block";
        String packageName = "com.azure.";

        // verify register problem
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }

    /**
     * This test method tests the use of blockOptional() method on async clients.
     * This method should be flagged by the inspection tool.
     */
    @Test
    public void testUseOfDifferentBlockOnAsyncClientsVisitor() {

        // assert visitor
        assertVisitor();

        int numberOfInvocations = 1;
        String methodName = "blockOptional";
        String packageName = "com.azure.";

        // verify register problem
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }

    /**
     * This test method tests the use of blockFirst() method on non-azure async clients.
     * This method should not be flagged by the inspection tool.
     */
    @Test
    public void testUseOfBlockOnAsyncClientsVisitorWithNonAzureClient() {

        // assert visitor
        assertVisitor();

        int numberOfInvocations = 0;
        String methodName = "blockFirst";
        String packageName = "com.notAzure.";

        // verify register problem
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }

    /**
     * This test method tests the use of a different method call on async clients.
     * This method should not be flagged by the inspection tool.
     */
    @Test
    public void testVisitOnDifferentMethodCall() {

        // assert visitor
        assertVisitor();

        int numberOfInvocations = 0;
        String methodName = "nonBlockingMethod";
        String packageName = "com.azure.";

        // verify register problem
        verifyRegisterProblem(methodName, packageName, numberOfInvocations);
    }

    /** Create a visitor object for the test
     * @return JavaElementVisitor
     */
    private JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        UseOfBlockOnAsyncClientsVisitor visitor = new UseOfBlockOnAsyncClientsCheck.UseOfBlockOnAsyncClientsVisitor(mockHolder, isOnTheFly);
        return visitor;
    }

    /** Assert that the visitor object is not null and is an instance of JavaElementVisitor
     * @return void
     */
    private void assertVisitor() {
        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    /** Verify that the registerProblem method is called the expected number of times
     * @param methodName The name of the method call -- used to determine if the method call is a block method
     * @param packageName The package name of the class containing the method call -- used to determine if the client is an Azure SDK client
     * @param numberOfInvocations Number of times registerProblem should be called
     */
    private void verifyRegisterProblem(String methodName, String packageName, int numberOfInvocations) {

        // Arrange
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiExpression expression = mock(PsiExpression.class);
        PsiType type = mock(PsiType.class);
        PsiClass containingClass = mock(PsiClass.class);
        PsiTreeUtil mockTreeUtil = mock(PsiTreeUtil.class);

        when(mockElement.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getReferenceName()).thenReturn(methodName);

        when(referenceExpression.getQualifierExpression()).thenReturn(expression);
        when(expression.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn("reactor.core.publisher.Flux");

        when(mockTreeUtil.getParentOfType(mockElement, PsiClass.class)).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        // Act
        mockVisitor.visitMethodCallExpression(mockElement);

        // Verify registerProblem is not called
        verify(mockHolder, times(numberOfInvocations)).registerProblem(Mockito.eq(mockElement), Mockito.contains("Use of block methods on asynchronous clients detected. Switch to synchronous APIs instead."));
    }
}

