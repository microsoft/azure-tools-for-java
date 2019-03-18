package com.microsoft.azuretools.telemetrywrapper;

import java.util.Map;

/**
 * Each time when you start a transaction, we will generate a operationId(the operationId will store in thread local),
 * then you can send info,warn,error with this operationId, you need to end this transaction.
 * If you start a new transaction without close the previous one, we just generate a new operationId.
 * If you send info, error, warn or end transaction without start a transaction, we just give you a new operationId.
 * Take care, each time when you start a transaction you need to end it. Or you cannot correctly trace the operation.
 *
 * The snappy code just like this:
 *   try {
 *       startTransaction();
 *       doSomething();
 *       sendInfo();
 *   } catch (Exception e) {
 *       sendError();
 *   } finally {
 *       endTransaction();
 *   }
 *
 *
 *   Sequence Diagram:
 *
 *                         operationId
 *   start a transaction        |
 *   send info                  |
 *   send info                  |
 *   send error                 |
 *   end a transaction          v
 */
public interface Producer {

    void startTransaction(String eventName, String operName, Map<String, String> properties);

    void endTransaction(String eventName, String operName, Map<String, String> properties, long time);

    void sendError(String eventName, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties);

    void sendInfo(String eventName, String operName, Map<String, String> properties);

    void sendWarn(String eventName, String operName, Map<String, String> properties);

}
