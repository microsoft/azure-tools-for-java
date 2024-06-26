package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

// Import necessary libraries
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiTypeElement;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


/**
 * This class tests the ServiceBusReceiverAsyncClientCheck class by mocking the ProblemsHolder and PsiElementVisitor
 * and verifying that a problem is registered when the ServiceBusReceiverAsyncClient is used.
 * The test also verifies that a problem is not registered when the PsiElement is null.
 *
 * Here are some examples of test data where registerProblem should be called:
 * 1. ServiceBusReceiverAsyncClient client = new ServiceBusReceiverAsyncClient();
 * 2.private ServiceBusReceiverAsyncClient receiver;
 * 3. final ServiceBusReceiverAsyncClient autoCompleteReceiver =
 *             toClose(getReceiverBuilder(false, entityType, index, false)
 *                 .buildAsyncClient());
 */

public class ServiceBusReceiverAsyncClientCheckTest {

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

    @Test
    public void testProblemRegisteredWhenUsingServiceBusReceiverAsyncClient() {
        // Assert
        assertVisitor();

        String CLIENT_TO_CHECK = "ServiceBusReceiverAsyncClient";

        // Visit Type Element
        int NUMBER_OF_INVOCATIONS = 1;  // Number of times registerProblem should be called
        visitTypeElement(mockTypeElement, NUMBER_OF_INVOCATIONS, CLIENT_TO_CHECK);
    }

    /**
     * Test that a problem is not registered when the client name is not ServiceBusReceiverAsyncClient.
     *
     * This test is important because it verifies that the code does not
     * register a problem when the client name is not ServiceBusReceiverAsyncClient.
     */

    @Test
    void testProblemNotRegisteredWhenCheckingForDiifferentClient() {

        String CLIENT_TO_CHECK = "ServiceBusProcessorClient";

        // Visit Type Element
        int NUMBER_OF_INVOCATIONS = 0;  // Number of times registerProblem should be called
        visitTypeElement(mockTypeElement, NUMBER_OF_INVOCATIONS, CLIENT_TO_CHECK);

    }

    /**
     * Test that a problem is not registered when the PsiTypeElement is null and the client name is empty.
     */
    @Test
    void testProblemNotRegisteredWhenClientIsEmpty() {
        // Visit Type Element
        int NUMBER_OF_INVOCATIONS = 0;  // Number of times registerProblem should be called
        String CLIENT_TO_CHECK = "";
        visitTypeElement(null, NUMBER_OF_INVOCATIONS, "");
    }

    // Create a visitor by calling the buildVisitor method of ServiceBusReceiverAsyncClientCheck
    private PsiElementVisitor createVisitor() {
        ServiceBusReceiverAsyncClientCheck check = new ServiceBusReceiverAsyncClientCheck();
        boolean isOnTheFly = true;
        return check.buildVisitor(mockHolder, isOnTheFly);
    }

    // Assert that the visitor is not null and is an instance of JavaElementVisitor
    private void assertVisitor() {
        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    /** Visit a Type Element and verify that a problem was registered
     * when the ServiceBusReceiverAsyncClient is used.
     * @param typeElement The PsiTypeElement to visit
     * @param NUMBER_OF_INVOCATIONS The number of times registerProblem should be called
     */
    private void visitTypeElement(PsiTypeElement typeElement, int NUMBER_OF_INVOCATIONS, String CLIENT_TO_CHECK) {

        PsiType mockType = mock(PsiType.class);

        when(mockTypeElement.getType()).thenReturn(mockType);
        when(mockType.getPresentableText()).thenReturn(CLIENT_TO_CHECK);

        ((JavaElementVisitor) mockVisitor).visitTypeElement(mockTypeElement);
        verify(mockHolder, times(NUMBER_OF_INVOCATIONS)).registerProblem((Mockito.eq(mockTypeElement)), Mockito.contains("Use of ServiceBusReceiverAsyncClient detected. Use ServiceBusProcessorClient instead."));
    }

}