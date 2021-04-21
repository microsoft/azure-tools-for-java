---
guid: 6dd4e233-efe8-4faf-a3ee-2619362355fe
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, TOPICNAME, SUBSCRIPTIONNAME, (CONNECTIONVALUE)
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
TOPICNAME-expression: constant("mytopic")
SUBSCRIPTIONNAME-expression: constant("mysubscription")
---

# funcsbtopic

Creates an Azure Function method with a Service Bus topic trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([ServiceBusTrigger("$TOPICNAME$", "$SUBSCRIPTIONNAME$", Connection = "$CONNECTIONVALUE$")]string mySbMsg, ILogger log)
{
    log.LogInformation($"C# ServiceBus topic trigger function processed message: {mySbMsg}");$END$
}
```
