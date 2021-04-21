---
guid: 3c11cff7-99a9-47c5-90dd-eb39bf4adf27
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=ServiceBusQueueTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("myqueue")
CONNECTIONVALUE-expression: constant("")
---

# Service Bus Queue Trigger

```
$HEADER$using System;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [Function("$CLASS$")]
        public static void Run([ServiceBusTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")] string myQueueItem, FunctionContext context)
        {
            var logger = context.GetLogger("$CLASS$");
            logger.LogInformation($"C# ServiceBus queue trigger function processed message: {myQueueItem}");$END$
        }
    }
}
```
