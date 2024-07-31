package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.psi.PsiClassType;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.UseOfBlockOnAsyncClientsCheck.UseOfBlockOnAsyncClientsVisitor;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class is used to test the UseOfBlockOnAsyncClientsCheck class.
 * The UseOfBlockOnAsyncClientsCheck class is an inspection tool that checks for the use of blocking method on async clients in Azure SDK.
 * This inspection will check for the use of blocking method on reactive types like Mono, Flux, etc.
 *  This is an example of what should be flagged:
 *
 *  private ServiceBusReceiverAsyncClient receiver;
 *  receiver.complete(received).block(Duration.ofSeconds(15));
 *
 *  private final ServiceBusReceiverAsyncClient client;
 *  try {
 *                 if (isComplete) {
 *                     client.complete(message)
 *                         .doOnSuccess(success -> System.out.println("Message completed successfully"))
 *                         .doOnError(error -> System.err.println("Error completing message: " + error.getMessage()))
 *                         .log()
 *                         .timeout(Duration.ofSeconds(30))
 *                         .retry(3)
 *                         .block();
 *
 *                 } else {
 *                     client.abandon(message).block();
 *                 }
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
     * This is the main test method that tests the use of blocking method on async clients.
     * This method should be flagged by the inspection tool as it is a blocking method calls on an async client.
     */
    @Test
    public void testUseOfBlockOnAsyncClient() {

        int numberOfInvocations = 1;
        String methodName = "block";
        String clientPackageName = "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient";
        String reactivePackageName = "reactor.core.publisher.Flux";

        // verify register problem
        verifyRegisterProblem(methodName, clientPackageName, numberOfInvocations, reactivePackageName);
    }

    /**
     * This test method tests the use of blockOptional() method on async clients.
     * This method should be flagged by the inspection tool as it is a blockOptional() method call on an async client.
     */
    @Test
    public void testUseOfDifferentBlockOnAsyncClient() {

        int numberOfInvocations = 1;
        String methodName = "blockOptional";
        String clientPackageName = "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient";
        String reactivePackageName = "reactor.core.publisher.Mono";

        // verify register problem
        verifyRegisterProblem(methodName, clientPackageName, numberOfInvocations, reactivePackageName);
    }

    /**
     * This test method tests the use of blockFirst() method on non-azure async clients.
     * This method should not be flagged by the inspection tool
     */
    @Test
    public void testBlockOnAsyncClientsWithNonAzureClient() {

        int numberOfInvocations = 0;
        String methodName = "blockFirst";
        String clientPackageName = "com.notAzure.";
        String reactivePackageName = "reactor.core.publisher.Flux";

        // verify register problem
        verifyRegisterProblem(methodName, clientPackageName, numberOfInvocations, reactivePackageName);
    }

    /**
     * This test method tests the use of a different method call on async clients.
     * This method should not be flagged by the inspection tool.
     */
    @Test
    public void testVisitOnDifferentMethodCall() {

        int numberOfInvocations = 0;
        String methodName = "nonBlockingMethod";
        String clientPackageName = "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient";
        String reactivePackageName = "reactor.core.publisher.Flux";

        // verify register problem
        verifyRegisterProblem(methodName, clientPackageName, numberOfInvocations, reactivePackageName);
    }

    /**
     * This test method tests the use of blocking method on a non-reactive type.
     * This method should not be flagged by the inspection tool.
     */
    @Test
    public void testBlockOnNonReactiveType() {
        int numberOfInvocations = 0;
        String methodName = "block";
        String clientPackageName = "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient";
        String nonReactivePackageName = "java.util.List";

        // verify register problem
        verifyRegisterProblem(methodName, clientPackageName, numberOfInvocations, nonReactivePackageName);
    }

    /**
     * This test method tests the use of blocking method on a non-async client.
     * This method should not be flagged by the inspection tool.
     */
    @Test
    public void testBlockOnAzureNonAsyncClient() {
        int numberOfInvocations = 0;
        String methodName = "block";
        String nonAsyncClientPackageName = "com.azure.messaging.servicebus.ServiceBusReceiverClient";
        String reactivePackageName = "reactor.core.publisher.Mono";

        // verify register problem
        verifyRegisterProblem(methodName, nonAsyncClientPackageName, numberOfInvocations, reactivePackageName);
    }

    /**
     * Create a visitor object for the test
     *
     * @return JavaElementVisitor
     */
    private JavaElementVisitor createVisitor() {
        return new UseOfBlockOnAsyncClientsVisitor(mockHolder);
    }

    /**
     * This method is used to verify the registerProblem method is called when the method call is a blocking method call on an async client.
     *
     * @param methodName          String - the name of the method called
     * @param clientPackageName   String - the package name of the async client
     * @param numberOfInvocations int - the number of times registerProblem should be called
     * @param reactivePackageName String - the package name of the reactive type
     */
    private void verifyRegisterProblem(String methodName, String clientPackageName, int numberOfInvocations, String reactivePackageName) {

        // Arrange
        PsiReferenceExpression referenceExpression = mock(PsiReferenceExpression.class);
        PsiMethodCallExpression expression = mock(PsiMethodCallExpression.class);
        PsiClassType type = mock(PsiClassType.class);
        PsiClass qualifierReturnTypeClass = mock(PsiClass.class);

        PsiReferenceExpression clientReferenceExpression = mock(PsiReferenceExpression.class);
        PsiReferenceExpression clientQualifierExpression = mock(PsiReferenceExpression.class);
        PsiClassType clientType = mock(PsiClassType.class);
        PsiClass clientReturnTypeClass = mock(PsiClass.class);

        // visitMethodCallExpression method
        when(mockElement.getMethodExpression()).thenReturn(referenceExpression);
        when(referenceExpression.getReferenceName()).thenReturn(methodName);

        // checkIfAsyncContext method
        when(referenceExpression.getQualifierExpression()).thenReturn(expression);
        when(expression.getType()).thenReturn(type);
        when(type.resolve()).thenReturn(qualifierReturnTypeClass);

        // isReactiveType method
        when(qualifierReturnTypeClass.getQualifiedName()).thenReturn(reactivePackageName);

        // isAzureAsyncClient method
        when(expression.getMethodExpression()).thenReturn(clientReferenceExpression);
        when(clientReferenceExpression.getQualifierExpression()).thenReturn(clientQualifierExpression);
        when(clientQualifierExpression.getType()).thenReturn(clientType);
        when(clientType.resolve()).thenReturn(clientReturnTypeClass);
        when(clientReturnTypeClass.getQualifiedName()).thenReturn(clientPackageName);

        // Act
        mockVisitor.visitMethodCallExpression(mockElement);

        // Verify registerProblem is not called
        verify(mockHolder, times(numberOfInvocations)).registerProblem(Mockito.eq(mockElement), Mockito.contains("Use of block methods on asynchronous clients detected. Switch to synchronous APIs instead."));
    }
}
