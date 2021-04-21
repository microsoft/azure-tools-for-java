---
guid: cde7ab6b-9766-45ea-a6aa-09e325e85275
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, SCHEDULE
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
SCHEDULE-expression: constant("0 */5 * * * *")
---

# functimer

Creates an Azure Function method with a timer trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([TimerTrigger("$SCHEDULE$")]TimerInfo myTimer, ILogger log)
{
    log.LogInformation($"C# Timer trigger function executed at: {DateTime.UtcNow}");$END$
}
```
