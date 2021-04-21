---
guid: 3306036b-fc35-4560-97cb-e6584a5f5ce3
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, PATHVALUE, (CONNECTIONVALUE)
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("myqueue")
---

# funcsbqueue

Creates an Azure Function method with a Service Bus queue trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([ServiceBusTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")]string myQueueItem, ILogger log)
{
    log.LogInformation($"C# ServiceBus queue trigger function processed message: {myQueueItem}");$END$
}
```
