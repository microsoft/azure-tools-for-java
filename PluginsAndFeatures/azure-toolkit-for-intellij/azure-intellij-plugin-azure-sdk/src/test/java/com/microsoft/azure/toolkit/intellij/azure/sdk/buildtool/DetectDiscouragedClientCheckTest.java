package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

// Import necessary libraries

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiTypeElement;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiType;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.DetectDiscouragedClientCheck.DetectDiscouragedClientVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * This class tests the ServiceBusReceiverAsyncClientCheck class by mocking the ProblemsHolder and PsiElementVisitor
 * and verifying that a problem is registered when the ServiceBusReceiverAsyncClient is used.
 * The test also verifies that a problem is not registered when the PsiElement is null.
 * <p>
 * Here are some examples of test data where registerProblem should be called:
 * 1. ServiceBusReceiverAsyncClient client = new ServiceBusReceiverAsyncClient();
 * 2.private ServiceBusReceiverAsyncClient receiver;
 * 3. final ServiceBusReceiverAsyncClient autoCompleteReceiver =
 * toClose(getReceiverBuilder(false, entityType, index, false)
 * .buildAsyncClient());
 * <p>
 * 4. final EventHubConsumerAsyncClient consumerClient = partitionPump.getClient();
 * 5. EventHubConsumerAsyncClient eventHubConsumer = eventHubClientBuilder.buildAsyncClient()
 * .createConsumer(claimedOwnership.getConsumerGroup(), prefetch, true);
 */

public class DetectDiscouragedClientCheckTest {

    // Create a mock ProblemsHolder to be used in the test
    @Mock
    private ProblemsHolder mockHolder;

    // Create a mock PsiElementVisitor for visiting the PsiTypeElement
    @Mock
    private PsiElementVisitor mockVisitor;

    // Create a mock PsiTypeElement.
    @Mock
    private PsiTypeElement mockTypeElement;


    @BeforeEach
    public void setUp() {
        // Set up the test
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockTypeElement = mock(PsiTypeElement.class);
    }

    /**
     * Test that a problem is registered when the client name is ServiceBusReceiverAsyncClient.
     * <p>
     * This test is important because it verifies that the code registers a problem
     * when the client name is ServiceBusReceiverAsyncClient.
     */
    @Test
    public void testProblemRegisteredWhenUsingServiceBusReceiverAsyncClient() {

        String clientToCheck = "ServiceBusReceiverAsyncClient";
        String suggestionMessage = "Use of ServiceBusReceiverAsyncClient detected. Use ServiceBusProcessorClient instead.";

        // Visit Type Element
        int numberOfInvocations = 1;  // Number of times registerProblem should be called
        visitTypeElement(mockTypeElement, numberOfInvocations, clientToCheck, suggestionMessage);
    }

    /**
     * Test that a problem is registered when the client name is EventHubConsumerAsyncClient.
     * <p>
     * This test is important because it verifies that the code registers a problem
     * when the client name is EventHubConsumerAsyncClient.
     */
    @Test
    public void testProblemRegisteredWhenUsingEventHubConsumerAsyncClientCheck() {

        String clientToCheck = "EventHubConsumerAsyncClient";
        String suggestionMessage = "Use of EventHubConsumerAsyncClient detected. Use EventProcessorClient instead which provides a higher-level abstraction that simplifies event processing, making it the preferred choice for most developers.";

        // Visit Type Element
        int numberOfInvocations = 1;  // Number of times registerProblem should be called
        visitTypeElement(mockTypeElement, numberOfInvocations, clientToCheck, suggestionMessage);
    }

    /**
     * Test that a problem is not registered when the client name is not ServiceBusReceiverAsyncClient.
     * <p>
     * This test is important because it verifies that the code does not
     * register a problem when the client name is not ServiceBusReceiverAsyncClient.
     */

    @Test
    public void testProblemNotRegisteredWhenCheckingForDiifferentClient() {

        String clientToCheck = "ServiceBusProcessorClient";

        // Visit Type Element
        int numberOfInvocations = 0;  // Number of times registerProblem should be called

        String suggestionMessage = "";
        visitTypeElement(mockTypeElement, numberOfInvocations, clientToCheck, suggestionMessage);

    }

    /**
     * Test that a problem is not registered when the PsiTypeElement is null and the client name is empty.
     */
    @Test
    public void testProblemNotRegisteredWhenClientIsEmpty() {
        // Visit Type Element
        int numberOfInvocations = 0;  // Number of times registerProblem should be called
        String clientToCheck = "";
        String suggestionMessage = "";
        visitTypeElement(null, numberOfInvocations, clientToCheck, suggestionMessage);
    }

    /**
     * Create a visitor by calling the buildVisitor method of ServiceBusReceiverAsyncClientCheck
     * and return the visitor.
     *
     * @return PsiElementVisitor
     */
    private PsiElementVisitor createVisitor() {

        DetectDiscouragedClientVisitor mockVisitor = new DetectDiscouragedClientVisitor(mockHolder);
        return mockVisitor;
    }

    /**
     * Visit a Type Element and verify that a problem was registered
     * when the ServiceBusReceiverAsyncClient is used.
     *
     * @param typeElement         The PsiTypeElement to visit
     * @param numberOfInvocations The number of times registerProblem should be called
     */
    private void visitTypeElement(PsiTypeElement typeElement, int numberOfInvocations, String clientToCheck, String suggestionMessage) {

        PsiType mockType = mock(PsiType.class);

        when(mockTypeElement.getType()).thenReturn(mockType);
        when(mockType.getPresentableText()).thenReturn(clientToCheck);

        ((JavaElementVisitor) mockVisitor).visitTypeElement(mockTypeElement);
        verify(mockHolder, times(numberOfInvocations)).registerProblem((Mockito.eq(mockTypeElement)), Mockito.contains(suggestionMessage));
    }
}
