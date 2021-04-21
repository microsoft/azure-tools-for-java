---
guid: bfccc7d5-0a43-4fc2-a4d4-580d1265b536
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=QueueTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsDefaultWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("myqueue")
CONNECTIONVALUE-expression: constant("")
---

# Queue Trigger

```
namespace $NAMESPACE$

open System
open Microsoft.Azure.WebJobs
open Microsoft.Azure.WebJobs.Host
open Microsoft.Extensions.Logging

module $CLASS$ =
    [<FunctionName("$CLASS$")>]
    let run([<QueueTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")>]myQueueItem: string, log: ILogger) =
        let msg = sprintf "F# Queue trigger function processed: %s" myQueueItem
        log.LogInformation msg$END$
```
