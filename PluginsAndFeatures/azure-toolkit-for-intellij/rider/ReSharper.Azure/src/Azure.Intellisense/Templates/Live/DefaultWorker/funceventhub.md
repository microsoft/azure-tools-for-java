---
guid: 4f0b0b19-4c28-4301-aa99-d1368cbea42e
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, PATHVALUE, (CONNECTIONVALUE)
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("samples-workitems")
---

# funceventhub

Creates an Azure Function method with an Event Hub trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([EventHubTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")]string myEventHubMessage, ILogger log)
{
    log.LogInformation($"C# Event Hub trigger function processed a message: {myEventHubMessage}");$END$
}
```
