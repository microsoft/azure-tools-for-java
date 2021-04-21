---
guid: 05e6f400-869c-4d10-b9e5-1bec3a50dd75
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=QueueTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("myqueue")
CONNECTIONVALUE-expression: constant("")
---

# Queue Trigger

```
$HEADER$using System;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [Function("$CLASS$")]
        public static void Run([QueueTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")] string myQueueItem,
            FunctionContext context)
        {
            var logger = context.GetLogger("$CLASS$");
            logger.LogInformation($"C# Queue trigger function processed: {myQueueItem}");$END$
        }
    }
}
```
