---
guid: eb5c9aa3-d87b-44cc-8c42-7321fe699bbc
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, PATHVALUE, (CONNECTIONVALUE)
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("messages/events")
---

# funciothub

Creates an Azure Function method with an IoT Hub trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([IoTHubTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")]EventData message, ILogger log)
{
    log.LogInformation($"C# IoT Hub trigger function processed a message: {Encoding.UTF8.GetString(message.Body.Array)}");$END$
}
```
