---
guid: b5eeee7f-1d84-4918-8b03-5d63b94e3dea
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, PATHVALUE, (CONNECTIONVALUE)
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("samples-workitems")
---

# funcblob

Creates an Azure Function method with a blob trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([BlobTrigger("$PATHVALUE$/{name}", Connection = "$CONNECTIONVALUE$")]Stream myBlob, string name, ILogger log)
{
    log.LogInformation($"C# Blob trigger function Processed blob\n Name:{name} \n Size: {myBlob.Length} Bytes");$END$
}
```
