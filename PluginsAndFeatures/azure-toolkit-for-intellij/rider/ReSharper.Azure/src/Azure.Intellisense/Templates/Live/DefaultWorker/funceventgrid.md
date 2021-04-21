---
guid: c8fb7e0d-9157-4647-9fb5-63d1023c20c6
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
---

# funceventgrid

Creates an Azure Function method with an Event Grid trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([EventGridTrigger]EventGridEvent eventGridEvent, ILogger log)
{
    log.LogInformation(eventGridEvent.Data.ToString());$END$
}
```
