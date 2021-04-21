---
guid: 3950e8cc-8dcd-4fee-ace4-aa88d290cfa9
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, PATHVALUE, (CONNECTIONVALUE)
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("myqueue")
---

# funcqueue

Creates an Azure Function method with a queue trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([QueueTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")]string myQueueItem, ILogger log)
{
    log.LogInformation($"C# Queue trigger function processed: {myQueueItem}");$END$
}
```
